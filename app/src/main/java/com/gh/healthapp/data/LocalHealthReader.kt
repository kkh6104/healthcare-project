package com.gh.healthapp.data

import com.gh.healthapp.BodyMetric
import com.gh.healthapp.HeartRateSummary
import com.gh.healthapp.MetricEntry
import com.gh.healthapp.MetricSummary
import com.gh.healthapp.SleepNight
import com.gh.healthapp.SleepSummary
import com.gh.healthapp.WeightEntry
import com.gh.healthapp.WeightSummary
import com.gh.healthapp.buildMetricSummary
import com.gh.healthapp.buildWeightSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/** DB 데이터를 기존 화면용 데이터 클래스로 변환 */
class LocalHealthReader(private val dao: HealthDao) {

    private val zone get() = ZoneId.systemDefault()

    suspend fun stepsDaily(days: Int = 7): List<Pair<LocalDate, Long>> {
        val today = LocalDate.now(zone)
        return (0 until days).map { i ->
            val d = today.minusDays(i.toLong())
            d to (dao.summary(d.toString())?.steps ?: 0L)
        }
    }

    suspend fun heartRateToday(now: Instant = Instant.now()): HeartRateSummary {
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val rows = preferHealthConnect(dao.recordsBetween(RecordType.HEART_RATE, start, now.toEpochMilli()))
        val stats = hrStats(rows) ?: return HeartRateSummary(null, null, null, null, null)
        val latest = rows.maxByOrNull { it.startTime }!!
        return HeartRateSummary(
            latestBpm = latest.value.roundToLong(),
            latestTime = Instant.ofEpochMilli(latest.startTime),
            avg = stats.avg.roundToLong(),
            min = stats.min.roundToLong(),
            max = stats.max.roundToLong(),
        )
    }

    suspend fun sleep(days: Int = 7): SleepSummary {
        val now = Instant.now()
        val today = now.atZone(zone).toLocalDate()
        val from = today.minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val nights = dao.recordsBetween(RecordType.SLEEP_SESSION, from, now.toEpochMilli())
            .filter { it.endTime <= now.toEpochMilli() }
            .let { preferPerDate(it) { r -> Instant.ofEpochMilli(r.endTime).atZone(zone).toLocalDate() } }
            .map { toNight(it) }
        val byDate = nights.groupBy { it.wakeDate }
            .mapValues { (_, l) -> l.fold(Duration.ZERO) { acc, n -> acc + n.asleep } }
        val recent = (0 until days).map { i ->
            val d = today.minusDays(i.toLong())
            d to (byDate[d] ?: Duration.ZERO)
        }
        val cutoff = now.minus(Duration.ofHours(36))
        val last = nights.filter { it.end >= cutoff && it.end <= now }.maxByOrNull { it.end }
        return SleepSummary(last, recent)
    }

    suspend fun weight(days: Int = 30): WeightSummary {
        val entries = preferPerDate(rowsFor(RecordType.WEIGHT, days)) { dateOf(it) }.map { WeightEntry(dateOf(it), Instant.ofEpochMilli(it.startTime), it.value) }
        return buildWeightSummary(entries)
    }

    suspend fun body(metric: BodyMetric, days: Int = 30): MetricSummary {
        val type = when (metric) {
            BodyMetric.BODY_FAT -> RecordType.BODY_FAT
            BodyMetric.BONE_MASS -> RecordType.BONE_MASS
            BodyMetric.LEAN_BODY_MASS -> RecordType.LEAN_BODY_MASS
            BodyMetric.BODY_WATER_MASS -> RecordType.BODY_WATER_MASS
        }
        val entries = preferPerDate(rowsFor(type, days)) { dateOf(it) }.map { MetricEntry(dateOf(it), Instant.ofEpochMilli(it.startTime), it.value) }
        return buildMetricSummary(metric, entries)
    }

    fun lastSyncFlow(): Flow<Long?> =
        dao.syncStatesFlow().map { list -> list.maxOfOrNull { it.syncedUntil } }

    private suspend fun rowsFor(type: String, days: Int): List<HealthRecordEntity> {
        val from = LocalDate.now(zone).minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.recordsBetween(type, from, System.currentTimeMillis())
    }

    /** 날짜별로 health_connect 행이 있으면 그것만, 없으면 다른 출처 행 사용 */
    private fun preferPerDate(rows: List<HealthRecordEntity>, dateOf: (HealthRecordEntity) -> LocalDate): List<HealthRecordEntity> =
        rows.groupBy(dateOf).values.flatMap { preferHealthConnect(it) }

    private fun dateOf(r: HealthRecordEntity): LocalDate =
        Instant.ofEpochMilli(r.startTime).atZone(zone).toLocalDate()

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
