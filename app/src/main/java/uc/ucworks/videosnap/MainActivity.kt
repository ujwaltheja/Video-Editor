package uc.ucworks.videosnap

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import uc.ucworks.videosnap.ui.onboarding.OnboardingScreen
import uc.ucworks.videosnap.ui.theme.VideoSnapTheme

/**
 * Main activity for the VideoSnap video editor application.
 * Handles onboarding flow and launches the main editor screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Setup lifecycle observer for autosave
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_PAUSE) {
                    // Autosave current project
                    // Handled by ViewModel
                }
            }
        })

        setContent {
            VideoSnapTheme(darkTheme = true) { // Default to dark theme for video editing
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showOnboarding by remember {
                        mutableStateOf(!sharedPreferences.getBoolean("onboarding_shown", false))
                    }

                    if (showOnboarding) {
                        OnboardingScreen(
                            onComplete = {
                                sharedPreferences.edit()
                                    .putBoolean("onboarding_shown", true)
                                    .apply()
                                showOnboarding = false
                            }
                        )
                    } else {
                        VideoEditorScreen()
                    }
                }
            }
        }
    }
}
