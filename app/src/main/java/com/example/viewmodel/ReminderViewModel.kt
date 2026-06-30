package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.ReminderEntity
import com.example.db.ReminderManager
import com.example.db.YogaDatabase
import com.example.db.YogaSessionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = YogaDatabase.getDatabase(application)
    private val repository = YogaSessionRepository(database.yogaSessionDao())

    val allReminders = database.reminderDao().getAllReminders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteFlowIds = repository.favoriteFlowIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addReminder(reminder: ReminderEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
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
                ReminderManager.scheduleNextAlarm(getApplication(), reminderWithId)
                
                val updatedReminders = database.reminderDao().getAllReminders().first()
                ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
                
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            database.reminderDao().update(reminder)
            ReminderManager.scheduleNextAlarm(getApplication(), reminder)
            
            val updatedReminders = database.reminderDao().getAllReminders().first()
            ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            ReminderManager.cancelAlarm(getApplication(), reminder.id)
            database.reminderDao().delete(reminder)
            
            val updatedReminders = database.reminderDao().getAllReminders().first()
            ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
        }
    }

    fun toggleFavoriteFlow(flowId: String) {
        viewModelScope.launch {
            val isCurrentlyFavorite = favoriteFlowIds.value.contains(flowId)
            repository.toggleFavorite(flowId, !isCurrentlyFavorite)
        }
    }

    fun rescheduleAllReminders() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dbReminders = database.reminderDao().getAllReminders().first()
                val backupReminders = ReminderManager.getRemindersBackup(getApplication())
                
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
                        ReminderManager.scheduleNextAlarm(getApplication(), reminderWithId)
                    }
                    val updatedReminders = database.reminderDao().getAllReminders().first()
                    ReminderManager.saveRemindersBackup(getApplication(), updatedReminders)
                } else {
                    for (reminder in dbReminders) {
                        ReminderManager.scheduleNextAlarm(getApplication(), reminder)
                    }
                    ReminderManager.saveRemindersBackup(getApplication(), dbReminders)
                }
            } catch (e: Exception) {
                android.util.Log.e("ReminderViewModel", "Failed to restore/reschedule reminders: ${e.message}")
            }
        }
    }
}