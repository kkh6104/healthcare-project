package com.gh.healthapp

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class BodyMetric(val label: String, val unit: String) {
    BODY_FAT("체지방률", "%"),
    BONE_MASS("골질량", "kg"),
    LEAN_BODY_MASS("제지방량", "kg"),
    BODY_WATER_MASS("체내 수분량", "kg"),
}

data class MetricEntry(val date: LocalDate, val time: Instant, val value: Double)

data class MetricSummary(
    val metric: BodyMetric,
    val latest: MetricEntry?,
    val changeFromWeekAgo: Double?,
    val recent: List<MetricEntry>,
)

class BodyCompositionRepository(private val client: HealthConnectClient) {

    companion object {
        fun readPermission(metric: BodyMetric): String = when (metric) {
            BodyMetric.BODY_FAT -> HealthPermission.getReadPermission(BodyFatRecord::class)
            BodyMetric.BONE_MASS -> HealthPermission.getReadPermission(BoneMassRecord::class)
            BodyMetric.LEAN_BODY_MASS -> HealthPermission.getReadPermission(LeanBodyMassRecord::class)
            BodyMetric.BODY_WATER_MASS -> HealthPermission.getReadPermission(BodyWaterMassRecord::class)
        }
    }

    /** 한 번에 한 종류만 조회 */
    suspend fun summary(metric: BodyMetric, days: Int = 30): MetricSummary {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).minusDays(days.toLong()).atStartOfDay(zone).toInstant()
        val range = TimeRangeFilter.between(start, Instant.now())
        val entries: List<MetricEntry> = when (metric) {
            BodyMetric.BODY_FAT ->
                client.readAllPages("readRecords(bodyFat)") { token ->
                    ReadRecordsRequest(BodyFatRecord::class, range, pageSize = 1000, pageToken = token)
                }.map { entry(it.time, it.percentage.value, zone) }
            BodyMetric.BONE_MASS ->
                client.readAllPages("readRecords(boneMass)") { token ->
                    ReadRecordsRequest(BoneMassRecord::class, range, pageSize = 1000, pageToken = token)
                }.map { entry(it.time, it.mass.inKilograms, zone) }
            BodyMetric.LEAN_BODY_MASS ->
                client.readAllPages("readRecords(leanBodyMass)") { token ->
                    ReadRecordsRequest(LeanBodyMassRecord::class, range, pageSize = 1000, pageToken = token)
                }.map { entry(it.time, it.mass.inKilograms, zone) }
            BodyMetric.BODY_WATER_MASS ->
                client.readAllPages("readRecords(bodyWaterMass)") { token ->
                    ReadRecordsRequest(BodyWaterMassRecord::class, range, pageSize = 1000, pageToken = token)
                }.map { entry(it.time, it.mass.inKilograms, zone) }
        }
        return buildMetricSummary(metric, entries)
    }

    private fun entry(time: Instant, value: Double, zone: ZoneId) =
        MetricEntry(time.atZone(zone).toLocalDate(), time, value)
}

internal fun buildMetricSummary(metric: BodyMetric, entries: List<MetricEntry>): MetricSummary {
    val recent = entries
        .groupBy { it.date }
        .map { (_, list) -> list.maxByOrNull { it.time }!! }
        .sortedByDescending { it.date }
    val latest = recent.firstOrNull()
    val change = latest?.let { l ->
        recent.firstOrNull { ChronoUnit.DAYS.between(it.date, l.date) >= 7 }?.let { l.value - it.value }
    }
    return MetricSummary(metric, latest, change, recent)
}
