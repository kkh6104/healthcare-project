package com.gh.healthapp

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicInteger

/** refresh 한 번 동안의 HC 호출/실패 수 집계용 전역 카운터 */
object HcStats {
    val calls = AtomicInteger(0)
    val fails = AtomicInteger(0)
}

/** 섹션 하나의 로딩 결과: 성공 데이터 또는 실패 메시지 */
sealed interface Section<out T> {
    data class Ok<T>(val data: T) : Section<T>
    data class Failed(val message: String) : Section<Nothing>
}

/** Health Connect 호출 1건을 HCDEBUG 태그로 기록하고 결과/예외는 그대로 전달 */
suspend fun <T> hcCall(name: String, count: (T) -> Int? = { null }, block: suspend () -> T): T {
    val t0 = System.nanoTime()
    fun ms() = (System.nanoTime() - t0) / 1_000_000
    HcStats.calls.incrementAndGet()
    Log.i("HCDEBUG", "[$name] begin")
    try {
        val result = block()
        val c = count(result)
        Log.i("HCDEBUG", "[$name] ok ${ms()}ms" + (if (c != null) " count=$c" else ""))
        return result
    } catch (e: CancellationException) {
        Log.i("HCDEBUG", "[$name] CANCELLED ${ms()}ms")
        throw e
    } catch (e: Throwable) {
        HcStats.fails.incrementAndGet()
        val causes = generateSequence(e.cause) { it.cause }.take(2).joinToString(",") { it.javaClass.simpleName }
        Log.i(
            "HCDEBUG",
            "[$name] FAIL ${ms()}ms ${e.javaClass.simpleName}: ${e.message}" +
                (if (causes.isNotEmpty()) " causes=[$causes]" else ""),
        )
        throw e
    }
}

/** readAllPagesResult의 결과: truncated=true면 maxPages 때문에 다 못 읽은 것 */
data class PagedResult<T>(val records: List<T>, val truncated: Boolean)

/** 페이지 루프 공용 함수 (records만 반환). 빈/반복 토큰이나 maxPages에서 종료 */
suspend fun <T : Record> HealthConnectClient.readAllPages(
    name: String,
    maxPages: Int = 5,
    request: (pageToken: String?) -> ReadRecordsRequest<T>,
): List<T> = readAllPagesResult(name, maxPages, request).records

/** 페이지 루프 공용 함수. 다 못 읽었는지(truncated)도 함께 반환 */
suspend fun <T : Record> HealthConnectClient.readAllPagesResult(
    name: String,
    maxPages: Int = 5,
    request: (pageToken: String?) -> ReadRecordsRequest<T>,
): PagedResult<T> {
    val all = mutableListOf<T>()
    var token: String? = null
    var pages = 0
    var truncated = false
    runCatching {
        val r = request(null)
        val f = r.timeRangeFilter
        Log.i(
            "HCDEBUG",
            "[$name] range=${f?.startTime ?: f?.localStartTime}~${f?.endTime ?: f?.localEndTime} " +
                "pageSize=${r.pageSize} maxPages=$maxPages",
        )
    }
    while (true) {
        val prev = token
        val response = hcCall(name, { it.records.size }) { readRecords(request(prev)) }
        all += response.records
        pages++
        val next = response.pageToken
        val shown = when {
            next == null -> "null"
            next.isEmpty() -> "EMPTY"
            else -> next.take(20) + "(len=${next.length})"
        }
        Log.i("HCDEBUG", "[$name] page=$pages token=$shown")
        if (next.isNullOrEmpty() || next == prev) {
            val why = if (next == null) "null" else if (next.isEmpty()) "empty" else "same-token"
            Log.i("HCDEBUG", "[$name] end: $why")
            break
        }
        if (pages >= maxPages) {
            Log.w("HCDEBUG", "[$name] maxPages reached")
            Log.i("HCDEBUG", "[$name] end: maxPages")
            truncated = true
            break
        }
        token = next
    }
    return PagedResult(all, truncated)
}
