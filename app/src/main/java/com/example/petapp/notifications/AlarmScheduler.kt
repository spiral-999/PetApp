// CLASSE PARA PREPARAR AS NOTIFICAÇÕES
package com.example.petapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(
        reminderId: Int, // ID único do lembrete
        time: LocalDateTime,
        title: String,
        message: String,
        channelId: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_title", title)
            putExtra("notification_message", message)
            putExtra("notification_channel", channelId)
            putExtra("notification_id", reminderId) // Passa o ID para o receiver
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId, // Usa o ID único como Request Code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Agenda o alarme
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}