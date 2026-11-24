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
/**
 * 수면일 하나의 합산. 수면일 = 세션 종료 시각의 날짜(기기 시간대)라서 자정을 넘긴 세션은 다음날로 센다.
 * 단계(deep/light/rem/awake)는 단계 정보가 있는 세션만 더하고, 하나도 없으면 null.
 */
data class SleepDay(
    val date: LocalDate,
    val sessions: List<SleepNight>,
    val asleep: Duration,
    val deep: Duration?,
    val light: Duration?,
    val rem: Duration?,
    val awake: Duration?,
    val firstStart: Instant,
    val lastEnd: Instant,
)

/** day: 화면에 보여 줄 하루 합산 (오늘 수면일, 오늘 기록이 없으면 어제). 없으면 null */
data class SleepSummary(
    val lastNight: SleepNight?,
    val recent: List<Pair<LocalDate, Duration>>,
    val day: SleepDay? = null,
)

/** 세션들을 수면일(종료 날짜)별로 합산. 최신 날짜가 먼저 */
internal fun groupSleepDays(nights: List<SleepNight>): List<SleepDay> =
    nights.groupBy { it.wakeDate }.map { (date, list) ->
        val sessions = list.sortedBy { it.start }
        fun sum(pick: (SleepNight) -> Duration?): Duration? {
            val parts = sessions.mapNotNull(pick)
            return if (parts.isEmpty()) null else parts.fold(Duration.ZERO) { a, d -> a + d }
        }
        SleepDay(
            date = date,
            sessions = sessions,
            asleep = sessions.fold(Duration.ZERO) { a, n -> a + n.asleep },
            deep = sum { it.deep },
            light = sum { it.light },
            rem = sum { it.rem },
            awake = sum { it.awake },
            firstStart = sessions.minOf { it.start },
            lastEnd = sessions.maxOf { it.end },
        )
    }.sortedByDescending { it.date }

/** 오늘 수면일이 있으면 그것, 없으면 어제 */
internal fun pickDisplayDay(days: List<SleepDay>, today: LocalDate): SleepDay? =
    days.firstOrNull { it.date == today } ?: days.firstOrNull { it.date == today.minusDays(1) }

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
    return SleepSummary(last, recent, pickDisplayDay(groupSleepDays(nights), today))
}
