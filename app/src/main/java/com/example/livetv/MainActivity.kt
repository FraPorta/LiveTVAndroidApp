package com.example.livetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.livetv.ui.HomeScreen
import com.example.livetv.ui.theme.LiveTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveTVTheme {
                HomeScreen()
            }
        }
    }
}
