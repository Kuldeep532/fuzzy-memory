# Add project specific ProGuard rules here.

# Koin
-keepnames class org.koin.** { *; }

# Retrofit + Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-dontwarn coil.**

# Data classes (prevent R8 from stripping fields used in JSON parsing)
-keep class com.nexuswavetech.nexusplus.features.radio.RadioStation { *; }
-keep class com.nexuswavetech.nexusplus.features.music.MusicTrack { *; }
-keep class com.nexuswavetech.nexusplus.features.iptv.IptvChannel { *; }
