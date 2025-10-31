package uc.ucworks.videosnap

/**
 * A data class that represents a video project.
 *
 * @property id Unique identifier for the project.
 * @property tracks A list of timeline tracks.
 * @property name The name of the project.
 * @property lastModified The last modified time of the project.
 */
data class VideoProject(
    val id: String,
    val tracks: List<TimelineTrack> = emptyList(),
    val name: String,
    val lastModified: Long
) {
    companion object {
        fun createNew(name: String, tracks: List<TimelineTrack> = emptyList()): VideoProject {
            return VideoProject(
                id = System.currentTimeMillis().toString(),
                tracks = tracks,
                name = name,
                lastModified = System.currentTimeMillis()
            )
        }
    }
}
