package com.example.assignmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.assignmate.model.Notification // Ensure you have this model or create a simple map
import com.google.firebase.database.FirebaseDatabase

class NotificationHelper(private val context: Context) {

    private val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java) as NotificationManager

    // FIX: Replaced DatabaseHelper with FirebaseDatabase
    private val db = FirebaseDatabase.getInstance().reference

    companion object {
        const val CHANNEL_ID = "assignmate_channel_id"
        const val CHANNEL_NAME = "AssignMate Notifications"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for AssignMate"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // FIX: Changed userId type from Int to String (Firebase uses String IDs)
    fun sendNotification(userId: String, title: String, message: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // 1. Save to Firebase instead of SQLite
        saveNotificationToFirebase(userId, title, message)

        // 2. Show System Notification
        notificationManager.notify(notificationId, builder.build())
    }

    private fun saveNotificationToFirebase(userId: String, title: String, message: String) {
        // Generate a unique key for the notification
        val notifRef = db.child("Notifications").child(userId).push()
        val notifId = notifRef.key ?: return

        val notificationData = mapOf(
            "id" to notifId,
            "userId" to userId,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        notifRef.setValue(notificationData)
    }
}