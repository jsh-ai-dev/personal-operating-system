package com.jsh.pos.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.jsh.pos.application.port.out.AiSummaryException
import com.jsh.pos.application.port.out.AiSummaryPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * OpenAI Chat Completions API를 호출해 텍스트를 요약하는 어댑터입니다.
 *
 * 역할 (어댑터 패턴):
 * - AiSummaryPort(인터페이스)를 구현해 외부 AI 서비스 연동
 * - 애플리케이션 계층은 OpenAI가 무엇인지 몰라도 됨
 *
 * 설정:
 * - OPENAI_API_KEY 환경변수 또는 application.yaml의 pos.ai.openai.api-key 값을 사용
 * - 모델: gpt-4o-mini (빠르고 저렴, 요약 품질 충분)
 *
 * 요약 형식:
 * - 항상 한국어 3줄 이내 bullet 요약으로 고정
 */
@Component
@ConditionalOnProperty(prefix = "pos.ai", name = ["provider"], havingValue = "openai")
class OpenAiSummaryAdapter(
    @Value("\${pos.ai.openai.api-key:}") private val apiKey: String,
    @Value("\${pos.ai.openai.model:gpt-4o-mini}") private val model: String,
    @Value("\${pos.ai.openai.max-tokens:600}") private val maxTokens: Int,
) : AiSummaryPort {

    // RestClient: Spring Boot 3.2+에 내장된 동기식 HTTP 클라이언트 (별도 의존성 불필요)
    private val restClient = RestClient.builder()
        .baseUrl("https://api.openai.com")
        .build()

    override fun summarize(text: String, modelTier: String): String {
        // 1. API 키 확인: 설정이 없으면 바로 실패 (서버 기동 시점이 아닌 최초 호출 시 검증)
        if (apiKey.isBlank()) {
            throw AiSummaryException(
                "OpenAI API 키가 설정되지 않았습니다. " +
                    "OPENAI_API_KEY 환경변수를 설정하거나 application.yaml의 " +
                    "pos.ai.openai.api-key 값을 지정해주세요."
            )
        }

        // 2. 요청 바디 구성
        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(role = "user", content = text),
            ),
            maxTokens = maxTokens,
        )

        // 3. OpenAI API 호출
        val response = try {
            restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(ChatResponse::class.java)
        } catch (e: RestClientException) {
            throw AiSummaryException("OpenAI API 호출 중 오류가 발생했습니다: ${e.message}", e)
        }

        // 4. 응답 파싱
        return response?.choices?.firstOrNull()?.message?.content
            ?: throw AiSummaryException("OpenAI 응답에서 요약 결과를 찾을 수 없습니다.")
    }

    companion object {
        private val SYSTEM_PROMPT = """
            당신은 텍스트를 간결하게 요약하는 AI 어시스턴트입니다.
            다음 규칙을 반드시 따르세요:
            1. 항상 한국어로 답변합니다.
            2. 핵심 내용을 3줄 이내로 요약합니다.
            3. 각 줄은 "• " 으로 시작합니다.
            4. 서론이나 부연 설명 없이 요약 내용만 제공합니다.
        """.trimIndent()
    }
}

// ── 요청/응답 DTO (이 파일 내부에서만 사용하는 Jackson 직렬화 모델) ──

private data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    // JSON 키는 snake_case로 변환 필요 → @JsonProperty 사용
    @com.fasterxml.jackson.annotation.JsonProperty("max_tokens")
    val maxTokens: Int,
)

private data class Message(
    val role: String,
    val content: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ChatResponse(
    val choices: List<Choice>,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Choice(val message: Message)
}

