package com.jsh.pos.application.port.out

/**
 * AI 요약 외부 연동 포트입니다.
 *
 * 역할:
 * - 실제 AI 서비스(OpenAI 등)를 호출하는 어댑터가 구현하는 인터페이스
 * - 애플리케이션 서비스는 이 인터페이스에만 의존 → OpenAI를 다른 AI로 교체해도 서비스 코드 변경 없음
 *
 * 설계 포인트:
 * - "텍스트를 넣으면 요약이 나온다"는 계약만 정의
 * - HTTP 통신, API 키, 모델 선택 등의 세부사항은 어댑터에서 처리
 */
interface AiSummaryPort {

    /**
     * 주어진 텍스트를 AI로 요약합니다.
     *
     * @param text 요약할 원본 텍스트
     * @return AI가 생성한 요약문
     * @throws AiSummaryException API 호출 실패 또는 응답 오류 시
     */
    fun summarize(text: String): String
}

/**
 * AI 요약 호출 관련 예외입니다.
 */
class AiSummaryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

