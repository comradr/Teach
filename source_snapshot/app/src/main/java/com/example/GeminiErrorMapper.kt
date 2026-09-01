package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Serializable
private data class GoogleErrorEnvelope(val error: GoogleError? = null)

@Serializable
private data class GoogleError(val code: Int? = null, val message: String? = null, val status: String? = null)

object GeminiErrorMapper {
    private const val MAX_ERROR_BODY_BYTES = 256L * 1024L
    private const val MAX_AUTOMATIC_RATE_LIMIT_WAIT_SECONDS = 45L
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val retryDelayPattern = Regex("""\"retryDelay\"\s*:\s*\"(\d+(?:\.\d+)?)s\"""")
    private val zeroQuotaPattern = Regex("""\"quotaValue\"\s*:\s*\"?0(?:\.0+)?\"?""", RegexOption.IGNORE_CASE)

    private enum class QuotaKind {
        REQUESTS_PER_MINUTE,
        INPUT_TOKENS_PER_MINUTE,
        DAILY_REQUESTS,
        SPEND,
        UNAVAILABLE,
        UNKNOWN
    }

    fun fromHttp(exception: HttpException, hasImages: Boolean = false): String {
        val body = peekErrorBody(exception)
        val detail = parseMessage(body)
        val normalized = (detail + " " + body).lowercase()

        return when (exception.code()) {
            400 -> when {
                normalized.contains("api key") || normalized.contains("api_key") || normalized.contains("key not valid") ->
                    "API-ключ Gemini недействителен. Нажмите на верхнего маскота на экране «Создать» и сохраните актуальный ключ."
                normalized.contains("quota") || normalized.contains("billing") ->
                    "Для этого API-ключа недоступен запрос Gemini. Проверьте квоту и настройки проекта Google AI."
                isLocationUnsupportedText(normalized) ->
                    "Google Gemini отклонил соединение из-за определения региона. Приложение уже попробовало повторить запрос через IPv4. Попробуйте другую сеть или VPN-сервер в поддерживаемом регионе."
                hasImages && (normalized.contains("image") || normalized.contains("mime") || normalized.contains("inline") || normalized.contains("media")) ->
                    "Gemini не смог обработать прикреплённое изображение. Попробуйте другое фото или уменьшите его размер."
                else -> friendlyBadRequest(detail)
            }
            401, 403 -> "Нет доступа к Gemini. Проверьте личный API-ключ в настройках маскота и разрешения ключа в Google AI."
            404 -> "Модель Gemini сейчас недоступна для этого API-ключа. Попробуйте обновить приложение или выбрать другой ключ."
            408 -> "Gemini слишком долго отвечал. Повторите запрос."
            429 -> rateLimitMessage(
                kind = classifyQuota(normalized, body),
                retryDelaySeconds = retryDelaySeconds(exception)
            )
            500, 502, 503, 504 -> "Gemini временно недоступен. Повторите запрос чуть позже."
            else -> if (detail.isNotBlank()) "Ошибка Gemini (${exception.code()}): ${detail.take(220)}" else "Ошибка Gemini (${exception.code()})."
        }
    }

    fun isLocationUnsupported(exception: HttpException): Boolean {
        if (exception.code() != 400 && exception.code() != 403) return false
        return isLocationUnsupportedText(peekErrorBody(exception).lowercase())
    }

    /** True only for a short-lived limit that can realistically recover during this request. */
    internal fun shouldRetry(exception: HttpException): Boolean {
        if (exception.code() in 500..599) return true
        if (exception.code() != 429) return false
        val body = peekErrorBody(exception)
        val normalized = body.lowercase()
        val kind = classifyQuota(normalized, body)
        val requestedWait = retryDelaySeconds(exception)

        // Gemini can include several quota descriptors in one 429 response. A
        // short RetryInfo delay or a per-minute metric is actionable even when
        // the same payload also names a per-day quota. Previously the broad
        // "perDay" check incorrectly turned this into a daily-limit error.
        if (kind == QuotaKind.SPEND || kind == QuotaKind.UNAVAILABLE) return false
        if (requestedWait != null) return requestedWait <= MAX_AUTOMATIC_RATE_LIMIT_WAIT_SECONDS
        return kind != QuotaKind.DAILY_REQUESTS
    }

    /** A stable secondary model is used only for model-specific persistent limits. */
    internal fun shouldUseModelFallback(exception: HttpException): Boolean {
        if (exception.code() == 404) return true
        if (exception.code() != 429) return false
        val body = peekErrorBody(exception)
        val kind = classifyQuota(body.lowercase(), body)
        return kind == QuotaKind.UNAVAILABLE || kind == QuotaKind.DAILY_REQUESTS
    }

