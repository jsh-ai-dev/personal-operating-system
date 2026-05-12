package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note
import java.math.BigDecimal

/**
 * 노트에 AI 요약문을 저장하는 유스케이스입니다.
 */
interface SaveNoteSummaryUseCase {
    fun save(command: Command): Note?

    data class Command(
        val id: String,
        val summary: String,
        val modelTier: String? = null,
        val inputTokens: Int? = null,
        val outputTokens: Int? = null,
        val estimatedCostUsd: BigDecimal? = null,
    )
}

