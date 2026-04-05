package com.jsh.pos.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.jsh.pos.application.port.out.AiSummaryException
import com.jsh.pos.application.port.out.AiSummaryPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Gemini API를 호출해 텍스트를 요약하는 어댑터입니다.
 *
 * 활성 조건:
 * - pos.ai.provider=gemini (또는 미설정 시 기본값)
 */
@Component
@ConditionalOnProperty(prefix = "pos.ai", name = ["provider"], havingValue = "gemini", matchIfMissing = true)
class GeminiSummaryAdapter(
    @Value("\${pos.ai.gemini.api-key:}") private val apiKey: String,
    @Value("\${pos.ai.gemini.flash-model:${GeminiModelResolver.DEFAULT_FLASH_MODEL}}") private val flashModel: String,
    @Value("\${pos.ai.gemini.pro-model:${GeminiModelResolver.DEFAULT_PRO_MODEL}}") private val proModel: String,
    @Value("\${pos.ai.gemini.max-output-tokens:600}") private val maxOutputTokens: Int,
) : AiSummaryPort {

    private val restClient = RestClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com")
        .build()

    override fun summarize(text: String, modelTier: String): String {
        if (apiKey.isBlank()) {
            throw AiSummaryException(
                "Gemini API 키가 설정되지 않았습니다. " +
                    "GEMINI_API_KEY 환경변수를 설정하거나 application.yaml의 " +
                    "pos.ai.gemini.api-key 값을 지정해주세요."
            )
        }

        val model = resolveModelName(modelTier)

        val requestBody = GeminiGenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "$SYSTEM_PROMPT\n\n요약 대상 텍스트:\n$text")
                    )
                )
            ),
            generationConfig = GenerationConfig(
                maxOutputTokens = maxOutputTokens,
                temperature = 0.2,
            )
        )

        val response = try {
            restClient.post()
                .uri("/v1beta/models/$model:generateContent?key=$apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiGenerateContentResponse::class.java)
        } catch (e: HttpClientErrorException.NotFound) {
            throw AiSummaryException(
                "Gemini 모델 '$model' 을(를) generateContent로 찾을 수 없습니다. " +
                    "현재 키에서 지원되는 모델로 바꿔주세요. 예: " +
                    "${GeminiModelResolver.DEFAULT_FLASH_MODEL}, ${GeminiModelResolver.DEFAULT_PRO_MODEL}. " +
                    "필요하면 .env 의 GEMINI_FLASH_MODEL / GEMINI_PRO_MODEL 값을 수정한 뒤 서버를 다시 시작하세요.",
                e,
            )
        } catch (e: RestClientException) {
            throw AiSummaryException("Gemini API 호출 중 오류가 발생했습니다: ${e.message}", e)
        }

        return response?.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw AiSummaryException("Gemini 응답에서 요약 결과를 찾을 수 없습니다.")
    }

    private fun resolveModelName(modelTier: String): String = GeminiModelResolver.resolve(
        modelTier = modelTier,
        flashModel = flashModel,
        proModel = proModel,
    )

    companion object {
        private val SYSTEM_PROMPT = """
            당신은 텍스트를 간결하게 요약하는 AI 어시스턴트입니다.
            다음 규칙을 반드시 따르세요:
            1. 항상 한국어로 답변합니다.
            2. 핵심 내용을 3줄 이내로 요약합니다.
            3. 각 줄은 "- " 으로 시작합니다.
            4. 서론이나 부연 설명 없이 요약 내용만 제공합니다.
        """.trimIndent()
    }
}

internal object GeminiModelResolver {
    internal const val DEFAULT_FLASH_MODEL = "gemini-3-flash-preview"
    internal const val DEFAULT_PRO_MODEL = "gemini-3.1-pro-preview"

    fun resolve(modelTier: String, flashModel: String, proModel: String): String {
        val configuredModel = when (modelTier.trim().lowercase()) {
            "pro" -> proModel
            "flash" -> flashModel
            else -> flashModel
        }

        return normalize(configuredModel).ifBlank {
            if (modelTier.trim().lowercase() == "pro") DEFAULT_PRO_MODEL else DEFAULT_FLASH_MODEL
        }
    }

    private fun normalize(modelName: String): String = modelName
        .trim()
        .removePrefix("models/")
}

private data class GeminiGenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig,
)

private data class Content(
    val parts: List<Part>,
)

private data class Part(
    val text: String,
)

private data class GenerationConfig(
    @com.fasterxml.jackson.annotation.JsonProperty("maxOutputTokens")
    val maxOutputTokens: Int,
    val temperature: Double,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiGenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Candidate(
    val content: CandidateContent? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class CandidateContent(
    val parts: List<CandidatePart> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class CandidatePart(
    val text: String? = null,
)

