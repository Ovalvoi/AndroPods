package com.ovalvoi.andropods.ui.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * The "Display over other apps" grant, which the connect popup needs to draw
 * outside the app.
 *
 * This one is not a runtime permission: there is no dialog to request, only a
 * Settings page to send the user to, and the result arrives as a changed
 * [canDrawOverlays] rather than a callback. Hence the plain object instead of
 * an ActivityResult contract.
 */
object OverlayPermission {

    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Open the per-app overlay settings page.
     *
     * NEW_TASK is required because this can be called from a non-Activity
     * context, and harmless when it is not.
     */
    fun requestIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
