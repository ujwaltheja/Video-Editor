package uc.ucworks.videosnap

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Video Editor.
 * Enables Hilt dependency injection.
 */
@HiltAndroidApp
class VideoEditorApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
