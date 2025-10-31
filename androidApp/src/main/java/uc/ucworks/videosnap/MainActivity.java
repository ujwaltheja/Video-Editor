package uc.ucworks.videosnap;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.compose.setContent;
import uc.ucworks.videosnap.ui.theme.VideoSnapTheme;

/**
 * The main activity of the application.
 */
public class MainActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContent {
            VideoSnapTheme {
                VideoEditorScreen();
            }
        }
    }
}
