package com.majortomman.school

import android.app.Application
import com.majortomman.school.visualization.renderers.math.MathematicsVisualizationModule

class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MathematicsVisualizationModule.install()
    }
}
