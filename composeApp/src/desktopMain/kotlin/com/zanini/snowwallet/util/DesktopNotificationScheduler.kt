package com.zanini.snowwallet.util

class DesktopNotificationScheduler : NotificationScheduler {
    override fun scheduleNotification(id: Int, title: String, message: String, timeInMillis: Long) {
        // No Desktop, poderíamos usar java.awt.TrayIcon ou notificações do S.O.
        // Como o app é voltado primordialmente para Android, manteremos stub inicialmente.
        println("Simulação Servidor Desktop: Notificação Agendada -> $title para $timeInMillis")
    }

    override fun cancelNotification(id: Int) {
        println("Simulação Servidor Desktop: Notificação Cancelada -> ID: $id")
    }
}
