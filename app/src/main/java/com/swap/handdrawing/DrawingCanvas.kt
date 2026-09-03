package com.swap.handdrawing

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

data class PathData(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class PaperStyle {
    PLAIN, GRID, DOTS, RULED
}

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    paths: List<PathData>,
    currentPath: Path?,
    pathUpdateTrigger: Long = 0L,
    currentPathColor: Color,
    currentPathStrokeWidth: Float,
    isEraserMode: Boolean,
    paperStyle: PaperStyle = PaperStyle.PLAIN,
    onPathStarted: (Offset) -> Unit,
    onPathMoved: (Offset) -> Unit,
    onPathEnded: () -> Unit,
    backgroundImage: Bitmap? = null,
    backgroundOpacity: Float = 1f
) {
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var cachedCanvas by remember { mutableStateOf<Canvas?>(null) }

    // Transformation state for Zoom and Pan
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }

    // Cache tracking state
    class CacheState {
        var drawnPathsCount = 0
        var lastBackgroundImage: Bitmap? = null
        var lastBackgroundOpacity = 1f
        var lastCachedSize = IntSize.Zero
    }
    val cacheState = remember { CacheState() }

    fun updateCache(width: Int, height: Int) {
        val c = cachedCanvas ?: return
        val androidCanvas = c.nativeCanvas

        val needsFullRedraw = cacheState.drawnPathsCount > paths.size ||
                backgroundImage != cacheState.lastBackgroundImage ||
                backgroundOpacity != cacheState.lastBackgroundOpacity ||
                cacheState.lastCachedSize.width != width ||
                cacheState.lastCachedSize.height != height

        if (needsFullRedraw) {
            androidCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            backgroundImage?.let { bg ->
                val bgPaint = Paint().apply {
                    alpha = (backgroundOpacity * 255).toInt().coerceIn(0, 255)
                }
                androidCanvas.drawBitmap(
                    bg,
                    null,
                    Rect(0, 0, width, height),
                    bgPaint
                )
            }

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            paths.forEach { pathData ->
                paint.strokeWidth = pathData.strokeWidth
                if (pathData.isEraser) {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    paint.xfermode = null
                    paint.color = pathData.color.toArgb()
                }
                androidCanvas.drawPath(pathData.path.asAndroidPath(), paint)
            }
            cacheState.drawnPathsCount = paths.size
        } else if (cacheState.drawnPathsCount < paths.size) {
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            for (i in cacheState.drawnPathsCount until paths.size) {
                val pathData = paths[i]
                paint.strokeWidth = pathData.strokeWidth
                if (pathData.isEraser) {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    paint.xfermode = null
                    paint.color = pathData.color.toArgb()
                }
                androidCanvas.drawPath(pathData.path.asAndroidPath(), paint)
            }
            cacheState.drawnPathsCount = paths.size
        }

        cacheState.lastBackgroundImage = backgroundImage
        cacheState.lastBackgroundOpacity = backgroundOpacity
        cacheState.lastCachedSize = IntSize(width, height)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
                .pointerInput(scale, offset) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val down = event.changes.firstOrNull { it.pressed } ?: break

                            if (event.changes.size > 1) continue

                            val canvasDown = (down.position - offset) / scale
                            onPathStarted(canvasDown)

                            while (true) {
                                val moveEvent = awaitPointerEvent()
                                val anyPressed = moveEvent.changes.any { it.pressed }

                                if (moveEvent.changes.size > 1) {
                                    onPathEnded()
                                    break
                                }

                                if (anyPressed) {
                                    val change = moveEvent.changes.first()
                                    val canvasPos = (change.position - offset) / scale
                                    onPathMoved(canvasPos)
                                    change.consume()
                                } else {
                                    onPathEnded()
                                    break
                                }
                            }
                        }
                    }
                }
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            @Suppress("UNUSED_VARIABLE")
            val trigger = pathUpdateTrigger

            val width = size.width.toInt()
            val height = size.height.toInt()

            if (width > 0 && height > 0) {
                if (cachedBitmap == null || cachedBitmap!!.width != width || cachedBitmap!!.height != height) {
                    val newBitmap = ImageBitmap(width, height)
                    cachedBitmap = newBitmap
                    cachedCanvas = Canvas(newBitmap)
                }

                updateCache(width, height)
            }

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // Draw Paper Style Pattern
                when (paperStyle) {
                    PaperStyle.GRID -> {
                        val step = 36.dp.toPx()
                        for (x in 0..(width / step).toInt()) {
                            drawLine(Color.LightGray.copy(alpha = 0.35f), Offset(x * step, 0f), Offset(x * step, size.height))
                        }
                        for (y in 0..(height / step).toInt()) {
                            drawLine(Color.LightGray.copy(alpha = 0.35f), Offset(0f, y * step), Offset(size.width, y * step))
                        }
                    }
                    PaperStyle.DOTS -> {
                        val step = 32.dp.toPx()
                        for (x in 0..(width / step).toInt()) {
                            for (y in 0..(height / step).toInt()) {
                                drawCircle(Color.LightGray.copy(alpha = 0.6f), radius = 2f, center = Offset(x * step, y * step))
                            }
                        }
                    }
                    PaperStyle.RULED -> {
                        val step = 40.dp.toPx()
                        for (y in 1..(height / step).toInt()) {
                            drawLine(Color(0xFF90CAF9).copy(alpha = 0.4f), Offset(0f, y * step), Offset(size.width, y * step))
                        }
                        // Red margin line
                        drawLine(Color(0xFFEF9A9A).copy(alpha = 0.5f), Offset(48.dp.toPx(), 0f), Offset(48.dp.toPx(), size.height), strokeWidth = 1.5f)
                    }
                    PaperStyle.PLAIN -> {}
                }

                cachedBitmap?.let {
                    drawImage(it)
                }

                currentPath?.let { path ->
                    drawPath(
                        path = path,
                        color = if (isEraserMode) Color.Transparent else currentPathColor,
                        style = Stroke(
                            width = currentPathStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (isEraserMode) BlendMode.Clear else BlendMode.SrcOver
                    )
                }
            }
        }

        // Floating Reset Zoom Chip
        AnimatedVisibility(
            visible = scale != 1f || offset != Offset.Zero,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 90.dp)
        ) {
            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomInMap,
                        contentDescription = "Reset Zoom",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(scale * 100).toInt()}% • Reset View",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
