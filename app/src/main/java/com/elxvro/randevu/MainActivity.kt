package com.elxvro.randevu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elxvro.randevu.ui.RandevuApp
import com.elxvro.randevu.ui.RandevuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RandevuTheme {
                RandevuApp()
            }
        }
    }
}
