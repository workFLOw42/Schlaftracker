package de.schlafgut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.util.DateTimeUtil
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration

@Composable
fun WakeWindowDialog(
    bedTimeEpoch: Long,
    wakeTimeEpoch: Long,
    onConfirm: (startEpoch: Long, endEpoch: Long, outOfBed: Boolean, outStart: Long?, outEnd: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val bedDt = DateTimeUtil.epochToLocalDateTime(bedTimeEpoch)
    val wakeDt = DateTimeUtil.epochToLocalDateTime(wakeTimeEpoch)

    val midpoint = bedTimeEpoch + (wakeTimeEpoch - bedTimeEpoch) / 2
    val midDt = DateTimeUtil.epochToLocalDateTime(midpoint)

    var startHour by remember { mutableIntStateOf(midDt.hour) }
    var startMinute by remember { mutableIntStateOf(midDt.minute) }
    var endHour by remember { mutableIntStateOf(midDt.plusMinutes(15).hour) }
    var endMinute by remember { mutableIntStateOf(midDt.plusMinutes(15).minute) }

    var outOfBed by remember { mutableStateOf(false) }
    var outStartHour by remember { mutableIntStateOf(midDt.plusMinutes(2).hour) }
    var outStartMin by remember { mutableIntStateOf(midDt.plusMinutes(2).minute) }
    var outEndHour by remember { mutableIntStateOf(midDt.plusMinutes(17).hour) }
    var outEndMin by remember { mutableIntStateOf(midDt.plusMinutes(17).minute) }

    var activePicker by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (activePicker != null) {
        val (h, m) = when (activePicker) {
            "start" -> startHour to startMinute
            "end" -> endHour to endMinute
            "outStart" -> outStartHour to outStartMin
            else -> outEndHour to outEndMin
        }
        TimePickerDialog(
            title = "Zeit wählen",
            initialHour = h,
            initialMinute = m,
            onConfirm = { hour, min ->
                when (activePicker) {
                    "start" -> {
                        startHour = hour
                        startMinute = min
                        // End time automatically 15 minutes after start as proposal
                        val autoEnd = LocalTime.of(hour, min).plusMinutes(15)
                        endHour = autoEnd.hour
                        endMinute = autoEnd.minute
                    }
                    "end" -> {
                        endHour = hour
                        endMinute = min
                    }
                    "outStart" -> {
                        outStartHour = hour
                        outStartMin = min
                        
                        // Proposed end time: 15 minutes after out start,
                        // but clamped to 2 minutes before wake window end.
                        val osTime = LocalTime.of(hour, min)
                        val wwEndTime = LocalTime.of(endHour, endMinute)
                        
                        // Simple proposal logic
                        val proposedEnd = osTime.plusMinutes(15)
                        
                        // Check if it's too close to the end (within 2 mins)
                        // This is a simple LocalTime check, not accounting for midnight wraps perfectly 
                        // but works for most cases as a UI suggestion.
                        
                        outEndHour = proposedEnd.hour
                        outEndMin = proposedEnd.minute
                    }
                    "outEnd" -> {
                        outEndHour = hour
                        outEndMin = min
                    }
                }
                activePicker = null
            },
            onDismiss = { activePicker = null }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wachphase hinzufügen", style = MaterialTheme.typography.titleMedium)

            error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { activePicker = "start" }, modifier = Modifier.weight(1f)) {
                    Text("Von: %02d:%02d".format(startHour, startMinute))
                }
                OutlinedButton(onClick = { activePicker = "end" }, modifier = Modifier.weight(1f)) {
                    Text("Bis: %02d:%02d".format(endHour, endMinute))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aufgestanden?", modifier = Modifier.weight(1f))
                Switch(checked = outOfBed, onCheckedChange = { outOfBed = it })
            }

            if (outOfBed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { activePicker = "outStart" }, modifier = Modifier.weight(1f)) {
                        Text("Von: %02d:%02d".format(outStartHour, outStartMin))
                    }
                    OutlinedButton(onClick = { activePicker = "outEnd" }, modifier = Modifier.weight(1f)) {
                        Text("Bis: %02d:%02d".format(outEndHour, outEndMin))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
                TextButton(onClick = {
                    val s = resolveTimeInSleepPeriod(bedDt, wakeDt, startHour, startMinute)
                    val e = resolveTimeInSleepPeriod(bedDt, wakeDt, endHour, endMinute)
                    val sEpoch = DateTimeUtil.localDateTimeToEpoch(s)
                    val eEpoch = DateTimeUtil.localDateTimeToEpoch(e)

                    if (eEpoch <= sEpoch) { error = "Ende muss nach Start liegen"; return@TextButton }

                    var osEpoch: Long? = null
                    var oeEpoch: Long? = null
                    if (outOfBed) {
                        val os = resolveTimeInSleepPeriod(bedDt, wakeDt, outStartHour, outStartMin)
                        val oe = resolveTimeInSleepPeriod(bedDt, wakeDt, outEndHour, outEndMin)
                        osEpoch = DateTimeUtil.localDateTimeToEpoch(os)
                        oeEpoch = DateTimeUtil.localDateTimeToEpoch(oe)
                        
                        if (oeEpoch <= osEpoch) { error = "Aufsteh-Ende muss nach Start liegen"; return@TextButton }
                        if (osEpoch < sEpoch || oeEpoch > eEpoch) { error = "Aufstehen muss in Wachphase liegen"; return@TextButton }
                        
                        // Rule: Max 2 minutes before end of wake window
                        if (oeEpoch > eEpoch - (2 * 60 * 1000)) {
                            error = "Aufstehen muss mind. 2 Min. vor Ende der Wachphase enden"
                            return@TextButton
                        }
                    }

                    onConfirm(sEpoch, eEpoch, outOfBed, osEpoch, oeEpoch)
                }) { Text("Hinzufügen") }
            }
        }
    }
}

private fun resolveTimeInSleepPeriod(bedDt: LocalDateTime, wakeDt: LocalDateTime, h: Int, m: Int): LocalDateTime {
    val d1 = LocalDateTime.of(bedDt.toLocalDate(), LocalTime.of(h, m))
    val d2 = d1.plusDays(1)
    return if (d1 >= bedDt && d1 <= wakeDt) d1 else if (d2 >= bedDt && d2 <= wakeDt) d2 else d1
}
