package com.gh.healthapp.chat

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.StringReader
import java.net.SocketTimeoutException
import java.io.IOException

class GeminiJsonTest {
    @Test fun requestContainsInstructionAndFiveTools() {
        val request = GeminiJson.request(JSONArray().put(GeminiJson.content("user", "안녕")))
        assertTrue(request.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text").contains("진단"))
        assertEquals(5, request.getJSONArray("tools").getJSONObject(0).getJSONArray("functionDeclarations").length())
        assertEquals(0.6, request.getJSONObject("generationConfig").getDouble("temperature"), 0.0)
    }

    @Test fun parseTextAndFunctionCallPreservesParts() {
        val parts = JSONArray().put(JSONObject().put("text", "안녕"))
            .put(JSONObject().put("functionCall", JSONObject().put("name", "get_daily_summary").put("args", JSONObject())))
        val raw = JSONObject().put("candidates", JSONArray().put(JSONObject().put("content", JSONObject().put("role", "model").put("parts", parts))))
        val parsed = GeminiJson.response(raw).getJSONArray("parts")
        assertEquals("안녕", parsed.getJSONObject(0).getString("text"))
        assertEquals("get_daily_summary", parsed.getJSONObject(1).getJSONObject("functionCall").getString("name"))
    }

    @Test fun signedParallelCallsAreReplayedWithOriginalPartsInOrder() = runBlocking {
        val original = JSONArray("""[{"text":"계산 중","custom":{"x":1}},{"functionCall":{"name":"first","args":{"x":1}},"thoughtSignature":"sig-1","extra":true},{"functionCall":{"name":"second","id":"call-2","args":{"y":2}},"thoughtSignature":"sig-2"}]""")
        val stream = StreamAccumulator()
        for (i in 0 until original.length()) {
            stream.accept(JSONObject().put("candidates", JSONArray().put(JSONObject().put("content", JSONObject().put("parts", JSONArray().put(original.getJSONObject(i)))))).toString())
        }
        val contents = JSONArray().put(GeminiJson.content("user", "question"))
        val names = mutableListOf<String>()
        appendToolTurns(contents, stream.content()) { call -> names.add(call.getString("name")); JSONObject().put("ok", true) }
        val replay = GeminiJson.request(contents).getJSONArray("contents").getJSONObject(1).getJSONArray("parts")
        assertEquals(original.toString(), replay.toString())
        assertEquals(listOf("first", "second"), names)
        val replies = contents.getJSONObject(2).getJSONArray("parts")
        assertEquals("first", replies.getJSONObject(0).getJSONObject("functionResponse").getString("name"))
        assertEquals("second", replies.getJSONObject(1).getJSONObject("functionResponse").getString("name"))
        assertEquals("call-2", replies.getJSONObject(1).getJSONObject("functionResponse").getString("id"))
    }

    @Test fun splitSignatureAndFunctionArgumentsRemainTogether() {
        val parts = ModelParts()
        parts.add(JSONObject("""{"functionCall":{"name":"first","args":{"text":"ab"}}}"""))
        parts.add(JSONObject("""{"functionCall":{"args":{"text":"cd","limit":3}}}"""))
        parts.add(JSONObject("""{"thoughtSignature":"opaque"}"""))
        val result = parts.finish()
        assertEquals(1, result.length())
        assertEquals("opaque", result.getJSONObject(0).getString("thoughtSignature"))
        assertEquals("abcd", result.getJSONObject(0).getJSONObject("functionCall").getJSONObject("args").getString("text"))
        assertEquals(3, result.getJSONObject(0).getJSONObject("functionCall").getJSONObject("args").getInt("limit"))
    }

    @Test fun errorLogRedactsKeyAndUserQuestion() {
        val request = GeminiJson.request(JSONArray().put(GeminiJson.content("user", "요약\n\n질문: 비밀질문")))
        val message = safeErrorMessage("INVALID key123 비밀질문", "key123", request)
        assertFalse(message.contains("key123"))
        assertFalse(message.contains("비밀질문"))
        assertTrue(message.startsWith("INVALID"))
    }

    @Test fun apiErrorIsRejected() {
        try { GeminiJson.response(JSONObject().put("error", JSONObject().put("message", "bad"))); fail() }
        catch (e: ChatException) { assertTrue(e.error is ChatError.Other) }
    }

    @Test fun httpErrorsAreClassified() {
        assertEquals(ChatError.RateLimited, httpError(429))
        assertEquals(ChatError.Other("API 키 오류"), httpError(401))
        assertEquals(ChatError.Other("API 키 오류"), httpError(403))
        assertNull(httpError(200))
        assertEquals(ChatError.Other("API 키가 올바르지 않습니다"), httpError(400, "{\"error\":{\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}"))
        assertEquals(ChatError.Other("서버 오류 (400)"), httpError(400, "bad request"))
        listOf(500, 502, 503, 504).forEach { assertTrue(isRetryableStatus(it)) }
        assertFalse(isRetryableStatus(429))
    }

    @Test fun retriesThenFallsBackAndKeepsModelForToolRound() = runBlocking {
        val waits = mutableListOf<Long>()
        val selector = QuestionAttempts(wait = { waits.add(it) }, jitter = { it }, availability = ModelAvailability { 100L })
        val calls = mutableListOf<String>()
        val result = selector.execute { model ->
            calls += model
            if (model == GeminiConfig.MODELS.first() && isRetryableStatus(503)) throw RetryableServerException()
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(listOf(GeminiConfig.MODELS[0], GeminiConfig.MODELS[1]), calls)
        assertEquals(listOf(1000L), waits)
    }

    @Test fun allModelsFailWithBusy() = runBlocking {
        var attempts = 0
        var waits = 0
        val selector = QuestionAttempts(wait = { waits++ }, jitter = { it }, availability = ModelAvailability { 100L })
        try {
            selector.execute<Unit> { attempts++; throw RetryableServerException() }
            fail()
        } catch (e: ChatException) { assertEquals(ChatError.Busy, e.error) }
        assertEquals(5, attempts)
        assertEquals(4, waits)
    }

    @Test fun dateRangesAreInclusiveAndBounded() {
        assertNotNull(parseDateRange("2024-01-01", "2024-04-29", 120))
        assertNull(parseDateRange("2024-01-01", "2024-04-30", 120))
        assertNotNull(parseDateRange("2024-01-01", "2024-02-29", 60))
        assertNull(parseDateRange("2024-01-01", "2024-03-01", 60))
        assertNull(parseDateRange("2024-02-30", "2024-03-01", 60))
        assertNull(parseDateRange("2024-03-02", "2024-03-01", 60))
    }

    @Test fun sseMultipleChunksAndEofWithoutDone() {
        val events = mutableListOf<String>()
        readSse(StringReader("data: {\"a\":1}\n\ndata: {\"a\":2}\n\n\ndata: {\"a\":3}").buffered()) { events.add(it) }
        assertEquals(listOf("{\"a\":1}", "{\"a\":2}", "{\"a\":3}"), events)
        readSse(StringReader("data: [DONE]\n\n").buffered()) { fail() }
    }

    @Test fun timeoutFallsBackImmediatelyAndStartsCooldown() = runBlocking {
        var time = 100L
        val cooldown = ModelAvailability { time }
        var waits = 0
        val selector = QuestionAttempts(wait = { waits++ }, jitter = { it }, availability = cooldown)
        val calls = mutableListOf<String>()
        selector.execute { model -> calls.add(model); if (model == GeminiConfig.MODELS[0]) throw SocketTimeoutException(); "ok" }
        assertEquals(GeminiConfig.MODELS.take(2), calls)
        assertEquals(1, waits)
        assertEquals(GeminiConfig.MODELS[1], QuestionAttempts(availability = cooldown).execute { it })
        GeminiConfig.MODELS.drop(1).forEach(cooldown::markBusy)
        assertEquals(GeminiConfig.MODELS[1], QuestionAttempts(availability = cooldown).execute { it })
        time += 300_000
        assertEquals(GeminiConfig.MODELS[0], QuestionAttempts(availability = cooldown).execute { it })
    }

    @Test fun thinkingInvalidArgumentRetriesWithoutConfig() = runBlocking {
        val calls = mutableListOf<Boolean>()
        assertTrue(isThinkingUnsupported(400, "INVALID_ARGUMENT: thinkingConfig unsupported"))
        assertFalse(isThinkingUnsupported(400, "INVALID_ARGUMENT: other"))
        val result = withThinkingFallback { enabled ->
            calls.add(enabled)
            if (enabled) throw ThinkingUnsupportedException()
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(listOf(true, false), calls)
        assertFalse(GeminiJson.request(JSONArray(), false).getJSONObject("generationConfig").has("thinkingConfig"))
    }

    @Test fun corruptedSseChunkIsSkippedAndFinishReasonsArePreserved() {
        val stream = StreamAccumulator()
        val events = "data: {broken}\n\ndata: {\"usageMetadata\":{}}\n\ndata: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hello\"}]}}]}\n\ndata: {\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}\n\n"
        readSse(StringReader(events).buffered(), stream::accept)
        assertEquals(1, stream.parseErrors)
        assertEquals("hello", stream.text.toString())
        assertEquals("\n\n(답변이 길어 여기까지만 표시했어요)", finishSuffix(stream.finishReason))
        assertEquals("\n\n(답변이 중간에 끊겼어요)", finishSuffix("SAFETY"))
        assertEquals("", finishSuffix("STOP"))
        assertEquals(2048, GeminiJson.request(JSONArray()).getJSONObject("generationConfig").getInt("maxOutputTokens"))
    }

    @Test fun brokenStreamRetriesWithoutUsingFirstPartial() = runBlocking {
        val selector = QuestionAttempts(wait = {}, jitter = { it }, availability = ModelAvailability { 100L })
        val result = selector.execute {
            val stream = StreamAccumulator()
            if (selector.attempts == 1) {
                stream.accept("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"discarded\"}]}}]}")
                throw StreamInterruptedException(IOException())
            }
            stream.accept("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"complete\"}]}}]}")
            stream.text.toString()
        }
        assertEquals("complete", result)
        assertEquals(2, selector.attempts)
    }

    @Test fun missingModelIsExcludedAndBackoffSequenceIsBounded() = runBlocking {
        val state = ModelAvailability { 100L }
        val delays = mutableListOf<Long>()
        val models = mutableListOf<String>()
        val selector = QuestionAttempts(availability = state, wait = { delays.add(it) }, jitter = { it })
        try {
            selector.execute<Unit> { model ->
                models.add(model)
                if (model == GeminiConfig.MODELS[0]) throw ModelNotFoundException()
                throw RetryableServerException()
            }
            fail()
        } catch (e: ChatException) { assertEquals(ChatError.Busy, e.error) }
        assertEquals(5, models.size)
        assertEquals(1, models.count { it == GeminiConfig.MODELS[0] })
        assertEquals(listOf(1000L, 2000L, 4000L, 4000L), delays)
        assertTrue(state.isMissing(GeminiConfig.MODELS[0]))
        assertEquals(512, GeminiJson.request(JSONArray(), model = "gemini-2.5-flash").getJSONObject("generationConfig").getJSONObject("thinkingConfig").getInt("thinkingBudget"))
    }

    @Test fun streamErrorEventIsRecognized() {
        try { StreamAccumulator().accept("{\"error\":{\"code\":503}}") ; fail() }
        catch (e: StreamServerException) { assertEquals(503, e.code) }
    }
}
