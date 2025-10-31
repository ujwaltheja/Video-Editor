package uc.ucworks.videosnap

object DefaultExportPresets {
    val presets = listOf(
        ExportPreset("YouTube 1080p", "mp4", 1920, 1080, 8000000),
        ExportPreset("YouTube 720p", "mp4", 1280, 720, 5000000),
        ExportPreset("Instagram 1080p", "mp4", 1080, 1080, 5000000),
        ExportPreset("TikTok 1080p", "mp4", 1080, 1920, 8000000)
    )
}
