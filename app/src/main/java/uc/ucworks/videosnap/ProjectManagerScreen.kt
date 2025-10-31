package uc.ucworks.videosnap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * A composable that displays the project manager screen.
 *
 * @param projects A list of projects.
 * @param onLoadProject A callback that is invoked when the user loads a project.
 * @param onCreateProject A callback that is invoked when the user creates a project.
 * @param onDeleteProject A callback that is invoked when the user deletes a project.
 * @param onShowHistory A callback that is invoked when the user views the history of a project.
 * @param onSaveProject A callback that is invoked when the user saves a project.
 * @param onBackupProject A callback that is invoked when the user backs up a project.
 */
@Composable
fun ProjectManagerScreen(
    projects: List<VideoProject>,
    onLoadProject: (VideoProject) -> Unit,
    onCreateProject: () -> Unit,
    onDeleteProject: (VideoProject) -> Unit,
    onShowHistory: (VideoProject) -> Unit,
    onSaveProject: (VideoProject) -> Unit,
    onBackupProject: (VideoProject) -> Unit
) {
    Column {
        Text(text = "Project Manager")
        Button(onClick = { onCreateProject() }) {
            Text(text = "Create New Project")
        }
        LazyColumn {
            items(projects) {
                project ->
                Row {
                    Text(text = project.name)
                    Button(onClick = { onLoadProject(project) }) {
                        Text(text = "Load")
                    }
                    Button(onClick = { onDeleteProject(project) }) {
                        Text(text = "Delete")
                    }
                    Button(onClick = { onShowHistory(project) }) {
                        Text(text = "History")
                    }
                    Button(onClick = { onSaveProject(project) }) {
                        Text(text = "Save")
                    }
                    Button(onClick = { onBackupProject(project) }) {
                        Text(text = "Backup")
                    }
                }
            }
        }
    }
}
