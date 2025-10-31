package uc.ucworks.videosnap.domain

import java.util.UUID

data class VideoProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tracks: List<TimelineTrack> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
)
