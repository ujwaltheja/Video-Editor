package uc.ucworks.videosnap.domain

import uc.ucworks.videosnap.TimelineTrack
import java.util.UUID

data class VideoProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tracks: List<TimelineTrack> = emptyList(),
    val lastModified: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val thumbnailPath: String? = null,
    val resolution: String = "1920x1080",
    val frameRate: Int = 30
)
