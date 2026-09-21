package com.ovalvoi.andropods.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PodsRepository
import com.ovalvoi.andropods.ui.MainActivity

/** Builds the ongoing status notification and the case-opened alert. */
object PodsNotifications {

    private const val TAG = "PodsNotifications"

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

    /**
     * The ongoing status notification.
     *
     * Uses a custom content view -- three battery rings on one row -- rather
     * than the standard template, whose single text line wrapped onto two
     * rows once it held three labelled percentages.
     *
     * [setContentTitle] and [setContentText] are still set. They are what the
     * platform falls back to when a launcher ignores the custom view, and
     * they are what a screen reader and Android Auto read, so the readout
     * stays available even where the rings are not.
     */
    fun ongoing(context: Context, state: PodsState?, lastCase: LastCaseReading? = null): Notification {
        val name = PodsRepository.deviceName.value ?: context.getString(R.string.app_name)
        val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText(
                state?.let { summary(context, it, lastCase) }
                    ?: context.getString(R.string.status_searching)
            )
            .setContentIntent(launchIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        // Only once there is something to draw: a row of three empty rings
        // while searching says less than the text line does.
        //
        // Wrapped because this notification is the foreground service's, and
        // the service dies if building it throws. A custom view is the one
        // part here that can fail on an unusual device or OEM skin, and the
        // standard template built above is a complete fallback -- so a
        // failure costs the rings, not the app.
        if (state != null) {
            try {
                val content = batteryRow(context, name, state, lastCase)
                builder
                    .setCustomContentView(content)
                    .setCustomBigContentView(content)
                    // DecoratedCustom keeps the system header -- app name,
                    // time, expand chevron -- and lets the platform re-colour
                    // the view for the current shade, which a bare custom
                    // view does not get.
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            } catch (e: Exception) {
                Log.w(TAG, "Custom notification view failed; using the text template", e)
            }
        }

        return builder.build()
    }

    /**
     * The custom row: the device name, then one ring per component.
     *
     * Each ring is a Bitmap carrying both the gauge and its number, so the
     * row is three images and three labels rather than an alternating run of
     * views that wraps differently on every launcher.
     */
    private fun batteryRow(
        context: Context,
        name: String,
        state: PodsState,
        lastCase: LastCaseReading?,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.notification_pods).apply {
        setTextViewText(R.id.notification_title, name)

        setImageViewBitmap(R.id.glyph_left, BatteryGlyph.render(context, state.leftBattery))
        setImageViewBitmap(R.id.glyph_right, BatteryGlyph.render(context, state.rightBattery))

        // Out of the case the beacon cannot report its level, so fall back to
        // the remembered one, marked with a "~" exactly as the app does.
        val isLiveCase = state.caseBattery != null
        setImageViewBitmap(
            R.id.glyph_case,
            BatteryGlyph.render(
                context = context,
                level = state.caseBattery ?: lastCase?.level,
                isCase = true,
                isRemembered = !isLiveCase && lastCase != null,
            ),
        )
    }

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
