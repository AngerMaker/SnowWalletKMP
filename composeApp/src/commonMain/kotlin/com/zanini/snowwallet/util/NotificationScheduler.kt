package com.zanini.snowwallet.util

interface NotificationScheduler {
    fun scheduleNotification(id: Int, title: String, message: String, timeInMillis: Long)
    fun cancelNotification(id: Int)
}
