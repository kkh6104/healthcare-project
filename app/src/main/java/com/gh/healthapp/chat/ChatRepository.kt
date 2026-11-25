package com.gh.healthapp.chat

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.gh.healthapp.BuildConfig
import com.gh.healthapp.data.ChatMessageEntity
import com.gh.healthapp.data.HealthDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDate

data class ChatMessage(val id: Long, val role: String, val text: String, val time: Long, val sources: List<KbSource> = emptyList())
data class ChatProgress(val attempt: Int, val maxAttempts: Int, val retrying: Boolean, val startedAt: Long)
sealed interface ChatError {
    data object NoKey : ChatError
    data object Network : ChatError
    data object RateLimited : ChatError
    data object Busy : ChatError
    data class Other(val message: String) : ChatError
}
class ChatException(val error: ChatError) : Exception()

object ChatConsent {
    private const val FILE = "chat_consent"
    fun isGiven(context: Context): Boolean = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("given", false)
    fun set(context: Context, given: Boolean) { context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("given", given).apply() }
}

class ChatRepository(context: Context) {
    private val context = context.applicationContext
    private val db = HealthDatabase.get(this.context)
    private val dao = db.chatDao()
    private val mutex = Mutex()
    private val mutableStreaming = MutableStateFlow<String?>(null)
    val streaming: StateFlow<String?> = mutableStreaming.asStateFlow()
    private val mutableProgress = MutableStateFlow<ChatProgress?>(null)
    val progress: StateFlow<ChatProgress?> = mutableProgress.asStateFlow()
    val messages: Flow<List<ChatMessage>> = dao.messages().map { list -> list.map { ChatMessage(it.id, it.role, it.text, it.time, sourcesFromJson(it.sourcesJson)) } }
    fun hasApiKey(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()
    suspend fun clear() = mutex.withLock { dao.clear() }

    suspend fun send(userText: String): Result<Unit> = mutex.withLock { sendLocked(userText, null) }

    suspend fun resend(userMessageId: Long): Result<Unit> = mutex.withLock { sendLocked(null, userMessageId) }

    private suspend fun sendLocked(newText: String?, userMessageId: Long?): Result<Unit> {
        val started = SystemClock.elapsedRealtime()
        val startedAt = System.currentTimeMillis()
        mutableProgress.value = ChatProgress(0, 5, false, startedAt)
        var client: GeminiClient? = null
        var resultLabel = "ok"
        return try {
            val id = if (newText != null) dao.insert(ChatMessageEntity(role = "user", text = newText, time = System.currentTimeMillis())) else userMessageId!!
            val records = dao.recent(Int.MAX_VALUE).asReversed()
            val target = records.firstOrNull { it.id == id && it.role == "user" }
                ?: throw ChatException(ChatError.Other("사용자 메시지를 찾을 수 없습니다"))
            if (newText == null && records.any { it.id > id && it.role == "model" })
                throw ChatException(ChatError.Other("이미 응답한 메시지입니다"))
            if (!ChatConsent.isGiven(context)) throw ChatException(ChatError.Other("데이터 전송 동의가 필요합니다"))
            if (!hasApiKey()) throw ChatException(ChatError.NoKey)
            val previous = records.filter { it.id < id }.takeLast(20)
            // Only complete turns are sent; failed user messages remain in local history.
            val turns = mutableListOf<ChatMessage>()
            var pending: ChatMessage? = null
            previous.forEach { item ->
                val message = ChatMessage(item.id, item.role, item.text, item.time)
                if (item.role == "user") pending = message
                else if (item.role == "model" && pending != null) { turns.add(pending!!); turns.add(message); pending = null }
            }
            val tools = HealthTools(db)
            val today = LocalDate.now()
            val summary = tools.execute("get_daily_summary", JSONObject().put("start_date", today.minusDays(6).toString()).put("end_date", today.toString()))
            val sources = KnowledgeBase.search(context, target.text)
            client = GeminiClient(BuildConfig.GEMINI_API_KEY, tools) { attempt, retrying -> mutableProgress.value = ChatProgress(attempt, 5, retrying, startedAt) }
            val answer = client.generate(turns.takeLast(20), target.text, summary, sources, today)
            val used = Regex("\\[(\\d+)\\]").findAll(answer).mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()
            val citedSources = sources.filter { it.index in used }
            dao.insert(ChatMessageEntity(role = "model", text = answer, time = System.currentTimeMillis(), sourcesJson = sourcesToJson(citedSources)))
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            val error = (e as? ChatException)?.error ?: ChatError.Other("요청을 처리하지 못했습니다")
            resultLabel = when (error) { ChatError.NoKey -> "no_key"; ChatError.Network -> "network"; ChatError.RateLimited -> "rate_limited"; ChatError.Busy -> "busy"; is ChatError.Other -> "other" }
            Result.failure(ChatException(error))
        } finally {
            mutableStreaming.value = null
            mutableProgress.value = null
            Log.i("HCDEBUG", "chat send attempts=${client?.attempts ?: 0} model=${client?.model ?: "none"} tools=${client?.calledTools?.joinToString(",") ?: ""} ms=${SystemClock.elapsedRealtime() - started} firstTokenMs=${client?.firstTokenMs ?: -1} stream=true result=$resultLabel")
        }
    }
}
