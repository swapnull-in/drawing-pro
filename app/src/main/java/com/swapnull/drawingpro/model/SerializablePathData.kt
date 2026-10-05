package com.swapnull.drawingpro.model

data class SerializablePathData(
    val actions: List<DrawAction>,
    val colorArgb: Int,
    val strokeWidth: Float,
    val isEraser: Boolean,
    val brushType: String
)
