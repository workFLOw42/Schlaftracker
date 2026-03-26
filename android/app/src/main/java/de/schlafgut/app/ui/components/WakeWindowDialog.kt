package de.schlafgut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.util.DateTimeUtil
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Dialog zum Hinzufügen einer Wachphase.
 * Der User wählt Start- und Endzeit (nur Uhrzeit, Datum wird aus bedTime/wakeTime abgeleitet).
 */
@Composable
fun WakeWindowDialog(
    bedTimeEpoch: Long,
    wakeTimeEpoch: Long,
    onConfirm: (startEpoch: Long, endEpoch: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val bedDt = DateTimeUtil.epochToLocalDateTime(bedTimeEpoch)
    val wakeDt = DateTimeUtil.epochToLocalDateTime(wakeTimeEpoch)

    // Default: middle of sleep period, 15 min window
    val midpoint = bedTimeEpoch + (wakeTimeEpoch - bedTimeEpoch) / 2
    val midDt = DateTimeUtil.epochToLocalDateTime(midpoint)

    var startHour by remember { mutableIntStateOf(midDt.hour) }
    var startMinute by remember { mutableIntStateOf(midDt.minute) }
    var endHour by remember { mutableIntStateOf(midDt.plusMinutes(15).hour) }
    var endMinute by remember { mutableIntStateOf(midDt.plusMinutes(15).minute) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showStartPicker) {
        TimePickerDialog(
            title = "Wachphase Start",
            initialHour = startHour,
            initialMinute = startMinute,
            onConfirm = { h, m ->
                startHour = h
                startMinute = m
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
        return
    }

    if (showEndPicker) {
        TimePickerDialog(
            title = "Wachphase Ende",
            initialHour = endHour,
            initialMinute = endMinute,
            onConfirm = { h, m ->
                endHour = h
                endMinute = m
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Wachphase hinzuf\u00fcgen",
                style = MaterialTheme.typography.titleMedium
            )

            error?.let {
                Text(text = it, color = DangerRed, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start: %02d:%02d".format(startHour, startMinute))
                }
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ende: %02d:%02d".format(endHour, endMinute))
                }
            }

            Text(
                text = "Die Zeiten m\u00fcssen innerhalb von ${DateTimeUtil.formatTime(bedTimeEpoch)} \u2013 ${DateTimeUtil.formatTime(wakeTimeEpoch)} liegen.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
                TextButton(onClick = {
                    // Build epoch from date of bedTime + chosen time
                    val startDt = resolveTimeInSleepPeriod(bedDt, wakeDt, startHour, startMinute)
                    val endDt = resolveTimeInSleepPeriod(bedDt, wakeDt, endHour, endMinute)

                    val startEpoch = DateTimeUtil.localDateTimeToEpoch(startDt)
                    val endEpoch = DateTimeUtil.localDateTimeToEpoch(endDt)

                    if (endEpoch <= startEpoch) {
                        error = "Das Ende muss nach dem Anfang liegen."
                        return@TextButton
                    }
                    if (startEpoch < bedTimeEpoch || endEpoch > wakeTimeEpoch) {
                        error = "Die Wachphase muss innerhalb der Schlafzeit liegen."
                        return@TextButton
                    }

                    onConfirm(startEpoch, endEpoch)
                }) {
                    Text("Hinzuf\u00fcgen")
                }
            }
        }
    }
}

/**
 * Löst eine Uhrzeit im Kontext einer Schlafperiode auf, die über Mitternacht gehen kann.
 * Wenn die Uhrzeit vor der Bettzeit liegt, nehmen wir an, dass sie am nächsten Tag ist.
 */
private fun resolveTimeInSleepPeriod(
    bedDt: LocalDateTime,
    wakeDt: LocalDateTime,
    hour: Int,
    minute: Int
): LocalDateTime {
    val onBedDate = LocalDateTime.of(bedDt.toLocalDate(), LocalTime.of(hour, minute))
    val onNextDate = onBedDate.plusDays(1)

    // If time on bed date is within sleep period, use it
    return if (onBedDate >= bedDt && onBedDate <= wakeDt) {
        onBedDate
    } else if (onNextDate >= bedDt && onNextDate <= wakeDt) {
        onNextDate
    } else {
        // Fallback: use bed date
        onBedDate
    }
}
