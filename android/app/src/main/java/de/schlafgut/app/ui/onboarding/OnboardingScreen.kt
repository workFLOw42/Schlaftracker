package de.schlafgut.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.schlafgut.app.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onComplete: (name: String, defaultLatency: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var latency by remember { mutableIntStateOf(15) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\uD83C\uDF19",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Willkommen bei SchlafGut",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Tracke deinen Schlaf \u2014 lokal, privat, ohne Account",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Dein Name (optional)") },
            placeholder = { Text("z.B. Florian") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Standard-Einschlafzeit: $latency min",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = latency.toFloat(),
            onValueChange = { latency = it.toInt() },
            valueRange = 0f..60f,
            steps = 11, // 5-min steps
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Wird automatisch bei neuen Eintr\u00E4gen vorausgef\u00FCllt",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { onComplete(name, latency) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Los geht's!")
        }
    }
}
