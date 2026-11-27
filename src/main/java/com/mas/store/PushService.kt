package com.mas.store

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

object Push {
    const val CHANNEL = "store_updates"
    const val TOPIC = "store"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "New apps and updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    /** Safe when Firebase is not configured (no google-services.json): does nothing. */
    fun subscribe() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
        } catch (_: Throwable) {
        }
    }
}

class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        val d = msg.data
        val title = d["title"] ?: msg.notification?.title ?: return
        val body = d["body"] ?: msg.notification?.body ?: ""
        val appId = d["appId"]
        Push.ensureChannel(this)
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (!appId.isNullOrEmpty()) i.putExtra("open", appId)
        val pi = PendingIntent.getActivity(
            this, (appId ?: "").hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, Push.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            getSystemService(NotificationManager::class.java).notify((appId ?: title).hashCode(), n)
        } catch (_: SecurityException) {
        }
    }
}
