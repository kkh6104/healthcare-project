package com.gh.healthapp.chat

import com.gh.healthapp.data.KbChunkEntity
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import java.time.LocalDate

class KbTest {
    @Test fun frontMatterAndOverlappingChunks() {
        val text = "---\ntitle: 수면\nsource: 기관\nsource_url: https://example.org/a\nupdated: 2026\n---\n## 생활\n" + "가".repeat(1200) + "\n## 시간\n충분히 자기"
        val chunks = KbChunker.split("sleep.md", text)
        assertEquals("수면", chunks[0].title)
        assertEquals("기관", chunks[0].source)
        assertEquals("https://example.org/a", chunks[0].sourceUrl)
        assertTrue(chunks.all { it.text.length <= 600 })
        assertEquals("생활", chunks[0].heading)
        assertEquals(chunks[0].text.takeLast(100), chunks[1].text.substringAfter('\n').take(100))
        assertEquals("시간", chunks.last().heading)
    }
    @Test fun vectorsAndRanking() {
        val v = floatArrayOf(1f, 2f, -0.5f)
        assertArrayEquals(v, bytesVector(vectorBytes(v)), 0f)
        assertEquals(1.0, cosine(v, v), 1e-9)
        assertEquals(0.0, cosine(FloatArray(3), v), 0.0)
        fun row(id: Long, vector: FloatArray) = KbChunkEntity(id, "a", "t", "h", "x", "s", null, vectorBytes(vector), "hash")
        val rows = listOf(row(1, floatArrayOf(-1f, 0f)), row(2, floatArrayOf(1f, 0f))) + (3L..7L).map { row(it, floatArrayOf(1f, 0.2f)) }
        val hits = rankChunks(floatArrayOf(1f, 0f), rows)
        assertEquals(4, hits.size)
        assertEquals(2L, hits.first().first.id)
        assertTrue(hits.all { it.second >= KB_THRESHOLD })
    }
    @Test fun sourcesRoundTripAndDateInstruction() {
        val sources = listOf(KbSource(1, "제목", "절", "출처", null, "내용"), KbSource(2, "t", "h", "s", "https://example.org", "x"))
        assertEquals(sources, sourcesFromJson(sourcesToJson(sources)))
        val request = GeminiJson.request(JSONArray(), today = LocalDate.of(2026, 11, 25))
        val instruction = request.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text")
        assertTrue(instruction.contains("2026-11-25(수)"))
        assertTrue(instruction.contains("반드시 도구"))
        assertEquals("AUTO", request.getJSONObject("toolConfig").getJSONObject("functionCallingConfig").getString("mode"))
    }
}
