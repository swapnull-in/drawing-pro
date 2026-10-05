package com.swapnull.drawingpro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.swapnull.drawingpro.database.DrawingDao
import com.swapnull.drawingpro.database.DrawingProject
import com.swapnull.drawingpro.model.ActionType
import com.swapnull.drawingpro.model.SerializablePathData
import kotlinx.coroutines.flow.Flow

class DrawingRepository(private val drawingDao: DrawingDao) {

    val allProjects: Flow<List<DrawingProject>> = drawingDao.getAllProjects()

    suspend fun saveProject(
        id: Long = 0L,
        name: String,
        paths: List<PathData>,
        paperStyle: PaperStyle
    ): Long {
        val serializablePaths = paths.map { pathData ->
            SerializablePathData(
                actions = pathData.actions,
                colorArgb = pathData.color.toArgb(),
                strokeWidth = pathData.strokeWidth,
                isEraser = pathData.isEraser,
                brushType = pathData.brushType.name
            )
        }

        val json = Gson().toJson(serializablePaths)
        
        val project = DrawingProject(
            id = id,
            name = name,
            updatedAt = System.currentTimeMillis(),
            pathsJson = json,
            paperStyle = paperStyle.name
        )
        return drawingDao.insertProject(project)
    }

    suspend fun loadProjectPaths(project: DrawingProject): Pair<List<PathData>, PaperStyle> {
        val listType = object : TypeToken<List<SerializablePathData>>() {}.type
        val serializablePaths: List<SerializablePathData> = Gson().fromJson(project.pathsJson, listType) ?: emptyList()

        val paths = serializablePaths.map { sPath ->
            val path = Path()
            sPath.actions.forEach { action ->
                when (action.type) {
                    ActionType.MOVE_TO -> path.moveTo(action.x1, action.y1)
                    ActionType.LINE_TO -> path.lineTo(action.x1, action.y1)
                    ActionType.QUAD_TO -> path.quadraticTo(action.x1, action.y1, action.x2, action.y2)
                    ActionType.ADD_RECT -> path.addRect(androidx.compose.ui.geometry.Rect(action.x1, action.y1, action.x2, action.y2))
                    ActionType.ADD_OVAL -> path.addOval(androidx.compose.ui.geometry.Rect(action.x1, action.y1, action.x2, action.y2))
                    ActionType.RESET -> path.reset()
                }
            }

            PathData(
                path = path,
                actions = sPath.actions,
                color = Color(sPath.colorArgb),
                strokeWidth = sPath.strokeWidth,
                isEraser = sPath.isEraser,
                brushType = BrushType.valueOf(sPath.brushType)
            )
        }

        val style = try {
            PaperStyle.valueOf(project.paperStyle)
        } catch (e: Exception) {
            PaperStyle.PLAIN
        }

        return Pair(paths, style)
    }

    suspend fun deleteProject(project: DrawingProject) {
        drawingDao.deleteProject(project)
    }
}
