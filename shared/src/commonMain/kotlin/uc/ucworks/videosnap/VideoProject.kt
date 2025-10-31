package uc.ucworks.videosnap

/**
 * A data class that represents a video project with comprehensive features.
 *
 * @property id Unique identifier for the project.
 * @property tracks A list of timeline tracks.
 * @property name The name of the project.
 * @property lastModified The last modified time of the project.
 * @property createdTime The creation time of the project.
 * @property duration Total duration of the project in milliseconds.
 * @property resolution Video resolution (width x height).
 * @property frameRate Frame rate (fps).
 * @property sampleRate Audio sample rate (Hz).
 * @property currentPosition Current playback position in milliseconds.
 * @property thumbnailPath Path to project thumbnail.
 * @property exportSettings Export settings for the project.
 * @property autosaveEnabled Whether autosave is enabled.
 * @property version Project version number.
 */
data class VideoProject(
    val id: String,
    val tracks: List<TimelineTrack> = emptyList(),
    val name: String,
    val lastModified: Long,
    val createdTime: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val resolution: VideoResolution = VideoResolution.HD_1080P,
    val frameRate: Int = 30,
    val sampleRate: Int = 48000,
    val currentPosition: Long = 0L,
    val thumbnailPath: String = "",
    val exportSettings: ExportPreset? = null,
    val autosaveEnabled: Boolean = true,
    val version: Int = 1
) {
    companion object {
        fun createNew(name: String, tracks: List<TimelineTrack> = emptyList()): VideoProject {
            return VideoProject(
                id = System.currentTimeMillis().toString(),
                tracks = tracks,
                name = name,
                lastModified = System.currentTimeMillis(),
                createdTime = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Video resolution presets.
 */
enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    SD_480P(854, 480, "480p SD"),
    HD_720P(1280, 720, "720p HD"),
    HD_1080P(1920, 1080, "1080p Full HD"),
    QHD_1440P(2560, 1440, "1440p QHD"),
    UHD_4K(3840, 2160, "4K UHD"),
    CUSTOM(0, 0, "Custom")
}
