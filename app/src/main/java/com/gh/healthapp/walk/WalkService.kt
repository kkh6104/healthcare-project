package com.gh.healthapp.walk

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gh.healthapp.MainActivity
import com.gh.healthapp.data.DailySummaryEntity
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.WalkPointEntity
import com.gh.healthapp.data.WalkSessionEntity
import com.gh.healthapp.data.WalkStatusValue
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * 걷기 기록 포그라운드 서비스 (foregroundServiceType=location).
 * 위치 2초 간격 → 점 필터 → 거리 누적, 10개/10초마다 DB에 묶어 저장, 알림 5초마다 갱신.
 */
class WalkService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dbLock = Mutex()
    private val db by lazy { HealthDatabase.get(applicationContext) }
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var sessionId: Long = 0
    private var startedAt = 0L
    private var status = WalkStatus.IDLE

    // 활동 시간: 누적 + (RUNNING이면) 지금까지의 구간
    private var activeAccumMs = 0L
    private var runningSince = 0L // SystemClock.elapsedRealtime 기준, RUNNING이 아니면 0

    // 거리/점
    private val tracker = DistanceTracker() // 앵커 방식 거리 누적 (재개 직후엔 앵커 초기화 → 이전 점과 잇지 않음)
    private val distanceM: Double get() = tracker.distanceM
    private val rawTail = ArrayDeque<RawPoint>() // 표시용 이동 평균에 쓰는 최근 원본 점
    private val mapPoints = mutableListOf<GeoPoint>()
    private val pending = mutableListOf<WalkPointEntity>()
    private var lastFlushAt = 0L

    // 걸음 센서
    private var stepsAccum = 0
    private var stepBase: Float? = null
    private var steps = 0
    private var stepListenerRegistered = false

    private var tickJob: Job? = null
    private var lastNotifyAt = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (status != WalkStatus.RUNNING) return
            result.locations.forEach { loc ->
                val p = RawPoint(
                    time = loc.time,
                    lat = loc.latitude,
                    lon = loc.longitude,
                    accuracy = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE,
                    altitude = if (loc.hasAltitude()) loc.altitude else null,
                )
                when (tracker.onPoint(p)) {
                    PointResult.DROPPED -> return@forEach
                    PointResult.ANCHOR_RESET -> Log.i("HCDEBUG", "walk anchor reset after ${ANCHOR_RESET_AFTER} rejects")
                    PointResult.STORED -> Unit
                }
                // 지도에는 최근 5점을 정확도 가중 평균으로 다듬은 점을 쓰고, DB에는 원본을 저장한다
                rawTail.addLast(p)
                while (rawTail.size > SMOOTH_WINDOW) rawTail.removeFirst()
                val shown = smoothWindow(rawTail.toList())
                mapPoints += GeoPoint(shown.lat, shown.lon)
                pending += WalkPointEntity(
                    sessionId = sessionId, time = p.time, lat = p.lat, lon = p.lon,
                    accuracy = p.accuracy, altitude = p.altitude,
                )
            }
            publish()
            flushIfNeeded()
        }
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (status != WalkStatus.RUNNING) return
            val v = event.values.firstOrNull() ?: return
            val base = stepBase ?: v.also { stepBase = it }
            steps = stepsAccum + (v - base).toInt().coerceAtLeast(0)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession()
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_STOP -> stopSession()
            else -> if (status == WalkStatus.IDLE) stopSelf()
        }
        return START_NOT_STICKY
    }

    // ---- 명령 처리 ----

    private fun startSession() {
        if (status != WalkStatus.IDLE) return
        // Android 14+는 위치 권한 없이 location 포그라운드 서비스를 시작하면 SecurityException이 나므로 먼저 확인
        if (!WalkPermissions.hasLocation(this)) {
            Log.w("HCDEBUG", "walk fgs failed: location permission missing")
            abortStart()
            return
        }
        startedAt = System.currentTimeMillis()
        status = WalkStatus.RUNNING
        runningSince = SystemClock.elapsedRealtime()
        // startForegroundService 후 5초 안에 알림을 올려야 하므로 DB 작업보다 먼저
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } catch (e: Exception) {
            val notAllowed = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                e is android.app.ForegroundServiceStartNotAllowedException
            if (e is SecurityException || notAllowed) {
                Log.w("HCDEBUG", "walk fgs failed: ${e.javaClass.simpleName}: ${e.message}")
                abortStart()
                return
            }
            throw e
        }
        isRunning = true
        scope.launch {
            sessionId = withDb {
                db.walkDao().insertSession(
                    WalkSessionEntity(
                        startTime = startedAt, endTime = null, distanceM = 0.0, activeMs = 0,
                        steps = 0, status = WalkStatusValue.RUNNING,
                    )
                )
            }
            lastFlushAt = SystemClock.elapsedRealtime()
            requestLocation()
            registerSteps()
            startTicker()
            publish()
        }
    }

    /** 시작하지 못했을 때: 세션을 만들지 않고 상태를 IDLE로 되돌리고 서비스 종료 */
    private fun abortStart() {
        status = WalkStatus.IDLE
        runningSince = 0L
        isRunning = false
        WalkTracker.reset()
        stopSelf()
    }

    private fun pauseSession() {
        if (status != WalkStatus.RUNNING) return
        activeAccumMs = activeMs()
        runningSince = 0L
        status = WalkStatus.PAUSED
        stepsAccum = steps
        stepBase = null
        fused.removeLocationUpdates(locationCallback)
        unregisterSteps()
        tracker.resetAnchor()
        rawTail.clear()
        scope.launch {
            flush()
            saveSession(WalkStatusValue.PAUSED, null)
            publish()
            notifyNow()
        }
    }

    private fun resumeSession() {
        if (status != WalkStatus.PAUSED) return
        status = WalkStatus.RUNNING
        runningSince = SystemClock.elapsedRealtime()
        tracker.resetAnchor() // 일시정지 동안 움직인 거리는 잇지 않는다
        rawTail.clear()
        stepBase = null
        requestLocation()
        registerSteps()
        scope.launch {
            saveSession(WalkStatusValue.RUNNING, null)
            publish()
            notifyNow()
        }
    }

    private fun stopSession() {
        if (status == WalkStatus.IDLE) return
        activeAccumMs = activeMs()
        runningSince = 0L
        status = WalkStatus.IDLE
        fused.removeLocationUpdates(locationCallback)
        unregisterSteps()
        tickJob?.cancel()
        Log.i(
            "HCDEBUG",
            "walk end dist=${distanceM.toInt()}m steps=${steps} stepDist≈${(steps * 0.7).toInt()}(=steps*0.7) " +
                "accepted=${tracker.accepted} rejected=${tracker.rejected}",
        )
        scope.launch {
            flush()
            saveSession(WalkStatusValue.DONE, System.currentTimeMillis())
            updateDailyDistance(db, startedAt)
            WalkTracker.reset()
            isRunning = false
            ServiceCompat.stopForeground(this@WalkService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ---- 위치/센서 ----

    @Suppress("MissingPermission")
    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "location permission missing")
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(2f)
            .build()
        fused.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun registerSteps() {
        if (stepListenerRegistered) return
        // 권한이 없거나 센서가 없으면 걸음은 0으로 둔다
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) return
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        try {
            stepListenerRegistered = sm.registerListener(stepListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } catch (e: SecurityException) {
            Log.w(TAG, "step sensor denied")
        }
    }

    private fun unregisterSteps() {
        if (!stepListenerRegistered) return
        (getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(stepListener)
        stepListenerRegistered = false
    }

    // ---- 상태/저장/알림 ----

    private fun activeMs(): Long =
        activeAccumMs + if (runningSince > 0) SystemClock.elapsedRealtime() - runningSince else 0L

    private fun publish() {
        WalkTracker.update(
            WalkLiveState(
                status = status,
                sessionId = sessionId.takeIf { it != 0L },
                points = mapPoints.toList(),
                distanceM = distanceM,
                activeMs = activeMs(),
                steps = steps,
                startedAt = startedAt,
            )
        )
    }

    /** 1초마다 시간/걸음 갱신, 10초마다 점 저장, 5초마다 알림 갱신 */
    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000)
                if (status == WalkStatus.RUNNING) {
                    publish()
                    flushIfNeeded()
                }
                if (SystemClock.elapsedRealtime() - lastNotifyAt >= 5000) notifyNow()
            }
        }
    }

    private fun flushIfNeeded() {
        val due = pending.size >= 10 || (pending.isNotEmpty() && SystemClock.elapsedRealtime() - lastFlushAt >= 10_000)
        if (due) scope.launch {
            flush()
            saveSession(if (status == WalkStatus.PAUSED) WalkStatusValue.PAUSED else WalkStatusValue.RUNNING, null)
        }
    }

    private suspend fun flush() {
        if (pending.isEmpty() || sessionId == 0L) {
            lastFlushAt = SystemClock.elapsedRealtime()
            return
        }
        val batch = pending.toList()
        pending.clear()
        lastFlushAt = SystemClock.elapsedRealtime()
        withDb { db.walkDao().insertPoints(batch) }
    }

    private suspend fun saveSession(statusValue: String, endTime: Long?) {
        if (sessionId == 0L) return
        withDb {
            db.walkDao().updateSession(
                WalkSessionEntity(
                    id = sessionId, startTime = startedAt, endTime = endTime, distanceM = distanceM,
                    activeMs = activeMs(), steps = steps, status = statusValue,
                )
            )
        }
    }

    private suspend fun <T> withDb(block: suspend () -> T): T = dbLock.withLock { block() }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "걷기 기록", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val km = String.format(Locale.KOREAN, "%.2fkm", distanceM / 1000.0)
        val title = if (status == WalkStatus.PAUSED) "일시정지" else "걷는 중"
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(label: String, act: String, code: Int) = NotificationCompat.Action.Builder(
            0, label,
            PendingIntent.getService(this, code, intent(this, act), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        ).build()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("${title} · ${km} · ${formatElapsed(activeMs())}")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        if (status == WalkStatus.PAUSED) builder.addAction(action("재개", ACTION_RESUME, 2))
        else builder.addAction(action("일시정지", ACTION_PAUSE, 1))
        builder.addAction(action("종료", ACTION_STOP, 3))
        return builder.build()
    }

    private fun notifyNow() {
        lastNotifyAt = SystemClock.elapsedRealtime()
        if (status == WalkStatus.IDLE) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        // 정상 종료(stopSession) 말고 시스템이 끝낸 경우를 위한 정리. 저장된 기록은 restore()가 마감한다
        fused.removeLocationUpdates(locationCallback)
        unregisterSteps()
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WalkService"
        private const val CHANNEL_ID = "walk"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.gh.healthapp.walk.START"
        const val ACTION_PAUSE = "com.gh.healthapp.walk.PAUSE"
        const val ACTION_RESUME = "com.gh.healthapp.walk.RESUME"
        const val ACTION_STOP = "com.gh.healthapp.walk.STOP"

        /** 서비스가 살아 있는 동안 true (같은 프로세스에서만 의미 있음) */
        @Volatile var isRunning = false
            internal set

        fun intent(context: Context, action: String) =
            Intent(context, WalkService::class.java).setAction(action)

        /** 그날 끝난(DONE) 걷기 거리 합을 daily_summary.walkDistanceM에 반영 (행이 없으면 다른 값은 null로 생성) */
        internal suspend fun updateDailyDistance(db: HealthDatabase, sessionStart: Long) {
            val zone = ZoneId.systemDefault()
            val date = Instant.ofEpochMilli(sessionStart).atZone(zone).toLocalDate()
            val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sum = db.walkDao().doneBetween(from, to).sumOf { it.distanceM }
            val now = System.currentTimeMillis()
            if (db.dao().updateWalkDistance(date.toString(), sum, now) == 0) {
                db.dao().upsertSummaries(
                    listOf(
                        DailySummaryEntity(
                            date = date.toString(), steps = null, hrAvg = null, hrMin = null, hrMax = null,
                            sleepMinutes = null, weightKg = null, bodyFatPct = null, boneMassKg = null,
                            leanMassKg = null, bodyWaterKg = null, updatedAt = now, walkDistanceM = sum,
                        )
                    )
                )
            }
        }
    }
}
