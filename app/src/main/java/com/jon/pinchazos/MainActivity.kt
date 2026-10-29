package com.jon.pinchazos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jon.pinchazos.ui.PinchazosRaiz
import com.jon.pinchazos.ui.theme.PinchazosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PinchazosTheme {
                PinchazosRaiz()
            }
        }
    }
}
