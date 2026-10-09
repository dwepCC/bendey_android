package com.bendey.restaurant.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bendey.restaurant.MainActivity
import com.bendey.restaurant.R
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Canal de los avisos de pedidos (mismo id que manda el backend en `android.notification.channel_id`). */
const val CHANNEL_ORDERS = "pedidos"

object Push {
    /** Hay Firebase solo si se copio google-services.json; sin el todo lo de push es un no-op. */
    fun isAvailable(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ORDERS, "Pedidos del marketplace", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pedido nuevo, sin atender o cancelado por el cliente"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Token actual de FCM, o null si Firebase no esta configurado o falla. */
    suspend fun currentToken(context: Context): String? {
        if (!isAvailable(context)) return null
        return suspendCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
                cont.resume(if (t.isSuccessful) t.result else null)
            }
        }
    }

    fun show(context: Context, title: String, body: String, id: Int) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
