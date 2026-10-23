package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val reminders = db.appDao().getAllRemindersList()
                    for (r in reminders) {
                        if (r.isNotificationEnabled && !r.isCompleted) {
                            scheduleNotification(context, r)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Error rescheduling reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val isAr = com.example.util.Tr.forcedLanguage == "ar" || com.example.util.I18n.isRtl()
        val defaultTitle = if (isAr) "تذكير ClassHeure" else "Rappel ClassHeure"
        val defaultDesc = if (isAr) "حان الوقت لتنظيم مهامك وأنشطتك !" else "Il est temps de s'organiser !"
        val title = intent.getStringExtra("reminder_title")?.let { com.example.util.Tr.translateNotificationMessage(it, isAr) } ?: defaultTitle
        val desc = intent.getStringExtra("reminder_desc")?.let { com.example.util.Tr.translateNotificationMessage(it, isAr) } ?: defaultDesc
        val reminderId = intent.getIntExtra("reminder_id", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (isAr) "تذكيرات ClassHeure" else "Rappels ClassHeure"
            val channelDesc = if (isAr) "إشعارات تذكيرات تطبيق ClassHeure للأستاذ" else "Notification des rappels de ClassHeure"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "reminders")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId, notification)
    }

    companion object {
        fun scheduleNotification(context: Context, reminder: ReminderEntity) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
                    putExtra("reminder_title", reminder.title)
                    putExtra("reminder_desc", reminder.description)
                    putExtra("reminder_id", reminder.id)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val parts = reminder.date.split("-")
                val timeParts = reminder.time.split(":")
                if (parts.size == 3 && timeParts.size == 2) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, parts[0].toInt())
                        set(Calendar.MONTH, parts[1].toInt() - 1)
                        set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                    }

                    if (calendar.timeInMillis > System.currentTimeMillis()) {
                        var scheduled = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            try {
                                alarmManager.setAlarmClock(
                                    AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                                    pendingIntent
                                )
                                scheduled = true
                            } catch (e: Exception) {
                                Log.w("ReminderReceiver", "setAlarmClock failed, falling back to setExactAndAllowWhileIdle", e)
                            }
                        }

                        if (!scheduled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        calendar.timeInMillis,
                                        pendingIntent
                                    )
                                } catch (se: SecurityException) {
                                    Log.w("ReminderReceiver", "Exact alarm permission restricted, falling back to set()", se)
                                    alarmManager.set(
                                        AlarmManager.RTC_WAKEUP,
                                        calendar.timeInMillis,
                                        pendingIntent
                                    )
                                }
                            } else {
                                try {
                                    alarmManager.setExact(
                                        AlarmManager.RTC_WAKEUP,
                                        calendar.timeInMillis,
                                        pendingIntent
                                    )
                                } catch (se: SecurityException) {
                                    alarmManager.set(
                                        AlarmManager.RTC_WAKEUP,
                                        calendar.timeInMillis,
                                        pendingIntent
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to schedule notification", e)
            }
        }

        fun cancelNotification(context: Context, id: Int) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, ReminderNotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to cancel notification", e)
            }
        }
    }
}
