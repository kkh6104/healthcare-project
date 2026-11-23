package com.gh.healthapp.data

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** 오래된(기본 90일 이전) 심박 원본을 (출처, 1시간) 단위 행으로 묶어 DB 크기를 줄인다 */
class HeartRateRetention(private val dao: HealthDao) {

    /** 정리한 원본 행 수를 반환 */
    suspend fun run(now: Instant = Instant.now(), keepDays: Long = 90): Int {
        val before = now.minus(java.time.Duration.ofDays(keepDays)).toEpochMilli()
        val raw = dao.rawRecordsBefore(RecordType.HEART_RATE, before)
        if (raw.isEmpty()) return 0
        val (rolled, ids) = rollUpHourly(raw, ZoneId.systemDefault())
        dao.replaceRecords(rolled, ids)
        return ids.size
    }
}

/** DB 접근 없는 순수 함수: 원본 행 → (1시간 묶음 행 목록, 삭제할 원본 id 목록) */
internal fun rollUpHourly(raw: List<HealthRecordEntity>, zone: ZoneId): Pair<List<HealthRecordEntity>, List<Long>> {
    val groups = raw.groupBy { r ->
        val hourStart = Instant.ofEpochMilli(r.startTime).atZone(zone).truncatedTo(ChronoUnit.HOURS)
            .toInstant().toEpochMilli()
        r.source to hourStart
    }
    val rolled = groups.map { (key, rows) ->
        val (source, hourStart) = key
        val values = rows.map { it.value }
        val mins = rows.map { extraNumber(it, "min") ?: it.value }
        val maxs = rows.map { extraNumber(it, "max") ?: it.value }
        HealthRecordEntity(
            type = RecordType.HEART_RATE,
            startTime = hourStart,
            endTime = hourStart + 3_600_000L,
            zoneOffsetSec = rows.first().zoneOffsetSec,
            value = values.average(),
            unit = rows.first().unit,
            source = source,
            originPackage = rows.first().originPackage,
            externalId = "hourly:$source:$hourStart",
            lastModified = rows.mapNotNull { it.lastModified }.maxOrNull(),
            extraJson = JSONObject().apply {
                put("min", mins.min())
                put("max", maxs.max())
                put("n", rows.size)
            }.toString(),
        )
    }
    return rolled to raw.map { it.id }
}

/** extraJson의 숫자 필드 (없거나 파싱 실패면 null) */
internal fun extraNumber(r: HealthRecordEntity, key: String): Double? =
    r.extraJson?.let { s ->
        runCatching { JSONObject(s).takeIf { it.has(key) && !it.isNull(key) }?.getDouble(key) }.getOrNull()
    }
