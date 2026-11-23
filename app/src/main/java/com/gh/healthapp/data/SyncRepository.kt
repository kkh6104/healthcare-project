package com.gh.healthapp.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.gh.healthapp.hcCall
import com.gh.healthapp.readAllPagesResult
import com.gh.healthapp.toSleepNight
import org.json.JSONObject
import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset

data class SyncResult(
    val type: String,
    val fetched: Int,
    val affectedDates: Set<LocalDate>,
    val stepsByDate: Map<LocalDate, Long> = emptyMap(),
    val truncated: Boolean = false,
)

/** Health Connect → Room 동기화. [sync]는 한 종류만 처리하며 ViewModel이 종류별로 순차 호출한다. */
class SyncRepository(
    private val client: HealthConnectClient,
    private val dao: HealthDao,
) {
    companion object {
        const val INITIAL_DAYS = 30L
        const val DATE_PATTERN = "yyyy-MM-dd"
    }

    /** 한 종류의 레코드를 읽어(오래된 것부터) 매핑한다. 5페이지 제한에 걸리면 truncated=true */
    private suspend fun <T : Record> fetch(
        name: String,
        request: (String?) -> ReadRecordsRequest<T>,
        map: (T) -> List<HealthRecordEntity>,
    ): Pair<List<HealthRecordEntity>, Boolean> {
        val paged = client.readAllPagesResult(name, maxPages = 5, request)
        return paged.records.flatMap(map) to paged.truncated
    }

    suspend fun sync(type: String, now: Instant = Instant.now()): SyncResult {
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        val state = dao.syncState(type)
        val start: Instant =
            if (state == null) today.minusDays(INITIAL_DAYS).atStartOfDay(zone).toInstant()
            else Instant.ofEpochMilli(state.syncedUntil).minus(Duration.ofDays(1))
        val range = TimeRangeFilter.between(start, now)

        val (rows, truncated) = when (type) {
            RecordType.STEPS -> fetch("sync(steps)", { t ->
                ReadRecordsRequest(StepsRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapSteps(it)) }
            RecordType.HEART_RATE -> fetch("sync(heartRate)", { t ->
                ReadRecordsRequest(HeartRateRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { mapHeartRate(it) }
            RecordType.SLEEP_SESSION -> fetch("sync(sleep)", { t ->
                ReadRecordsRequest(SleepSessionRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapSleep(it, zone)) }
            RecordType.WEIGHT -> fetch("sync(weight)", { t ->
                ReadRecordsRequest(WeightRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapWeight(it)) }
            RecordType.BODY_FAT -> fetch("sync(bodyFat)", { t ->
                ReadRecordsRequest(BodyFatRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapBodyFat(it)) }
            RecordType.BONE_MASS -> fetch("sync(boneMass)", { t ->
                ReadRecordsRequest(BoneMassRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapBoneMass(it)) }
            RecordType.LEAN_BODY_MASS -> fetch("sync(leanBodyMass)", { t ->
                ReadRecordsRequest(LeanBodyMassRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapLeanBodyMass(it)) }
            RecordType.BODY_WATER_MASS -> fetch("sync(bodyWaterMass)", { t ->
                ReadRecordsRequest(BodyWaterMassRecord::class, range, ascendingOrder = true, pageSize = 1000, pageToken = t)
            }) { listOf(mapBodyWaterMass(it)) }
            else -> throw IllegalArgumentException("unknown record type: $type")
        }

        dao.upsertRecords(rows)

        val dates = affectedDates(rows, type, start, now, zone)
        val stepsByDate = if (type == RecordType.STEPS) dailyStepTotals(dates) else emptyMap()

        // 모든 작업이 성공한 뒤에만 동기화 시각 갱신. 다 못 읽었으면 받은 것 중 가장 늦은 시각까지만
        val syncedUntil = if (truncated) rows.maxOfOrNull { it.endTime } ?: state?.syncedUntil ?: now.toEpochMilli() else now.toEpochMilli()
        if (truncated) {
            Log.w("HCDEBUG", "sync($type) truncated, syncedUntil=${Instant.ofEpochMilli(syncedUntil)}")
        }
        dao.upsertSyncState(SyncStateEntity(type, syncedUntil))
        return SyncResult(type, rows.size, dates, stepsByDate, truncated)
    }

    /** 폰+워치 중복은 Health Connect가 정리해 주는 집계값 사용 (원본 합산 금지) */
    private suspend fun dailyStepTotals(dates: Set<LocalDate>): Map<LocalDate, Long> {
        val first = dates.minOrNull() ?: return emptyMap()
        val last = dates.maxOrNull() ?: return emptyMap()
        val groups = hcCall("sync(stepsDaily)", { it.size }) {
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        first.atStartOfDay(),
                        last.plusDays(1).atStartOfDay(),
                    ),
                    timeRangeSlicer = Period.ofDays(1),
                )
            )
        }
        val byDate = groups.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }
            .associateWith { byDate[it] ?: 0L }
    }
}

// ---- HealthConnectClient 없이 테스트 가능한 순수 함수들 ----

internal fun dateString(date: LocalDate): String = date.toString() // yyyy-MM-dd

internal fun affectedDates(
    rows: List<HealthRecordEntity>,
    type: String,
    rangeStart: Instant,
    now: Instant,
    zone: ZoneId,
): Set<LocalDate> {
    val dates = mutableSetOf<LocalDate>()
    var d = rangeStart.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    while (!d.isAfter(today)) {
        dates += d
        d = d.plusDays(1)
    }
    rows.forEach { r ->
        // 수면은 깬 날짜(endTime), 그 외는 시작 시각 기준
        val t = if (type == RecordType.SLEEP_SESSION) r.endTime else r.startTime
        dates += Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
    }
    return dates
}

private fun Record.base(
    type: String,
    start: Instant,
    end: Instant,
    offset: ZoneOffset?,
    value: Double,
    unit: String,
    externalId: String,
    extraJson: String? = null,
) = HealthRecordEntity(
    type = type,
    startTime = start.toEpochMilli(),
    endTime = end.toEpochMilli(),
    zoneOffsetSec = offset?.totalSeconds,
    value = value,
    unit = unit,
    source = RecordSource.HEALTH_CONNECT,
    originPackage = metadata.dataOrigin.packageName,
    externalId = externalId,
    lastModified = metadata.lastModifiedTime.toEpochMilli(),
    extraJson = extraJson,
)

internal fun mapSteps(r: StepsRecord) =
    r.base(RecordType.STEPS, r.startTime, r.endTime, r.startZoneOffset, r.count.toDouble(), "count", r.metadata.id)

internal fun mapHeartRate(r: HeartRateRecord): List<HealthRecordEntity> =
    r.samples.mapIndexed { i, s ->
        r.base(RecordType.HEART_RATE, s.time, s.time, r.startZoneOffset, s.beatsPerMinute.toDouble(), "bpm", "${r.metadata.id}#$i")
    }

internal fun mapSleep(r: SleepSessionRecord, zone: ZoneId): HealthRecordEntity {
    val n = toSleepNight(r, zone)
    val extra = JSONObject().apply {
        n.deep?.let { put("deep", it.toMinutes()) }
        n.light?.let { put("light", it.toMinutes()) }
        n.rem?.let { put("rem", it.toMinutes()) }
        n.awake?.let { put("awake", it.toMinutes()) }
    }
    return r.base(
        RecordType.SLEEP_SESSION, r.startTime, r.endTime, r.startZoneOffset,
        n.asleep.toMinutes().toDouble(), "min", r.metadata.id, extra.toString(),
    )
}

internal fun mapWeight(r: WeightRecord) =
    r.base(RecordType.WEIGHT, r.time, r.time, r.zoneOffset, r.weight.inKilograms, "kg", r.metadata.id)

internal fun mapBodyFat(r: BodyFatRecord) =
    r.base(RecordType.BODY_FAT, r.time, r.time, r.zoneOffset, r.percentage.value, "%", r.metadata.id)

internal fun mapBoneMass(r: BoneMassRecord) =
    r.base(RecordType.BONE_MASS, r.time, r.time, r.zoneOffset, r.mass.inKilograms, "kg", r.metadata.id)

internal fun mapLeanBodyMass(r: LeanBodyMassRecord) =
    r.base(RecordType.LEAN_BODY_MASS, r.time, r.time, r.zoneOffset, r.mass.inKilograms, "kg", r.metadata.id)

internal fun mapBodyWaterMass(r: BodyWaterMassRecord) =
    r.base(RecordType.BODY_WATER_MASS, r.time, r.time, r.zoneOffset, r.mass.inKilograms, "kg", r.metadata.id)
