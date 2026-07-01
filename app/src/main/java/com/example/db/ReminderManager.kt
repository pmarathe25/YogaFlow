package com.example.db

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.AlarmReceiver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val database: YogaDatabase,
    private val repository: YogaSessionRepository
) {
    private val PREFS_NAME = "reminder_prefs"
    private val KEY_REMINDERS = "reminders_backup"

    val allReminders: StateFlow<List<ReminderEntity>> = database.reminderDao().getAllReminders().stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteFlowIds: StateFlow<List<String>> = repository.favoriteFlowIds.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addReminder(reminder: ReminderEntity, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            val existingReminders = database.reminderDao().getRemindersForFlow(reminder.flowId).first()
            val newDays = reminder.daysOfWeek.split(",").filter { it.isNotEmpty() }.toSet()
            
            val actualIsSubset = existingReminders.any {
                val existingDays = it.daysOfWeek.split(",").filter { d -> d.isNotEmpty() }.toSet()
                it.hour == reminder.hour &&
                it.minute == reminder.minute &&
                (if (newDays.isEmpty()) existingDays.isEmpty() else existingDays.containsAll(newDays))
            }
            
            if (!actualIsSubset) {
                val id = database.reminderDao().insert(reminder).toInt()
                val reminderWithId = reminder.copy(id = id)
                scheduleNextAlarm(context, reminderWithId)
                
                val updatedReminders = database.reminderDao().getAllReminders().first()
                saveRemindersBackup(context, updatedReminders)
                
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        scope.launch {
            database.reminderDao().update(reminder)
            scheduleNextAlarm(context, reminder)
            
            val updatedReminders = database.reminderDao().getAllReminders().first()
            saveRemindersBackup(context, updatedReminders)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        scope.launch {
            cancelAlarm(context, reminder.id)
            database.reminderDao().delete(reminder)
            
            val updatedReminders = database.reminderDao().getAllReminders().first()
            saveRemindersBackup(context, updatedReminders)
        }
    }

    fun toggleFavoriteFlow(flowId: String) {
        scope.launch {
            val isCurrentlyFavorite = favoriteFlowIds.value.contains(flowId)
            repository.toggleFavorite(flowId, !isCurrentlyFavorite)
        }
    }

    fun rescheduleAllReminders() {
        scope.launch(Dispatchers.IO) {
            try {
                val dbReminders = database.reminderDao().getAllReminders().first()
                val backupReminders = getRemindersBackup(context)
                
                if (dbReminders.isEmpty() && backupReminders.isNotEmpty()) {
                    for (backup in backupReminders) {
                        val newReminder = ReminderEntity(
                            flowId = backup.flowId,
                            flowName = backup.flowName,
                            hour = backup.hour,
                            minute = backup.minute,
                            daysOfWeek = backup.daysOfWeek
                        )
                        val newId = database.reminderDao().insert(newReminder).toInt()
                        val reminderWithId = newReminder.copy(id = newId)
                        scheduleNextAlarm(context, reminderWithId)
                    }
                    val updatedReminders = database.reminderDao().getAllReminders().first()
                    saveRemindersBackup(context, updatedReminders)
                } else {
                    for (reminder in dbReminders) {
                        scheduleNextAlarm(context, reminder)
                    }
                    saveRemindersBackup(context, dbReminders)
                }
            } catch (e: Exception) {
                Log.e("ReminderManager", "Failed to restore/reschedule reminders: ${e.message}")
            }
        }
    }

    private fun saveRemindersBackup(context: Context, reminders: List<ReminderEntity>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(reminders)
        prefs.edit().putString(KEY_REMINDERS, json).apply()
    }

    private fun getRemindersBackup(context: Context): List<ReminderEntity> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        val type = object : TypeToken<List<ReminderEntity>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun scheduleNextAlarm(context: Context, reminder: ReminderEntity) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("FLOW_ID", reminder.flowId)
                putExtra("FLOW_NAME", reminder.flowName)
                putExtra("REMINDER_ID", reminder.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, reminder.hour)
                set(Calendar.MINUTE, reminder.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
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
}
