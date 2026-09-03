package com.swap.handdrawing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomColorPicker(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hue by remember(currentColor) { mutableFloatStateOf(0f) }
    var saturation by remember(currentColor) { mutableFloatStateOf(1f) }
    var value by remember(currentColor) { mutableFloatStateOf(1f) }

    val rainbowBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    }

    val selectedHsvColor = remember(hue, saturation, value) {
        Color.hsv(hue, saturation, value)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Custom Color",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Current picked color preview pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#${selectedHsvColor.toHex()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(selectedHsvColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onColorChanged(selectedHsvColor) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hue Slider with Rainbow Gradient
        Text("Hue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(rainbowBrush)
        )
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                onColorChanged(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = selectedHsvColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )

        // Saturation Slider
        Text("Saturation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                onColorChanged(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = selectedHsvColor,
                activeTrackColor = selectedHsvColor
            )
        )

        // Value / Brightness Slider
        Text("Brightness", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = {
                value = it
                onColorChanged(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = selectedHsvColor,
                activeTrackColor = selectedHsvColor
            )
        )
    }
}

private fun Color.toHex(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("%02X%02X%02X", red, green, blue)
}
