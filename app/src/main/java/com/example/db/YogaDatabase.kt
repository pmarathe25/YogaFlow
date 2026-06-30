package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [YogaSession::class, FavoriteFlow::class, ReminderEntity::class, GardenItemEntity::class], version = 5, exportSchema = false)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun yogaSessionDao(): YogaSessionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun gardenItemDao(): GardenItemDao

    companion object {
        @Volatile
        private var INSTANCE: YogaDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        fun getDatabase(context: Context): YogaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YogaDatabase::class.java,
                    "yoga_database"
                ).addMigrations(MIGRATION_4_5).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

