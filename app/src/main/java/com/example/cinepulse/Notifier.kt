package com.example.cinepulse

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** App settings saved on the phone (dark mode, notifications on/off). */
object AppPrefs {
    const val FILE = "cinepulse_settings"
    const val KEY_DARK = "dark_mode"
    const val KEY_NOTIFICATIONS = "notifications_enabled"

    fun get(ctx: Context): SharedPreferences = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

/** Creates the notification channel and shows local notifications. */
object Notifier {
    private const val TAG = "Notifier"
    const val CHANNEL_ID = "cinepulse_updates"   // same id as in AndroidManifest.xml

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = ctx.getString(R.string.channel_desc) }
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun isEnabled(ctx: Context): Boolean =
        AppPrefs.get(ctx).getBoolean(AppPrefs.KEY_NOTIFICATIONS, true)

    fun setEnabled(ctx: Context, enabled: Boolean) {
        AppPrefs.get(ctx).edit().putBoolean(AppPrefs.KEY_NOTIFICATIONS, enabled).apply()
    }

    /** Shows a notification. Tapping it opens the app. */
    @SuppressLint("MissingPermission")
    fun show(ctx: Context, title: String, body: String) {
        if (!isEnabled(ctx)) {
            Log.d(TAG, "Notifications switched off in Settings")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "No notification permission - skipped: $title")
            return
        }

        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_star)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "Notification shown: $title")
    }
}