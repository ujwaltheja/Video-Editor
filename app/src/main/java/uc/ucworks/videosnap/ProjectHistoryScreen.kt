package uc.ucworks.videosnap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * A composable that displays the project history screen.
 *
 * @param projectHistory A list of project versions.
 * @param onRevert A callback that is invoked when the user reverts to a project version.
 */
@Composable
fun ProjectHistoryScreen(
    projectHistory: List<VideoProject>,
    onRevert: (VideoProject) -> Unit
) {
    Column {
        Text(text = "Project History")
        LazyColumn {
            items(projectHistory) {
                project ->
                Row {
                    Text(text = "${project.name} - ${project.lastModified}")
                    Button(onClick = { onRevert(project) }) {
                        Text(text = "Revert")
                    }
                }
            }
        }
    }
}
