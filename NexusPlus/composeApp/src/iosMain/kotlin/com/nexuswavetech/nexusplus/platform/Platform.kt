package com.nexuswavetech.nexusplus.platform

import platform.Foundation.NSCalendar

actual fun getPlatformName(): String = "iOS"

actual fun getCurrentHour(): Int {
    val calendar = NSCalendar.currentCalendar()
    val components = calendar.components(platform.Foundation.NSCalendarUnitHour, fromDate = platform.Foundation.NSDate())
    return components.hour.toInt()
}
