package com.swapnull.drawingpro

import android.app.Application
import com.swapnull.drawingpro.database.DrawingDatabase

class DrawingProApplication : Application() {
    val database by lazy { DrawingDatabase.getDatabase(this) }
    val repository by lazy { DrawingRepository(database.drawingDao()) }
}
