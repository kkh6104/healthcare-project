package com.gh.healthapp.data

import com.gh.healthapp.SleepDay
import com.gh.healthapp.SleepNight
import com.gh.healthapp.groupSleepDays
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/** 상세 화면용 DB 읽기 (Health Connect 호출 없음). 출처 우선순위: health_connect 행이 있으면 그것만 */
class DetailReader(private val dao: HealthDao) {

    private val zone get() = ZoneId.systemDefault()

    /** 최근 days일 걸음 (오늘 포함, 최신 날짜가 먼저, 기록 없는 날은 0) */
    suspend fun stepsDaily(days: Int): List<Pair<LocalDate, Long>> {
        val today = LocalDate.now(zone)
        val by = dao.summariesBetween(today.minusDays(days - 1L).toString(), today.toString()).associateBy { it.date }
        return (0 until days).map { i ->
            val d = today.minusDays(i.toLong())
            d to (by[d.toString()]?.steps ?: 0L)
        }
    }

    /** 그날 심박을 시간별로: (시, 평균, (최저, 최고)). 기록 있는 시간만, 시 오름차순 */
    suspend fun heartRateHourly(date: LocalDate): List<Triple<Int, Double, Pair<Double, Double>>> {
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rows = preferHealthConnect(dao.recordsBetween(RecordType.HEART_RATE, from, to - 1))
        return rows.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).hour }
            .toSortedMap()
            .mapNotNull { (hour, list) ->
                hrStats(list)?.let { Triple(hour, it.avg, it.min to it.max) }
            }
    }

    /** 최근 days일 일별 평균 심박 (최신 날짜가 먼저, 기록 없으면 null) */
    suspend fun hrDailyAvg(days: Int): List<Pair<LocalDate, Double?>> {
        val today = LocalDate.now(zone)
        val by = dao.summariesBetween(today.minusDays(days - 1L).toString(), today.toString()).associateBy { it.date }
        return (0 until days).map { i ->
            val d = today.minusDays(i.toLong())
            d to by[d.toString()]?.hrAvg
        }
    }

    /** 최근 days일 수면 세션 (깬 날짜별 출처 우선순위 적용, 최신순) */
    suspend fun sleepNights(days: Int): List<SleepNight> {
        val now = Instant.now()
        // 기간 = 오늘 포함 정확히 days일(깬 날짜 기준). 전날 밤에 시작해 기간 첫날 아침에 끝난 세션도 받도록 하루 앞에서부터 읽는다
        val firstDate = LocalDate.now(zone).minusDays(days - 1L)
        val from = firstDate.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rows = dao.recordsBetween(RecordType.SLEEP_SESSION, from, now.toEpochMilli())
            .filter { it.endTime <= now.toEpochMilli() }
            .filter { !Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate().isBefore(firstDate) }
        return rows.groupBy { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }
            .values.flatMap { preferHealthConnect(it) }
            .map { toNight(it) }
            .sortedByDescending { it.end }
    }

    /** 최근 days일을 수면일(종료 날짜)별로 합산 (최신 날짜가 먼저) */
    suspend fun sleepDays(days: Int): List<SleepDay> = groupSleepDays(sleepNights(days))

    /** WEIGHT/BODY_FAT 등: 최근 days일, 날짜별 마지막 측정 1건 (시간 오름차순) */
    suspend fun metricHistory(type: String, days: Int): List<Pair<Instant, Double>> {
        // 오늘 포함 정확히 days일
        val from = LocalDate.now(zone).minusDays(days - 1L).atStartOfDay(zone).toInstant().toEpochMilli()
        val rows = dao.recordsBetween(type, from, System.currentTimeMillis())
        return rows.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
            .values.mapNotNull { list -> preferHealthConnect(list).maxByOrNull { it.startTime } }
            .sortedBy { it.startTime }
            .map { Instant.ofEpochMilli(it.startTime) to it.value }
    }

    private fun toNight(r: HealthRecordEntity): SleepNight {
        val json = r.extraJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        fun stage(key: String): Duration? =
            if (json != null && json.has(key) && !json.isNull(key)) Duration.ofMinutes(json.getLong(key)) else null
        val end = Instant.ofEpochMilli(r.endTime)
        return SleepNight(
            wakeDate = end.atZone(zone).toLocalDate(),
            start = Instant.ofEpochMilli(r.startTime),
            end = end,
            asleep = Duration.ofMinutes(r.value.roundToLong()),
            deep = stage("deep"),
            light = stage("light"),
            rem = stage("rem"),
            awake = stage("awake"),
        )
    }
}
