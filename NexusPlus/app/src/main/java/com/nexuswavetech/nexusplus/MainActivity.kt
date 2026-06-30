package com.nexuswavetech.nexusplus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.nexuswavetech.nexusplus.navigation.NexusNavHost
import com.nexuswavetech.nexusplus.ui.theme.NexusPlusTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionCode = packageManager
            .getPackageInfo(packageName, 0)
            .longVersionCode.toInt()
        setContent {
            NexusPlusTheme {
                NexusNavHost(currentVersionCode = versionCode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Prevent automatic theme flickering on resume
        window.decorView.postInvalidate()
    }
}
