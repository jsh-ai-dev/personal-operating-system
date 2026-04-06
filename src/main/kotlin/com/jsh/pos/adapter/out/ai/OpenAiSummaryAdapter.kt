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
 * - 개발자 학습/지식정리 목적의 구조화 Markdown 요약
 */
@Component
@ConditionalOnProperty(prefix = "pos.ai", name = ["provider"], havingValue = "openai")
class OpenAiSummaryAdapter(
    @Value("\${pos.ai.openai.api-key:}") private val apiKey: String,
    @Value("\${pos.ai.openai.model:gpt-4o-mini}") private val model: String,
    @Value("\${pos.ai.openai.max-tokens:2048}") private val maxTokens: Int,
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
            당신은 개발자의 학습/지식정리를 돕는 기술 요약 어시스턴트입니다.
            출력은 반드시 한국어 일반 텍스트로 작성하고, 아래 형식을 정확히 지키세요.

            주제 한줄:
            - 이 문서가 어떤 기술/개념에 대한 내용인지 한 줄로 설명

            왜 필요한가:
            - 이 기술을 왜 써야 하는지 2~4개 bullet
            - 기존 방식의 단점과 이 기술이 보완하는 지점을 함께 설명

            한눈에 요약:
            - 핵심만 4~6개 bullet

            핵심 개념 정리:
            - 개념명: 설명 형식으로 3~7개 bullet

            실무 적용 포인트:
            - 바로 적용 가능한 체크리스트 3~5개 bullet

            기술면접 예상 질문과 답변:
            - 질문: 형식으로 2~3개
            - 답변: 각 질문에 대해 2~4문장으로 핵심만 설명

            규칙:
            1. 강조 마크다운(**, `, _, ~)과 코드 펜스는 사용하지 않습니다.
            2. 목록은 모두 '- '로 시작합니다.
            3. 원문에 없는 내용을 단정하지 말고, 추론이 필요하면 '(추론)' 표시를 붙입니다.
            4. 장황한 서론/결론은 금지하고 본문만 출력합니다.
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

