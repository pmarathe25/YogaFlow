package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.db.YogaDatabase
import com.example.db.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.MY_PACKAGE_REPLACED") {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = YogaDatabase.getDatabase(appContext)
                    val reminders = db.reminderDao().getAllReminders().first()
                    for (reminder in reminders) {
                        ReminderManager.scheduleNextAlarm(appContext, reminder)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AlarmReceiver", "Failed to reschedule on boot/upgrade: ${e.message}")
                }
            }
            return
        }

        val message = intent.getStringExtra("MESSAGE") ?: "Time for your Yoga Flow!"
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val flowId = intent.getStringExtra("FLOW_ID")
        
        showNotification(context, message, reminderId, flowId)

        if (reminderId != -1) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = YogaDatabase.getDatabase(appContext)
                    val reminder = db.reminderDao().getReminderById(reminderId)
                    if (reminder != null) {
                        ReminderManager.scheduleNextAlarm(appContext, reminder)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AlarmReceiver", "Failed to reschedule alarm $reminderId: ${e.message}")
                }
            }
        }
    }

    private fun showNotification(context: Context, message: String, reminderId: Int, flowId: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("yoga_reminder", "Yoga Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        // Build Intent to launch MainActivity and jump to the flow
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            this.action = "OPEN_FLOW"
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            this.putExtra("FLOW_ID", flowId)
        }

        val requestCode = if (reminderId != -1) reminderId else System.currentTimeMillis().toInt()
        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, "yoga_reminder")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Yoga Reminder")
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
