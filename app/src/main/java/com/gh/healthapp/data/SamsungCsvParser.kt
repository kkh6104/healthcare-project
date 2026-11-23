package com.gh.healthapp.data

import java.io.Reader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

enum class CsvKind { PEDOMETER_DAY_SUMMARY, HEART_RATE, WEIGHT, SLEEP, SLEEP_STAGE }

data class CsvImportResult(
    val records: List<HealthRecordEntity>,
    val countsByType: Map<String, Int>,
    val dropped: Int,
    val warnings: List<String>,
)

/** 삼성헬스 내보내기 CSV 해석기 (Android 의존 없음) */
object SamsungCsvParser {

    private const val PEDOMETER = "com.samsung.shealth.tracker.pedometer_day_summary"
    private const val HEART = "com.samsung.shealth.tracker.heart_rate"
    private const val WEIGHT = "com.samsung.health.weight"
    private const val SLEEP = "com.samsung.shealth.sleep"
    private const val SLEEP_STAGE = "com.samsung.health.sleep_stage"

    fun kindOf(fileName: String): CsvKind? {
        val n = fileName.substringAfterLast('/').substringAfterLast('\\')
        if (!n.endsWith(".csv", ignoreCase = true)) return null
        fun has(prefix: String) = n.startsWith(prefix) && n.length > prefix.length && n[prefix.length] == '.'
        return when {
            has(PEDOMETER) -> CsvKind.PEDOMETER_DAY_SUMMARY
            has(HEART) -> CsvKind.HEART_RATE
            has(WEIGHT) -> CsvKind.WEIGHT
            has(SLEEP_STAGE) -> CsvKind.SLEEP_STAGE
            has(SLEEP) -> CsvKind.SLEEP
            else -> null
        }
    }

    // ---------- 공통 ----------

