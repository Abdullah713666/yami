package com.example.yami.sound

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.yami.R
import com.example.yami.data.YamiPrefs

class SoundPlayer(
    private val context: Context,
    private val prefs: YamiPrefs
) {
    companion object {
        private const val TAG = "SoundPlayer"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            stopCurrent()
        }
    }

    private var currentPlayer: MediaPlayer? = null
    private var fadeAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var chirpStopRunnable: Runnable? = null

    private sealed class SoundSource {
        data class RawRes(val resId: Int) : SoundSource()
        data class Custom(val uri: Uri) : SoundSource()
    }

    private fun requestFocus(): Boolean {
        val am = audioManager ?: return true
        if (hasAudioFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val afr = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = afr
            am.requestAudioFocus(afr)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonFocus() {
        if (!hasAudioFocus) return
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    private fun stopCurrent() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
        chirpStopRunnable?.let { handler.removeCallbacks(it) }
        chirpStopRunnable = null

        fadeAnimator?.cancel()
        fadeAnimator = null

        currentPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {}
            try {
                player.release()
            } catch (_: Exception) {}
        }
        currentPlayer = null

        abandonFocus()
    }

    fun play() {
        val choice = prefs.soundChoice
        if (choice == "off") return

        val source = when (choice) {
            "siren" -> SoundSource.RawRes(R.raw.sound_siren)
            "custom" -> {
                val uriStr = prefs.soundCustomUri
                if (uriStr.isNullOrBlank()) {
                    SoundSource.RawRes(R.raw.sound_whistle)
                } else {
                    SoundSource.Custom(Uri.parse(uriStr))
                }
            }
            else -> SoundSource.RawRes(R.raw.sound_whistle)
        }

        startInternal(source, isChirp = false, isFallback = false)
    }

    fun playChirp() {
        if (prefs.soundChoice == "off") return
        startInternal(SoundSource.RawRes(R.raw.sound_whistle), isChirp = true, isFallback = false)
    }

    private fun startInternal(source: SoundSource, isChirp: Boolean, isFallback: Boolean) {
        stopCurrent()

        val player = MediaPlayer()
        currentPlayer = player
        player.setAudioAttributes(audioAttributes)

        val dataSourceSet = try {
            when (source) {
                is SoundSource.RawRes -> {
                    val afd = context.resources.openRawResourceFd(source.resId)
                    if (afd != null) {
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        true
                    } else {
                        false
                    }
                }
                is SoundSource.Custom -> {
                    player.setDataSource(context, source.uri)
                    true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set data source for $source", e)
            false
        }

        if (!dataSourceSet) {
            if (source is SoundSource.Custom && !isFallback) {
                startInternal(SoundSource.RawRes(R.raw.sound_whistle), isChirp, isFallback = true)
            } else {
                stopCurrent()
            }
            return
        }

        player.setOnErrorListener { mp, what, extra ->
            Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
            if (mp == currentPlayer) {
                if (source is SoundSource.Custom && !isFallback) {
                    startInternal(SoundSource.RawRes(R.raw.sound_whistle), isChirp, isFallback = true)
                } else {
                    stopCurrent()
                }
            }
            true
        }

        player.setOnPreparedListener { mp ->
            if (mp != currentPlayer) return@setOnPreparedListener

            if (!requestFocus()) {
                Log.w(TAG, "Audio focus not granted, skipping playback")
                stopCurrent()
                return@setOnPreparedListener
            }

            try {
                mp.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MediaPlayer", e)
                if (source is SoundSource.Custom && !isFallback) {
                    startInternal(SoundSource.RawRes(R.raw.sound_whistle), isChirp, isFallback = true)
                } else {
                    stopCurrent()
                }
                return@setOnPreparedListener
            }

            if (isChirp) {
                val stopRunnable = Runnable {
                    if (currentPlayer == mp) {
                        stopCurrent()
                    }
                }
                chirpStopRunnable = stopRunnable
                handler.postDelayed(stopRunnable, 380L)
            } else {
                val fRunnable = Runnable {
                    if (currentPlayer == mp) {
                        val anim = ValueAnimator.ofFloat(1f, 0f).apply {
                            duration = 400L
                            addUpdateListener { va ->
                                val vol = va.animatedValue as Float
                                try {
                                    if (currentPlayer == mp) {
                                        mp.setVolume(vol, vol)
                                    }
                                } catch (_: Exception) {}
                            }
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    if (currentPlayer == mp) {
                                        stopCurrent()
                                    }
                                }
                            })
                        }
                        fadeAnimator = anim
                        anim.start()
                    }
                }
                fadeRunnable = fRunnable
                handler.postDelayed(fRunnable, 4600L)
            }
        }

        player.setOnCompletionListener { mp ->
            if (mp == currentPlayer) {
                stopCurrent()
            }
        }

        try {
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepareAsync", e)
            if (source is SoundSource.Custom && !isFallback) {
                startInternal(SoundSource.RawRes(R.raw.sound_whistle), isChirp, isFallback = true)
            } else {
                stopCurrent()
            }
        }
    }

    fun release() {
        stopCurrent()
    }
}