    internal fun retryDelayMillis(exception: HttpException, attempt: Int): Long {
        val serverSeconds = retryDelaySeconds(exception)
        if (serverSeconds != null) {
            return (serverSeconds * 1_000L).coerceIn(
                1_000L,
                MAX_AUTOMATIC_RATE_LIMIT_WAIT_SECONDS * 1_000L
            )
        }
        return listOf(2_000L, 6_000L, 12_000L)[attempt.coerceIn(0, 2)]
    }

    internal fun isLocationUnsupportedText(text: String): Boolean {
        val normalized = text.lowercase()
        return normalized.contains("user location is not supported") ||
            (normalized.contains("location") && normalized.contains("not supported") && normalized.contains("api"))
    }

    fun fromException(exception: Exception): String = when (exception) {
        is HttpException -> fromHttp(exception)
        is UnknownHostException -> "Нет подключения к интернету."
        is SocketTimeoutException -> "Gemini не ответил вовремя. Проверьте интернет и повторите запрос."
        else -> {
            val message = exception.localizedMessage.orEmpty().lowercase()
            when {
                message.contains("timeout") -> "Gemini не ответил вовремя. Проверьте интернет и повторите запрос."
                message.contains("connect") -> "Не удалось подключиться к Gemini. Проверьте интернет."
                else -> "Не удалось выполнить запрос Gemini. Повторите попытку."
            }
        }
    }

    private fun peekErrorBody(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()
            if (errorBody != null) {
                val source = errorBody.source()
                source.request(MAX_ERROR_BODY_BYTES)
                val copy = source.buffer.clone()
                copy.readUtf8(minOf(copy.size, MAX_ERROR_BODY_BYTES))
            } else {
                exception.response()?.raw()?.peekBody(MAX_ERROR_BODY_BYTES)?.string().orEmpty()
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseMessage(body: String): String {
        if (body.isBlank()) return ""
        return try {
            json.decodeFromString<GoogleErrorEnvelope>(body).error?.message.orEmpty().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun retryDelaySeconds(exception: HttpException): Long? {
        val headerSeconds = exception.response()?.headers()?.get("Retry-After")?.trim()?.toLongOrNull()
        if (headerSeconds != null) return headerSeconds
        val raw = retryDelayPattern.find(peekErrorBody(exception))?.groupValues?.getOrNull(1)
        return raw?.toDoubleOrNull()?.toLong()
    }

    private fun classifyQuota(normalized: String, rawBody: String): QuotaKind {
        val hasPerMinute = normalized.contains("perminute") ||
            normalized.contains("per_minute") ||
            normalized.contains("per minute")
        return when {
            normalized.contains("inputtokens") && hasPerMinute -> QuotaKind.INPUT_TOKENS_PER_MINUTE
            (normalized.contains("generaterequests") || normalized.contains("requests")) && hasPerMinute ->
                QuotaKind.REQUESTS_PER_MINUTE
            normalized.contains("spend") -> QuotaKind.SPEND
            normalized.contains("billing") || zeroQuotaPattern.containsMatchIn(rawBody) -> QuotaKind.UNAVAILABLE
            normalized.contains("perday") || normalized.contains("per_day") ||
                normalized.contains("per day") || normalized.contains("daily quota") ->
                QuotaKind.DAILY_REQUESTS
            else -> QuotaKind.UNKNOWN
        }
    }

    private fun rateLimitMessage(kind: QuotaKind, retryDelaySeconds: Long?): String {
        val wait = retryDelaySeconds?.takeIf { it in 1..120 }?.let { " Повторите примерно через $it сек." }.orEmpty()
        return when (kind) {
            QuotaKind.INPUT_TOKENS_PER_MINUTE ->
                "Gemini временно ограничил объём текста в запросах за минуту.$wait Приложение уже выполнило безопасные повторы; попробуйте ещё раз немного позже."
            QuotaKind.REQUESTS_PER_MINUTE ->
                "Слишком много запросов к Gemini за короткое время.$wait Приложение уже выполнило безопасные повторы."
            QuotaKind.DAILY_REQUESTS ->
                "Дневная квота Gemini закончилась. Дождитесь её обновления или увеличьте лимит проекта Google AI."
            QuotaKind.SPEND ->
                "Достигнут лимит расходов Gemini для проекта. Подождите или проверьте лимиты оплаты в Google AI."
            QuotaKind.UNAVAILABLE ->
                "Для проекта сейчас нет доступной квоты Gemini. Проверьте квоту и оплату проекта Google AI."
            QuotaKind.UNKNOWN ->
                "Gemini временно ограничил запрос.$wait Приложение уже выполнило безопасные повторы; попробуйте ещё раз немного позже."
        }
    }

    private fun friendlyBadRequest(detail: String): String {
        if (detail.isBlank()) return "Gemini отклонил запрос. Проверьте API-ключ и параметры урока."
        return "Gemini отклонил запрос: ${detail.replace('\n', ' ').take(220)}"
    }
}
