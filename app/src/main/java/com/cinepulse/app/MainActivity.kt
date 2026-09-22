// MainActivity.kt
package com.cinepulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.cinepulse.app.ui.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CinePulseApp
        setContent {
            MaterialTheme {
                Surface {
                    AppNavHost(app)
                }
            }
        }
    }
}