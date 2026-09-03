package com.swap.handdrawing

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swap.handdrawing.components.StudioBottomDock
import com.swap.handdrawing.components.StudioBrushSheet
import com.swap.handdrawing.components.StudioTopBar
import com.swap.handdrawing.theme.StudioDrawingTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DrawingViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val sheetState = rememberModalBottomSheetState()

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
            paperStyle = viewModel.paperStyle
        )
        if (uri != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Artwork saved to Pictures!")
            }
        }
    }

    fun handleShareDrawing() {
        val uri = saveImageToMediaStore(
            context = context,
            paths = viewModel.paths,
            backgroundImage = viewModel.backgroundImage,
            size = canvasSize,
            paperStyle = viewModel.paperStyle
        )
        if (uri != null) {
            shareImage(context, uri)
        }
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
                isEraserMode = viewModel.isEraserMode,
                paperStyle = viewModel.paperStyle,
                onPathStarted = { offset -> viewModel.startPath(offset) },
                onPathMoved = { offset -> viewModel.movePath(offset) },
                onPathEnded = { viewModel.endPath() },
                backgroundImage = viewModel.backgroundImage
            )

            // Floating Top Studio Glass Bar
            StudioTopBar(
                canUndo = viewModel.paths.isNotEmpty(),
                canRedo = viewModel.undonePaths.isNotEmpty(),
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                paperStyle = viewModel.paperStyle,
                onOpenPaperPicker = { viewModel.showBrushSettingsSheet = true },
                selectedColor = viewModel.selectedColor,
                strokeWidth = viewModel.strokeWidth,
                isEraserMode = viewModel.isEraserMode,
                onOpenBrushSheet = { viewModel.showBrushSettingsSheet = true },
                hasBackgroundImage = viewModel.backgroundImage != null,
                onRemoveBackgroundImage = { viewModel.updateBackgroundImage(null) },
                onClearCanvas = { viewModel.showClearConfirmDialog = true },
                onSave = { handleSaveDrawing() },
                onShare = { handleShareDrawing() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
            )

            // Floating Bottom Studio Glass Dock
            StudioBottomDock(
                isEraserMode = viewModel.isEraserMode,
                onToggleEraser = { viewModel.toggleEraserMode() },
                selectedColor = viewModel.selectedColor,
                onColorSelected = { color -> viewModel.updateSelectedColor(color) },
                onOpenBrushSheet = { viewModel.showBrushSettingsSheet = true },
                onPickGalleryImage = { galleryLauncher.launch("image/*") },
                onCaptureCameraPhoto = { launchCamera() },
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
                    paperStyle = viewModel.paperStyle,
                    onPaperStyleChanged = { style -> viewModel.updatePaperStyle(style) },
                    isEraserMode = viewModel.isEraserMode
                )
            }
        }
    }
}

fun saveImageToMediaStore(
    context: Context,
    paths: List<PathData>,
    backgroundImage: Bitmap?,
    size: IntSize,
    paperStyle: PaperStyle
): Uri? {
    if (size.width <= 0 || size.height <= 0) return null

    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
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
        val src = Rect(0, 0, it.width, it.height)
        val dst = Rect(0, 0, size.width, size.height)
        canvas.drawBitmap(it, src, dst, null)
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
