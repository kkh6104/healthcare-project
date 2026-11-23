package com.gh.healthapp

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SleepNight(
    val wakeDate: LocalDate,
    val start: Instant,
    val end: Instant,
    val asleep: Duration,
    val deep: Duration?,
    val light: Duration?,
    val rem: Duration?,
    val awake: Duration?,
)

/** recent: 최근 7일, 최신 날짜 먼저, 기록 없으면 Duration.ZERO */
data class SleepSummary(val lastNight: SleepNight?, val recent: List<Pair<LocalDate, Duration>>)

class SleepRepository(private val client: HealthConnectClient) {

    suspend fun summary(days: Int = 7): SleepSummary {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val start = LocalDate.now(zone).minusDays(days.toLong()).atStartOfDay(zone).toInstant()
        val sessions = hcCall("readRecords(sleep)", { it.records.size }) {
            client.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, now), pageSize = 1000)
            )
        }.records
        return buildSleepSummary(sessions, days, now, zone)
    }
}

private val AWAKE_TYPES = setOf(
    SleepSessionRecord.STAGE_TYPE_AWAKE,
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
)

private fun SleepSessionRecord.stageSum(types: Set<Int>): Duration =
    stages.filter { it.stage in types }
        .fold(Duration.ZERO) { acc, st -> acc + Duration.between(st.startTime, st.endTime) }

/** HealthConnectClient 호출과 분리된 순수 계산 */
internal fun toSleepNight(record: SleepSessionRecord, zone: ZoneId): SleepNight {
    val total = Duration.between(record.startTime, record.endTime)
    val date = record.endTime.atZone(zone).toLocalDate()
    if (record.stages.isEmpty()) {
        return SleepNight(date, record.startTime, record.endTime, total, null, null, null, null)
    }
    val awake = record.stageSum(AWAKE_TYPES)
    return SleepNight(
        wakeDate = date,
        start = record.startTime,
        end = record.endTime,
        asleep = (total - awake).coerceAtLeast(Duration.ZERO),
        deep = record.stageSum(setOf(SleepSessionRecord.STAGE_TYPE_DEEP)),
        light = record.stageSum(setOf(SleepSessionRecord.STAGE_TYPE_LIGHT)),
        rem = record.stageSum(setOf(SleepSessionRecord.STAGE_TYPE_REM)),
        awake = awake,
    )
}

internal fun buildSleepSummary(
    sessions: List<SleepSessionRecord>,
    days: Int,
    now: Instant,
    zone: ZoneId,
): SleepSummary {
    val nights = sessions.filter { !it.endTime.isAfter(now) }.map { toSleepNight(it, zone) }
    val today = now.atZone(zone).toLocalDate()
    val byDate = nights.groupBy { it.wakeDate }
        .mapValues { (_, list) -> list.fold(Duration.ZERO) { acc, n -> acc + n.asleep } }
    val recent = (0 until days).map { i ->
        val d = today.minusDays(i.toLong())
        d to (byDate[d] ?: Duration.ZERO)
    }
    val cutoff = now.minus(Duration.ofHours(36))
    val last = nights.filter { it.end >= cutoff && it.end <= now }.maxByOrNull { it.end }
    return SleepSummary(last, recent)
}
