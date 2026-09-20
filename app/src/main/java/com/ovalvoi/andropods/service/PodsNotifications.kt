package com.ovalvoi.andropods.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.ui.MainActivity

/** Builds the ongoing status notification and the case-opened alert. */
object PodsNotifications {

    const val ONGOING_CHANNEL_ID = "pods_status"
    const val LID_CHANNEL_ID = "pods_lid"
    const val ONGOING_NOTIFICATION_ID = 1
    const val LID_NOTIFICATION_ID = 2

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // MIN keeps the mandatory FGS notification quiet and collapsed -- it is
        // a status readout, not an alert.
        manager.createNotificationChannel(
            NotificationChannel(
                ONGOING_CHANNEL_ID,
                context.getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.service_channel_description)
                setShowBadge(false)
            }
        )

        // LOW: visible and transient, but silent. A case-open popup that buzzes
        // every time would be worse than no popup.
        manager.createNotificationChannel(
            NotificationChannel(
                LID_CHANNEL_ID,
                context.getString(R.string.lid_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.lid_channel_description)
                setShowBadge(false)
            }
        )
    }

    fun ongoing(context: Context, state: PodsState?, lastCase: LastCaseReading? = null): Notification =
        NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                state?.let { summary(context, it, lastCase) }
                    ?: context.getString(R.string.status_searching)
            )
            .setContentIntent(launchIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

    /**
     * @param timeoutMs dismiss by itself after this long, or null to stay until
     *   swiped. Comes from Settings; the popup is informational either way.
     */
    fun lidOpened(
        context: Context,
        state: PodsState,
        lastCase: LastCaseReading?,
        timeoutMs: Long?,
    ): Notification =
        NotificationCompat.Builder(context, LID_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(summary(context, state, lastCase))
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .setSilent(true)
            .apply { if (timeoutMs != null) setTimeoutAfter(timeoutMs) }
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    /**
     * e.g. "Left 80%  ·  Right 70%  ·  Case 90%".
     *
     * A remembered case level renders as "Case ~90%": the pods are out of the
     * case, so this is the last reading, not a live one. Never seen renders
     * as a dash, never as 0%.
     */
    private fun summary(context: Context, state: PodsState, lastCase: LastCaseReading?): String {
        val unknown = context.getString(R.string.battery_unknown)
        fun level(value: Int?) = value?.let { "$it%" } ?: unknown
        val case = state.caseBattery?.let { "$it%" }
            ?: lastCase?.let { "~${it.level}%" }
            ?: unknown
        return buildString {
            append(context.getString(R.string.pod_left)).append(' ').append(level(state.leftBattery))
            append("  ·  ")
            append(context.getString(R.string.pod_right)).append(' ').append(level(state.rightBattery))
            append("  ·  ")
            append(context.getString(R.string.pod_case)).append(' ').append(case)
        }
    }

    private fun launchIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
