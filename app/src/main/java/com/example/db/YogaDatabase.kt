package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [YogaSession::class, FavoriteFlow::class, ReminderEntity::class, GardenItemEntity::class], version = 4, exportSchema = false)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun yogaSessionDao(): YogaSessionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun gardenItemDao(): GardenItemDao

    companion object {
        @Volatile
        private var INSTANCE: YogaDatabase? = null

        fun getDatabase(context: Context): YogaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YogaDatabase::class.java,
                    "yoga_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

