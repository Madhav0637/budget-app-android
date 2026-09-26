package com.madhav0637.budgetapp.notifications

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
import com.madhav0637.budgetapp.MainActivity
import com.madhav0637.budgetapp.R
import com.madhav0637.budgetapp.domain.BudgetAlerts

/**
 * Shows budget alerts as notifications. Used when an expense is logged from the quick-entry pop-up, where the app
 * isn't on screen; inside the app the same alert appears as a banner instead.
 */
object BudgetNotifier {
    private const val CHANNEL_ID = "budget_alerts"

    /** Android 13+ needs the user's permission to show notifications; older versions allow them by default. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** True when notifications for the app are switched off (or were never allowed). */
    fun isDenied(context: Context): Boolean = !NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(context: Context, alert: BudgetAlerts.Alert) {
        if (needsPermission(context) || isDenied(context)) return
        createChannel(context)
        val message = BudgetAlerts.message(alert)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_koku)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            // One id per level, so a repeat replaces the old notification instead of stacking.
            NotificationManagerCompat.from(context).notify(alert.level.percent, notification)
        } catch (_: SecurityException) {
            // Permission was withdrawn between the check and now; the alert simply isn't shown.
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "When this month's spending reaches 80% and 100% of your budget"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
