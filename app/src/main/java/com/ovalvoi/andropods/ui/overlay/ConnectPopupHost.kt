package com.ovalvoi.andropods.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.AppSettings
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PodsRepository
import com.ovalvoi.andropods.data.PopupPosition
import com.ovalvoi.andropods.ui.theme.AndroPodsTheme
import com.ovalvoi.andropods.ui.theme.resolveForOverlay
import kotlin.math.abs

/**
 * Shows [ConnectPopupCard] in a floating system window over whatever app is on
 * screen.
 *
 * A notification cannot be themed, so the popup is a real overlay window. That
 * costs the SYSTEM_ALERT_WINDOW grant, and it means hosting Compose outside an
 * Activity: a [ComposeView] refuses to compose unless the view tree carries a
 * lifecycle, a ViewModelStore and a SavedStateRegistry, none of which a
 * Service provides. [PopupViewTreeOwner] supplies all three.
 *
 * Every public method must run on the main thread; the service calls in from a
 * background dispatcher, so each one hops through [handler] rather than
 * trusting the caller.
 */
class ConnectPopupHost(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(WindowManager::class.java)

    private var owner: PopupViewTreeOwner? = null
    private var composeView: ComposeView? = null

    /**
     * Drives the enter/exit animation; removal waits for the exit to finish.
     *
     * Seeded false and flipped true *after* the composition exists, so
     * AnimatedVisibility sees a real false -> true transition. Setting it
     * before the first composition would compose the card already visible and
     * skip the enter animation, which is what made the popup snap into place.
     */
    private val cardVisibility = MutableTransitionState(false)
    private val autoDismiss = Runnable { dismiss() }
    private val removeAfterExit = Runnable { detach() }

    /**
     * Show the card, or update it in place if it is already up.
     *
     * Safe to call when the overlay permission is missing -- it logs and
     * returns, because the grant can be revoked between the settings toggle
     * and the pods connecting.
     */
    fun show(
        state: PodsState,
        lastCase: LastCaseReading?,
        settings: AppSettings,
    ): Unit = onMainThread {
        if (!OverlayPermission.isGranted(context)) {
            Log.w(TAG, "Overlay permission not granted; skipping popup")
            return@onMainThread
        }
        if (windowManager == null) {
            Log.w(TAG, "No WindowManager; skipping popup")
            return@onMainThread
        }

        // A re-show while the card is still on screen must not restart the
        // window, only its content and its timer.
        handler.removeCallbacks(removeAfterExit)
        handler.removeCallbacks(autoDismiss)

        val view = composeView ?: attach(settings.popupPosition) ?: return@onMainThread
        // Read outside the composition: collecting a flow here would need a
        // scope this window does not have, and the name is fixed for the life
        // of a connection anyway -- it arrives on the ACL edge, before the
        // first beacon that triggers this popup.
        val deviceName = PodsRepository.deviceName.value
        view.setContent {
            AndroPodsTheme(
                colorTheme = settings.colorTheme,
                darkTheme = settings.darkMode.resolveForOverlay(context),
            ) {
                ConnectPopupCard(
                    state = state,
                    lastCase = lastCase,
                    position = settings.popupPosition,
                    visibleState = cardVisibility,
                    onDismiss = ::dismiss,
                    deviceName = deviceName,
                )
            }
        }
        // Posted, not assigned inline: the composition created by
        // setContent above has not run yet on this frame, so flipping the
        // state now would still be the card's initial value. Next frame it is
        // a transition the animation can play.
        handler.post { cardVisibility.targetState = true }

        if (settings.autoDismissPopup) {
            handler.postDelayed(autoDismiss, settings.popupTimeoutMs)
        }
    }

    /** Animate the card out, then take the window down. */
    fun dismiss(): Unit = onMainThread {
        handler.removeCallbacks(autoDismiss)
        if (composeView == null) return@onMainThread
        cardVisibility.targetState = false
        handler.postDelayed(removeAfterExit, EXIT_GRACE_MS)
    }

    /** Tear down immediately, without animating. For service shutdown. */
    fun destroy(): Unit = onMainThread { detach() }

    private fun attach(position: PopupPosition): ComposeView? {
        val treeOwner = PopupViewTreeOwner()
        val view = ComposeView(context).apply {
            // Explicitly transparent. A ComposeView inherits an opaque
            // background from the theme, and because the window is
            // WRAP_CONTENT around a card that slides within it, that showed
            // as a grey rectangle under the card during and after the
            // animation -- visible mainly in light mode, where it did not
            // blend into a dark background.
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(treeOwner)
            setViewTreeViewModelStoreOwner(treeOwner)
            setViewTreeSavedStateRegistryOwner(treeOwner)
            // Tapping outside the card falls through to the app underneath
            // (the window is not touch-modal), but a tap that lands on the
            // card's own margins should dismiss rather than do nothing.
            setSwipeToDismiss()
        }

        return try {
            windowManager?.addView(view, layoutParams(position))
            treeOwner.onAttached()
            owner = treeOwner
            composeView = view
            view
        } catch (e: Exception) {
            // BadTokenException when the grant is revoked mid-flight, and
            // IllegalStateException if the view is somehow already attached.
            // Neither is recoverable; the readout in the app still works.
            Log.e(TAG, "Could not add overlay window", e)
            treeOwner.onDestroyed()
            null
        }
    }

    private fun detach() {
        handler.removeCallbacks(autoDismiss)
        handler.removeCallbacks(removeAfterExit)
        val view = composeView ?: return
        composeView = null
        cardVisibility.targetState = false
        try {
            windowManager?.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Overlay window already removed", e)
        }
        // After the window is gone, so the composition is disposed while its
        // lifecycle is still valid.
        owner?.onDestroyed()
        owner = null
    }

    /**
     * NOT_FOCUSABLE is the important flag: without it the overlay steals input
     * focus from the app underneath and, on a keyboard, closes the IME. The
     * popup is a readout with one close button; it never needs focus.
     */
    private fun layoutParams(position: PopupPosition) = WindowManager.LayoutParams(
        // Inset from both edges rather than MATCH_PARENT: a card running the
        // full width of the screen reads as a system banner, not as something
        // belonging to this app.
        (screenWidthPx() - sidePaddingPx() * 2).coerceAtMost(maxWidthPx()),
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // No FLAG_LAYOUT_NO_LIMITS. With it the window is allowed to extend
        // past the system insets, and the card's own surface filled the strip
        // under the navigation bar -- visible as a slab hanging off the
        // bottom of the card, most obviously in light mode. Without it the
        // window stops at the inset and the card ends where it looks like it
        // should.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.CENTER_HORIZONTAL or when (position) {
            PopupPosition.TOP -> Gravity.TOP
            PopupPosition.BOTTOM -> Gravity.BOTTOM
        }
        // Clear of the status bar / gesture pill either way.
        //
        // Just the margin: without FLAG_LAYOUT_NO_LIMITS the window already
        // fits the system bars (dumpsys shows fitTypes including
        // navigationBars), so adding the inset here counted it twice and left
        // a strip below the card that its own surface filled.
        y = edgeMarginPx()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /**
     * Swipe the card vertically to dismiss it, the gesture people already
     * expect from a heads-up notification.
     *
     * The listener returns false throughout, so it observes the gesture
     * without consuming it and the card's own close button still receives its
     * taps. A tap (no travel) is therefore left alone.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun View.setSwipeToDismiss() {
        var downY = 0f
        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> downY = event.rawY
                MotionEvent.ACTION_UP ->
                    if (abs(event.rawY - downY) > SWIPE_DISMISS_PX) dismiss()
            }
            false
        }
    }

    /** The edge inset in real pixels; a fixed pixel value would shrink on a dense screen. */
    private fun edgeMarginPx(): Int = dp(EDGE_MARGIN_DP)

    private fun sidePaddingPx(): Int = dp(SIDE_PADDING_DP)

    /** Keeps the card from stretching absurdly wide on a tablet or unfolded device. */
    private fun maxWidthPx(): Int = dp(MAX_WIDTH_DP)

    private fun screenWidthPx(): Int = context.resources.displayMetrics.widthPixels

    private fun dp(value: Float): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private inline fun onMainThread(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post { block() }
    }

    private companion object {
        const val TAG = "ConnectPopupHost"

        /** Slightly longer than the card's own exit animation. */
        const val EXIT_GRACE_MS = 300L
        const val EDGE_MARGIN_DP = 16f
        const val SIDE_PADDING_DP = 14f
        const val MAX_WIDTH_DP = 460f
        const val SWIPE_DISMISS_PX = 80f
    }
}

/**
 * The three owners a [ComposeView] needs when it is not inside an Activity.
 *
 * The saved-state registry is restored from null rather than a Bundle: an
 * overlay has nothing to restore, but Compose still requires the controller to
 * have been performed before the lifecycle passes CREATED.
 */
private class PopupViewTreeOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onAttached() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroyed() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
