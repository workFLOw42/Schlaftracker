package de.schlafgut.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val route: String) {
    Dashboard("dashboard"),
    SleepLogger("sleep_logger?entryId={entryId}"),
    Statistics("statistics"),
    Settings("settings"),
    AllEntries("all_entries");

    companion object {
        fun sleepLoggerRoute(entryId: String? = null): String =
            if (entryId != null) "sleep_logger?entryId=$entryId"
            else "sleep_logger"
    }
}

data class NavigationItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val navigationItems = listOf(
    NavigationItem(Screen.Dashboard, "Startseite", Icons.Default.Home),
    NavigationItem(Screen.AllEntries, "Alle Einträge", Icons.Default.History),
    NavigationItem(Screen.Statistics, "Statistik", Icons.Default.BarChart),
    NavigationItem(Screen.Settings, "Einstellungen", Icons.Default.Settings)
)
