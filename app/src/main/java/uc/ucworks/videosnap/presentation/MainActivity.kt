package uc.ucworks.videosnap.presentation

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uc.ucworks.videosnap.ui.theme.VideoSnapTheme
import uc.ucworks.videosnap.presentation.home.HomeScreen
import uc.ucworks.videosnap.presentation.editor.VideoEditorScreen

/**
 * Main activity for the Video Snap video editor application.
 * Handles navigation between home and editor screens.
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
                }
            }
        })

        setContent {
            VideoSnapTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onProjectSelected = { projectId ->
                                    navController.navigate("editor/$projectId")
                                },
                                onNewProject = { projectId ->
                                    navController.navigate("editor/$projectId")
                                }
                            )
                        }
                        composable(
                            "editor/{projectId}",
                            arguments = listOf(
                                navArgument("projectId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                            VideoEditorScreen(
                                projectId = projectId,
                                onBackPress = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
