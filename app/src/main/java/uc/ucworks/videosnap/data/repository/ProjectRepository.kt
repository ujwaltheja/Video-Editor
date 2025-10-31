package uc.ucworks.videosnap.data.repository

import kotlinx.coroutines.flow.Flow
import uc.ucworks.videosnap.domain.VideoProject

interface ProjectRepository {
    fun getAllProjects(): Flow<List<VideoProject>>
    fun getProjectById(projectId: String): Flow<VideoProject?>
    suspend fun getProjectByIdSuspend(projectId: String): VideoProject?
    suspend fun saveProject(project: VideoProject)
    suspend fun deleteProject(projectId: String)
    suspend fun deleteProject(project: VideoProject)
    fun getRecentProjects(limit: Int): Flow<List<VideoProject>>
    suspend fun createNewProject(name: String, resolution: String = "1920x1080", frameRate: Int = 30): VideoProject
    suspend fun autoSaveProject(project: VideoProject)
}
