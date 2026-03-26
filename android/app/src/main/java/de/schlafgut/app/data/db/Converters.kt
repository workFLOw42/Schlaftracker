package de.schlafgut.app.data.db

import androidx.room.TypeConverter
import de.schlafgut.app.data.entity.WakeWindow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromWakeWindowList(value: List<WakeWindow>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toWakeWindowList(value: String): List<WakeWindow> =
        json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        json.decodeFromString(value)
}
