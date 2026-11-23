package com.gh.healthapp

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class WeightEntry(val date: LocalDate, val time: Instant, val kg: Double)

data class WeightSummary(
    val latest: WeightEntry?,
    val changeKg: Double?,
    val recent: List<WeightEntry>,
)

class WeightRepository(private val client: HealthConnectClient) {

    suspend fun summary(days: Int = 30): WeightSummary {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).minusDays(days.toLong()).atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val records = client.readAllPages("readRecords(weight)") { token ->
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageSize = 1000,
                pageToken = token,
            )
        }
        val entries = records.map { WeightEntry(it.time.atZone(zone).toLocalDate(), it.time, it.weight.inKilograms) }
        return buildWeightSummary(entries)
    }
}

internal fun buildWeightSummary(entries: List<WeightEntry>): WeightSummary {
    val recent = entries
        .groupBy { it.date }
        .map { (_, list) -> list.maxByOrNull { it.time }!! }
        .sortedByDescending { it.date }
    val latest = recent.firstOrNull()
    val changeKg = latest?.let { l ->
        recent.firstOrNull { ChronoUnit.DAYS.between(it.date, l.date) >= 7 }?.let { l.kg - it.kg }
    }
    return WeightSummary(latest, changeKg, recent)
}
