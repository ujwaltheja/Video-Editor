package uc.ucworks.videosnap

/**
 * A data class that represents an export preset.
 *
 * @property name The name of the preset.
 * @property format The format of the preset.
 * @property width The width of the video.
 * @property height The height of the video.
 * @property bitrate The bitrate of the video.
 */
data class ExportPreset(
    val name: String,
    val format: String,
    val width: Int,
    val height: Int,
    val bitrate: Int
)
