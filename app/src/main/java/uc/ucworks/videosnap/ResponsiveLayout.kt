package uc.ucworks.videosnap

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun ResponsiveLayout(
    portraitContent: @Composable () -> Unit,
    landscapeContent: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    if (configuration.screenWidthDp > configuration.screenHeightDp) {
        landscapeContent()
    } else {
        portraitContent()
    }
}
