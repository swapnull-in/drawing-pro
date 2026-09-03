package com.swap.handdrawing.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.swap.handdrawing.theme.AccentCobalt
import com.swap.handdrawing.theme.AccentCoral

@Composable
fun StudioBottomDock(
    isEraserMode: Boolean,
    onToggleEraser: () -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onOpenBrushSheet: () -> Unit,
    onPickGalleryImage: () -> Unit,
    onCaptureCameraPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMediaMenu by remember { mutableStateOf(false) }

    val quickColors = remember {
        listOf(
            Color.Black,
            Color(0xFF635BFF), // Cobalt
            Color(0xFFFF5263), // Crimson
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color.White
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Tool Switchers (Brush vs Eraser)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                // Brush Toggle Button
                val brushBg by animateColorAsState(
                    targetValue = if (!isEraserMode) AccentCobalt else Color.Transparent,
                    animationSpec = tween(200),
                    label = "brushBg"
                )
                val brushContentColor by animateColorAsState(
                    targetValue = if (!isEraserMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "brushContent"
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brushBg)
                        .clickable { if (isEraserMode) onToggleEraser() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Brush Mode",
                        tint = brushContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Eraser Toggle Button
                val eraserBg by animateColorAsState(
                    targetValue = if (isEraserMode) AccentCoral else Color.Transparent,
                    animationSpec = tween(200),
                    label = "eraserBg"
                )
                val eraserContentColor by animateColorAsState(
                    targetValue = if (isEraserMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "eraserContent"
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(eraserBg)
                        .clickable { if (!isEraserMode) onToggleEraser() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Eraser Mode",
                        tint = eraserContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Color Swatches Bar
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickColors) { color ->
                    val isSelected = !isEraserMode && selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) AccentCobalt else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }

            // More Tools & Customization Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Color Wheel & Brush Tune Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            ComposeBrush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        )
                        .clickable { onOpenBrushSheet() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Color & Brush Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                // Media Tracing Popover Menu
                Box {
                    IconButton(
                        onClick = { showMediaMenu = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Image Background / Tracing",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMediaMenu,
                        onDismissRequest = { showMediaMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Gallery Image") },
                            leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                            onClick = {
                                showMediaMenu = false
                                onPickGalleryImage()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Take Camera Photo") },
                            leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                            onClick = {
                                showMediaMenu = false
                                onCaptureCameraPhoto()
                            }
                        )
                    }
                }
            }
        }
    }
}
