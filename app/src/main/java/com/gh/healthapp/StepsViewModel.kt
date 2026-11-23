package com.gh.healthapp

import android.app.Application
import android.os.SystemClock
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.gh.healthapp.data.CsvImportResult
import com.gh.healthapp.data.CsvKind
import com.gh.healthapp.data.HealthRecordEntity
import com.gh.healthapp.data.SamsungCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.time.Instant
import java.time.ZoneId
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.gh.healthapp.data.RecordType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/** 화면에 보여줄 상태 */
sealed interface StepsUiState {
    data object Loading : StepsUiState
    data class Unavailable(val availability: HealthConnectManager.Availability) : StepsUiState
    data object NeedPermission : StepsUiState
    data class Loaded(
        val steps: Section<List<Pair<LocalDate, Long>>>,
        val heartRate: Section<HeartRateSummary>,
        val sleep: Section<SleepSummary>,
        val weight: Section<WeightSummary>,
        val body: List<Section<MetricSummary>> = emptyList(),
        val banner: String? = null,
    ) : StepsUiState
    data class Error(val message: String) : StepsUiState
}

class StepsViewModel(app: Application) : AndroidViewModel(app) {

    val manager = HealthConnectManager(app)

    private val _state = MutableStateFlow<StepsUiState>(StepsUiState.Loading)
    val state: StateFlow<StepsUiState> = _state

    private var refreshJob: Job? = null
    private var diagJob: Job? = null
    private var lastLoaded: StepsUiState.Loaded? = null
    private var permissionsConfirmed = false
    private var cooldownUntil: Long = 0L

    private val _diagnostics = MutableStateFlow<StepsDiagnostics?>(null)
    val diagnostics: StateFlow<StepsDiagnostics?> = _diagnostics

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    /** DB에 기록된 마지막 동기화 시각 (없으면 null) */
    val lastSync: Flow<Long?> = manager.local.lastSyncFlow()

