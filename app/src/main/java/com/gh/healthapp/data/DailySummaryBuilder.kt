package com.gh.healthapp.data

import java.time.LocalDate
import java.time.ZoneId

class DailySummaryBuilder(private val dao: HealthDao) {

    suspend fun rebuild(dates: Set<LocalDate>, stepsByDate: Map<LocalDate, Long>) {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val result = dates.sorted().map { date ->
            val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val stepsRows = dao.recordsBetween(RecordType.STEPS, from, to - 1)
            val csvSteps = stepsRows.filter { it.source == RecordSource.SAMSUNG_CSV }
                .takeIf { it.isNotEmpty() }?.sumOf { it.value }?.toLong()
            val steps = stepsByDate[date] ?: dao.summary(date.toString())?.steps
            val sleepRows = dao.recordsBetween(RecordType.SLEEP_SESSION, from - DAY_MS, to)
            buildDailySummary(
                date = date,
                zone = zone,
                steps = steps,
                hrRows = dao.recordsBetween(RecordType.HEART_RATE, from, to - 1),
                sleepRows = sleepRows,
                weightRows = dao.recordsBetween(RecordType.WEIGHT, from, to - 1),
                bodyFatRows = dao.recordsBetween(RecordType.BODY_FAT, from, to - 1),
                boneRows = dao.recordsBetween(RecordType.BONE_MASS, from, to - 1),
                leanRows = dao.recordsBetween(RecordType.LEAN_BODY_MASS, from, to - 1),
                waterRows = dao.recordsBetween(RecordType.BODY_WATER_MASS, from, to - 1),
                nowMs = now,
                csvSteps = csvSteps,
            )
        }
        dao.upsertSummaries(result)
    }

    /** DB에 기록이 있는 모든 날짜를 다시 계산 (CSV 가져오기 후 사용) */
    suspend fun rebuildAll() {
        val zone = ZoneId.systemDefault()
        val min = dao.minRecordTime() ?: return
        val max = dao.maxRecordTime() ?: return
        val first = java.time.Instant.ofEpochMilli(min).atZone(zone).toLocalDate()
        val last = java.time.Instant.ofEpochMilli(max).atZone(zone).toLocalDate()
        val dates = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toSet()
        rebuild(dates, emptyMap())
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

/** 행 목록 → 하루 요약 (DB 접근 없는 순수 함수). 행은 해당 날짜 후보를 넉넉히 받아도 됨 */
internal fun buildDailySummary(
    date: LocalDate,
    zone: ZoneId,
    steps: Long?,
    hrRows: List<HealthRecordEntity>,
    sleepRows: List<HealthRecordEntity>,
    weightRows: List<HealthRecordEntity>,
    bodyFatRows: List<HealthRecordEntity>,
    boneRows: List<HealthRecordEntity>,
    leanRows: List<HealthRecordEntity>,
    waterRows: List<HealthRecordEntity>,
    nowMs: Long,
    csvSteps: Long? = null,
): DailySummaryEntity {
    val sleepOfDay = preferHealthConnect(sleepRows.filter {
        java.time.Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() == date
    })
    fun latest(rows: List<HealthRecordEntity>) = preferHealthConnect(rows).maxByOrNull { it.startTime }?.value
    val hr = hrStats(hrRows)
    return DailySummaryEntity(
        date = date.toString(),
        // HC 집계(폰+워치 중복 제거)와 CSV 중 큰 값
        steps = listOfNotNull(steps, csvSteps).maxOrNull(),
        hrAvg = hr?.avg,
        hrMin = hr?.min,
        hrMax = hr?.max,
        sleepMinutes = if (sleepOfDay.isEmpty()) null else sleepOfDay.sumOf { it.value }.toLong(),
        weightKg = latest(weightRows),
        bodyFatPct = latest(bodyFatRows),
        boneMassKg = latest(boneRows),
        leanMassKg = latest(leanRows),
        bodyWaterKg = latest(waterRows),
        updatedAt = nowMs,
    )
}

/** 출처 우선순위: health_connect 행이 있으면 그것만, 없으면 나머지(samsung_csv) 사용 */
internal fun preferHealthConnect(rows: List<HealthRecordEntity>): List<HealthRecordEntity> {
    val hc = rows.filter { it.source == RecordSource.HEALTH_CONNECT }
    return if (hc.isNotEmpty()) hc else rows
}

internal data class HrStats(val avg: Double, val min: Double, val max: Double)

/** 심박 행들의 통계. 출처 우선순위 적용, avg는 행의 n(없으면 1) 가중 평균, min/max는 행의 min/max(없으면 value) */
internal fun hrStats(rows: List<HealthRecordEntity>): HrStats? {
    val use = preferHealthConnect(rows)
    if (use.isEmpty()) return null
    var weight = 0.0
    var sum = 0.0
    for (r in use) {
        val n = extraNumber(r, "n") ?: 1.0
        weight += n
        sum += r.value * n
    }
    return HrStats(
        avg = sum / weight,
        min = use.minOf { extraNumber(it, "min") ?: it.value },
        max = use.maxOf { extraNumber(it, "max") ?: it.value },
    )
}
