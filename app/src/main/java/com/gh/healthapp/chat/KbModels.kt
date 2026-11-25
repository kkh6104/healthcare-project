package com.gh.healthapp.chat

import com.gh.healthapp.data.KbChunkEntity
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

data class KbSource(val index: Int, val title: String, val heading: String, val source: String, val sourceUrl: String?, val text: String)
data class KbStatus(val chunks: Int, val indexed: Boolean, val lastError: String?)
internal data class KbChunk(val docId: String, val title: String, val heading: String, val text: String, val source: String, val sourceUrl: String?)

internal object KbChunker {
    fun split(docId: String, input: String): List<KbChunk> {
        val normalized = input.replace("\r\n", "\n").removePrefix("\uFEFF")
        val lines = normalized.lines()
        var bodyStart = 0
        val meta = mutableMapOf<String, String>()
        if (lines.firstOrNull()?.trim() == "---") {
            // A front-matter terminator must occur on a later line.
            val close = (1 until lines.size).firstOrNull { lines[it].trim() == "---" }
            if (close != null) {
                for (line in lines.subList(1, close)) {
                    val colon = line.indexOf(':')
                    if (colon > 0) meta[line.substring(0, colon).trim()] = line.substring(colon + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                }
                bodyStart = close + 1
            }
        }
        val title = meta["title"]?.takeIf(String::isNotBlank) ?: docId.removeSuffix(".md")
        val source = meta["source"].orEmpty()
        val url = meta["source_url"]?.takeIf { it.isNotBlank() }
        val result = mutableListOf<KbChunk>()
        var heading = ""
        val body = StringBuilder()
        fun flush() {
            val content = body.toString().trim()
            if (content.isNotEmpty()) {
                var offset = 0
                val prefix = (if (heading.isBlank()) title else "$title – $heading").take(400)
                val capacity = 600 - prefix.length - 1
                while (offset < content.length) {
                    val end = minOf(offset + capacity, content.length)
                    result.add(KbChunk(docId, title, heading, "$prefix\n${content.substring(offset, end)}", source, url))
                    if (end == content.length) break
                    offset = end - 100
                }
            }
            body.setLength(0)
        }
        for (line in lines.drop(bodyStart)) {
            if (line.startsWith("## ")) { flush(); heading = line.removePrefix("## ").trim() }
            else body.append(line).append('\n')
        }
        flush()
        return result
    }
}

internal fun vectorBytes(vector: FloatArray): ByteArray = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { vector.forEach { putFloat(it) } }.array()
internal fun bytesVector(bytes: ByteArray): FloatArray {
    require(bytes.size % 4 == 0)
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(bytes.size / 4) { buffer.float }
}
internal fun cosine(a: FloatArray, b: FloatArray): Double {
    if (a.isEmpty() || a.size != b.size) return 0.0
    var dot = 0.0; var aa = 0.0; var bb = 0.0
    for (i in a.indices) { dot += a[i].toDouble() * b[i]; aa += a[i].toDouble() * a[i]; bb += b[i].toDouble() * b[i] }
    return if (aa == 0.0 || bb == 0.0) 0.0 else dot / sqrt(aa * bb)
}
internal const val KB_THRESHOLD = 0.55
internal fun rankChunks(query: FloatArray, chunks: List<KbChunkEntity>): List<Pair<KbChunkEntity, Double>> = chunks.mapNotNull {
    val score = runCatching { cosine(query, bytesVector(it.vector)) }.getOrDefault(0.0)
    if (score >= KB_THRESHOLD) it to score else null
}.sortedByDescending { it.second }.take(4)

internal fun sourcesToJson(sources: List<KbSource>): String = JSONArray().apply { sources.forEach { s -> put(JSONObject().apply {
    put("index", s.index); put("title", s.title); put("heading", s.heading); put("source", s.source); put("sourceUrl", s.sourceUrl); put("text", s.text)
}) } }.toString()
internal fun sourcesFromJson(json: String?): List<KbSource> = runCatching {
    if (json == null) return emptyList()
    val array = JSONArray(json)
    (0 until array.length()).map { i -> array.getJSONObject(i).let { o -> KbSource(o.getInt("index"), o.getString("title"), o.getString("heading"), o.getString("source"), if (o.isNull("sourceUrl")) null else o.optString("sourceUrl").takeIf(String::isNotEmpty), o.getString("text")) } }
}.getOrDefault(emptyList())
