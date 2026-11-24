package com.gh.healthapp

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.gh.healthapp.data.DailySummaryBuilder
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.HeartRateRetention
import com.gh.healthapp.data.LocalHealthReader
import com.gh.healthapp.data.SyncRepository
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

data class HeartRateSummary(val latestBpm: Long?, val latestTime: Instant?, val avg: Long?, val min: Long?, val max: Long?)

data class StepsDiagnostics(val recordCount: Int, val sources: List<String>)

/** Health Connect와 통신하는 부분을 모아 둔 클래스 */
class HealthConnectManager(private val context: Context) {

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        /** 앱이 요청할 권한 목록. 데이터 종류를 늘릴 때 여기에 추가 */
        val PERMISSIONS: Set<String> = buildSet {
            add(HealthPermission.getReadPermission(StepsRecord::class))
            add(HealthPermission.getReadPermission(HeartRateRecord::class))
            add(HealthPermission.getReadPermission(SleepSessionRecord::class))
            add(HealthPermission.getReadPermission(WeightRecord::class))
            BodyMetric.entries.forEach { add(BodyCompositionRepository.readPermission(it)) }
        }
    }

    enum class Availability { AVAILABLE, INSTALL_OR_UPDATE_REQUIRED, UNSUPPORTED }

    fun availability(): Availability =
        when (HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.INSTALL_OR_UPDATE_REQUIRED
            else -> Availability.UNSUPPORTED
        }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val sleep by lazy { SleepRepository(client) }
    val weight by lazy { WeightRepository(client) }
    val body by lazy { BodyCompositionRepository(client) }

    private val dao by lazy { HealthDatabase.get(context).dao() }
    val syncRepo by lazy { SyncRepository(client, dao) }
    val summaryBuilder by lazy { DailySummaryBuilder(dao) }
    val local by lazy { LocalHealthReader(dao) }
    val retention by lazy { HeartRateRetention(dao) }
    val detail by lazy { com.gh.healthapp.data.DetailReader(dao) }

    /** 가져오기 등 외부 레코드를 500개씩 나눠 저장 (externalId 기준 덮어쓰기라 중복 안 생김) */
    suspend fun saveRecords(list: List<com.gh.healthapp.data.HealthRecordEntity>) {
        list.chunked(500).forEach { dao.upsertRecords(it) }
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean =
        hcCall("getGrantedPermissions") { client.permissionController.getGrantedPermissions() }.containsAll(PERMISSIONS)

    /** 최근 [days]일의 날짜별 걸음 수 (오늘 포함, 최신 날짜가 먼저) */
    suspend fun dailySteps(days: Int = 7): List<Pair<LocalDate, Long>> {
        val today = LocalDate.now()
        val start = today.minusDays((days - 1).toLong()).atStartOfDay()
        val end = today.plusDays(1).atStartOfDay()
        val groups = hcCall("aggregateGroupByPeriod(steps)", { it.size }) {
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1),
                )
            )
        }
        val byDate = groups.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        return (0 until days).map { i -> today.minusDays(i.toLong()).let { it to (byDate[it] ?: 0L) } }
    }

    /** 오늘 0시부터 지금까지의 심박수 요약 */
    suspend fun heartRateToday(): HeartRateSummary {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val now = Instant.now()
        val range = TimeRangeFilter.between(start, now)
        val agg = hcCall("aggregate(heartRate)") {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                    timeRangeFilter = range,
                )
            )
        }
        val latest = hcCall("readRecords(heartRate)", { it.records.size }) {
            client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, range, ascendingOrder = false, pageSize = 1)
            )
        }.records.firstOrNull()?.samples?.filter { it.time >= start && it.time <= now }?.maxByOrNull { it.time }
        return HeartRateSummary(
            latestBpm = latest?.beatsPerMinute,
            latestTime = latest?.time,
            avg = agg[HeartRateRecord.BPM_AVG],
            min = agg[HeartRateRecord.BPM_MIN],
            max = agg[HeartRateRecord.BPM_MAX],
        )
    }

    /** 걸음 수 진단용: 최근 [days]일 걸음 기록 개수와 출처 앱 (첫 페이지만) */
    suspend fun stepsDiagnostics(days: Int = 7): StepsDiagnostics {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).minusDays((days - 1).toLong()).atStartOfDay(zone).toInstant()
        val records = hcCall("readRecords(steps)", { it.records.size }) {
            client.readRecords(
                ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, Instant.now()), pageSize = 1000)
            )
        }.records
        return StepsDiagnostics(records.size, records.map { it.metadata.dataOrigin.packageName }.distinct())
    }

    /** Health Connect 설치/업데이트를 위한 플레이스토어 화면 */
    fun playStoreIntent(): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse("market://details?id=$PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
        }

    /** 플레이스토어 앱 → 브라우저 → 토스트 순서로 시도 */
    fun openPlayStore(context: Context) {
        try {
            context.startActivity(playStoreIntent())
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE"))
                )
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, "플레이스토어를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
