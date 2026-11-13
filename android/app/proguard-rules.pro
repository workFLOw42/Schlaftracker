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
-keep class com.google.api.client.http.javanet.NetHttpTransport { *; }
-keep class com.google.api.client.json.gson.GsonFactory { *; }
-keep class com.google.api.services.drive.Drive { *; }
-keep class com.google.api.services.drive.Drive$Files { *; }
-keep class com.google.api.services.drive.Drive$Files$* { *; }
-keep class com.google.api.services.drive.model.File { *; }
-keep class com.google.api.services.drive.model.FileList { *; }
-keep class com.google.http.client.json.JsonFactory { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**
-dontwarn javax.naming.**
-dontwarn com.google.errorprone.**

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

# ===== Hilt / Dagger =====
-dontwarn dagger.hilt.internal.**