    init {
        // 권한·동기화와 무관하게 DB 내용을 즉시 표시
        viewModelScope.launch {
            try {
                val loaded = readLocal()
                if (_state.value == StepsUiState.Loading) {
                    lastLoaded = loaded
                    _state.value = loaded
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i("HCDEBUG", "initial DB read fail: ${e.message}")
            }
        }
    }

    private val _importProgress = MutableStateFlow<String?>(null)
    /** 삼성헬스 가져오기 진행 문구 (진행 중이 아니면 null) */
    val importProgress: StateFlow<String?> = _importProgress

    private val refreshSeq = AtomicInteger(0)

    private fun cooldownLeftSec() = ((cooldownUntil - SystemClock.elapsedRealtime()) / 1000).coerceAtLeast(0)

    private fun now() = SystemClock.elapsedRealtime()
    private fun inCooldown() = now() < cooldownUntil

    private fun isQuota(e: Throwable): Boolean {
        var t: Throwable? = e
        var depth = 0
        while (t != null && depth++ < 10) {
            val msg = t.message
            if (msg != null && (msg.contains("quota", true) || msg.contains("rate limit", true))) return true
            t = t.cause
        }
        return false
    }

    private fun cooldownMessage(): String {
        val minutes = ((cooldownUntil - now() + 59_999L) / 60_000L).coerceAtLeast(1L)
        return "요청이 많아 Health Connect가 잠시 제한했습니다. ${minutes}분 뒤 다시 시도하세요."
    }

    private fun showMessage(msg: String) {
        val kept = lastLoaded
        _state.value = if (kept != null) kept.copy(banner = msg) else StepsUiState.Error(msg)
    }

    private fun handleFailure(e: Exception) {
        if (isQuota(e)) {
            cooldownUntil = now() + COOLDOWN_MS
            Log.i("HCDEBUG", "cooldown start 15m")
            refreshJob?.cancel()
            diagJob?.cancel()
            showMessage(cooldownMessage())
        } else {
            showMessage(e.message ?: e.toString())
        }
    }

    /** DB에서 화면용 데이터를 읽어 Loaded 상태를 만든다 */
    private suspend fun readLocal(): StepsUiState.Loaded {
        val r = manager.local
        return StepsUiState.Loaded(
            Section.Ok(r.stepsDaily(7)),
            Section.Ok(r.heartRateToday()),
            Section.Ok(r.sleep(7)),
            Section.Ok(r.weight(30)),
            BodyMetric.entries.map { Section.Ok(r.body(it)) },
        )
    }

    fun refreshIfStale(maxAgeMs: Long = 5 * 60_000L, reason: String = "resume") {
        if (inCooldown()) {
            Log.i("HCDEBUG", "refresh ignored: cooldown ${cooldownLeftSec()}s")
            return
        }
        if (refreshJob?.isActive == true) {
            Log.i("HCDEBUG", "refresh ignored: in progress")
            return
        }
        viewModelScope.launch {
            val last = manager.local.lastSyncFlow().first()
            if (refreshJob?.isActive == true) {
                Log.i("HCDEBUG", "refresh ignored: in progress")
                return@launch
            }
            val fresh = last != null && System.currentTimeMillis() - last < maxAgeMs
            if (!fresh) refresh(reason)
        }
    }

    /** 권한 확인 → Health Connect 순차 동기화 → 요약 재계산 → DB에서 다시 읽기 */
    fun refresh(reason: String = "button") {
        if (inCooldown()) {
            Log.i("HCDEBUG", "refresh ignored: cooldown ${cooldownLeftSec()}s")
            showMessage(cooldownMessage())
            return
        }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val seq = refreshSeq.incrementAndGet()
            val t0 = now()
            val calls0 = HcStats.calls.get()
            val fails0 = HcStats.fails.get()
            var result = "ok"
            Log.i("HCDEBUG", "=== refresh start #$seq reason=$reason ===")
            // 동기화 상태 복구/중단 시 요약 재계산에 쓰려고 try 밖에 둔다
            val dates = mutableSetOf<LocalDate>()
            val stepsByDate = mutableMapOf<LocalDate, Long>()
            try {
                val availability = manager.availability()
                if (availability != HealthConnectManager.Availability.AVAILABLE) {
                    lastLoaded = null
                    _state.value = StepsUiState.Unavailable(availability)
                    return@launch
                }
                if (!permissionsConfirmed) {
                    if (!manager.hasAllPermissions()) {
                        lastLoaded = null
                        _state.value = StepsUiState.NeedPermission
                        return@launch
                    }
                    permissionsConfirmed = true
                }
                // 화면은 Loading으로 바꾸지 않는다. 권한 화면 등에서 돌아오면 DB 내용을 먼저 보여 준다
                if (_state.value !is StepsUiState.Loaded) {
                    val local = readLocal()
                    lastLoaded = local
                    _state.value = local
                }

                // 종류별 순차 동기화. 일반 실패는 계속, 한도 초과는 중단 + 나머지 건너뜀
                _syncing.value = true
                var quota = false
                val failures = mutableListOf<String>()
                val truncatedLabels = mutableListOf<String>()
                try {
                    for ((type, label) in SYNC_TYPES) {
                        if (quota) {
                            Log.i("HCDEBUG", "--- section sync:$type skipped (quota) ---")
                            failures += "$label(건너뜀)"
                            continue
                        }
                        Log.i("HCDEBUG", "--- section sync:$type start ---")
                        try {
                            val r = manager.syncRepo.sync(type)
                            dates += r.affectedDates
                            stepsByDate += r.stepsByDate
                            if (r.truncated) truncatedLabels += label
                            Log.i("HCDEBUG", "--- section sync:$type ok (fetched=${r.fetched} dates=${r.affectedDates.size}) ---")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: SecurityException) {
                            throw e
                        } catch (e: Exception) {
                            Log.i("HCDEBUG", "--- section sync:$type fail (${e.javaClass.simpleName}: ${e.message}) ---")
                            failures += "$label(${e.message ?: e.javaClass.simpleName})"
                            if (isQuota(e)) {
                                quota = true
                                cooldownUntil = now() + COOLDOWN_MS
                                Log.i("HCDEBUG", "cooldown start 15m")
                            }
                        }
                    }
                    // 성공한 종류가 있으면 요약 1회 재계산
                    if (dates.isNotEmpty()) {
                        try {
                            manager.summaryBuilder.rebuild(dates, stepsByDate)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.i("HCDEBUG", "--- section rebuild fail (${e.javaClass.simpleName}: ${e.message}) ---")
                            failures += "요약(${e.message ?: e.javaClass.simpleName})"
                        }
                    }
                } finally {
                    _syncing.value = false
                }

                // 오래된 심박 원본을 1시간 단위로 정리 (실패해도 동기화 결과에는 영향 없음)
                try {
                    val rolled = manager.retention.run()
                    Log.i("HCDEBUG", "retention: rolled=$rolled")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.i("HCDEBUG", "retention fail: ${e.javaClass.simpleName}: ${e.message}")
                }

                if (quota) result = "ratelimited"
                else if (failures.isNotEmpty() || truncatedLabels.isNotEmpty()) result = "partial"

                val lines = mutableListOf<String>()
                if (failures.isNotEmpty()) {
                    lines += "동기화 실패: " + failures.joinToString(", ") + (if (quota) "\n" + cooldownMessage() else "")
                }
                truncatedLabels.forEach { lines += "$it: 데이터가 많아 일부만 받았습니다. 새로고침하면 이어서 받습니다." }
                val banner = if (lines.isEmpty()) null else lines.joinToString("\n")
                val loaded = readLocal().copy(banner = banner)
                lastLoaded = loaded
                _state.value = loaded
            } catch (e: CancellationException) {
                result = "cancelled"
                throw e
            } catch (e: SecurityException) {
                // NeedPermission으로 가기 전에 그때까지 성공한 종류의 요약을 먼저 반영
                if (dates.isNotEmpty()) {
                    try {
                        manager.summaryBuilder.rebuild(dates, stepsByDate)
                    } catch (e2: CancellationException) {
                        throw e2
                    } catch (e2: Exception) {
                        Log.i("HCDEBUG", "rebuild before NeedPermission fail: ${e2.message}")
                    }
                }
                permissionsConfirmed = false
                lastLoaded = null
                _state.value = StepsUiState.NeedPermission
            } catch (e: Exception) {
                result = if (isQuota(e)) "ratelimited" else "partial"
                handleFailure(e)
            } finally {
                Log.i(
                    "HCDEBUG",
                    "=== refresh end #$seq calls=${HcStats.calls.get() - calls0} fail=${HcStats.fails.get() - fails0} " +
                        "${now() - t0}ms result=$result ===",
                )
            }
        }
    }

