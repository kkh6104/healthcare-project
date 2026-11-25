package com.gh.healthapp.chat

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.gh.healthapp.BuildConfig
import com.gh.healthapp.data.HealthDatabase
import com.gh.healthapp.data.KbChunkEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

internal class EmbeddingClient(private val key: String) {
    suspend fun embed(text: String, query: Boolean): FloatArray = withContext(Dispatchers.IO) {
        var last: Exception? = null
        repeat(3) { attempt ->
            try { return@withContext request(text, query) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { last = e; if (attempt < 2) delay(1000L shl attempt) }
        }
        throw last ?: IOException("embedding unavailable")
    }
    private fun request(text: String, query: Boolean): FloatArray {
        val model = "gemini-embedding-001"
        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:embedContent").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = 15_000; conn.readTimeout = 60_000; conn.doOutput = true
            conn.setRequestProperty("x-goog-api-key", key); conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = JSONObject().put("model", "models/$model").put("content", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text))))
                .put("taskType", if (query) "RETRIEVAL_QUERY" else "RETRIEVAL_DOCUMENT").put("outputDimensionality", 768)
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) throw IOException("embedding HTTP ${conn.responseCode}")
            val values = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }).getJSONObject("embedding").getJSONArray("values")
            require(values.length() == 768)
            return FloatArray(768) { values.getDouble(it).toFloat() }.also { require(it.all(Float::isFinite)) }
        } finally { conn.disconnect() }
    }
}

object KnowledgeBase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    @Volatile private var lastError: String? = null
    @Volatile private var scheduled = false
    private data class Documents(val hash: String, val chunks: List<KbChunk>)
    private fun documents(context: Context): Documents {
        val files = context.assets.list("kb").orEmpty().filter { it.endsWith(".md", ignoreCase = true) }.sorted()
        val digest = MessageDigest.getInstance("SHA-256")
        val chunks = mutableListOf<KbChunk>()
        files.forEach { name ->
            val bytes = context.assets.open("kb/$name").use { it.readBytes() }
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(nameBytes.size).array()); digest.update(nameBytes)
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.size).array()); digest.update(bytes)
            chunks.addAll(KbChunker.split(name, bytes.toString(Charsets.UTF_8)))
        }
        return Documents(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }, chunks)
    }
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("kb_index", Context.MODE_PRIVATE)
    /** Called at database startup; work is always done off the main thread. */
    fun start(context: Context) {
        synchronized(this) { if (scheduled) return; scheduled = true }
        scope.launch { ensureIndexed(context.applicationContext) }
    }
    internal fun requestIndex(context: Context) { scope.launch { ensureIndexed(context.applicationContext) } }
    suspend fun ensureIndexed(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        lock.withLock {
            val started = SystemClock.elapsedRealtime()
            var count = 0
            var outcome = "ok"
            try {
                val app = context.applicationContext
                val docs = documents(app)
                val dao = HealthDatabase.get(app).kbDao()
                count = dao.count()
                if (prefs(app).getString("hash", null) == docs.hash && count == docs.chunks.size) {
                    lastError = null
                    return@withLock Result.success(count)
                }
                if (docs.chunks.isNotEmpty() && BuildConfig.GEMINI_API_KEY.isBlank()) throw IllegalStateException("API 키가 설정되지 않았습니다")
                val client = EmbeddingClient(BuildConfig.GEMINI_API_KEY)
                val rows = docs.chunks.map { chunk ->
                    val vector = client.embed(chunk.text, false)
                    KbChunkEntity(docId = chunk.docId, title = chunk.title, heading = chunk.heading, text = chunk.text, source = chunk.source, sourceUrl = chunk.sourceUrl, vector = vectorBytes(vector), contentHash = docs.hash)
                }
                dao.replaceAll(rows)
                prefs(app).edit().putString("hash", docs.hash).apply()
                count = rows.size
                lastError = null
                Result.success(count)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                outcome = "error"
                lastError = "색인 생성에 실패했습니다"
                Result.failure(e)
            } finally { Log.i("HCDEBUG", "kb index chunks=$count ms=${SystemClock.elapsedRealtime() - started} result=$outcome") }
        }
    }
    suspend fun status(context: Context): KbStatus = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val docs = documents(app)
        val count = HealthDatabase.get(app).kbDao().count()
        KbStatus(count, prefs(app).getString("hash", null) == docs.hash && count == docs.chunks.size, lastError)
    }
    internal suspend fun search(context: Context, query: String): List<KbSource> = withContext(Dispatchers.IO) {
        try {
            val app = context.applicationContext
            val state = status(app)
            if (!state.indexed) { requestIndex(app); Log.i("HCDEBUG", "chat rag hits=0 top=0.00"); return@withContext emptyList() }
            if (state.chunks == 0 || BuildConfig.GEMINI_API_KEY.isBlank()) { Log.i("HCDEBUG", "chat rag hits=0 top=0.00"); return@withContext emptyList() }
            val vector = EmbeddingClient(BuildConfig.GEMINI_API_KEY).embed(query, true)
            val hits = rankChunks(vector, HealthDatabase.get(app).kbDao().all())
            Log.i("HCDEBUG", "chat rag hits=${hits.size} top=${String.format(Locale.US, "%.2f", hits.firstOrNull()?.second ?: 0.0)}")
            hits.mapIndexed { i, (row, _) -> KbSource(i + 1, row.title, row.heading, row.source, row.sourceUrl, row.text) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Log.i("HCDEBUG", "chat rag hits=0 top=0.00"); emptyList() }
    }
}
