package com.jsh.pos.application.port.`in`

/**
 * 텍스트 요약 유스케이스입니다.
 *
 * 역할:
 * - 업로드된 파일의 텍스트 내용을 받아 AI 요약 결과를 반환
 * - 어댑터(컨트롤러)는 이 인터페이스에만 의존 → AI 구현체 교체 가능
 */
interface SummarizeUseCase {

    fun summarize(command: Command): Result

    /**
     * 요약 요청 커맨드
     *
     * @param text      파일에서 추출한 원본 텍스트
     * @param fileName  업로드된 파일명 (결과 화면 표시용)
     * @param modelTier 요약 모델 선택값
     */
    data class Command(
        val text: String,
        val fileName: String,
        val modelTier: String,
    )

    /**
     * 요약 결과
     *
     * @param summary        AI가 생성한 요약문
     * @param fileName       원본 파일명
     * @param originalLength 원본 텍스트 길이 (참고용)
     * @param modelTier      실제 적용된 모델 티어
     */
    data class Result(
        val summary: String,
        val fileName: String,
        val originalLength: Int,
        val modelTier: String,
        val inputTokens: Int? = null,
        val outputTokens: Int? = null,
        val estimatedCostUsd: java.math.BigDecimal? = null,
    )
}

