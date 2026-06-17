package com.nexuswavetech.nexusplus.platform

import java.util.Calendar

actual fun getPlatformName(): String = "Desktop"

actual fun getCurrentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
