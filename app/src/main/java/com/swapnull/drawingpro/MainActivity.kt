package com.swapnull.drawingpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.swapnull.drawingpro.theme.StudioDrawingTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as DrawingProApplication
        
        enableEdgeToEdge()
        setContent {
            StudioDrawingTheme {
                val viewModel: DrawingViewModel = viewModel(
                    factory = DrawingViewModelFactory(app.repository)
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
