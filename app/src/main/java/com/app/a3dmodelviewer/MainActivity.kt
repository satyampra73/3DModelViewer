package com.app.a3dmodelviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.a3dmodelviewer.engine.FilamentManager
import com.app.a3dmodelviewer.ui.screen.ModelViewerScreen
import com.app.a3dmodelviewer.ui.theme.ModelViewerTheme

/**
 * Single Activity hosting the 100% Jetpack Compose multi-model canvas.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        enableEdgeToEdge()

        // Initialize single shared Filament Engine
        FilamentManager.init(applicationContext)

        setContent {
            ModelViewerTheme {
                ModelViewerScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown shared Filament Engine when activity finishes
        if (isFinishing) {
            FilamentManager.destroy()
        }
    }
}