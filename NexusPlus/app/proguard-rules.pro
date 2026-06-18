# Add project specific ProGuard rules here.

# ── Kotlin & Coroutines ──────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# ── Koin ────────────────────────────────────────────────────────────────────
-keepnames class org.koin.** { *; }
-keep class org.koin.** { *; }

# ── Retrofit + Gson ──────────────────────────────────────────────────────────
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Media3 / ExoPlayer ───────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coil ─────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Kotlinx Serialization ────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ── ML Kit ───────────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── CameraX ──────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── ZXing ────────────────────────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }

# ── App data classes used in JSON parsing ────────────────────────────────────
-keep class com.nexuswavetech.nexusplus.features.radio.RadioStation { *; }
-keep class com.nexuswavetech.nexusplus.features.music.MusicTrack { *; }
-keep class com.nexuswavetech.nexusplus.features.iptv.IptvChannel { *; }
-keep class com.nexuswavetech.nexusplus.science.** { *; }
-keep class com.nexuswavetech.nexusplus.news.** { *; }
