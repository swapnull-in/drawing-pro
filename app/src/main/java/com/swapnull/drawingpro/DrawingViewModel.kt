package com.swapnull.drawingpro

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlin.math.hypot
import androidx.compose.ui.geometry.Rect
import com.swapnull.drawingpro.database.DrawingProject
import com.swapnull.drawingpro.model.ActionType
import com.swapnull.drawingpro.model.DrawAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DrawingMode {
    FREEHAND, LINE, RECTANGLE, CIRCLE
}

class DrawingViewModel(private val repository: DrawingRepository) : ViewModel() {

    private val _projects = MutableStateFlow<List<DrawingProject>>(emptyList())
    val projects: StateFlow<List<DrawingProject>> = _projects.asStateFlow()

    var currentProjectId by mutableLongStateOf(0L)
        private set

    init {
        viewModelScope.launch {
            repository.allProjects.collect { projectList ->
                _projects.value = projectList
            }
        }
    }

    val paths = mutableStateListOf<PathData>()
    val undonePaths = mutableStateListOf<PathData>()

    var currentPath by mutableStateOf<Path?>(null)
        private set

    var pathUpdateTrigger by mutableLongStateOf(0L)
        private set

    var isEraserMode by mutableStateOf(false)
        private set

    var selectedColor by mutableStateOf(Color.Black)
        private set

    var currentBrushType by mutableStateOf(BrushType.PEN)
        private set

    var drawingMode by mutableStateOf(DrawingMode.FREEHAND)
        private set

    var strokeWidth by mutableFloatStateOf(8f)
        private set

    var paperStyle by mutableStateOf(PaperStyle.PLAIN)
        private set

    var backgroundImage by mutableStateOf<Bitmap?>(null)
        private set

    var backgroundOpacity by mutableFloatStateOf(0.7f)
        private set

    var exportTransparentBg by mutableStateOf(false)
        private set

    var showBrushSettingsSheet by mutableStateOf(false)

    var showClearConfirmDialog by mutableStateOf(false)

    private var previousPoint: Offset? = null
    private var startPoint: Offset? = null
    private var activePathStrokeWidth = 8f
    private var activePathIsEraser = false
    private var activePathColor = Color.Black
    private var activePathBrushType = BrushType.PEN
    private var currentActions = mutableListOf<DrawAction>()

    fun startPath(offset: Offset) {
        undonePaths.clear()
        currentActions.clear()
        activePathStrokeWidth = if (isEraserMode) 60f else strokeWidth
        activePathIsEraser = isEraserMode
        activePathColor = selectedColor
        activePathBrushType = currentBrushType

        val newPath = Path().apply {
            moveTo(offset.x, offset.y)
        }
        currentActions.add(DrawAction(ActionType.MOVE_TO, x1 = offset.x, y1 = offset.y))
        
        currentPath = newPath
        previousPoint = offset
        startPoint = offset
        pathUpdateTrigger++
    }

