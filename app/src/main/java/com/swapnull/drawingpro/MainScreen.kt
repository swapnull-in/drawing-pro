package com.swapnull.drawingpro

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swapnull.drawingpro.components.StudioBottomDock
import com.swapnull.drawingpro.components.StudioBrushSheet
import com.swapnull.drawingpro.components.StudioTopBar
import com.swapnull.drawingpro.theme.StudioDrawingTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DrawingViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    var showProjectsGallery by remember { mutableStateOf(false) }
    var showSaveProjectDialog by remember { mutableStateOf(false) }
    var projectNameInput by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()
    val gallerySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    viewModel.updateBackgroundImage(bitmap)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Background photo loaded")
                    }
                }
            } catch (_: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to load image from gallery")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            try {
                context.contentResolver.openInputStream(photoUri!!)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    viewModel.updateBackgroundImage(bitmap)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Camera photo added to canvas")
                    }
                }
            } catch (_: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to load photo")
                }
            }
        }
    }

    fun launchCameraWithUri() {
        try {
            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            photoUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera unavailable on this device")
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraWithUri()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera permission is required")
            }
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraWithUri()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun handleSaveDrawing() {
        val uri = saveImageToMediaStore(
            context = context,
            paths = viewModel.paths,
            backgroundImage = viewModel.backgroundImage,
            size = canvasSize,
            paperStyle = viewModel.paperStyle,
            backgroundOpacity = viewModel.backgroundOpacity,
            exportTransparentBg = viewModel.exportTransparentBg
        )
        if (uri != null) {
            coroutineScope.launch {
                val message = if (viewModel.exportTransparentBg) "Artwork saved as Transparent PNG!" else "Artwork saved to Pictures!"
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    fun handleShareDrawing() {
        val uri = saveImageToMediaStore(
            context = context,
            paths = viewModel.paths,
            backgroundImage = viewModel.backgroundImage,
            size = canvasSize,
            paperStyle = viewModel.paperStyle,
            backgroundOpacity = viewModel.backgroundOpacity,
            exportTransparentBg = viewModel.exportTransparentBg
        )
        if (uri != null) {
            shareImage(context, uri)
        }
    }

    if (showSaveProjectDialog) {
        AlertDialog(
            onDismissRequest = { showSaveProjectDialog = false },
            title = { Text("Save Project") },
            text = {
                OutlinedTextField(
                    value = projectNameInput,
                    onValueChange = { projectNameInput = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = projectNameInput.takeIf { it.isNotBlank() } ?: "Untitled Project"
                        viewModel.saveCurrentProject(name)
                        showSaveProjectDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Project saved successfully!")
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (viewModel.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showClearConfirmDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to clear your drawing canvas and background photo? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCanvas()
                        viewModel.showClearConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Canvas cleared")
                        }
                    }
                ) {
                    Text("Clear Canvas", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            // Full Screen Edge-to-Edge Canvas
            DrawingCanvas(
                paths = viewModel.paths,
                currentPath = viewModel.currentPath,
                pathUpdateTrigger = viewModel.pathUpdateTrigger,
                currentPathColor = viewModel.selectedColor,
                currentPathStrokeWidth = if (viewModel.isEraserMode) 60f else viewModel.strokeWidth,
                currentBrushType = viewModel.currentBrushType,
                isEraserMode = viewModel.isEraserMode,
                paperStyle = viewModel.paperStyle,
                onPathStarted = { offset -> viewModel.startPath(offset) },
                onPathMoved = { offset -> viewModel.movePath(offset) },
                onPathEnded = { viewModel.endPath() },
                backgroundImage = viewModel.backgroundImage,
                backgroundOpacity = viewModel.backgroundOpacity
            )

            // Floating Top Studio Glass Bar
            StudioTopBar(
                canUndo = viewModel.paths.isNotEmpty(),
                canRedo = viewModel.undonePaths.isNotEmpty(),
                onUndo = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.undo() 
                },
                onRedo = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.redo() 
                },
                paperStyle = viewModel.paperStyle,
                onOpenPaperPicker = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.showBrushSettingsSheet = true 
                },
                selectedColor = viewModel.selectedColor,
                strokeWidth = viewModel.strokeWidth,
                currentBrushType = viewModel.currentBrushType,
                isEraserMode = viewModel.isEraserMode,
                onOpenBrushSheet = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.showBrushSettingsSheet = true 
                },
                hasBackgroundImage = viewModel.backgroundImage != null,
                onRemoveBackgroundImage = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.updateBackgroundImage(null) 
                },
                onClearCanvas = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.showClearConfirmDialog = true 
                },
                onSave = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSaveProjectDialog = true
                    handleSaveDrawing() 
                },
                onShare = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    handleShareDrawing() 
                },
                onOpenGallery = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showProjectsGallery = true
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
            )

            // Floating Bottom Studio Glass Dock
            StudioBottomDock(
                isEraserMode = viewModel.isEraserMode,
                onToggleEraser = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleEraserMode() 
                },
                selectedColor = viewModel.selectedColor,
                onColorSelected = { color -> 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.updateSelectedColor(color) 
                },
                onOpenBrushSheet = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.showBrushSettingsSheet = true 
                },
                onPickGalleryImage = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    galleryLauncher.launch("image/*") 
                },
                onCaptureCameraPhoto = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    launchCamera() 
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
            )
        }

        if (viewModel.showBrushSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showBrushSettingsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                StudioBrushSheet(
                    selectedColor = viewModel.selectedColor,
                    onColorSelected = { color -> viewModel.updateSelectedColor(color) },
                    strokeWidth = viewModel.strokeWidth,
                    onStrokeWidthChanged = { width -> viewModel.updateStrokeWidth(width) },
                    currentBrushType = viewModel.currentBrushType,
                    onBrushTypeChanged = { type -> viewModel.updateBrushType(type) },
                    drawingMode = viewModel.drawingMode,
                    onDrawingModeChanged = { mode -> viewModel.updateDrawingMode(mode) },
                    paperStyle = viewModel.paperStyle,
                    onPaperStyleChanged = { style -> viewModel.updatePaperStyle(style) },
                    isEraserMode = viewModel.isEraserMode,
                    hasBackgroundImage = viewModel.backgroundImage != null,
                    backgroundOpacity = viewModel.backgroundOpacity,
                    onBackgroundOpacityChanged = { opacity -> viewModel.updateBackgroundOpacity(opacity) },
                    exportTransparentBg = viewModel.exportTransparentBg,
                    onToggleExportTransparentBg = { viewModel.toggleExportTransparentBg() }
                )
            }
        }
        if (showProjectsGallery) {
            val projects by viewModel.projects.collectAsState()
            
            ModalBottomSheet(
                onDismissRequest = { showProjectsGallery = false },
                sheetState = gallerySheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth().height(400.dp)) {
                    Text("Saved Projects", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                    
                    if (projects.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No saved projects yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(projects) { project ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        viewModel.loadProject(project)
                                        showProjectsGallery = false
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Loaded ${project.name}") }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "Style: ${project.paperStyle} • Last updated: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(project.updatedAt))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteProject(project) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun saveImageToMediaStore(
    context: Context,
    paths: List<PathData>,
    backgroundImage: Bitmap?,
    size: IntSize,
    paperStyle: PaperStyle,
    backgroundOpacity: Float = 1f,
    exportTransparentBg: Boolean = false
): Uri? {
    if (size.width <= 0 || size.height <= 0) return null

    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    if (!exportTransparentBg) {
        canvas.drawColor(Color.WHITE)

        val paperPaint = Paint().apply {
            color = Color.LTGRAY
            alpha = 60
            strokeWidth = 2f
        }

        when (paperStyle) {
            PaperStyle.GRID -> {
                val step = 100f
                for (x in 0..(size.width / step).toInt()) {
                    canvas.drawLine(x * step, 0f, x * step, size.height.toFloat(), paperPaint)
                }
                for (y in 0..(size.height / step).toInt()) {
                    canvas.drawLine(0f, y * step, size.width.toFloat(), y * step, paperPaint)
                }
            }
            PaperStyle.DOTS -> {
                val step = 100f
                for (x in 0..(size.width / step).toInt()) {
                    for (y in 0..(size.height / step).toInt()) {
                        canvas.drawCircle(x * step, y * step, 6f, paperPaint)
                    }
                }
            }
            PaperStyle.RULED -> {
                val step = 120f
                for (y in 1..(size.height / step).toInt()) {
                    canvas.drawLine(0f, y * step, size.width.toFloat(), y * step, paperPaint)
                }
                val redMarginPaint = Paint().apply {
                    color = Color.RED
                    alpha = 100
                    strokeWidth = 4f
                }
                canvas.drawLine(150f, 0f, 150f, size.height.toFloat(), redMarginPaint)
            }
            PaperStyle.PLAIN -> {}
        }

        backgroundImage?.let {
            val bgPaint = Paint().apply {
                alpha = (backgroundOpacity * 255).toInt().coerceIn(0, 255)
            }
            val src = Rect(0, 0, it.width, it.height)
            val dst = Rect(0, 0, size.width, size.height)
            canvas.drawBitmap(it, src, dst, bgPaint)
        }
    }

    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    paths.forEach { pathData ->
        if (pathData.isEraser) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            paint.xfermode = null
            paint.color = pathData.color.toArgb()
        }
        paint.strokeWidth = pathData.strokeWidth
        canvas.drawPath(pathData.path.asAndroidPath(), paint)
    }

    val filename = "Drawing_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
        outputStream?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
    return uri
}

fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Drawing"))
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    StudioDrawingTheme {
        MainScreen()
    }
}
