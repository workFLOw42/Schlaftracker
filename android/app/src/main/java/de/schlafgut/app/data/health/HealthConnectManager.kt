package de.schlafgut.app.data.health

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val permissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    val requestPermissionContract by lazy {
        PermissionController.createRequestPermissionResultContract()
    }

    enum class Availability {
        AVAILABLE,
        NOT_INSTALLED,
        NOT_SUPPORTED
    }

    fun checkAvailability(): Availability {
        val status = HealthConnectClient.getSdkStatus(context)
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NOT_INSTALLED
            else -> Availability.NOT_SUPPORTED
        }
    }

    fun getClient(): HealthConnectClient? {
        return if (checkAvailability() == Availability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    fun getInstallIntent(): Intent {
        val uri = "market://details".toUri()
            .buildUpon()
            .appendQueryParameter("id", "com.google.android.apps.healthdata")
            .appendQueryParameter("url", "healthconnect://onboarding")
            .build()
        return Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = uri
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
        }
    }
}
