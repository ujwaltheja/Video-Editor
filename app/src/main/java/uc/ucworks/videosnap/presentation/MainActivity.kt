package uc.ucworks.videosnap.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import uc.ucworks.videosnap.ui.theme.VideoSnapTheme
import uc.ucworks.videosnap.presentation.home.HomeScreen
import uc.ucworks.videosnap.presentation.editor.VideoEditorScreen
import uc.ucworks.videosnap.ui.onboarding.OnboardingScreen

/**
 * Main activity for the Video Snap video editor application.
 * Handles navigation between home, onboarding, and editor screens.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VideoSnapTheme(darkTheme = true) {
                val systemUiController = rememberSystemUiController()
                val backgroundColor = MaterialTheme.colorScheme.background

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = backgroundColor,
                        darkIcons = false
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onComplete = {
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
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