    /** 삼성헬스 내보내기 폴더(CSV)를 읽어 DB에 저장. 개인 수치는 로그에 남기지 않고 건수만 기록 */
    fun importSamsung(treeUri: Uri) {
        if (_importProgress.value != null) return
        _importProgress.value = "가져오는 중… (폴더 확인)"
        viewModelScope.launch {
            Log.i("HCDEBUG", "import start")
            try {
                // 동기화와 겹치지 않도록 진행 중인 refresh를 취소하고 끝날 때까지 기다린 뒤 시작
                refreshJob?.takeIf { it.isActive }?.let {
                    Log.i("HCDEBUG", "import: cancelling running refresh")
                    it.cancelAndJoin()
                }
                val parsed = withContext(Dispatchers.IO) { readSamsungFolder(treeUri) }
                val total = parsed.sumOf { it.records.size }
                _importProgress.value = "가져오는 중… (저장 중)"
                manager.saveRecords(parsed.flatMap { it.records })
                _importProgress.value = "가져오는 중… (요약 계산 중)"
                manager.summaryBuilder.rebuildAll()
                val rolled = manager.retention.run()
                Log.i("HCDEBUG", "retention: rolled=$rolled")
                Log.i("HCDEBUG", "import end total=$total")

                val counts = HashMap<String, Int>()
                parsed.forEach { r -> r.countsByType.forEach { (k, n) -> counts[k] = (counts[k] ?: 0) + n } }
                fun c(vararg types: String) = types.sumOf { counts[it] ?: 0 }
                val dropped = parsed.sumOf { it.dropped }
                val msg = "가져오기 완료: 걸음 ${c(RecordType.STEPS)}일 · 심박 ${c(RecordType.HEART_RATE)}건 · " +
                    "수면 ${c(RecordType.SLEEP_SESSION)}건 · 체중/체성분 " +
                    "${c(RecordType.WEIGHT, RecordType.BODY_FAT, RecordType.BONE_MASS, RecordType.LEAN_BODY_MASS, RecordType.BODY_WATER_MASS)}건 " +
                    "(잘못된 기록 ${dropped}건 제외)"
                val loaded = readLocal().copy(banner = msg)
                lastLoaded = loaded
                _state.value = loaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i("HCDEBUG", "import fail: ${e.javaClass.simpleName}: ${e.message}")
                val base = lastLoaded ?: try { readLocal() } catch (e2: Exception) { null }
                val msg = "가져오기 실패: ${e.message ?: e.javaClass.simpleName}"
                if (base != null) {
                    lastLoaded = base.copy(banner = msg)
                    _state.value = lastLoaded!!
                } else {
                    _state.value = StepsUiState.Error(msg)
                }
            } finally {
                _importProgress.value = null
            }
        }
    }

