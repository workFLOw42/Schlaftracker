package de.schlafgut.app.ui.theme

import androidx.compose.ui.graphics.Color

// Night palette (backgrounds)
val Night900 = Color(0xFF0F172A)
val Night800 = Color(0xFF1E293B)
val Night700 = Color(0xFF334155)
val Night600 = Color(0xFF475569)
val Night500 = Color(0xFF64748B)

// Dream palette (primary accent)
val Dream600 = Color(0xFF4F46E5)
val Dream500 = Color(0xFF6366F1)
val Dream400 = Color(0xFF818CF8)
val Dream300 = Color(0xFFA5B4FC)

// Quality colors
val QualityGood = Color(0xFF10B981)      // Emerald >= 8
val QualityMedium = Color(0xFF6366F1)    // Indigo 5-7
val QualityPoor = Color(0xFFF43F5E)      // Rose < 5

// Functional colors
val WakeWindowColor = Color(0xFFFB923C)  // Orange
val LatencyColor = Color(0xFF4B5563)     // Gray
val NapColor = Color(0xFFFB923C)         // Orange
val MedianBedColor = Color(0xFF6366F1)   // Dream/Purple
val MedianWakeColor = Color(0xFF14B8A6)  // Teal

// Surface / text
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val SurfaceCard = Color(0xFF1E293B)
val SurfaceBorder = Color(0xFF334155)

// Danger
val DangerRed = Color(0xFFEF4444)
val DangerRedBg = Color(0x33EF4444)

fun qualityColor(quality: Int): Color = when {
    quality >= 8 -> QualityGood
    quality >= 5 -> QualityMedium
    else -> QualityPoor
}
