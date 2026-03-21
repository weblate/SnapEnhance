package me.rhunk.snapenhance

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import me.rhunk.snapenhance.ui.manager.data.updater.UpdateFetcher

class UpdateNotification : BroadcastReceiver() {
    companion object {
        private fun getPendingIntent(context: Context, noCreate: Boolean = false): PendingIntent? {
            val intent = Intent(context, UpdateNotification::class.java)
            val flags = if (noCreate) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }
            return PendingIntent.getBroadcast(context, 0, intent, flags)
        }

        fun updateCheckRepeater(context: Context, force: Boolean = false) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val remoteContext = RemoteSideContext(context)
            val existingIntent = getPendingIntent(context, noCreate = true)
            if (existingIntent != null && !force) return
            val isEnabled = remoteContext.sharedPreferences.getBoolean("app_update_checker", true)
            if (!isEnabled) {
                existingIntent?.let { alarmManager.cancel(it) }
                return
            }
            val fetchInterval = remoteContext.sharedPreferences.getString("app_update_check_interval", "daily")
            val intervalMillis = when (fetchInterval) {
                "daily" -> 86400000L
                "weekly" -> 604800000L
                "monthly" -> 2592000000L
                else -> 86400000L
            }
            val pendingIntent = getPendingIntent(context, noCreate = false)!!
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                5000L,
                intervalMillis,
                pendingIntent
            )
        }

        fun showUpdateNotification(context: Context, update: UpdateFetcher.LatestRelease) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "app_update"
            val remoteContext = RemoteSideContext(context)
            remoteContext.translation.load()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "App Updates", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for new SE-Extended builds"
                }
                manager.createNotificationChannel(channel)
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.launcher_icon_monochrome)
                .setContentTitle(remoteContext.translation["manager.update_notification.content_title"])
                .setContentText(remoteContext.translation.format("manager.update_notification.content_description", "versionName" to update.versionName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(1001, notification)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        UpdateFetcher.checkForUpdates(context)
    }
}