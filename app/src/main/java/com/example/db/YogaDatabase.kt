package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [YogaSession::class, ReminderEntity::class, FavoriteFlow::class], version = 6, exportSchema = false)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun yogaSessionDao(): YogaSessionDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: YogaDatabase? = null

        fun getDatabase(context: Context): YogaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YogaDatabase::class.java,
                    "yoga_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
