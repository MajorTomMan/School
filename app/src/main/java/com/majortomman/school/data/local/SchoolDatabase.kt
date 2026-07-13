package com.majortomman.school.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PracticeAttemptEntity::class,
        ReviewScheduleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SchoolDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao

    companion object {
        @Volatile
        private var instance: SchoolDatabase? = null

        fun getInstance(context: Context): SchoolDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SchoolDatabase::class.java,
                "school.db",
            ).build().also { instance = it }
        }
    }
}
