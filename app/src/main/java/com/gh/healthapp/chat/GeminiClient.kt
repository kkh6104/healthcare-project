package com.gh.healthapp.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.BufferedReader
import kotlin.random.Random
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object GeminiConfig {
    val MODELS = listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash")
    fun endpoint(model: String) = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"
}

internal object GeminiJson {
    const val INSTRUCTION = "너는 사용자의 개인 건강 데이터 도우미다. 한국어로 짧고 친근하게 숫자 근거를 들어 설명한다. 생활 습관, 운동, 수면 위생에 대한 일반적 조언은 적극적으로 하되 진단이나 약 추천은 하지 않는다. 걱정되는 패턴에는 의료 전문가 상담을 권한다. 측정 한계를 설명할 수 있다. 데이터가 없으면 지어내지 말고 없다고 말하며 필요한 기간은 도구로 조회한다."
    private fun declaration(name: String, description: String, dates: Boolean): JSONObject {
        val properties = JSONObject()
        if (dates) {
            properties.put("start_date", JSONObject().put("type", "STRING").put("description", "시작일 yyyy-MM-dd"))
            properties.put("end_date", JSONObject().put("type", "STRING").put("description", "종료일 yyyy-MM-dd"))
        }
        val parameters = JSONObject().put("type", "OBJECT").put("properties", properties)
        if (dates) parameters.put("required", JSONArray().put("start_date").put("end_date"))
        return JSONObject().put("name", name).put("description", description).put("parameters", parameters)
    }
    fun instruction(today: LocalDate): String = "$INSTRUCTION 오늘 날짜는 ${today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd(E)", Locale.KOREAN))}. 최근 7일 밖의 기간, 지난주·지난달·작년 비교, 추세 또는 특정 날짜 질문은 반드시 도구로 조회한 뒤 답한다. 컨텍스트에 없는 숫자를 추측하지 않는다. 참고 자료를 근거로 조언할 때는 문장 끝에 [1]처럼 출처 번호를 붙인다. 참고 자료에 없는 의학 정보는 일반 상식 수준으로만 말하고 확신이 없으면 밝힌다."
    fun request(contents: JSONArray, thinking: Boolean = true, model: String = GeminiConfig.MODELS.first(), today: LocalDate = LocalDate.now()): JSONObject = JSONObject()
        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction(today)))))
        .put("contents", contents)
        .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray()
            .put(declaration("get_daily_summary", "날짜별 건강 요약, 최대 120일", true))
            .put(declaration("get_period_stats", "기간 통계와 수면 단계 평균, 최대 120일", true))
            .put(declaration("get_sleep_days", "날짜별 수면과 단계, 최대 60일", true))
            .put(declaration("get_walk_sessions", "걷기 기록, 최대 120일과 100건", true))
            .put(declaration("get_data_range", "종류별 기록 날짜 범위", false)))))
        .put("toolConfig", JSONObject().put("functionCallingConfig", JSONObject().put("mode", "AUTO")))
        .put("generationConfig", JSONObject().put("temperature", 0.6).put("maxOutputTokens", 2048).apply { if (thinking) put("thinkingConfig", if (model.startsWith("gemini-2.5")) JSONObject().put("thinkingBudget", 512) else JSONObject().put("thinkingLevel", "low")) })

    fun content(role: String, text: String): JSONObject = JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", text)))
    fun response(raw: JSONObject): JSONObject {
        if (raw.has("error")) throw ChatException(ChatError.Other("응답을 처리하지 못했습니다"))
        return raw.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")
            ?: throw ChatException(ChatError.Other("모델 응답이 없습니다"))
    }
}

internal fun httpError(code: Int, errorBody: String? = null): ChatError? = when {
    code == 400 && errorBody?.contains("API_KEY_INVALID") == true -> ChatError.Other("API 키가 올바르지 않습니다")
    code == 429 -> ChatError.RateLimited
    code == 401 || code == 403 -> ChatError.Other("API 키 오류")
    code in 200..299 -> null
    else -> ChatError.Other("서버 오류 ($code)")
}