    fun movePath(offset: Offset) {
        val current = currentPath ?: return
        val start = startPoint ?: return

        when (drawingMode) {
            DrawingMode.FREEHAND -> {
                val prev = previousPoint ?: return
                val midX = (prev.x + offset.x) / 2f
                val midY = (prev.y + offset.y) / 2f
                current.quadraticTo(prev.x, prev.y, midX, midY)
                currentActions.add(DrawAction(ActionType.QUAD_TO, x1 = prev.x, y1 = prev.y, x2 = midX, y2 = midY))
                previousPoint = offset
            }
            DrawingMode.LINE -> {
                current.reset()
                current.moveTo(start.x, start.y)
                current.lineTo(offset.x, offset.y)
                currentActions.clear()
                currentActions.add(DrawAction(ActionType.RESET))
                currentActions.add(DrawAction(ActionType.MOVE_TO, x1 = start.x, y1 = start.y))
                currentActions.add(DrawAction(ActionType.LINE_TO, x1 = offset.x, y1 = offset.y))
            }
            DrawingMode.RECTANGLE -> {
                current.reset()
                val rect = Rect(
                    left = minOf(start.x, offset.x),
                    top = minOf(start.y, offset.y),
                    right = maxOf(start.x, offset.x),
                    bottom = maxOf(start.y, offset.y)
                )
                current.addRect(rect)
                currentActions.clear()
                currentActions.add(DrawAction(ActionType.RESET))
                currentActions.add(DrawAction(ActionType.ADD_RECT, x1 = rect.left, y1 = rect.top, x2 = rect.right, y2 = rect.bottom))
            }
            DrawingMode.CIRCLE -> {
                current.reset()
                val radius = hypot(offset.x - start.x, offset.y - start.y)
                val rect = Rect(
                    left = start.x - radius,
                    top = start.y - radius,
                    right = start.x + radius,
                    bottom = start.y + radius
                )
                current.addOval(rect)
                currentActions.clear()
                currentActions.add(DrawAction(ActionType.RESET))
                currentActions.add(DrawAction(ActionType.ADD_OVAL, x1 = rect.left, y1 = rect.top, x2 = rect.right, y2 = rect.bottom))
            }
        }
        pathUpdateTrigger++
    }

    fun endPath() {
        currentPath?.let { path ->
            paths.add(
                PathData(
                    path = path,
                    actions = currentActions.toList(),
                    color = activePathColor,
                    strokeWidth = activePathStrokeWidth,
                    isEraser = activePathIsEraser,
                    brushType = activePathBrushType
                )
            )
        }
        currentPath = null
        currentActions.clear()
        previousPoint = null
        pathUpdateTrigger++
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            undonePaths.add(paths.removeAt(paths.size - 1))
            pathUpdateTrigger++
        }
    }

    fun redo() {
        if (undonePaths.isNotEmpty()) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            pathUpdateTrigger++
        }
    }

    fun clearCanvas() {
        paths.clear()
        undonePaths.clear()
        currentPath = null
        backgroundImage = null
        currentProjectId = 0L
        pathUpdateTrigger++
    }

    fun toggleEraserMode() {
        isEraserMode = !isEraserMode
        if (isEraserMode) {
            showBrushSettingsSheet = false
        }
    }

    fun updateSelectedColor(color: Color) {
        selectedColor = color
        if (isEraserMode) {
            isEraserMode = false
        }
    }

    fun updateBrushType(type: BrushType) {
        currentBrushType = type
        if (isEraserMode) {
            isEraserMode = false
        }
    }

    fun updateDrawingMode(mode: DrawingMode) {
        drawingMode = mode
        if (isEraserMode && mode != DrawingMode.FREEHAND) {
            isEraserMode = false
        }
    }

    fun updateStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun updatePaperStyle(style: PaperStyle) {
        paperStyle = style
    }

    fun updateBackgroundImage(bitmap: Bitmap?) {
        backgroundImage = bitmap
        pathUpdateTrigger++
    }

    fun updateBackgroundOpacity(opacity: Float) {
        backgroundOpacity = opacity.coerceIn(0.1f, 1.0f)
        pathUpdateTrigger++
    }

    fun toggleExportTransparentBg() {
        exportTransparentBg = !exportTransparentBg
    }

    fun saveCurrentProject(name: String) {
        viewModelScope.launch {
            val id = repository.saveProject(
                id = currentProjectId,
                name = name,
                paths = paths.toList(),
                paperStyle = paperStyle
            )
            currentProjectId = id
        }
    }

    fun loadProject(project: DrawingProject) {
        viewModelScope.launch {
            val (loadedPaths, style) = repository.loadProjectPaths(project)
            paths.clear()
            paths.addAll(loadedPaths)
            undonePaths.clear()
            paperStyle = style
            currentProjectId = project.id
            backgroundImage = null // Or handle background image reloading if stored
            pathUpdateTrigger++
        }
    }

    fun deleteProject(project: DrawingProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (currentProjectId == project.id) {
                clearCanvas()
            }
        }
    }
}

class DrawingViewModelFactory(private val repository: DrawingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DrawingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DrawingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
