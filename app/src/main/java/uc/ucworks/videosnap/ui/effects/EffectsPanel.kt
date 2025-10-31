package uc.ucworks.videosnap.ui.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import uc.ucworks.videosnap.EffectType
import uc.ucworks.videosnap.TransitionType

/**
 * Effects panel for applying video effects and transitions.
 */
@Composable
fun EffectsPanel(
    selectedClipId: String?,
    appliedEffects: List<String>,
    onEffectApply: (String) -> Unit,
    onEffectRemove: (String) -> Unit,
    onTransitionApply: (TransitionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        // Tab selector
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Effects") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Transitions") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Filters") }
            )
        }

        when (selectedTab) {
            0 -> EffectsTab(
                selectedClipId = selectedClipId,
                appliedEffects = appliedEffects,
                onEffectApply = onEffectApply,
                onEffectRemove = onEffectRemove
            )
            1 -> TransitionsTab(
                selectedClipId = selectedClipId,
                onTransitionApply = onTransitionApply
            )
            2 -> FiltersTab(
                selectedClipId = selectedClipId,
                onEffectApply = onEffectApply
            )
        }
    }
}

/**
 * Effects tab content.
 */
@Composable
fun EffectsTab(
    selectedClipId: String?,
    appliedEffects: List<String>,
    onEffectApply: (String) -> Unit,
    onEffectRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedClipId == null) {
        NoClipSelectedMessage()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Applied effects
        if (appliedEffects.isNotEmpty()) {
            item {
                Text(
                    "Applied Effects",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(appliedEffects) { effect ->
                AppliedEffectCard(
                    effectName = effect,
                    onRemove = { onEffectRemove(effect) }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // Available effects
        item {
            Text(
                "Available Effects",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Text("Color Effects", style = MaterialTheme.typography.labelLarge)
        }
        items(colorEffects) { effect ->
            EffectCard(
                effect = effect,
                isApplied = appliedEffects.contains(effect.name),
                onApply = { onEffectApply(effect.name) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Blur Effects", style = MaterialTheme.typography.labelLarge)
        }
        items(blurEffects) { effect ->
            EffectCard(
                effect = effect,
                isApplied = appliedEffects.contains(effect.name),
                onApply = { onEffectApply(effect.name) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Stylistic Effects", style = MaterialTheme.typography.labelLarge)
        }
        items(stylisticEffects) { effect ->
            EffectCard(
                effect = effect,
                isApplied = appliedEffects.contains(effect.name),
                onApply = { onEffectApply(effect.name) }
            )
        }
    }
}

/**
 * Transitions tab content.
 */
@Composable
fun TransitionsTab(
    selectedClipId: String?,
    onTransitionApply: (TransitionType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedClipId == null) {
        NoClipSelectedMessage()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Transition Effects",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(TransitionType.values()) { transition ->
            TransitionCard(
                transition = transition,
                onApply = { onTransitionApply(transition) }
            )
        }
    }
}

/**
 * Filters tab content.
 */
@Composable
fun FiltersTab(
    selectedClipId: String?,
    onEffectApply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedClipId == null) {
        NoClipSelectedMessage()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Preset Filters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(filterPresets) { filter ->
            EffectCard(
                effect = filter,
                isApplied = false,
                onApply = { onEffectApply(filter.name) }
            )
        }
    }
}

/**
 * Card for an individual effect.
 */
@Composable
fun EffectCard(
    effect: EffectInfo,
    isApplied: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        colors = CardDefaults.cardColors(
            containerColor = if (isApplied)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = effect.icon,
                contentDescription = effect.name,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effect.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = effect.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isApplied) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Applied",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Card for applied effect with remove option.
 */
@Composable
fun AppliedEffectCard(
    effectName: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = effectName,
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove effect")
            }
        }
    }
}

/**
 * Card for transition effect.
 */
@Composable
fun TransitionCard(
    transition: TransitionType,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onApply)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getTransitionIcon(transition),
                contentDescription = transition.name,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = transition.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

/**
 * Message shown when no clip is selected.
 */
@Composable
fun NoClipSelectedMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Select a clip to apply effects",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Effect information.
 */
data class EffectInfo(
    val name: String,
    val description: String,
    val icon: ImageVector
)

// Color effects
private val colorEffects = listOf(
    EffectInfo("Brightness", "Adjust brightness", Icons.Default.Brightness6),
    EffectInfo("Contrast", "Adjust contrast", Icons.Default.Contrast),
    EffectInfo("Saturation", "Adjust color saturation", Icons.Default.ColorLens),
    EffectInfo("Grayscale", "Convert to black and white", Icons.Default.FilterBAndW),
    EffectInfo("Sepia", "Apply sepia tone", Icons.Default.Palette),
    EffectInfo("Invert", "Invert colors", Icons.Default.InvertColors),
    EffectInfo("Color Grading", "Advanced color correction", Icons.Default.Tune)
)

// Blur effects
private val blurEffects = listOf(
    EffectInfo("Gaussian Blur", "Smooth blur effect", Icons.Default.Blur),
    EffectInfo("Motion Blur", "Directional blur", Icons.Default.BlurOn),
    EffectInfo("Radial Blur", "Radial zoom blur", Icons.Default.BlurCircular)
)

// Stylistic effects
private val stylisticEffects = listOf(
    EffectInfo("Sharpen", "Sharpen image details", Icons.Default.PhotoFilter),
    EffectInfo("Noise", "Add film grain", Icons.Default.GrainOutlined),
    EffectInfo("Pixelate", "Pixelate effect", Icons.Default.Apps),
    EffectInfo("Edge Detect", "Highlight edges", Icons.Default.BorderOuter),
    EffectInfo("Chroma Key", "Green screen removal", Icons.Default.Layers)
)

// Filter presets
private val filterPresets = listOf(
    EffectInfo("Vintage", "Classic film look", Icons.Default.CameraRoll),
    EffectInfo("Cinematic", "Movie-style grading", Icons.Default.Movie),
    EffectInfo("Warm", "Warm color tone", Icons.Default.WbSunny),
    EffectInfo("Cool", "Cool color tone", Icons.Default.AcUnit),
    EffectInfo("High Contrast", "Dramatic contrast", Icons.Default.Contrast),
    EffectInfo("Soft", "Soft dreamy look", Icons.Default.CloudQueue)
)

/**
 * Get icon for transition type.
 */
private fun getTransitionIcon(type: TransitionType): ImageVector {
    return when (type) {
        TransitionType.FADE -> Icons.Default.Opacity
        TransitionType.DISSOLVE -> Icons.Default.BlurOn
        TransitionType.WIPE_LEFT, TransitionType.WIPE_RIGHT,
        TransitionType.WIPE_UP, TransitionType.WIPE_DOWN -> Icons.Default.SwipeLeft
        TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT -> Icons.Default.SwipeRight
        TransitionType.ZOOM_IN, TransitionType.ZOOM_OUT -> Icons.Default.ZoomIn
        TransitionType.CIRCLE_OPEN, TransitionType.CIRCLE_CLOSE -> Icons.Default.Circle
    }
}
