package com.gh.healthapp.chat

import com.gh.healthapp.SleepNight
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.RecordType
import com.gh.healthapp.data.preferHealthConnect
import com.gh.healthapp.groupSleepDays
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

internal data class DateRange(val start: LocalDate, val end: LocalDate)

internal fun parseDateRange(start: String, end: String, maxDays: Long): DateRange? = try {
    val from = LocalDate.parse(start)
    val to = LocalDate.parse(end)
    if (ChronoUnit.DAYS.between(from, to) in 0 until maxDays) DateRange(from, to) else null
} catch (_: Exception) { null }

internal class HealthTools(private val db: HealthDatabase) {
    private val zone get() = ZoneId.systemDefault()
    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    suspend fun execute(name: String, args: JSONObject): JSONObject {
        if (name == "get_data_range") return dataRange().let { if (it.length() == 0) it.put("message", "해당 기간 데이터 없음") else it }
        val max = if (name == "get_sleep_days") 60L else 120L
        val range = parseDateRange(args.optString("start_date"), args.optString("end_date"), max)
            ?: return JSONObject().put("error", "날짜 또는 조회 기간이 올바르지 않습니다 (최대 ${max}일).")
        val result = when (name) {
            "get_daily_summary" -> daily(range)
            "get_period_stats" -> stats(range)
            "get_sleep_days" -> sleep(range)
            "get_walk_sessions" -> walks(range)
            else -> JSONObject().put("error", "알 수 없는 도구입니다.")
        }
        val empty = when (name) {
            "get_daily_summary", "get_sleep_days" -> result.optJSONArray("days")?.length() == 0
            "get_walk_sessions" -> result.optJSONArray("sessions")?.length() == 0
            "get_period_stats" -> listOf("steps", "sleep_minutes", "hr_avg", "weight_kg").all { result.optJSONObject(it)?.optInt("count", 0) == 0 }
            else -> false
        }
        if (empty) result.put("message", "해당 기간 데이터 없음")
        return result
    }
    private suspend fun daily(range: DateRange): JSONObject {
        val rows = db.dao().summariesBetween(range.start.toString(), range.end.toString())
        return JSONObject().put("days", JSONArray().apply { rows.forEach { r -> put(JSONObject().apply {
            put("date", r.date); put("steps", r.steps); put("sleep_minutes", r.sleepMinutes)
            put("hr_avg", r.hrAvg); put("hr_min", r.hrMin); put("hr_max", r.hrMax)
            put("weight_kg", r.weightKg); put("body_fat_pct", r.bodyFatPct); put("bone_mass_kg", r.boneMassKg)
            put("lean_mass_kg", r.leanMassKg); put("body_water_kg", r.bodyWaterKg); put("walk_distance_m", r.walkDistanceM)
        }) } })
    }
    private suspend fun sleepDays(range: DateRange): List<com.gh.healthapp.SleepDay> {
        val from = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = range.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rows = db.dao().recordsEndingBetween(RecordType.SLEEP_SESSION, from, to)
        val nights = rows.groupBy { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }
            .values.flatMap { preferHealthConnect(it) }.map { r ->
                val json = r.extraJson?.let { runCatching { JSONObject(it) }.getOrNull() }
                fun stage(key: String): Duration? = json?.optLong(key, Long.MIN_VALUE)?.takeIf { it != Long.MIN_VALUE }?.let(Duration::ofMinutes)
                val end = Instant.ofEpochMilli(r.endTime)
                SleepNight(end.atZone(zone).toLocalDate(), Instant.ofEpochMilli(r.startTime), end,
                    Duration.ofMinutes(r.value.roundToLong()), stage("deep"), stage("light"), stage("rem"), stage("awake"))
            }
        return groupSleepDays(nights)
    }
    private suspend fun sleep(range: DateRange): JSONObject = JSONObject().put("days", JSONArray().apply {
        sleepDays(range).forEach { d -> put(JSONObject().apply {
            put("date", d.date.toString()); put("total_min", d.asleep.toMinutes()); put("sessions", d.sessions.size)
            put("first_start", d.firstStart.atZone(zone).format(clock)); put("last_end", d.lastEnd.atZone(zone).format(clock))
            put("deep_min", d.deep?.toMinutes()); put("light_min", d.light?.toMinutes()); put("rem_min", d.rem?.toMinutes()); put("awake_min", d.awake?.toMinutes())
        }) }
    })
    private suspend fun stats(range: DateRange): JSONObject {
        val rows = db.dao().summariesBetween(range.start.toString(), range.end.toString())
        val nights = sleepDays(range)
        fun stats(values: List<Double>): JSONObject = JSONObject().apply {
            put("count", values.size)
            if (values.isNotEmpty()) { put("average", values.average()); put("min", values.min()); put("max", values.max()) }
        }
        fun avg(values: List<Long>): Double? = values.takeIf { it.isNotEmpty() }?.map { it.toDouble() }?.average()
        return JSONObject().apply {
            put("steps", stats(rows.mapNotNull { it.steps?.toDouble() }))
            put("sleep_minutes", stats(rows.mapNotNull { it.sleepMinutes?.toDouble() }))
            put("hr_avg", stats(rows.mapNotNull { it.hrAvg })); put("weight_kg", stats(rows.mapNotNull { it.weightKg }))
            put("sleep_stage_average_min", JSONObject().apply {
                put("deep", avg(nights.mapNotNull { it.deep?.toMinutes() })); put("light", avg(nights.mapNotNull { it.light?.toMinutes() }))
                put("rem", avg(nights.mapNotNull { it.rem?.toMinutes() })); put("awake", avg(nights.mapNotNull { it.awake?.toMinutes() }))
            })
        }
    }
    private suspend fun walks(range: DateRange): JSONObject {
        val from = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = range.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return JSONObject().put("sessions", JSONArray().apply { db.walkDao().recentDoneBetween(from, to).forEach { s ->
            val start = Instant.ofEpochMilli(s.startTime).atZone(zone)
            put(JSONObject().apply {
                put("date", start.toLocalDate().toString()); put("start", start.format(clock)); put("distance_m", s.distanceM)
                put("active_min", s.activeMs / 60000.0); put("steps", s.steps)
                if (s.distanceM > 0) put("pace_min_per_km", s.activeMs / 60000.0 / (s.distanceM / 1000.0))
            })
        } })
    }
    private suspend fun dataRange(): JSONObject {
        val result = JSONObject()
        db.dao().recordRanges().filter { it.type in setOf(RecordType.STEPS, RecordType.HEART_RATE, RecordType.SLEEP_SESSION, RecordType.WEIGHT, RecordType.BODY_FAT, RecordType.BONE_MASS, RecordType.LEAN_BODY_MASS, RecordType.BODY_WATER_MASS) }.forEach {
            result.put(it.type, JSONObject().put("first", Instant.ofEpochMilli(it.firstTime).atZone(zone).toLocalDate().toString()).put("last", Instant.ofEpochMilli(it.lastTime).atZone(zone).toLocalDate().toString()))
        }
        val first = db.walkDao().firstDone(); val last = db.walkDao().lastDone()
        if (first != null && last != null) result.put("walk", JSONObject().put("first", Instant.ofEpochMilli(first).atZone(zone).toLocalDate().toString()).put("last", Instant.ofEpochMilli(last).atZone(zone).toLocalDate().toString()))
        return result
    }
}