internal fun safeErrorMessage(message: String, key: String, request: JSONObject): String {
    var safe = message
    if (key.isNotEmpty()) safe = safe.replace(key, "[redacted]")
    val contents = request.optJSONArray("contents") ?: JSONArray()
    for (i in 0 until contents.length()) {
        val parts = contents.optJSONObject(i)?.optJSONArray("parts") ?: continue
        for (j in 0 until parts.length()) {
            val text = parts.optJSONObject(j)?.optString("text", "").orEmpty()
            if (text.isNotBlank()) {
                safe = safe.replace(text, "[redacted]")
                val question = text.substringAfter("\n\n질문: ", "")
                if (question.isNotBlank()) safe = safe.replace(question, "[redacted]")
            }
        }
    }
    return safe.replace(Regex("[\\r\\n\\t]"), " ").take(120)
}

internal class RetryableServerException : Exception()
internal fun isRetryableStatus(code: Int) = code in 500..599
internal class ThinkingUnsupportedException : Exception()
internal fun isThinkingUnsupported(code: Int, body: String?): Boolean =
    code == 400 && body?.contains("INVALID_ARGUMENT", ignoreCase = true) == true && body.contains("thinking", ignoreCase = true)
internal suspend fun <T> withThinkingFallback(request: suspend (Boolean) -> T): T =
    try { request(true) } catch (_: ThinkingUnsupportedException) { request(false) }

internal class ModelAvailability(private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private val until = mutableMapOf<String, Long>()
    private val missing = mutableSetOf<String>()
    @Synchronized fun markBusy(model: String) { until[model] = now() + 300_000 }
    @Synchronized fun markMissing(model: String) { missing.add(model) }
    @Synchronized fun isMissing(model: String) = model in missing
    @Synchronized fun isCooling(model: String) = (until[model] ?: 0L) > now()
}
private val availability = ModelAvailability()
internal class ModelNotFoundException : Exception()
internal val RETRY_DELAYS = listOf(1000L, 2000L, 4000L, 4000L)

internal class QuestionAttempts(
    private val availability: ModelAvailability = com.gh.healthapp.chat.availability,
    private val wait: suspend (Long) -> Unit = { delay(it) },
    private val jitter: (Long) -> Long = { base -> (base * Random.nextDouble(0.8, 1.2)).toLong() },
    private val onProgress: (Int, Boolean) -> Unit = { _, _ -> },
    private val onMissing: (String) -> Unit = {},
) {
    var attempts = 0
        private set
    var model = GeminiConfig.MODELS.first()
        private set
    suspend fun <T> execute(request: suspend (String) -> T): T {
        val order = listOf(0, 1, 2, 1, 2)
        var lastModel: String? = null
        while (attempts < 5) {
            val available = GeminiConfig.MODELS.filterNot(availability::isMissing)
            if (available.isEmpty()) throw ChatException(ChatError.Busy)
            val preferred = GeminiConfig.MODELS[order[attempts]]
            val fresh = available.filterNot(availability::isCooling)
            model = when {
                preferred in fresh -> preferred
                fresh.isNotEmpty() -> fresh.first()
                attempts == 0 && availability.isCooling(GeminiConfig.MODELS.first()) && available.any { it != GeminiConfig.MODELS.first() } -> available.first { it != GeminiConfig.MODELS.first() }
                preferred in available && preferred != lastModel -> preferred
                else -> available.firstOrNull { it != lastModel } ?: available.first()
            }
            attempts++
            onProgress(attempts, false)
            try { return request(model) }
            catch (e: ModelNotFoundException) { availability.markMissing(model); onMissing(model) }
            catch (e: RetryableServerException) { availability.markBusy(model) }
            catch (e: SocketTimeoutException) { availability.markBusy(model) }
            catch (e: StreamInterruptedException) { availability.markBusy(model) }
            lastModel = model
            if (attempts < 5 && GeminiConfig.MODELS.any { !availability.isMissing(it) }) {
                onProgress(attempts, true)
                wait(jitter(RETRY_DELAYS[attempts - 1]))
            }
        }
        throw ChatException(ChatError.Busy)
    }
}

