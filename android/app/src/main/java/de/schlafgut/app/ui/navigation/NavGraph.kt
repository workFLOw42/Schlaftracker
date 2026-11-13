package de.schlafgut.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Typsichere Routen für Navigation 3.
 *
 * `@Serializable`, damit der Backstack Prozess-Tod und Konfigurationswechsel übersteht.
 */
sealed interface Screen : NavKey {
    @Serializable data object Dashboard : Screen
    @Serializable data object AllEntries : Screen
    @Serializable data object Statistics : Screen
    @Serializable data object Settings : Screen

    /** Neuer Eintrag bei `entryId == null`, sonst Bearbeiten. */
    @Serializable data class SleepLogger(val entryId: String? = null) : Screen
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

/** Ziele, die über den Drawer erreichbar sind und FAB/Drawer-Geste zeigen. */
val topLevelScreens: Set<Screen> = navigationItems.map { it.screen }.toSet()
