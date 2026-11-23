package com.gh.healthapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader
import java.time.Instant
import java.time.ZoneId

class SamsungCsvParserTest {
    private val now = Instant.parse("2026-11-23T00:00:00Z")

    @Test fun kindOf() {
        assertEquals(CsvKind.WEIGHT, SamsungCsvParser.kindOf("com.samsung.health.weight.202611.csv"))
        assertEquals(CsvKind.SLEEP, SamsungCsvParser.kindOf("com.samsung.shealth.sleep.202611.csv"))
        assertEquals(CsvKind.SLEEP_STAGE, SamsungCsvParser.kindOf("com.samsung.health.sleep_stage.202611.csv"))
        assertEquals(CsvKind.HEART_RATE, SamsungCsvParser.kindOf("com.samsung.shealth.tracker.heart_rate.1.csv"))
        assertNull(SamsungCsvParser.kindOf("other.csv"))
    }

    @Test fun csvQuotes() {
        val rows = SamsungCsvParser.parseCsv(StringReader("a,\"b,c\",\"d\"\"e\"\n1,2,\n"))
        assertEquals(listOf("a", "b,c", "d\"e"), rows[0])
        assertEquals(listOf("1", "2", ""), rows[1])
    }

    @Test fun heartRateDropsBadYearsAndKeepsOffset() {
        val csv = "com.samsung.shealth.tracker.heart_rate,1,1\n" +
            "com.samsung.health.heart_rate.start_time,com.samsung.health.heart_rate.heart_rate,com.samsung.health.heart_rate.min,com.samsung.health.heart_rate.max,com.samsung.health.heart_rate.time_offset,com.samsung.health.heart_rate.datauuid,\n" +
            "2026-11-18 21:04:24.217,70,60,80,UTC+0900,u1,\n" +
            "2092-01-01 00:00:00.000,70,60,80,UTC+0900,u2,\n"
        val r = SamsungCsvParser.parseHeartRate(StringReader(csv), now)
        assertEquals(1, r.records.size)
        assertEquals(1, r.dropped)
        val rec = r.records[0]
        assertEquals(32400, rec.zoneOffsetSec)
        assertEquals(Instant.parse("2026-11-18T21:04:24.217Z").toEpochMilli(), rec.startTime)
        assertEquals("{\"min\":60,\"max\":80}", rec.extraJson)
        assertEquals(mapOf(RecordType.HEART_RATE to 1), r.countsByType)
    }

    @Test fun pedometerMaxPerDay() {
        val csv = "x,1,1\nday_time,step_count,deviceuuid,source_package_name\n" +
            "2026-11-20 00:00:00.000,3000,a,pkg\n2026-11-20 00:00:00.000,5000,b,pkg\n"
        val r = SamsungCsvParser.parsePedometerDaySummary(StringReader(csv), ZoneId.of("Asia/Seoul"), now)
        assertEquals(1, r.records.size)
        assertEquals(5000.0, r.records[0].value, 0.0)
        assertEquals("day:2026-11-20", r.records[0].externalId)
        assertEquals(Instant.parse("2026-11-19T15:00:00Z").toEpochMilli(), r.records[0].startTime)
    }

    @Test fun weightMakesMultipleRecords() {
        val csv = "x,1,1\nweight,body_fat,fat_free_mass,total_body_water,start_time,datauuid,pkg_name\n" +
            "72.4,24.1,55.0,,2026-11-20 01:00:00.000,id1,app\n"
        val r = SamsungCsvParser.parseWeight(StringReader(csv), now)
        assertEquals(3, r.records.size)
        assertTrue(r.records.any { it.externalId == "id1:WEIGHT" })
        assertTrue(r.records.any { it.type == RecordType.LEAN_BODY_MASS && it.value == 55.0 })
    }

    @Test fun sleepWithStages() {
        val sleep = "x,1,1\nstart_time,end_time,datauuid\n2026-11-20 14:00:00.000,2026-11-20 22:00:00.000,s1\n" +
            "2026-11-20 14:00:00.000,2026-11-20 14:00:00.000,s2\n"
        val stages = "x,1,1\nsleep_id,start_time,end_time,stage\n" +
            "s1,2026-11-20 14:00:00.000,2026-11-20 15:00:00.000,40001\n" +
            "s1,2026-11-20 15:00:00.000,2026-11-20 18:00:00.000,40002\n" +
            "s1,2026-11-20 18:00:00.000,2026-11-20 20:00:00.000,40003\n" +
            "s1,2026-11-20 20:00:00.000,2026-11-20 22:00:00.000,40004\n"
        val r = SamsungCsvParser.parseSleep(StringReader(sleep), StringReader(stages), now)
        assertEquals(1, r.records.size)
        assertEquals(1, r.dropped)
        assertEquals(420.0, r.records[0].value, 0.0)
        assertEquals("{\"deep\":120,\"light\":180,\"rem\":120,\"awake\":60}", r.records[0].extraJson)
    }
}
