package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.SummarizeUseCase
import com.jsh.pos.application.port.out.AiSummaryPort
import org.springframework.stereotype.Service

/**
 * 텍스트 요약 유스케이스 구현체입니다.
 *
 * 역할:
 * - 입력 텍스트를 검증하고 AiSummaryPort(외부 AI)로 위임
 * - 비즈니스 규칙(빈 텍스트 거부, 너무 큰 텍스트 제한 등)을 여기서 정의
 *
 * Clean Architecture 관점:
 * - AiSummaryPort(인터페이스)에만 의존 → OpenAI 교체 시 이 파일은 변경 불필요
 */
@Service
class SummarizeService(
    // port.out 주입: 실제 AI 호출은 어댑터가 처리 (Gemini/OpenAI 등)
    private val aiSummaryPort: AiSummaryPort,
) : SummarizeUseCase {

    override fun summarize(command: SummarizeUseCase.Command): SummarizeUseCase.Result {
        // 1. 입력 검증: 빈 텍스트 거부
        require(command.text.isNotBlank()) { "요약할 내용이 없습니다." }

        // 2. 텍스트 크기 제한: 너무 크면 앞부분만 사용 (토큰 비용 통제)
        //    약 12만자 ≈ GPT-4o-mini의 128K context 이하 (한글 기준 안전 마진 포함)
        val trimmedText = if (command.text.length > MAX_CHARS) {
            command.text.take(MAX_CHARS) + "\n\n[이후 내용은 길이 제한으로 생략됨]"
        } else {
            command.text
        }

        val normalizedModelTier = normalizeModelTier(command.modelTier)

        // 3. AI 요약 호출 (구현체는 설정된 provider에 따라 결정)
        val summary = aiSummaryPort.summarize(trimmedText, normalizedModelTier)

        return SummarizeUseCase.Result(
            summary = summary,
            fileName = command.fileName,
            originalLength = command.text.length,
            modelTier = normalizedModelTier,
        )
    }

    private fun normalizeModelTier(modelTier: String): String {
        val normalized = modelTier.trim().lowercase()
        return if (normalized in ALLOWED_MODEL_TIERS) normalized else DEFAULT_MODEL_TIER
    }

    companion object {
        // 파일 크기 제한과 별개로, 텍스트 길이도 제한 (토큰 비용 방어)
        private const val MAX_CHARS = 12_000
        private const val DEFAULT_MODEL_TIER = "flash"
        private val ALLOWED_MODEL_TIERS = setOf("flash", "pro")
    }
}

