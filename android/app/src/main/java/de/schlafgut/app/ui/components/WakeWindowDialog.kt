package de.schlafgut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.schlafgut.app.data.entity.OutOfBedPeriod
import de.schlafgut.app.data.entity.WakeWindow
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.util.DateTimeUtil
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun WakeWindowDialog(
    bedTimeEpoch: Long,
    wakeTimeEpoch: Long,
    onConfirm: (WakeWindow) -> Unit,
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

    // Mehrere Aufsteh-Phasen
    val outOfBedPeriods = remember { mutableStateListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>() }

    var activePicker by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (activePicker != null) {
        val (h, m) = when {
            activePicker == "start" -> startHour to startMinute
            activePicker == "end" -> endHour to endMinute
            activePicker!!.startsWith("oobStart_") -> {
                val idx = activePicker!!.substringAfter("oobStart_").toInt()
                outOfBedPeriods[idx].first
            }
            activePicker!!.startsWith("oobEnd_") -> {
                val idx = activePicker!!.substringAfter("oobEnd_").toInt()
                outOfBedPeriods[idx].second
            }
            else -> 0 to 0
        }
        TimePickerDialog(
            title = "Zeit wählen",
            initialHour = h,
            initialMinute = m,
            onConfirm = { hour, min ->
                when {
                    activePicker == "start" -> {
                        startHour = hour
                        startMinute = min
                        val autoEnd = LocalTime.of(hour, min).plusMinutes(15)
                        endHour = autoEnd.hour
                        endMinute = autoEnd.minute
                    }
                    activePicker == "end" -> {
                        endHour = hour
                        endMinute = min
                    }
                    activePicker!!.startsWith("oobStart_") -> {
                        val idx = activePicker!!.substringAfter("oobStart_").toInt()
                        val proposedEnd = LocalTime.of(hour, min).plusMinutes(10)
                        outOfBedPeriods[idx] = (hour to min) to (proposedEnd.hour to proposedEnd.minute)
                    }
                    activePicker!!.startsWith("oobEnd_") -> {
                        val idx = activePicker!!.substringAfter("oobEnd_").toInt()
                        outOfBedPeriods[idx] = outOfBedPeriods[idx].first to (hour to min)
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wachphase hinzufügen", style = MaterialTheme.typography.titleMedium)

            error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall) }

            // Wachphase Start / Ende
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { activePicker = "start" }, modifier = Modifier.weight(1f)) {
                    Text("Von: %02d:%02d".format(startHour, startMinute))
                }
                OutlinedButton(onClick = { activePicker = "end" }, modifier = Modifier.weight(1f)) {
                    Text("Bis: %02d:%02d".format(endHour, endMinute))
                }
            }

            // Aufsteh-Phasen
            Text("Aufgestanden", style = MaterialTheme.typography.labelLarge)

            outOfBedPeriods.forEachIndexed { index, (start, end) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { activePicker = "oobStart_$index" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Von: %02d:%02d".format(start.first, start.second))
                    }
                    OutlinedButton(
                        onClick = { activePicker = "oobEnd_$index" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bis: %02d:%02d".format(end.first, end.second))
                    }
                    TextButton(onClick = { outOfBedPeriods.removeAt(index) }) {
                        Text("✕", color = DangerRed)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    // Standard: Mitte der Wachphase, 10 min
                    val oobStartTime = LocalTime.of(startHour, startMinute).plusMinutes(2)
                    val oobEndTime = oobStartTime.plusMinutes(10)
                    outOfBedPeriods.add(
                        (oobStartTime.hour to oobStartTime.minute) to
                                (oobEndTime.hour to oobEndTime.minute)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Aufsteh-Phase hinzufügen")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
                TextButton(onClick = {
                    val s = resolveTimeInSleepPeriod(bedDt, wakeDt, startHour, startMinute)
                    val e = resolveTimeInSleepPeriod(bedDt, wakeDt, endHour, endMinute)
                    val sEpoch = DateTimeUtil.localDateTimeToEpoch(s)
                    val eEpoch = DateTimeUtil.localDateTimeToEpoch(e)

                    if (eEpoch <= sEpoch) {
                        error = "Ende muss nach Start liegen"
                        return@TextButton
                    }

                    // Aufsteh-Phasen validieren
                    val oobPeriods = mutableListOf<OutOfBedPeriod>()
                    for ((idx, oob) in outOfBedPeriods.withIndex()) {
                        val (oobS, oobE) = oob
                        val osDt = resolveTimeInSleepPeriod(bedDt, wakeDt, oobS.first, oobS.second)
                        val oeDt = resolveTimeInSleepPeriod(bedDt, wakeDt, oobE.first, oobE.second)
                        val osEpoch = DateTimeUtil.localDateTimeToEpoch(osDt)
                        val oeEpoch = DateTimeUtil.localDateTimeToEpoch(oeDt)

                        if (oeEpoch <= osEpoch) {
                            error = "Aufsteh-Phase ${idx + 1}: Ende muss nach Start liegen"
                            return@TextButton
                        }
                        if (osEpoch < sEpoch || oeEpoch > eEpoch) {
                            error = "Aufsteh-Phase ${idx + 1}: Muss in Wachphase liegen"
                            return@TextButton
                        }
                        oobPeriods.add(OutOfBedPeriod(start = osEpoch, end = oeEpoch))
                    }

                    // Überlappungs-Check der Aufsteh-Phasen
                    for (i in oobPeriods.indices) {
                        for (j in i + 1 until oobPeriods.size) {
                            if (oobPeriods[i].end > oobPeriods[j].start &&
                                oobPeriods[i].start < oobPeriods[j].end
                            ) {
                                error = "Aufsteh-Phasen ${i + 1} und ${j + 1} überlappen sich"
                                return@TextButton
                            }
                        }
                    }

                    @Suppress("DEPRECATION")
                    val wakeWindow = WakeWindow(
                        start = sEpoch,
                        end = eEpoch,
                        outOfBedPeriods = oobPeriods.sortedBy { it.start }
                    )
                    onConfirm(wakeWindow)
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
