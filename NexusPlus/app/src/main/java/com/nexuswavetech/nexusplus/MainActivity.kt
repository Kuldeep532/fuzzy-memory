package com.nexuswavetech.nexusplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nexuswavetech.nexusplus.navigation.NexusNavHost
import com.nexuswavetech.nexusplus.ui.theme.NexusPlusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexusPlusTheme {
                NexusNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Prevent automatic theme flickering on resume
        window.decorView.postInvalidate()
    }
}
