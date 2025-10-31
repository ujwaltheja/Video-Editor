package uc.ucworks.videosnap.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uc.ucworks.videosnap.TimelineTrack
import uc.ucworks.videosnap.data.local.dao.ProjectDao
import uc.ucworks.videosnap.data.local.entity.ProjectEntity
import uc.ucworks.videosnap.domain.VideoProject
import java.util.UUID
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val context: Context
) : ProjectRepository {

    private val gson = Gson()

    override fun getAllProjects(): Flow<List<VideoProject>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProjectById(projectId: String): Flow<VideoProject?> {
        return projectDao.getProjectByIdFlow(projectId).map { it?.toDomain() }
    }

    override suspend fun getProjectByIdSuspend(projectId: String): VideoProject? {
        return projectDao.getProjectById(projectId)?.toDomain()
    }

    override suspend fun saveProject(project: VideoProject) {
        projectDao.insertProject(project.toEntity())
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    override suspend fun deleteProject(project: VideoProject) {
        projectDao.deleteProjectById(project.id)
    }

    override fun getRecentProjects(limit: Int): Flow<List<VideoProject>> {
        return projectDao.getRecentProjects(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createNewProject(
        name: String,
        resolution: String,
        frameRate: Int
    ): VideoProject {
        val project = VideoProject(
            id = UUID.randomUUID().toString(),
            name = name,
            tracks = emptyList(),
            lastModified = System.currentTimeMillis(),
            duration = 0L,
            thumbnailPath = null,
            resolution = resolution,
            frameRate = frameRate
        )
        saveProject(project)
        return project
    }

    override suspend fun autoSaveProject(project: VideoProject) {
        val entity = project.toEntity().copy(lastModified = System.currentTimeMillis())
        projectDao.updateProject(entity)
    }

    private fun ProjectEntity.toDomain(): VideoProject {
        val tracksListType = object : TypeToken<List<TimelineTrack>>() {}.type
        val tracks: List<TimelineTrack> = try {
            gson.fromJson(tracksJson, tracksListType)
        } catch (e: Exception) {
            emptyList()
        }

        return VideoProject(
            id = id,
            name = name,
            tracks = tracks,
            lastModified = lastModified,
            duration = duration,
            thumbnailPath = thumbnailPath,
            resolution = resolution,
            frameRate = frameRate
        )
    }

    private fun VideoProject.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            tracksJson = gson.toJson(tracks),
            lastModified = lastModified,
            duration = duration,
            thumbnailPath = thumbnailPath,
            resolution = resolution,
            frameRate = frameRate
        )
    }
}
