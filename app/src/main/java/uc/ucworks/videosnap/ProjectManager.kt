package uc.ucworks.videosnap

import android.content.Context
import com.google.gson.Gson
import java.io.File

/**
 * An object that manages video projects.
 */
object ProjectManager {

    private const val PROJECTS_DIR = "projects"

    /**
     * Saves all projects.
     *
     * @param context The context.
     * @param projects The list of projects to save.
     */
    fun saveAllProjects(context: Context, projects: List<VideoProject>) {
        val projectsDir = File(context.filesDir, PROJECTS_DIR)
        if (!projectsDir.exists()) {
            projectsDir.mkdir()
        }
        projects.forEach { project ->
            saveProject(context, project)
        }
    }

    /**
     * Saves a project.
     *
     * @param context The context.
     * @param project The project to save.
     */
    fun saveProject(context: Context, project: VideoProject) {
        val gson = Gson()
        val json = gson.toJson(project)
        val projectsDir = File(context.filesDir, PROJECTS_DIR)
        if (!projectsDir.exists()) {
            projectsDir.mkdir()
        }
        val projectDir = File(projectsDir, project.id)
        if (!projectDir.exists()) {
            projectDir.mkdir()
        }
        val versionFile = File(projectDir, "${System.currentTimeMillis()}.json")
        versionFile.writeText(json)
    }

    /**
     * Loads all projects.
     *
     * @param context The context.
     * @return A list of all projects.
     */
    fun loadProjects(context: Context): List<VideoProject> {
        val projectsDir = File(context.filesDir, PROJECTS_DIR)
        if (!projectsDir.exists()) {
            return emptyList()
        }
        val gson = Gson()
        return projectsDir.listFiles()?.mapNotNull { projectDir ->
            val latestVersionFile = projectDir.listFiles()?.maxByOrNull { it.name.toLong() }
            if (latestVersionFile != null) {
                val json = latestVersionFile.readText()
                gson.fromJson(json, VideoProject::class.java)
            } else {
                null
            }
        } ?: emptyList()
    }

    /**
     * Loads the history of a project.
     *
     * @param context The context.
     * @param projectId The ID of the project.
     * @return A list of all versions of the project.
     */
    fun loadProjectHistory(context: Context, projectId: String): List<VideoProject> {
        val projectDir = File(context.filesDir, "$PROJECTS_DIR/$projectId")
        if (!projectDir.exists()) {
            return emptyList()
        }
        val gson = Gson()
        return projectDir.listFiles()?.mapNotNull { versionFile ->
            val json = versionFile.readText()
            gson.fromJson(json, VideoProject::class.java)
        }?.sortedByDescending { it.lastModified } ?: emptyList()
    }

    /**
     * Gets the latest version of a project file.
     *
     * @param context The context.
     * @param projectId The ID of the project.
     * @return The latest version of the project file, or null if the project does not exist.
     */
    fun getProjectFile(context: Context, projectId: String): File? {
        val projectDir = File(context.filesDir, "$PROJECTS_DIR/$projectId")
        if (!projectDir.exists()) {
            return null
        }
        return projectDir.listFiles()?.maxByOrNull { it.name.toLong() }
    }
}