    /** 폴더에서 CSV를 찾아 파싱 (IO 스레드에서 호출) */
    private fun readSamsungFolder(treeUri: Uri): List<CsvImportResult> {
        val app = getApplication<Application>()
        val resolver = app.contentResolver
        val root = DocumentFile.fromTreeUri(app, treeUri) ?: throw IllegalStateException("폴더를 열 수 없습니다")
        var files = root.listFiles().toList()
        if (files.none { it.isFile && SamsungCsvParser.kindOf(it.name ?: "") != null }) {
            val sub = files.filter { it.isDirectory && (it.name ?: "").startsWith("samsunghealth_") }
                .maxByOrNull { it.name ?: "" }
            if (sub != null) files = sub.listFiles().toList()
        }
        val byKind = LinkedHashMap<CsvKind, DocumentFile>()
        files.filter { it.isFile }.forEach { f ->
            val k = SamsungCsvParser.kindOf(f.name ?: "") ?: return@forEach
            byKind.putIfAbsent(k, f)
        }
        if (byKind.isEmpty()) throw IllegalStateException("삼성헬스 CSV 파일을 찾을 수 없습니다")

        fun open(f: DocumentFile): Reader {
            val stream = resolver.openInputStream(f.uri) ?: throw IllegalStateException("파일을 열 수 없습니다: ${f.name}")
            val br = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            br.mark(1)
            if (br.read() != 0xFEFF) br.reset() // BOM이 있으면 건너뜀
            return br
        }
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val out = mutableListOf<CsvImportResult>()
        fun step(kind: String, label: String, block: () -> CsvImportResult) {
            _importProgress.value = "가져오는 중… ($label 읽는 중)"
            val t0 = SystemClock.elapsedRealtime()
            val r = block()
            Log.i("HCDEBUG", "import $kind records=${r.records.size} dropped=${r.dropped} ${SystemClock.elapsedRealtime() - t0}ms")
            out += r
        }
        byKind[CsvKind.PEDOMETER_DAY_SUMMARY]?.let { f ->
            step("steps", "걸음") { open(f).use { SamsungCsvParser.parsePedometerDaySummary(it, zone, now) } }
        }
        byKind[CsvKind.HEART_RATE]?.let { f ->
            step("heartRate", "심박") { open(f).use { SamsungCsvParser.parseHeartRate(it, now) } }
        }
        byKind[CsvKind.WEIGHT]?.let { f ->
            step("weight", "체중/체성분") { open(f).use { SamsungCsvParser.parseWeight(it, now) } }
        }
        byKind[CsvKind.SLEEP]?.let { f ->
            step("sleep", "수면") {
                open(f).use { sleepR ->
                    val stageFile = byKind[CsvKind.SLEEP_STAGE]
                    if (stageFile != null) open(stageFile).use { SamsungCsvParser.parseSleep(sleepR, it, now) }
                    else SamsungCsvParser.parseSleep(sleepR, null, now)
                }
            }
        }
        return out
    }

    /** 버튼을 눌렀을 때만 걸음 기록 진단 1회 조회 */
    fun loadDiagnostics() {
        if (inCooldown()) {
            Log.i("HCDEBUG", "refresh ignored: cooldown ${cooldownLeftSec()}s")
            showMessage(cooldownMessage())
            return
        }
        diagJob?.cancel()
        diagJob = viewModelScope.launch {
            try {
                val diag = manager.stepsDiagnostics(7)
                if (inCooldown()) return@launch
                _diagnostics.value = diag
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                permissionsConfirmed = false
                lastLoaded = null
                _state.value = StepsUiState.NeedPermission
            } catch (e: Exception) {
                handleFailure(e)
            }
        }
    }

    private companion object {
        const val COOLDOWN_MS = 15 * 60_000L
        val SYNC_TYPES = listOf(
            RecordType.STEPS to "걸음 수",
            RecordType.HEART_RATE to "심박수",
            RecordType.SLEEP_SESSION to "수면",
            RecordType.WEIGHT to "몸무게",
            RecordType.BODY_FAT to "체지방률",
            RecordType.BONE_MASS to "골질량",
            RecordType.LEAN_BODY_MASS to "제지방량",
            RecordType.BODY_WATER_MASS to "체내 수분량",
        )
    }
}