/** SSE events may have multiple data lines; the final event may end at EOF. */
internal fun readSse(reader: BufferedReader, onData: (String) -> Unit) {
    val data = mutableListOf<String>()
    fun flush() {
        if (data.isEmpty()) return
        val event = data.joinToString("\n")
        data.clear()
        if (event != "[DONE]") onData(event)
    }
    while (true) {
        val line = reader.readLine() ?: break
        if (line.isEmpty()) flush()
        else if (line.startsWith("data:")) data.add(line.substring(5).removePrefix(" "))
    }
    flush()
}

private const val INTERRUPTED_SUFFIX = "\n\n(답변이 중간에 끊겼어요)"
internal fun finishSuffix(reason: String?): String = when (reason) {
    null, "", "STOP" -> ""
    "MAX_TOKENS" -> "\n\n(답변이 길어 여기까지만 표시했어요)"
    else -> INTERRUPTED_SUFFIX
}

/** Preserve model parts and associate a signature-only fragment with its adjacent call. */
internal class ModelParts {
    val parts = JSONArray()
    private var pendingSignature: Any? = null

    fun add(part: JSONObject) {
        val signatureOnly = part.has("thoughtSignature") && part.length() == 1
        if (signatureOnly) {
            val previous = if (parts.length() == 0) null else parts.optJSONObject(parts.length() - 1)
            if (previous?.has("functionCall") == true && !previous.has("thoughtSignature")) {
                previous.put("thoughtSignature", part.get("thoughtSignature"))
            } else {
                if (pendingSignature != null) parts.put(JSONObject().put("thoughtSignature", pendingSignature))
                pendingSignature = part.get("thoughtSignature")
            }
            return
        }
        val call = part.optJSONObject("functionCall")
        val previous = if (parts.length() == 0) null else parts.optJSONObject(parts.length() - 1)
        val previousCall = previous?.optJSONObject("functionCall")
        val continuation = call != null && previousCall != null &&
            ((!call.has("name") && !call.has("id")) || (call.has("id") && call.optString("id").isNotEmpty() && call.optString("id") == previousCall.optString("id")))
        if (continuation && pendingSignature == null && !part.has("text")) {
            mergeFragments(previousCall!!, call!!)
            val keys = part.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "functionCall") previous!!.put(key, part.get(key))
            }
            return
        }
        if (pendingSignature != null) {
            if (part.has("functionCall") && !part.has("thoughtSignature")) part.put("thoughtSignature", pendingSignature)
            else parts.put(JSONObject().put("thoughtSignature", pendingSignature))
            pendingSignature = null
        }
        parts.put(part)
    }

    fun finish(): JSONArray {
        if (pendingSignature != null) { parts.put(JSONObject().put("thoughtSignature", pendingSignature)); pendingSignature = null }
        return parts
    }

    private fun mergeFragments(target: JSONObject, fragment: JSONObject) {
        val keys = fragment.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = fragment.get(key)
            val current = target.opt(key)
            when {
                current is JSONObject && value is JSONObject -> mergeFragments(current, value)
                key != "id" && key != "name" && current is String && value is String -> target.put(key, current + value)
                else -> target.put(key, value)
            }
        }
    }
}

internal suspend fun appendToolTurns(contents: JSONArray, content: JSONObject, execute: suspend (JSONObject) -> JSONObject) {
    val parts = content.getJSONArray("parts")
    val responses = JSONArray()
    for (i in 0 until parts.length()) {
        val call = parts.optJSONObject(i)?.optJSONObject("functionCall") ?: continue
        val result = execute(call)
        val response = JSONObject().put("name", call.optString("name")).put("response", result)
        if (call.has("id")) response.put("id", call.get("id"))
        responses.put(JSONObject().put("functionResponse", response))
    }
    contents.put(JSONObject().put("role", "model").put("parts", parts))
    contents.put(JSONObject().put("role", "user").put("parts", responses))
}

internal class StreamAccumulator(private val onText: (String) -> Unit = {}, private val onParseError: () -> Unit = {}) {
    private val modelParts = ModelParts()
    val text = StringBuilder()
    var finishReason: String? = null
        private set
    var parseErrors = 0
        private set
    var sawContent = false
        private set

