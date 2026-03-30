# ===== Room =====
-keep class de.schlafgut.app.data.entity.** { *; }

# ===== Kotlinx Serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class de.schlafgut.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.schlafgut.app.**$$serializer { *; }

# ===== Google API Client / Drive =====
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.http.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**
-dontwarn javax.naming.**

# Keep Google Auth
-keep class com.google.auth.** { *; }

# ===== Gson (used by Google HTTP Client) =====
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== Tink =====
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.**

# ===== Hilt / Dagger =====
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ===== Health Connect =====
-keep class androidx.health.connect.client.** { *; }
-keep class androidx.health.connect.client.records.** { *; }

# ===== Vico Charts =====
-keep class com.patrykandpatrick.vico.core.** { *; }
-keep class com.patrykandpatrick.vico.compose.** { *; }
