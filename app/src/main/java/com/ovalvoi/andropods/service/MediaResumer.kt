package com.ovalvoi.andropods.service

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent

/**
 * Sends a media PLAY to whichever app last played audio, the way an iPhone
 * picks up where it left off when AirPods reconnect.
 *
 * Dispatched through [AudioManager.dispatchMediaKeyEvent], which routes to the
 * most recent media session without any permission. If nothing has played
 * recently there is no session to receive it and nothing happens, which is
 * the right outcome: "resume", not "start something".
 *
 * Deliberately delayed past the A2DP connection. The audio route flips to the
 * pods a moment after the profile reports connected, and a PLAY sent before
 * that comes out of the phone's speaker.
 */
object MediaResumer {

    private val handler = Handler(Looper.getMainLooper())

    fun resumeAfterAudioConnected(context: Context) {
        val audio = context.applicationContext.getSystemService(AudioManager::class.java)
        handler.removeCallbacksAndMessages(TOKEN)
        handler.postDelayed({
            if (audio.isMusicActive) {
                Log.d(TAG, "Music already playing; not sending PLAY")
                return@postDelayed
            }
            Log.d(TAG, "Sending media PLAY")
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        }, TOKEN, ROUTE_SETTLE_DELAY_MS)
    }

    private const val TAG = "MediaResumer"
    private val TOKEN = Any()

    /** Long enough for the audio route to move to the pods on this hardware. */
    private const val ROUTE_SETTLE_DELAY_MS = 1_500L
}
