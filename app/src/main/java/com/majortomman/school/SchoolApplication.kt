package com.majortomman.school

import android.app.Application
import com.majortomman.school.startup.SchoolLearningModules

class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SchoolLearningModules.install()
    }
}
