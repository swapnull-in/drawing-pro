package com.swap.handdrawing.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.swap.handdrawing.PaperStyle
import com.swap.handdrawing.theme.AccentCobalt
import kotlin.math.abs

@Composable
fun StudioBrushSheet(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChanged: (Float) -> Unit,
    paperStyle: PaperStyle,
    onPaperStyleChanged: (PaperStyle) -> Unit,
    isEraserMode: Boolean,
    modifier: Modifier = Modifier
) {
    var showCustomColorPicker by remember { mutableStateOf(false) }

    val essentialColors = listOf(
        Color.Black, Color(0xFF333333), Color(0xFF666666), Color(0xFF999999), Color.White
    )

    val vibrantColors = listOf(
        Color(0xFF635BFF), Color(0xFFFF5263), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF3B82F6),
        Color(0xFFEF4444), Color(0xFF84CC16)
    )

    val pastelColors = listOf(
        Color(0xFFA5B4FC), Color(0xFFFCA5A5), Color(0xFF6EE7B7), Color(0xFFFDE68A),
        Color(0xFFC4B5FD), Color(0xFFF472B6), Color(0xFF67E8F9), Color(0xFFFED7AA)
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Brush & Canvas Studio",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Live Brush Stroke Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 24.dp)) {
                val path = Path().apply {
                    val startX = 20f
                    val endX = size.width - 20f
                    val midY = size.height / 2f
                    moveTo(startX, midY)
                    quadraticTo(size.width * 0.25f, midY - 30f, size.width * 0.5f, midY)
                    quadraticTo(size.width * 0.75f, midY + 30f, endX, midY)
                }

                drawPath(
                    path = path,
                    color = if (isEraserMode) Color.Gray.copy(alpha = 0.6f) else selectedColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stroke Thickness Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Stroke Size",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${strokeWidth.toInt()} px",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Thickness Presets
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(2f to "Fine", 8f to "Medium", 18f to "Bold", 36f to "Heavy").forEach { (presetWidth, label) ->
                val isSelected = abs(strokeWidth - presetWidth) < 1f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) AccentCobalt else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onStrokeWidthChanged(presetWidth) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = strokeWidth,
            onValueChange = onStrokeWidthChanged,
            valueRange = 2f..100f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Color Palettes Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = { showCustomColorPicker = !showCustomColorPicker },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showCustomColorPicker) "Presets" else "Custom Wheel",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = showCustomColorPicker,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            CustomColorPicker(
                currentColor = selectedColor,
                onColorChanged = onColorSelected,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (!showCustomColorPicker) {
            Text("Vibrant", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ColorRow(colors = vibrantColors, selectedColor = selectedColor, onColorSelected = onColorSelected)

            Spacer(modifier = Modifier.height(10.dp))
            Text("Pastels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ColorRow(colors = pastelColors, selectedColor = selectedColor, onColorSelected = onColorSelected)

            Spacer(modifier = Modifier.height(10.dp))
            Text("Essentials", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ColorRow(colors = essentialColors, selectedColor = selectedColor, onColorSelected = onColorSelected)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Canvas Paper Style Cards
        Text(
            text = "Canvas Paper Style",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PaperCard(
                title = "Plain",
                isSelected = paperStyle == PaperStyle.PLAIN,
                onClick = { onPaperStyleChanged(PaperStyle.PLAIN) },
                modifier = Modifier.weight(1f),
                style = PaperStyle.PLAIN
            )
            PaperCard(
                title = "Grid",
                isSelected = paperStyle == PaperStyle.GRID,
                onClick = { onPaperStyleChanged(PaperStyle.GRID) },
                modifier = Modifier.weight(1f),
                style = PaperStyle.GRID
            )
            PaperCard(
                title = "Dots",
                isSelected = paperStyle == PaperStyle.DOTS,
                onClick = { onPaperStyleChanged(PaperStyle.DOTS) },
                modifier = Modifier.weight(1f),
                style = PaperStyle.DOTS
            )
            PaperCard(
                title = "Ruled",
                isSelected = paperStyle == PaperStyle.RULED,
                onClick = { onPaperStyleChanged(PaperStyle.RULED) },
                modifier = Modifier.weight(1f),
                style = PaperStyle.RULED
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ColorRow(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(colors) { color ->
            val isSelected = selectedColor == color
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.5.dp else 1.dp,
                        color = if (isSelected) AccentCobalt else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == Color.White || color.red > 0.8f && color.green > 0.8f) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    style: PaperStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) AccentCobalt.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentCobalt else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                val step = 10.dp.toPx()
                val width = size.width
                val height = size.height

                when (style) {
                    PaperStyle.GRID -> {
                        for (x in 0..(width / step).toInt()) {
                            drawLine(Color.LightGray, Offset(x * step, 0f), Offset(x * step, height), strokeWidth = 1f)
                        }
                        for (y in 0..(height / step).toInt()) {
                            drawLine(Color.LightGray, Offset(0f, y * step), Offset(width, y * step), strokeWidth = 1f)
                        }
                    }
                    PaperStyle.DOTS -> {
                        for (x in 0..(width / step).toInt()) {
                            for (y in 0..(height / step).toInt()) {
                                drawCircle(Color.Gray, radius = 1.5f, center = Offset(x * step, y * step))
                            }
                        }
                    }
                    PaperStyle.RULED -> {
                        for (y in 1..(height / step).toInt()) {
                            drawLine(Color(0xFF90CAF9), Offset(0f, y * step), Offset(width, y * step), strokeWidth = 1f)
                        }
                        drawLine(Color(0xFFEF9A9A), Offset(10.dp.toPx(), 0f), Offset(10.dp.toPx(), height), strokeWidth = 1.5f)
                    }
                    PaperStyle.PLAIN -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) AccentCobalt else MaterialTheme.colorScheme.onSurface
        )
    }
}
