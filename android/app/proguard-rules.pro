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
# noinspection ShrinkerUnresolvedReference
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential { *; }
-keep class com.google.api.client.http.javanet.NetHttpTransport { *; }
-keep class com.google.api.client.json.gson.GsonFactory { *; }
-keep class com.google.api.services.drive.Drive { *; }
-keep class com.google.api.services.drive.Drive$* { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keep class com.google.http.client.json.JsonFactory { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**
-dontwarn javax.naming.**

# ===== Gson (used by Google HTTP Client) =====
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class com.google.gson.Gson { *; }
-keep class com.google.gson.GsonBuilder { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== Tink =====
-keep class com.google.crypto.tink.aead.AeadConfig { *; }
-keep class com.google.crypto.tink.config.TinkConfig { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.**

# ===== Hilt / Dagger =====
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ===== Health Connect =====
-keep class androidx.health.connect.client.records.** { *; }

# ===== Vico Charts =====
-keep class com.patrykandpatrick.vico.core.model.** { *; }
-keep class com.patrykandpatrick.vico.core.cartesian.** { *; }
