package com.gh.healthapp.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkMathTest {

    private fun pt(timeSec: Long, lat: Double, lon: Double, acc: Float = 5f) =
        RawPoint(timeSec * 1000, lat, lon, acc)

    @Test
    fun haversine_sameAndKnownDistance() {
        assertEquals(0.0, haversineM(37.0, 127.0, 37.0, 127.0), 1e-9)
        // 위도 0.001도 ≈ 111.2m
        assertEquals(111.2, haversineM(37.0, 127.0, 37.001, 127.0), 0.5)
        // 서울 시청 ~ 광화문 부근은 약 0.5~1km 범위
        val d = haversineM(37.5662, 126.9779, 37.5759, 126.9769)
        assertTrue(d in 900.0..1200.0)
    }

    @Test
    fun badAccuracy_isRejected() {
        val tr = DistanceTracker()
        assertEquals(PointResult.DROPPED, tr.onPoint(pt(0, 37.0, 127.0, acc = 26f)))
        assertEquals(PointResult.STORED, tr.onPoint(pt(1, 37.0, 127.0, acc = 25f)))
    }

    @Test
    fun timeNotMovingForward_isDropped() {
        val tr = DistanceTracker()
        tr.onPoint(pt(10, 37.0, 127.0))
        assertEquals(PointResult.DROPPED, tr.onPoint(pt(10, 37.00001, 127.0)))
        assertEquals(PointResult.DROPPED, tr.onPoint(pt(5, 37.00001, 127.0)))
    }

    @Test
    fun firstPointAfterResume_isNotConnectedToPrevious() {
        val tr = DistanceTracker()
        tr.onPoint(pt(0, 37.0, 127.0))
        tr.resetAnchor()
        // 재개 직후 멀리 떨어진 점도 받아들이고, 거리는 더하지 않는다
        assertEquals(PointResult.STORED, tr.onPoint(pt(100, 37.5, 127.5)))
        assertEquals(0.0, tr.distanceM, 0.0)
    }

    @Test
    fun stationaryJitter_addsNoDistance() {
        // 반경 약 3m 안에서 흔들리는 30점 (정확도 12m, 2초 간격)
        val tr = DistanceTracker()
        val mPerDegLat = 111_195.0
        for (k in 0 until 30) {
            val r = 3.0 * ((k % 3) + 1) / 3.0
            val ang = Math.toRadians(k * 47.0)
            val lat = 37.0 + r * Math.sin(ang) / mPerDegLat
            val lon = 127.0 + r * Math.cos(ang) / (mPerDegLat * Math.cos(Math.toRadians(37.0)))
            tr.onPoint(pt(k * 2L, lat, lon, acc = 12f))
        }
        assertEquals(0.0, tr.distanceM, 0.0)
    }

    @Test
    fun straightWalk_distanceWithin10Percent() {
        // 1.4m/s로 2초 간격 60점, 정확도 8m
        val tr = DistanceTracker()
        val stepM = 2.8
        val mPerDegLat = 111_195.0
        for (k in 0 until 60) {
            tr.onPoint(pt(k * 2L, 37.0 + k * stepM / mPerDegLat, 127.0, acc = 8f))
        }
        val truth = 59 * stepM
        assertEquals(truth, tr.distanceM, truth * 0.10)
    }

    @Test
    fun singleJump_isIgnored() {
        val tr = DistanceTracker()
        tr.onPoint(pt(0, 37.0, 127.0, acc = 8f))
        // 2초 만에 약 55m 튄 점 → 버림
        assertEquals(PointResult.DROPPED, tr.onPoint(pt(2, 37.0005, 127.0, acc = 8f)))
        // 원래 자리 근처로 돌아오면 정상, 거리는 그대로 0
        assertEquals(PointResult.STORED, tr.onPoint(pt(4, 37.00001, 127.0, acc = 8f)))
        assertEquals(0.0, tr.distanceM, 0.0)
        assertEquals(1, tr.rejected)
    }

    @Test
    fun threeConsecutiveRejects_moveAnchorWithoutAddingDistance() {
        val tr = DistanceTracker()
        tr.onPoint(pt(0, 37.0, 127.0, acc = 8f))
        // 멀리 떨어진 곳에서 계속 머무는 점들
        assertEquals(PointResult.DROPPED, tr.onPoint(pt(2, 37.005, 127.0, acc = 8f)))
        assertEquals(PointResult.DROPPED, tr.onPoint(pt(4, 37.005, 127.0, acc = 8f)))
        assertEquals(PointResult.ANCHOR_RESET, tr.onPoint(pt(6, 37.005, 127.0, acc = 8f)))
        assertEquals(0.0, tr.distanceM, 0.0)
        // 새 앵커 기준으로 다시 정상 누적
        tr.onPoint(pt(10, 37.00506, 127.0, acc = 8f)) // 새 앵커에서 약 6.7m, 1.7m/s
        assertTrue(tr.distanceM > 0.0)
    }

    @Test
    fun smoothRoute_reducesJitterAndKeepsCount() {
        val raw = (0 until 20).map { k ->
            val off = if (k % 2 == 0) 0.00002 else -0.00002
            pt(k * 2L, 37.0 + off, 127.0, acc = 8f)
        }
        val smooth = smoothRoute(raw)
        assertEquals(raw.size, smooth.size)
        assertTrue(totalDistanceM(smooth) < totalDistanceM(raw) * 0.5)
        // 점이 하나뿐이면 그대로
        assertEquals(raw[0].lat, smoothRoute(raw.take(1))[0].lat, 0.0)
    }

    @Test
    fun totalDistance_sumsConsecutiveSegments() {
        val pts = listOf(pt(0, 37.0, 127.0), pt(2, 37.00005, 127.0), pt(4, 37.0001, 127.0))
        assertEquals(haversineM(37.0, 127.0, 37.0001, 127.0), totalDistanceM(pts), 0.01)
        assertEquals(0.0, totalDistanceM(emptyList()), 0.0)
        assertEquals(0.0, totalDistanceM(pts.take(1)), 0.0)
    }

    @Test
    fun formatElapsed_works() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("15:03", formatElapsed((15 * 60 + 3) * 1000L))
        assertEquals("1:02:03", formatElapsed((3600 + 2 * 60 + 3) * 1000L))
    }
}
