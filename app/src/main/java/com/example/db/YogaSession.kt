package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "yoga_sessions")
data class YogaSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val flowId: String,
    val flowName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int
)
