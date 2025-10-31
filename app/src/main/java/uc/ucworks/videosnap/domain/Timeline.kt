package uc.ucworks.videosnap.domain

import java.util.UUID

data class TimelineTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Track",
    val type: TrackType,
    val clips: List<TimelineClip> = emptyList(),
)

enum class TrackType {
    VIDEO,
    AUDIO
}

data class TimelineClip(
    val id: String = UUID.randomUUID().toString(),
    val mediaPath: String,
    val startTime: Long, // in milliseconds
    val endTime: Long, // in milliseconds
    val mediaType: MediaType,
    val effects: List<Effect> = emptyList(),
    val transitions: List<TransitionEffect> = emptyList()
) {
    val duration: Long
        get() = endTime - startTime
}

enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE
}
