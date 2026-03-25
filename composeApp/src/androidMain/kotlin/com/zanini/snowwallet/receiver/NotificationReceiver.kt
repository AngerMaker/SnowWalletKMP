package com.zanini.snowwallet.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zanini.snowwallet.MainActivity

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getIntExtra("EXTRA_NOTIFICATION_ID", 1) ?: 1
        val title = intent?.getStringExtra("EXTRA_TITLE") ?: "Lembrete"
        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: "Você tem um novo compromisso."

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "snowwallet_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lembretes e Notificações",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de lembretes e agendamentos"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // O Android requer ícones monocromáticos (alpha) para a Notification Bar.
        // O ic_launcher (mipmap) contém fundo sólido, por isso o Android renderiza como um "círculo cinza" (O).
        // Usamos o ic_stat_wallet criado perfeitamente para Lembretes do SnowWallet
        val resourceId = context.resources.getIdentifier("ic_stat_wallet", "drawable", context.packageName)
        val icon = if (resourceId != 0) resourceId else android.R.drawable.ic_popup_reminder

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
