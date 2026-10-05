package com.swapnull.drawingpro.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class DrawingProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val updatedAt: Long,
    val pathsJson: String,
    val paperStyle: String
)
