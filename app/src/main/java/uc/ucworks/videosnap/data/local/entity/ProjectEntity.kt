package uc.ucworks.videosnap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tracksJson: String, // JSON serialized tracks
    val lastModified: Long,
    val duration: Long,
    val thumbnailPath: String?,
    val resolution: String, // e.g., "1920x1080"
    val frameRate: Int,
    val isAutoSaveEnabled: Boolean = true,
    val autoSaveInterval: Long = 300000L // 5 minutes
)