    fun accept(event: String) {
        val chunk = try { JSONObject(event) } catch (_: org.json.JSONException) {
            parseErrors++
            onParseError()
            return
        }
        chunk.optJSONObject("error")?.let { error ->
            val code = error.optInt("code", 0)
            throw StreamServerException(code)
        }
        val candidate = chunk.optJSONArray("candidates")?.optJSONObject(0) ?: return
        candidate.optString("finishReason").takeIf { it.isNotEmpty() }?.let { finishReason = it }
        val items = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return
        for (i in 0 until items.length()) {
            val part = items.optJSONObject(i) ?: continue
            sawContent = true
            modelParts.add(part)
            val piece = if (part.optBoolean("thought", false)) "" else part.optString("text", "")
            if (piece.isNotEmpty()) { text.append(piece); onText(text.toString()) }
        }
    }
    fun content() = JSONObject().put("role", "model").put("parts", modelParts.finish()).put("finishReason", finishReason)
}

internal class StreamServerException(val code: Int) : Exception()
internal class StreamInterruptedException(cause: Exception) : Exception(cause)

internal class GeminiClient(private val key: String, private val tools: HealthTools, onProgress: (Int, Boolean) -> Unit = { _, _ -> }) {
    val calledTools = mutableListOf<String>()
    private val fallback = QuestionAttempts(onProgress = onProgress, onMissing = { Log.i("HCDEBUG", "chat model not found model=$it") })
    val model: String get() = fallback.model
    val attempts: Int get() = fallback.attempts
    private val startNanos = System.nanoTime()
    var firstTokenMs: Long? = null
        private set
    suspend fun generate(history: List<ChatMessage>, question: String, summary: JSONObject, sources: List<KbSource> = emptyList(), today: LocalDate = LocalDate.now()): String = withContext(Dispatchers.IO) {
        fallback.execute { model -> generateAttempt(model, history, question, summary, sources, today) }
    }
    private suspend fun generateAttempt(model: String, history: List<ChatMessage>, question: String, summary: JSONObject, sources: List<KbSource>, today: LocalDate): String {
        val contents = JSONArray()
        history.forEach { contents.put(GeminiJson.content(it.role, it.text)) }
        val refs = if (sources.isEmpty()) "" else "\n\n참고 자료:\n" + sources.joinToString("\n\n") { "[${it.index}] ${it.title} – ${it.heading} (${it.source})\n${it.text}" }
        contents.put(GeminiJson.content("user", "최근 7일 건강 요약 (이 요약은 ${today.minusDays(6)}~$today 기간만 포함): $summary$refs\n\n질문: $question"))
        repeat(5) { round ->
            val content = withThinkingFallback { thinking -> attempt(model, GeminiJson.request(contents, thinking, model, today)) }
            val parts = content.optJSONArray("parts") ?: throw ChatException(ChatError.Other("모델 응답이 없습니다"))
            val calls = (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optJSONObject("functionCall") }
            if (calls.isEmpty()) {
                val text = (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.takeUnless { part -> part.optBoolean("thought", false) }?.optString("text")?.takeIf(String::isNotEmpty) }.joinToString("")
                if (text.isBlank()) throw ChatException(ChatError.Other("모델 응답이 없습니다"))
                val reason = content.optString("finishReason", "")
                val suffix = finishSuffix(reason)
                if (suffix.isNotEmpty()) {
                    val safeReason = reason.takeIf { it.matches(Regex("[A-Z_]{1,40}")) } ?: "OTHER"
                    Log.w("HCDEBUG", "chat fail kind=finish code=0 finish=$safeReason ex=none")
                }
                val result = text + suffix
                return result
            }
            if (round >= 4) throw ChatException(ChatError.Other("도구 호출 횟수를 초과했습니다"))
            appendToolTurns(contents, content) { call ->
                val name = call.optString("name")
                calledTools.add(name.takeIf { it in setOf("get_daily_summary", "get_period_stats", "get_sleep_days", "get_walk_sessions", "get_data_range") } ?: "unknown")
                tools.execute(name, call.optJSONObject("args") ?: JSONObject())
            }
        }
        throw ChatException(ChatError.Other("도구 호출 횟수를 초과했습니다"))
    }
    private fun attempt(model: String, request: JSONObject): JSONObject {
        val start = System.nanoTime()
        var outcome = "ok"
        try { return post(model, request) }
        catch (e: Exception) { outcome = when (e) {
            is RetryableServerException -> "http"
            is ThinkingUnsupportedException -> "thinking"
            is StreamInterruptedException -> "stream"
            is SocketTimeoutException -> "timeout"
            is ChatException -> if (e.error == ChatError.Network) "network" else "http"
            else -> "error"
        }; throw e }
        finally { Log.i("HCDEBUG", "chat attempt model=$model ms=${(System.nanoTime() - start) / 1_000_000} outcome=$outcome") }
    }

