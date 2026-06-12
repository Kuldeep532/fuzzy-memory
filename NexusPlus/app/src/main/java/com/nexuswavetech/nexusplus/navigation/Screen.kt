package com.nexuswavetech.nexusplus.navigation

sealed class Screen(val route: String) {
    object Welcome         : Screen("welcome")
    object Main            : Screen("main")

    // Feature screens
    object RadioPlayer     : Screen("feature/radio")
    object PdfReader       : Screen("feature/pdf")
    object AiImageGenerator: Screen("feature/ai_image")
    object NexusTts        : Screen("feature/tts")
    object IptvPlayer      : Screen("feature/iptv")
    object MusicStreaming   : Screen("feature/music")

    // Legal screens
    object AboutUs         : Screen("legal/about")
    object PrivacyPolicy   : Screen("legal/privacy")
    object TermsConditions : Screen("legal/terms")

    // Stub placeholder for extended features not yet fully implemented
    object Stub            : Screen("feature/stub")
}

// Bottom navigation tab identifiers
sealed class BottomTab(val route: String, val label: String) {
    object Favorites   : BottomTab("tab/favorites",    "Favorites")
    object AllFeatures : BottomTab("tab/all_features", "All Features")
    object More        : BottomTab("tab/more",         "More")
}
