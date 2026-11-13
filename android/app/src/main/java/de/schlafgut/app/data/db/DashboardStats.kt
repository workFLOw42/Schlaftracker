package de.schlafgut.app.data.db

/** Ergebnis von [SleepEntryDao.getDashboardStats]. Durchschnittswerte sind null, wenn es keine Einträge gibt. */
data class DashboardStats(
    val nightCount: Int = 0,
    val avgNightQuality: Double? = null,
    val avgNightDurationMinutes: Double? = null,
    val napCount: Int = 0,
    val avgNapDurationMinutes: Double? = null
)
