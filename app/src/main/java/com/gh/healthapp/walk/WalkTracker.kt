package com.gh.healthapp.walk

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.WalkStatusValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class GeoPoint(val lat: Double, val lon: Double)

enum class WalkStatus { IDLE, RUNNING, PAUSED, AUTO_PAUSED }

data class WalkLiveState(
    val status: WalkStatus = WalkStatus.IDLE,
    val sessionId: Long? = null,
    val points: List<GeoPoint> = emptyList(),
    val distanceM: Double = 0.0,
    val activeMs: Long = 0,
    val steps: Int = 0,
    val startedAt: Long? = null,
    val inVehicle: Boolean = false,
    val countdown: Int? = null,
)

/** 화면이 걷기 상태를 보고 서비스에 명령을 보내는 창구 */
object WalkTracker {
    private val _state = MutableStateFlow(WalkLiveState())
    val state: StateFlow<WalkLiveState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var countdownJob: kotlinx.coroutines.Job? = null
    @Volatile private var countdownVoice: WalkVoice? = null

    fun startWithCountdown(context: Context, seconds: Int = 3) {
        cancelCountdown()
        val app = context.applicationContext
        countdownVoice = WalkVoice(app)
        countdownJob = scope.launch {
            for (n in seconds.coerceAtLeast(1) downTo 1) {
                _state.value = WalkLiveState(countdown = n)
                countdownVoice?.say(n.toString(), WalkSettings(app).getVoice())
                delay(1000)
            }
            countdownVoice?.shutdown()
            countdownVoice = null
            _state.value = WalkLiveState()
            start(app)
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        countdownVoice?.shutdown()
        countdownVoice = null
        if (_state.value.countdown != null) _state.value = WalkLiveState()
    }

    /** 서비스에서만 호출 */
    internal fun update(s: WalkLiveState) {
        _state.value = s
    }

    internal fun reset() {
        _state.value = WalkLiveState()
    }

    /**
     * 걷기 시작. 위치 권한이 없거나 서비스를 시작할 수 없으면 서비스를 띄우지 않고 false를 돌려준다.
     */
    fun start(context: Context): Boolean {
        if (!WalkPermissions.hasLocation(context)) return false
        return try {
            ContextCompat.startForegroundService(context, WalkService.intent(context, WalkService.ACTION_START))
            true
        } catch (e: Exception) {
            // 예: 앱이 백그라운드라 포그라운드 서비스 시작이 허용되지 않음 (API 31+)
            Log.w("HCDEBUG", "walk fgs failed: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    fun pause(context: Context) = send(context, WalkService.ACTION_PAUSE)
    fun resume(context: Context) = send(context, WalkService.ACTION_RESUME)
    fun stop(context: Context) = send(context, WalkService.ACTION_STOP)

    private fun send(context: Context, action: String) {
        // 서비스가 떠 있지 않으면 보낼 곳이 없으므로 무시
        if (!WalkService.isRunning) return
        context.startService(WalkService.intent(context, action))
    }

    /**
     * 앱이 다시 켜졌을 때 호출. 서비스가 살아 있으면(상태가 그대로 유지됨) 아무것도 하지 않고,
     * 서비스 없이 RUNNING/PAUSED로 남은 기록은 마지막으로 저장된 값으로 DONE 처리한다.
     */
    fun restore(context: Context) {
        if (WalkService.isRunning) return
        val app = context.applicationContext
        scope.launch {
            val db = HealthDatabase.get(app)
            val orphan = db.walkDao().runningSession() ?: return@launch
            if (WalkService.isRunning) return@launch
            val lastPoint = db.walkDao().points(orphan.id).lastOrNull()
            val end = maxOf(lastPoint?.time ?: orphan.startTime, orphan.startTime)
            db.walkDao().updateSession(orphan.copy(status = WalkStatusValue.DONE, endTime = end))
            WalkService.updateDailyDistance(db, orphan.startTime)
            reset()
        }
    }
}
