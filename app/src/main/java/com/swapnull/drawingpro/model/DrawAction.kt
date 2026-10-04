package com.swapnull.drawingpro.model

enum class ActionType {
    MOVE_TO, LINE_TO, QUAD_TO, ADD_RECT, ADD_OVAL, RESET
}

data class DrawAction(
    val type: ActionType,
    val x1: Float = 0f, val y1: Float = 0f,
    val x2: Float = 0f, val y2: Float = 0f
)
