package uc.ucworks.videosnap.domain

object DefaultExportPresets {
    val presets = listOf(
        ExportPreset("720p", "mp4", "high"),
        ExportPreset("1080p", "mp4", "high"),
        ExportPreset("4K", "mp4", "high"),
        ExportPreset("720p WebM", "webm", "high"),
        ExportPreset("1080p WebM", "webm", "high"),
    )
}