    private fun post(model: String, request: JSONObject): JSONObject {
        val conn = try { URL(GeminiConfig.endpoint(model)).openConnection() as HttpURLConnection }
            catch (e: IOException) { Log.w("HCDEBUG", "chat fail kind=network code=0 finish=none ex=${e.javaClass.simpleName}"); throw ChatException(ChatError.Network) }
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = 15_000; conn.readTimeout = 60_000; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code == 404) { Log.w("HCDEBUG", "chat fail kind=http code=404 finish=none ex=ModelNotFoundException"); throw ModelNotFoundException() }
            if (isRetryableStatus(code)) { Log.w("HCDEBUG", "chat fail kind=http code=$code finish=none ex=RetryableServerException"); throw RetryableServerException() }
            val errorBody = if (code == 400) conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } else null
            if (code == 400) {
                val error = errorBody?.let { runCatching { JSONObject(it).optJSONObject("error") }.getOrNull() }
                val status = error?.optString("status", "")?.takeIf { it.matches(Regex("[A-Z_]{1,40}")) } ?: "unknown"
                val message = safeErrorMessage(error?.optString("message", "").orEmpty(), key, request)
                Log.w("HCDEBUG", "chat fail kind=http code=400 status=$status msg=$message")
            }
            if (isThinkingUnsupported(code, errorBody)) { Log.w("HCDEBUG", "chat fail kind=http code=$code finish=none ex=ThinkingUnsupportedException"); throw ThinkingUnsupportedException() }
            httpError(code, errorBody)?.let { Log.w("HCDEBUG", "chat fail kind=http code=$code finish=none ex=ChatException"); throw ChatException(it) }
            val stream = StreamAccumulator(onText = { text ->
                if (firstTokenMs == null) firstTokenMs = (System.nanoTime() - startNanos) / 1_000_000
            }, onParseError = { Log.w("HCDEBUG", "chat fail kind=parse code=$code finish=none ex=JSONException") })
            try {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    readSse(reader, stream::accept)
                }
                if (!stream.sawContent && stream.parseErrors > 0) throw org.json.JSONException("invalid stream")
            } catch (e: StreamServerException) {
                Log.w("HCDEBUG", "chat fail kind=http code=${e.code} finish=none ex=${e.javaClass.simpleName}")
                if (e.code == 404) throw ModelNotFoundException()
                if (isRetryableStatus(e.code)) throw RetryableServerException()
                throw ChatException(httpError(e.code) ?: ChatError.Other("응답을 처리하지 못했습니다"))
            } catch (e: IOException) {
                val kind = if (e is SocketTimeoutException) "timeout" else "network"
                Log.w("HCDEBUG", "chat fail kind=$kind code=$code finish=none ex=${e.javaClass.simpleName}")
                if (e is UnknownHostException) throw ChatException(ChatError.Network)
                throw StreamInterruptedException(e)
            } catch (e: org.json.JSONException) {
                Log.w("HCDEBUG", "chat fail kind=parse code=$code finish=none ex=${e.javaClass.simpleName}")
                throw StreamInterruptedException(e)
            }
            if (stream.parseErrors > 0) Log.w("HCDEBUG", "chat fail kind=parse code=$code finish=none ex=JSONException count=${stream.parseErrors}")
            return stream.content()
        } catch (e: SocketTimeoutException) { Log.w("HCDEBUG", "chat fail kind=timeout code=0 finish=none ex=${e.javaClass.simpleName}"); throw e }
        catch (e: IOException) { Log.w("HCDEBUG", "chat fail kind=network code=0 finish=none ex=${e.javaClass.simpleName}"); throw ChatException(ChatError.Network) } finally { conn.disconnect() }
    }
}
