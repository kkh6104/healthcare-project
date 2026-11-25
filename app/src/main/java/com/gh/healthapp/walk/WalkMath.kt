package com.gh.healthapp.walk

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 위치 센서에서 받은 점 하나 (필터 판단용) */
internal data class RawPoint(
    val time: Long,
    val lat: Double,
    val lon: Double,
    val accuracy: Float,
    val altitude: Double? = null,
)

/** 정확도가 이보다 나쁜 점은 버린다 (m) */
internal const val MAX_ACCURACY_M = 25f
/** 앵커 대비 이 속도(m/s, 시속 9km)를 넘는 점은 튄 점으로 보고 버린다 */
internal const val MAX_SPEED_MPS = 2.5
/** 연속으로 이만큼 버려지면 실제로 멀리 이동한 것으로 보고 앵커를 옮긴다 */
internal const val ANCHOR_RESET_AFTER = 3
/** 거리를 더하기 위한 최소 이동량(m) */
internal const val MIN_STEP_M = 3.0
/** 표시용 경로 다듬기 이동 평균 창 크기 */
internal const val SMOOTH_WINDOW = 5
private const val EARTH_RADIUS_M = 6_371_000.0

/** 두 좌표 사이의 거리(m), Haversine */
internal fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(a))
}

internal fun distanceM(a: RawPoint, b: RawPoint): Double = haversineM(a.lat, a.lon, b.lat, b.lon)

internal enum class PointResult { DROPPED, STORED, ANCHOR_RESET }

/**
 * GPS 흔들림을 줄이는 거리 누적기 (앵커 방식).
 * - 정확도가 [MAX_ACCURACY_M]보다 나쁜 점은 버린다.
 * - 마지막으로 인정한 점(앵커)에서의 거리 d가 max(3m, (앵커 정확도 + 새 점 정확도)/2 * 0.5) 이상일 때만 거리에 d를 더하고 앵커를 옮긴다.
 *   그보다 가까우면 거리는 더하지 않지만 점은 저장/표시용으로 남긴다.
 * - 앵커 대비 속도가 [MAX_SPEED_MPS]를 넘으면 버린다. 연속 [ANCHOR_RESET_AFTER]번 버려지면 마지막 점을 새 앵커로
 *   삼는다(거리는 더하지 않음, ANCHOR_RESET).
 */
internal class DistanceTracker {
    var distanceM = 0.0
        private set
    var accepted = 0
        private set
    var rejected = 0
        private set
    private var anchor: RawPoint? = null
    private var consecutiveRejects = 0

    fun excludeDistance(meters: Double) { distanceM = (distanceM - meters).coerceAtLeast(0.0) }

    /** 일시정지/재개 직후: 이전 점과 거리를 잇지 않는다 */
    fun resetAnchor() {
        anchor = null
        consecutiveRejects = 0
    }

    fun onPoint(p: RawPoint): PointResult {
        if (p.accuracy > MAX_ACCURACY_M) {
            rejected++
            return PointResult.DROPPED
        }
        val a = anchor
        if (a == null) {
            anchor = p
            consecutiveRejects = 0
            accepted++
            return PointResult.STORED
        }
        val dtSec = (p.time - a.time) / 1000.0
        if (dtSec <= 0.0) {
            rejected++
            return PointResult.DROPPED
        }
        val d = distanceM(a, p)
        if (d / dtSec > MAX_SPEED_MPS) {
            rejected++
            consecutiveRejects++
            if (consecutiveRejects >= ANCHOR_RESET_AFTER) {
                anchor = p
                consecutiveRejects = 0
                return PointResult.ANCHOR_RESET
            }
            return PointResult.DROPPED
        }
        consecutiveRejects = 0
        accepted++
        val meanAccuracy = (a.accuracy + p.accuracy) / 2.0
        val threshold = if (meanAccuracy <= 8.0) 2.0 else maxOf(MIN_STEP_M, meanAccuracy * 0.5)
        if (d >= threshold) {
            distanceM += d
            anchor = p
        }
        return PointResult.STORED
    }
}

/** Inputs use elapsed realtime so wall-clock changes do not affect motion detection. */
internal class MotionMonitor(start: Long) {
    private val stepTimes = ArrayDeque<Long>()
    private val positions = ArrayDeque<Pair<Long, RawPoint>>()
    private var epoch = start
    private var pauseEpoch = start
    var autoPaused = false
        private set
    var inVehicle = false
        private set
    fun step(now: Long, count: Int = 1) { repeat(count.coerceAtLeast(0)) { stepTimes.addLast(now) } }
    fun point(now: Long, point: RawPoint) { if (point.accuracy <= MAX_ACCURACY_M) positions.addLast(now to point) }
    fun reset(now: Long) { stepTimes.clear(); positions.clear(); autoPaused = false; inVehicle = false; epoch = now; pauseEpoch = now }
    fun evaluate(now: Long, hasSteps: Boolean, autoEnabled: Boolean) {
        while (stepTimes.isNotEmpty() && stepTimes.first() < now - 20_000) stepTimes.removeFirst()
        while (positions.size > 1 && positions.elementAt(1).first <= now - 20_000) positions.removeFirst()
        val recent3 = stepTimes.count { it >= now - 3_000 }
        val recent10 = stepTimes.count { it > now - 10_000 }
        val recent20 = stepTimes.count { it > now - 20_000 }
        fun displacement(window: Long): Double {
            val last = positions.lastOrNull() ?: return 0.0
            val first = positions.lastOrNull { it.first <= now - window } ?: positions.first()
            return distanceM(first.second, last.second)
        }
        if (inVehicle) {
            if (recent3 >= 3) reset(now)
            return
        }
        if (hasSteps && now - epoch >= 20_000 && recent20 < 5 && displacement(20_000) >= 15.0) {
            inVehicle = true; autoPaused = false; return
        }
        if (!autoEnabled) { autoPaused = false; return }
        if (autoPaused) {
            if ((hasSteps && recent3 >= 3) || displacement(10_000) >= 8.0) { autoPaused = false; pauseEpoch = now }
        } else if (now - pauseEpoch >= 10_000 && (!hasSteps || recent10 == 0) && displacement(10_000) < 5.0) {
            autoPaused = true
        }
    }
}

/** 최근 점들([points] 마지막이 기준)의 정확도 가중 평균 위치. 시간·정확도는 마지막 점 값 */
internal fun smoothWindow(points: List<RawPoint>): RawPoint {
    val last = points.last()
    if (points.size == 1) return last
    var sw = 0.0
    var lat = 0.0
    var lon = 0.0
    for (p in points) {
        val acc = maxOf(p.accuracy.toDouble(), 1.0)
        val wgt = 1.0 / (acc * acc)
        sw += wgt
        lat += p.lat * wgt
        lon += p.lon * wgt
    }
    return last.copy(lat = lat / sw, lon = lon / sw)
}

/** 표시용 경로: 각 점을 최근 [window]점의 정확도 가중 이동 평균으로 바꾼다 (원본은 건드리지 않음) */
internal fun smoothRoute(points: List<RawPoint>, window: Int = SMOOTH_WINDOW): List<RawPoint> =
    points.indices.map { i -> smoothWindow(points.subList(maxOf(0, i - window + 1), i + 1)) }

/** 연속된 점들 사이 거리의 합(m) */
internal fun totalDistanceM(points: List<RawPoint>): Double =
    points.zipWithNext().sumOf { (a, b) -> distanceM(a, b) }

/** 걷는 시간 mm:ss (1시간 이상이면 h:mm:ss) */
internal fun formatElapsed(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
