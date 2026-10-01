package com.elxvro.randevu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elxvro.randevu.ui.RandevuTheme
import com.elxvro.randevu.ui.RandevuV3App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RandevuTheme {
                RandevuV3App()
            }
        }
    }
}
