package com.goldenthumb.android.chess

import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService

@RequiresApi(Build.VERSION_CODES.O)
class Notifications() {

    private val NOTIFIYTAG = "Chess"

    fun notify(context: Context, msg: String, n: Int) {

        val intent = Intent(context, Login::class.java)

        val notificationManager: NotificationManager =
                getSystemService(context, NotificationManager::class.java) as NotificationManager
        val mChannel = NotificationChannel("Chess", "Chess", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(mChannel)

        val builder = NotificationCompat.Builder(context, "Chess")
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle("♟️New Chess challenge ♟️")
                .setContentText(msg)
                .setNumber(n)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)

        val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ECLAIR) nm.notify(NOTIFIYTAG, 0, builder.build())
        else nm.notify(NOTIFIYTAG.hashCode(), builder.build())
    }
}
