package com.nexuswavetech.nexusplus.platform

/** Returns the current platform name. */
expect fun getPlatformName(): String

/** Returns true if running on Android. */
fun isAndroid(): Boolean = getPlatformName() == "Android"

/** Returns true if running on iOS. */
fun isIos(): Boolean = getPlatformName() == "iOS"

/** Returns true if running on Desktop (JVM). */
fun isDesktop(): Boolean = getPlatformName() == "Desktop"

/** Returns the current hour (0-23) for time-aware greetings. */
expect fun getCurrentHour(): Int
