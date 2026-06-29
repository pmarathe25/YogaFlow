package com.example.db

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.AlarmReceiver
import java.util.Calendar

object ReminderManager {
    private const val PREFS_NAME = "reminder_backup"
    private const val KEY_REMINDERS = "backup_reminders"

    fun saveRemindersBackup(context: Context, reminders: List<ReminderEntity>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serializedSet = reminders.map { reminder ->
            "${reminder.id}|${reminder.flowId}|${reminder.flowName.replace('|', ' ')}|${reminder.hour}|${reminder.minute}|${reminder.daysOfWeek}"
        }.toSet()
        prefs.edit().putStringSet(KEY_REMINDERS, serializedSet).apply()
        Log.d("ReminderManager", "Saved ${reminders.size} reminders to backup SharedPreferences.")
    }

    fun getRemindersBackup(context: Context): List<ReminderEntity> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serializedSet = prefs.getStringSet(KEY_REMINDERS, null) ?: return emptyList()
        val reminders = serializedSet.mapNotNull { serialized ->
            try {
                val parts = serialized.split("|")
                if (parts.size >= 6) {
                    ReminderEntity(
                        id = parts[0].toInt(),
                        flowId = parts[1],
                        flowName = parts[2],
                        hour = parts[3].toInt(),
                        minute = parts[4].toInt(),
                        daysOfWeek = parts[5]
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        Log.d("ReminderManager", "Retrieved ${reminders.size} reminders from backup SharedPreferences.")
        return reminders
    }

    fun scheduleNextAlarm(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MESSAGE", "Time for your practice: ${reminder.flowName}")
            putExtra("REMINDER_ID", reminder.id)
            putExtra("FLOW_ID", reminder.flowId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, reminder.hour)
        calendar.set(Calendar.MINUTE, reminder.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val days = reminder.daysOfWeek.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        
        if (days.isEmpty()) {
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Find the next matching day
            var daysToAdd = 0
            while (daysToAdd < 8) {
                if (calendar.timeInMillis > now && days.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                    break
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                daysToAdd++
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("ReminderManager", "Scheduled reminder ${reminder.id} at ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("ReminderManager", "Permission missing for exact alarms")
        }
    }

    fun cancelAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
