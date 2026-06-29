package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val flowId: String,
    val flowName: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String // comma separated string of Calendar.DAY_OF_WEEK integers, e.g. "1,3,5"
)
