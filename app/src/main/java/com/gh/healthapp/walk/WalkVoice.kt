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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

internal class WalkVoice(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes).setOnAudioFocusChangeListener { }.build()
    private val lastSaid = mutableMapOf<String, Long>()
    private var ready = false
    private data class Pending(val message: String, val enabled: Boolean, val queuedAt: Long)
    private var pending: Pending? = null
    private var tts: TextToSpeech? = null
    private val completions = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    init {
        tts = TextToSpeech(context.applicationContext) { result ->
            val engine = tts
            if (result == TextToSpeech.SUCCESS && engine != null && engine.setLanguage(Locale.KOREAN) >= TextToSpeech.LANG_AVAILABLE) {
                engine.setAudioAttributes(attributes)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { audio.abandonAudioFocusRequest(focus); utteranceId?.let { completions.remove(it)?.complete(Unit) } }
                    @Deprecated("Deprecated in Android") override fun onError(utteranceId: String?) { audio.abandonAudioFocusRequest(focus); utteranceId?.let { completions.remove(it)?.complete(Unit) } }
                    override fun onError(utteranceId: String?, errorCode: Int) = onError(utteranceId)
                })
                ready = true
                val queued = pending
                pending = null
                if (queued != null) {
                    if (SystemClock.elapsedRealtime() - queued.queuedAt <= 5_000L) sayInternal(queued.message, queued.enabled, true, null, null)
                    else Log.i("HCDEBUG", "walk voice skipped(queue expired)")
                }
            } else Log.w("HCDEBUG", "walk voice skipped(init or language)")
        }
    }
    fun say(message: String, enabled: Boolean, dedupeKey: String? = null) {
        if (!ready) {
            pending = Pending(message, enabled, SystemClock.elapsedRealtime())
            return
        }
        sayInternal(message, enabled, false, dedupeKey, null)
    }
    suspend fun sayAndWait(message: String, enabled: Boolean, timeoutMs: Long = 6_000L) {
        val id = "walk-end-${SystemClock.elapsedRealtime()}"
        val completion = CompletableDeferred<Unit>()
        if (!sayInternal(message, enabled, true, id, completion)) return
        withTimeoutOrNull(timeoutMs) { completion.await() }
    }
    private fun sayInternal(message: String, enabled: Boolean, force: Boolean, id: String?, completion: CompletableDeferred<Unit>?): Boolean {
        val now = SystemClock.elapsedRealtime()
        val types = setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_USB_HEADSET)
        val reason = when {
            !enabled -> "disabled"
            !ready -> "not ready"
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).none { it.type in types } -> "no headphones"
            !force && id == null && lastSaid[message]?.let { now - it < 15_000 } == true -> "repeat"
            audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> "audio focus"
            else -> null
        }
        if (reason != null) { Log.i("HCDEBUG", "walk voice skipped($reason)"); return false }
        val utteranceId = id ?: "walk"
        completion?.let { completions[utteranceId] = it }
        if (tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS) {
            lastSaid[message] = now
            Log.i("HCDEBUG", "walk voice said=$message")
            return true
        } else { completions.remove(utteranceId); audio.abandonAudioFocusRequest(focus); Log.i("HCDEBUG", "walk voice skipped(speak)"); return false }
    }
    fun shutdown() { ready = false; tts?.shutdown(); tts = null; audio.abandonAudioFocusRequest(focus) }
}

internal fun walkEndVoiceText(distanceM: Double, activeMs: Long): String {
        val roundedMeters = (distanceM / 10.0).let { kotlin.math.floor(it + 0.5).toInt() * 10 }
        val distance = if (roundedMeters >= 1000) "${String.format(Locale.US, "%.2f", roundedMeters / 1000.0).trimEnd('0').trimEnd('.')}킬로미터"
        else "${roundedMeters}미터"
    val seconds = (activeMs / 1000).toInt()
    val time = when {
        seconds >= 3600 -> "${seconds / 3600}시간${if (seconds % 3600 >= 60) " ${seconds % 3600 / 60}분" else ""}"
        seconds >= 60 -> "${seconds / 60}분${if (seconds % 60 > 0) " ${seconds % 60}초" else ""}"
        else -> "${seconds}초"
    }
    return "걷기를 종료했습니다. $distance, $time"
}

internal fun nextWalkAnnouncementKm(lastAnnouncedKm: Int, distanceM: Double, active: Boolean, enabled: Boolean): Int? {
    if (!active || !enabled) return null
    val reached = (distanceM / 1000.0).toInt()
    return if (reached > lastAnnouncedKm) reached else null
}

internal fun walkKmVoiceText(km: Int, activeMs: Long, steps: Int): String {
    val seconds = activeMs / 1000
    val time = when {
        seconds >= 3600 -> "${seconds / 3600}시간${if (seconds % 3600 >= 60) " ${seconds % 3600 / 60}분" else ""}"
        seconds >= 60 -> "${seconds / 60}분${if (seconds % 60 > 0) " ${seconds % 60}초" else ""}"
        else -> "${seconds}초"
    }
    return "${km}킬로미터, $time, ${java.text.NumberFormat.getIntegerInstance(Locale.KOREAN).format(steps)}걸음"
}

