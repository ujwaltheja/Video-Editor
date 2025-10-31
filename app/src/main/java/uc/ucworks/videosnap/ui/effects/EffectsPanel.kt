package uc.ucworks.videosnap.ui.effects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uc.ucworks.videosnap.domain.Effect
import uc.ucworks.videosnap.domain.TransitionType

@Composable
fun EffectsPanel(
    selectedClipId: String?,
    appliedEffects: List<Effect>,
    onEffectApply: (String) -> Unit,
    onEffectRemove: (String) -> Unit,
    onTransitionApply: (TransitionType) -> Unit,
    modifier: Modifier
) {
    val availableEffects = listOf("Grayscale", "Sepia", "Invert", "Brightness")

    Column(modifier = modifier) {
        Text(text = "Effects")
        LazyColumn {
            items(availableEffects) {
                effect ->
                Button(onClick = { onEffectApply(effect) }) {
                    Text(text = effect)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Applied Effects")
        LazyColumn {
            items(appliedEffects) {
                effect ->
                EffectItem(effect = effect, onEffectRemove = onEffectRemove)
            }
        }
    }
}

@Composable
fun EffectItem(effect: Effect, onEffectRemove: (String) -> Unit) {
    var brightness by remember { mutableStateOf(1f) }

    Row {
        Text(text = effect.name)
        Button(onClick = { onEffectRemove(effect.name) }) {
            Text(text = "Remove")
        }
    }

    if (effect.name == "Brightness") {
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0f..2f
        )
    }
}
