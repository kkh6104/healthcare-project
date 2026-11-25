package com.gh.healthapp.walk

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

internal class WalkVoice(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener { }.build()
    private val lastSaid = mutableMapOf<String, Long>()
    private var ready = false
    private var tts: TextToSpeech? = null
    init {
        tts = TextToSpeech(context.applicationContext) { result ->
            val engine = tts
            if (result == TextToSpeech.SUCCESS && engine != null && engine.setLanguage(Locale.KOREAN) >= TextToSpeech.LANG_AVAILABLE) {
                engine.setAudioAttributes(attributes)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { audio.abandonAudioFocusRequest(focus) }
                    @Deprecated("Deprecated in Android") override fun onError(utteranceId: String?) { audio.abandonAudioFocusRequest(focus) }
                })
                ready = true
            } else Log.w("HCDEBUG", "walk voice skipped(init or language)")
        }
    }
    fun say(message: String, enabled: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val types = setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_USB_HEADSET)
        val reason = when {
            !enabled -> "disabled"
            !ready -> "not ready"
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).none { it.type in types } -> "no headphones"
            lastSaid[message]?.let { now - it < 15_000 } == true -> "repeat"
            audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> "audio focus"
            else -> null
        }
        if (reason != null) { Log.i("HCDEBUG", "walk voice skipped($reason)"); return }
        if (tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "walk") == TextToSpeech.SUCCESS) {
            lastSaid[message] = now
            Log.i("HCDEBUG", "walk voice said=$message")
        } else { audio.abandonAudioFocusRequest(focus); Log.i("HCDEBUG", "walk voice skipped(speak)") }
    }
    fun shutdown() { ready = false; tts?.shutdown(); tts = null; audio.abandonAudioFocusRequest(focus) }
}