    /** RFC4180 파서. 따옴표 안의 쉼표/줄바꿈/"" 처리 */
    internal fun parseCsv(reader: Reader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var any = false
        var first = true
        fun endField() { row.add(sb.toString()); sb.setLength(0) }
        fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
            any = false
        }
        var c = reader.read()
        while (c != -1) {
            val ch = c.toChar()
            if (first) {
                first = false
                if (c == 0xFEFF) { c = reader.read(); continue }
            }
            if (inQuotes) {
                if (ch == '"') {
                    val next = reader.read()
                    if (next == '"'.code) sb.append('"')
                    else {
                        inQuotes = false
                        c = next
                        continue
                    }
                } else sb.append(ch)
            } else {
                when (ch) {
                    '"' -> { inQuotes = true; any = true }
                    ',' -> { endField(); any = true }
                    '\r' -> {
                        val next = reader.read()
                        endRow()
                        c = next
                        if (next == '\n'.code) c = reader.read()
                        continue
                    }
                    '\n' -> endRow()
                    else -> { sb.append(ch); any = true }
                }
            }
            c = reader.read()
        }
        if (any || sb.isNotEmpty()) endRow()
        return rows
    }

    private class Table(val header: Map<String, Int>, val rows: List<List<String>>) {
        fun get(row: List<String>, name: String): String? =
            header[name]?.let { row.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** 1행 메타데이터 건너뛰기, 2행 헤더(접미사로 컬럼 찾기) */
    private fun readTable(reader: Reader): Table? {
        val all = parseCsv(reader)
        if (all.size < 2) return null
        val header = HashMap<String, Int>()
        all[1].forEachIndexed { i, raw ->
            val name = raw.trim().substringAfterLast('.')
            if (name.isNotEmpty() && name !in header) header[name] = i
        }
        return Table(header, all.drop(2).filter { r -> r.any { it.isNotBlank() } })
    }

    private fun parseUtc(s: String?): Instant? {
        if (s == null) return null
        return try {
            LocalDateTime.parse(s.trim().replace(' ', 'T')).toInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            null
        }
    }

    private val OFFSET_RE = Regex("""UTC([+-])(\d{1,2}):?(\d{2})?""")

    private fun parseOffsetSec(s: String?): Int? {
        val m = OFFSET_RE.matchEntire(s?.trim() ?: return null) ?: return null
        val sign = if (m.groupValues[1] == "-") -1 else 1
        val h = m.groupValues[2].toInt()
        val min = m.groupValues[3].ifEmpty { "0" }.toInt()
        return sign * (h * 3600 + min * 60)
    }

    private fun plausible(t: Instant, now: Instant): Boolean =
        t >= Instant.parse("2000-01-01T00:00:00Z") && t <= now.plus(1, ChronoUnit.DAYS)

    private fun num(v: Double): String =
        if (v == Math.rint(v) && Math.abs(v) < 1e15) v.toLong().toString() else v.toString()

    private fun result(records: List<HealthRecordEntity>, dropped: Int, warnings: List<String>) =
        CsvImportResult(records, records.groupingBy { it.type }.eachCount(), dropped, warnings)

    private fun missing(name: String, table: Table, vararg cols: String): String? {
        val m = cols.filter { it !in table.header }
        return if (m.isEmpty()) null else "$name: 필요한 컬럼 없음 ${m.joinToString()}"
    }

    // ---------- 걸음 ----------

    fun parsePedometerDaySummary(reader: Reader, zone: ZoneId, now: Instant): CsvImportResult {
        val t = readTable(reader) ?: return result(emptyList(), 0, listOf("걸음 CSV: 내용이 없습니다."))
        missing("걸음 CSV", t, "day_time", "step_count")?.let { return result(emptyList(), 0, listOf(it)) }
        var dropped = 0
        class Day(val steps: Double, val pkg: String?, val offsetSec: Int?)
        val best = HashMap<LocalDate, Day>()
        for (r in t.rows) {
            val dayStr = t.get(r, "day_time")
            val steps = t.get(r, "step_count")?.toDoubleOrNull()
            val date = try { dayStr?.let { LocalDate.parse(it.trim().take(10)) } } catch (e: Exception) { null }
            if (date == null || steps == null) { dropped++; continue }
            if (!plausible(date.atStartOfDay(ZoneOffset.UTC).toInstant(), now)) { dropped++; continue }
            val cur = best[date]
            if (cur == null || steps > cur.steps) {
                best[date] = Day(steps, t.get(r, "source_package_name"), parseOffsetSec(t.get(r, "time_offset")))
            }
        }
        val records = best.entries.sortedBy { it.key }.map { (date, d) ->
            val z: ZoneId = d.offsetSec?.let { ZoneOffset.ofTotalSeconds(it) } ?: zone
            val start = date.atStartOfDay(z).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()
            HealthRecordEntity(
                type = RecordType.STEPS, startTime = start, endTime = end,
                zoneOffsetSec = d.offsetSec ?: z.rules.getOffset(Instant.ofEpochMilli(start)).totalSeconds,
                value = d.steps, unit = "count", source = RecordSource.SAMSUNG_CSV,
                originPackage = d.pkg, externalId = "day:$date", lastModified = null, extraJson = null,
            )
        }
        return result(records, dropped, emptyList())
    }

    // ---------- 심박 ----------

    fun parseHeartRate(reader: Reader, now: Instant): CsvImportResult {
        val t = readTable(reader) ?: return result(emptyList(), 0, listOf("심박 CSV: 내용이 없습니다."))
        missing("심박 CSV", t, "start_time", "heart_rate", "datauuid")?.let { return result(emptyList(), 0, listOf(it)) }
        var dropped = 0
        val out = mutableListOf<HealthRecordEntity>()
        for (r in t.rows) {
            val start = parseUtc(t.get(r, "start_time"))
            val value = t.get(r, "heart_rate")?.toDoubleOrNull()
            val id = t.get(r, "datauuid")
            if (start == null || value == null || id == null || !plausible(start, now)) { dropped++; continue }
            val end = parseUtc(t.get(r, "end_time"))?.takeIf { it >= start } ?: start
            val min = t.get(r, "min")?.toDoubleOrNull()
            val max = t.get(r, "max")?.toDoubleOrNull()
            val extra = listOfNotNull(min?.let { "\"min\":${num(it)}" }, max?.let { "\"max\":${num(it)}" })
                .takeIf { it.isNotEmpty() }?.joinToString(",", "{", "}")
            out += HealthRecordEntity(
                type = RecordType.HEART_RATE, startTime = start.toEpochMilli(), endTime = end.toEpochMilli(),
                zoneOffsetSec = parseOffsetSec(t.get(r, "time_offset")), value = value, unit = "bpm",
                source = RecordSource.SAMSUNG_CSV, originPackage = t.get(r, "pkg_name"),
                externalId = id, lastModified = null, extraJson = extra,
            )
        }
        return result(out, dropped, emptyList())
    }

    // ---------- 체중/체성분 ----------

    fun parseWeight(reader: Reader, now: Instant): CsvImportResult {
        val t = readTable(reader) ?: return result(emptyList(), 0, listOf("체중 CSV: 내용이 없습니다."))
        missing("체중 CSV", t, "start_time", "datauuid")?.let { return result(emptyList(), 0, listOf(it)) }
        val cols = listOf(
            "weight" to (RecordType.WEIGHT to "kg"),
            "body_fat" to (RecordType.BODY_FAT to "%"),
            "fat_free_mass" to (RecordType.LEAN_BODY_MASS to "kg"),
            "total_body_water" to (RecordType.BODY_WATER_MASS to "kg"),
        )
        var dropped = 0
        val out = mutableListOf<HealthRecordEntity>()
        for (r in t.rows) {
            val start = parseUtc(t.get(r, "start_time"))
            val id = t.get(r, "datauuid")
            if (start == null || id == null || !plausible(start, now)) { dropped++; continue }
            var any = false
            for ((col, tu) in cols) {
                val v = t.get(r, col)?.toDoubleOrNull() ?: continue
                any = true
                out += HealthRecordEntity(
                    type = tu.first, startTime = start.toEpochMilli(), endTime = start.toEpochMilli(),
                    zoneOffsetSec = parseOffsetSec(t.get(r, "time_offset")), value = v, unit = tu.second,
                    source = RecordSource.SAMSUNG_CSV, originPackage = t.get(r, "pkg_name"),
                    externalId = "$id:${tu.first}", lastModified = null, extraJson = null,
                )
            }
            if (!any) dropped++
        }
        return result(out, dropped, emptyList())
    }

    // ---------- 수면 ----------

    fun parseSleep(sleep: Reader, stages: Reader?, now: Instant): CsvImportResult {
        val t = readTable(sleep) ?: return result(emptyList(), 0, listOf("수면 CSV: 내용이 없습니다."))
        missing("수면 CSV", t, "start_time", "end_time", "datauuid")?.let { return result(emptyList(), 0, listOf(it)) }
        val warnings = mutableListOf<String>()
        // sleep_id -> stage code -> seconds
        val stageSec = HashMap<String, HashMap<Int, Long>>()
        if (stages != null) {
            val st = readTable(stages)
            if (st == null) warnings += "수면 단계 CSV: 내용이 없습니다."
            else {
                val miss = missing("수면 단계 CSV", st, "sleep_id", "start_time", "end_time", "stage")
                if (miss != null) warnings += miss
                else for (r in st.rows) {
                    val id = st.get(r, "sleep_id") ?: continue
                    val s = parseUtc(st.get(r, "start_time")) ?: continue
                    val e = parseUtc(st.get(r, "end_time")) ?: continue
                    val code = st.get(r, "stage")?.toDoubleOrNull()?.toInt() ?: continue
                    val sec = Math.max(0L, e.epochSecond - s.epochSecond)
                    stageSec.getOrPut(id) { HashMap() }.merge(code, sec, Long::plus)
                }
            }
        }
        var dropped = 0
        val out = mutableListOf<HealthRecordEntity>()
        for (r in t.rows) {
            val start = parseUtc(t.get(r, "start_time"))
            val end = parseUtc(t.get(r, "end_time"))
            val id = t.get(r, "datauuid")
            if (start == null || end == null || id == null || !plausible(start, now) || !plausible(end, now)) { dropped++; continue }
            val totalSec = end.epochSecond - start.epochSecond
            if (totalSec <= 0 || totalSec > 24 * 3600) { dropped++; continue }
            val total = totalSec / 60
            val sec = stageSec[id]
            var value = total.toDouble()
            var extra: String? = null
            if (sec != null && sec.isNotEmpty()) {
                val deep = (sec[40003] ?: 0L) / 60
                val light = (sec[40002] ?: 0L) / 60
                val rem = (sec[40004] ?: 0L) / 60
                val awake = (sec[40001] ?: 0L) / 60
                value = Math.max(0L, total - awake).toDouble()
                extra = "{\"deep\":$deep,\"light\":$light,\"rem\":$rem,\"awake\":$awake}"
            }
            out += HealthRecordEntity(
                type = RecordType.SLEEP_SESSION, startTime = start.toEpochMilli(), endTime = end.toEpochMilli(),
                zoneOffsetSec = parseOffsetSec(t.get(r, "time_offset")), value = value, unit = "min",
                source = RecordSource.SAMSUNG_CSV, originPackage = t.get(r, "pkg_name") ?: t.get(r, "deviceuuid"),
                externalId = id, lastModified = null, extraJson = extra,
            )
        }
        return result(out, dropped, warnings)
    }
}
