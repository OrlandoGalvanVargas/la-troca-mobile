# ================================================
# 🚀 CONFIGURACIÓN GENERAL
# ================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Mantener constructores públicos y data classes
-keep class * {
    public <init>(...);
}
-keepclassmembers class * {
    public <init>();
}

# Mantener enums
-keepclassmembers enum * { *; }

# ================================================
# 🌐 RETROFIT + OKHTTP + GSON
# ================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Mantener modelos (tu dominio y data)
-keep class com.troca.latroca.data.models.** { *; }
-keep class com.troca.latroca.domain.models.** { *; }

# ================================================
# ☕️ KOTLIN Y COROUTINES
# ================================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ================================================
# 🔥 FIREBASE Y GOOGLE PLAY SERVICES
# ================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Evitar eliminar clases de Google Tink (usado por Firebase para cifrado)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ================================================
# 🕰️ JODA-TIME (referenciado por Firebase/Tink)
# ================================================
-keep class org.joda.time.** { *; }
-dontwarn org.joda.time.**

# ================================================
# 🧩 COIL (CARGA DE IMÁGENES)
# ================================================
-keep class coil.** { *; }
-keep interface coil.** { *; }

# ================================================
# 🎨 JETPACK COMPOSE
# ================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ================================================
# 🧭 NAVIGATION
# ================================================
-keep class androidx.navigation.** { *; }

# ================================================
# 🔐 CREDENTIALS MANAGER
# ================================================
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# ================================================
# 🧩 SERIALIZACIÓN (KOTLINX & DATETIME)
# ================================================
-keep class kotlinx.serialization.** { *; }
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.datetime.**

# ================================================
# ⚙️ GOOGLE API CLIENT (usado por Tink internamente)
# ================================================
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
# ================================================
# 🔥 GOOGLE LOGIN REQUEST
# ================================================
-keep class com.troca.latroca.data.models.GoogleLoginRequest { *; }
-keepclassmembers class com.troca.latroca.data.models.GoogleLoginRequest { *; }
# ================================================
# 📅 FECHAS Y LOCALES
# ================================================
-dontwarn java.util.Locale
