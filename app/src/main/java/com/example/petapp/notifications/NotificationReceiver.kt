// CLASSE PARA O RECEBIMENTO DE NOTIFICAÇÕES
package com.example.petapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("notification_title") ?: "Lembrete do PetApp"
        val message = intent.getStringExtra("notification_message") ?: "Você tem um novo lembrete!"
        val channelId = intent.getStringExtra("notification_channel") ?: NotificationHelper.LOW_PRIORITY_CHANNEL_ID

        // Usa o ID passado pelo Intent, com um fallback para o horário atual
        val notificationId = intent.getIntExtra("notification_id", System.currentTimeMillis().toInt())

        val notificationHelper = NotificationHelper(context)

        // Garante que os canais estão criados. Não há problema em chamar isso aqui.
        notificationHelper.createNotificationChannels()

        notificationHelper.sendNotification(channelId, title, message, notificationId)
    }
}