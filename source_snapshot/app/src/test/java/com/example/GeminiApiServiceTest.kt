package com.example

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.Assert.*
import retrofit2.HttpException
import retrofit2.Response

class GeminiApiServiceTest {
    @Test
    fun testPlanContentInterpolation() {
        val testContent = "UNIQUE_TEST_PLAN_12345"
        
        val requestPresentation = buildPresentationRequest(testContent)
        val requestActivity = buildFiveMinuteActivityRequest(testContent)
        val requestHomework = buildHomeworkRequest(testContent, emptyList())
        
        val textPresentation = requestPresentation.contents.first().parts?.first()?.text
        val textActivity = requestActivity.contents.first().parts?.first()?.text
        val textHomework = requestHomework.contents.first().parts?.first()?.text
        
        // Assert contains the actual string
        assertTrue(textPresentation?.contains(testContent) == true)
        assertTrue(textActivity?.contains(testContent) == true)
        assertTrue(textHomework?.contains(testContent) == true)
        
        // Assert does not contain literal variable name
        assertFalse(textPresentation?.contains("\$planContent") == true)
        assertFalse(textActivity?.contains("\$planContent") == true)
        assertFalse(textHomework?.contains("\$planContent") == true)
    }

    @Test
    fun testHomeworkCanBeLockedToOneMksClass() {
        val request = buildHomeworkRequest("# Класс 1\nМатематика\n# Класс 2\nРусский", emptyList(), "Класс 2")
        val system = request.systemInstruction?.parts?.firstOrNull()?.text.orEmpty()
        val prompt = request.contents.first().parts?.firstOrNull()?.text.orEmpty()
        assertTrue(system.contains("ТОЛЬКО к Класс 2"))
        assertTrue(system.contains("только для Класс 2"))
        assertTrue(prompt.contains("Целевой класс: Класс 2"))
    }

    @Test
    fun locationUnsupportedMessageIsRecognizedForIpv4Fallback() {
        assertTrue(GeminiErrorMapper.isLocationUnsupportedText(
            "User location is not supported for the API use."
        ))
        assertTrue(GeminiErrorMapper.isLocationUnsupportedText(
            "FAILED_PRECONDITION: location is not supported for the API"
        ))
        assertFalse(GeminiErrorMapper.isLocationUnsupportedText(
            "API key not valid. Please pass a valid API key."
        ))
    }

    @Test
    fun dailyQuotaErrorIsExplainedWithoutPointlessImmediateRetry() {
        val exception = httpError(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded","details":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}}"""
        )

        assertTrue(GeminiErrorMapper.fromHttp(exception).contains("Дневная квота"))
        assertFalse(GeminiErrorMapper.shouldRetry(exception))
    }

    @Test
    fun shortRateLimitUsesServerRetryDelay() {
        val exception = httpError(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"Too many requests per minute","details":[{"retryDelay":"3s"}]}}"""
        )

        assertTrue(GeminiErrorMapper.fromHttp(exception).contains("через 3 сек"))
        assertTrue(GeminiErrorMapper.shouldRetry(exception))
        assertEquals(3_000L, GeminiErrorMapper.retryDelayMillis(exception, 0))
    }

    @Test
    fun mixedQuotaPayloadPrefersActionablePerMinuteLimit() {
        val exception = httpError(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded","details":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"},{"quotaId":"GenerateRequestsPerMinutePerProjectPerModel-FreeTier"},{"retryDelay":"18s"}]}}"""
        )

        val message = GeminiErrorMapper.fromHttp(exception)
        assertTrue(message.contains("короткое время"))
        assertFalse(message.contains("Дневная квота"))
        assertTrue(GeminiErrorMapper.shouldRetry(exception))
        assertEquals(18_000L, GeminiErrorMapper.retryDelayMillis(exception, 0))
    }

    @Test
    fun inputTokenLimitIsNotReportedAsDailyQuota() {
        val exception = httpError(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[{"quotaMetric":"generativelanguage.googleapis.com/generate_content_free_tier_input_token_count","quotaId":"GenerateContentInputTokensPerModelPerMinute-FreeTier"},{"retryDelay":"12s"}]}}"""
        )

        val message = GeminiErrorMapper.fromHttp(exception)
        assertTrue(message.contains("объём текста"))
        assertFalse(message.contains("Дневная квота"))
        assertTrue(GeminiErrorMapper.shouldRetry(exception))
    }

    @Test
    fun zeroModelQuotaSelectsStableFallback() {
        val exception = httpError(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[{"quotaMetric":"generativelanguage.googleapis.com/generate_content_free_tier_requests","quotaValue":"0"}]}}"""
        )

        assertTrue(GeminiErrorMapper.shouldUseModelFallback(exception))
        assertFalse(GeminiErrorMapper.shouldRetry(exception))
    }

    @Test
    fun transientMinuteLimitRetriesPrimaryBeforeFallback() {
        val exception = httpError(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[{"quotaId":"GenerateRequestsPerMinutePerProjectPerModel-FreeTier"},{"retryDelay":"5s"}]}}"""
        )

        assertFalse(GeminiErrorMapper.shouldUseModelFallback(exception))
        assertTrue(GeminiErrorMapper.shouldRetry(exception))
    }

    private fun httpError(code: Int, body: String): HttpException = HttpException(
        Response.error<GenerateContentResponse>(
            code,
            body.toResponseBody("application/json".toMediaType())
        )
    )
}
