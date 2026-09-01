package com.example

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Dns
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import java.net.Inet4Address
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    @kotlinx.serialization.SerialName("system_instruction")
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>? = null
)

@Serializable
data class InlineData(
    @kotlinx.serialization.SerialName("mime_type")
    val mimeType: String,
    val data: String
)

@Serializable
data class Part(
    val text: String? = null,
    @kotlinx.serialization.SerialName("inline_data")
    val inlineData: InlineData? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private fun baseClientBuilder() = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)

    private val okHttpClient = baseClientBuilder().build()

    private val ipv4OnlyDns = object : Dns {
        override fun lookup(hostname: String): List<java.net.InetAddress> {
            val ipv4 = Dns.SYSTEM.lookup(hostname).filterIsInstance<Inet4Address>()
            if (ipv4.isEmpty()) throw UnknownHostException("No IPv4 address for ${hostname}")
            return ipv4
        }
    }

    private val ipv4OkHttpClient = baseClientBuilder()
        .dns(ipv4OnlyDns)
        .build()

    private fun createService(client: OkHttpClient): GeminiApiService {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    val service: GeminiApiService by lazy { createService(okHttpClient) }

    val ipv4Service: GeminiApiService by lazy { createService(ipv4OkHttpClient) }
}

sealed class GeminiExtraResult {
    data class Success(val text: String) : GeminiExtraResult()
    data class Error(val message: String) : GeminiExtraResult()
}

internal suspend fun executeGeminiRequest(
    apiKey: String,
    request: GenerateContentRequest,
    hasImages: Boolean = false
): GeminiExtraResult {
    if (!GeminiApiKeyProvider.isUsable(apiKey)) {
        return GeminiExtraResult.Error("Ключ Gemini не настроен. На экране «Создать» нажмите на верхнего маскота и сохраните личный API-ключ.")
    }

    val primaryModel = "gemini-3.5-flash"
    val fallbackModel = "gemini-3.5-flash-lite"
    var activeModel = primaryModel
    var usingModelFallback = false
    var lastError: Exception? = null
    var useIpv4Fallback = false
    repeat(4) { attempt ->
        try {
            val service = if (useIpv4Fallback) RetrofitClient.ipv4Service else RetrofitClient.service
            val response = service.generateContent(activeModel, apiKey, request)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { !it.text.isNullOrBlank() }
                ?.text
                ?.trim()

            return if (!text.isNullOrBlank()) {
                GeminiExtraResult.Success(LessonTextSanitizer.sanitizeGeminiMarkdown(text))
            } else {
                GeminiExtraResult.Error("Gemini вернул пустой ответ. Повторите запрос.")
            }
        } catch (e: HttpException) {
            lastError = e
            if (!useIpv4Fallback && GeminiErrorMapper.isLocationUnsupported(e)) {
                // Google occasionally misclassifies supported IPv6 ranges as unsupported regions.
                // Retry the same request over IPv4 before surfacing the location error.
                useIpv4Fallback = true
                delay(150)
            } else if (!usingModelFallback && GeminiErrorMapper.shouldUseModelFallback(e)) {
                // Quotas are model-specific. Keep the established primary model,
                // but recover from a zero/daily model quota with Google's current
                // stable Flash-Lite model. Prompts and response handling stay the same.
                usingModelFallback = true
                activeModel = fallbackModel
                delay(150)
            } else {
                val retryable = GeminiErrorMapper.shouldRetry(e)
                if (attempt < 3 && retryable) {
                    delay(GeminiErrorMapper.retryDelayMillis(e, attempt))
                } else {
                    return GeminiExtraResult.Error(GeminiErrorMapper.fromHttp(e, hasImages))
                }
            }
        } catch (e: SocketTimeoutException) {
            lastError = e
            if (attempt < 2) delay(600) else return GeminiExtraResult.Error(GeminiErrorMapper.fromException(e))
        } catch (e: Exception) {
            return GeminiExtraResult.Error(GeminiErrorMapper.fromException(e))
        }
    }

    return GeminiExtraResult.Error(lastError?.let { GeminiErrorMapper.fromException(it) } ?: "Не удалось выполнить запрос Gemini.")
}

internal fun buildPresentationRequest(planContent: String): GenerateContentRequest {
    val systemPrompt = "Ты — школьный методист для 1–5 классов. На основе плана урока составь структуру презентации. Для каждого слайда напиши заголовок, текст и опиши визуальный ряд. Ответ выдай в формате Markdown."
    return GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = "План урока:\n$planContent")))),
        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
    )
}

internal fun buildFiveMinuteActivityRequest(planContent: String): GenerateContentRequest {
    val isTwoClasses = LessonPromptBuilder.looksLikeTwoClassPlan(planContent)
    val systemPrompt = if (isTwoClasses) {
        "Ты — школьный методист для 1–5 классов. Проанализируй этот план урока совмещённых классов и создай интересную 5-минутную активность, которая объединит оба класса. Ответ выдай в формате Markdown, без лишних вступлений."
    } else {
        "Ты — школьный методист для 1–5 классов. Проанализируй этот план урока и создай интересную 5-минутную активность для выбранного класса. Никаких упоминаний другого класса быть не должно. Ответ выдай в формате Markdown, без лишних вступлений."
    }
    return GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = "План урока:\n$planContent")))),
        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
    )
}

internal fun buildHomeworkRequest(
    planContent: String,
    images: List<Pair<String, String>>,
    targetClass: String? = null
): GenerateContentRequest {
    val targetRule = if (targetClass.isNullOrBlank()) {
        "Определи класс только из исходного плана и материала."
    } else {
        "Фотография относится ТОЛЬКО к $targetClass. Составь домашнее задание только для $targetClass и не добавляй задания второму классу."
    }
    val systemPrompt = "Ты — школьный методист для 1–5 классов. Проанализируй этот план урока и приложенные фотографии учебников. Составь домашнее задание, которое опирается на материал из учебников и соответствует плану урока. $targetRule Не добавляй задания для классов, которых нет в исходном плане/материале. Ответ выдай в формате Markdown."

    val promptPrefix = if (targetClass.isNullOrBlank()) "" else "Целевой класс: $targetClass\n"
    val parts = mutableListOf<Part>(Part(text = promptPrefix + "План урока:\n" + planContent))
    images.forEach { (mimeType, base64Image) ->
        parts.add(Part(inlineData = InlineData(mimeType = mimeType, data = base64Image)))
    }

    return GenerateContentRequest(
        contents = listOf(Content(parts = parts)),
        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
    )
}

suspend fun generatePresentationOutline(context: Context, planContent: String): GeminiExtraResult =
    executeGeminiRequest(
        apiKey = GeminiApiKeyProvider.get(context),
        request = buildPresentationRequest(planContent)
    )

suspend fun generateFiveMinuteActivity(context: Context, planContent: String): GeminiExtraResult =
    executeGeminiRequest(
        apiKey = GeminiApiKeyProvider.get(context),
        request = buildFiveMinuteActivityRequest(planContent)
    )

suspend fun generateHomeworkFromImage(
    context: Context,
    planContent: String,
    images: List<Pair<String, String>>,
    targetClass: String? = null
): GeminiExtraResult = executeGeminiRequest(
    apiKey = GeminiApiKeyProvider.get(context),
    request = buildHomeworkRequest(planContent, images, targetClass),
    hasImages = images.isNotEmpty()
)

fun getFriendlyErrorMessage(e: Exception): String = GeminiErrorMapper.fromException(e)
