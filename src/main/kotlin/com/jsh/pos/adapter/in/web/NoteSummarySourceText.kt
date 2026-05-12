package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.domain.note.Note
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

/**
 * 노트 AI 요약의 입력 텍스트를 만듭니다. (MVC NotePageController와 동일 규칙)
 */
object NoteSummarySourceText {
    private const val DEFAULT_MODEL_TIER = "gpt-5-nano"
    private val ALLOWED_MODEL_TIERS = setOf("gpt-5-nano", "gpt-5-mini")

    fun normalizeModelTier(modelTier: String): String {
        val normalized = modelTier.trim().lowercase()
        return if (normalized in ALLOWED_MODEL_TIERS) normalized else DEFAULT_MODEL_TIER
    }

    fun extract(note: Note): String {
        if (!note.hasStoredFile) {
            return note.content
        }

        val bytes = note.fileBytes ?: throw IllegalArgumentException("파일 본문을 읽을 수 없습니다. 다시 업로드해주세요.")
        val contentType = note.fileContentType?.lowercase().orEmpty()

        return when {
            contentType == "application/pdf" || note.originalFileName?.lowercase()?.endsWith(".pdf") == true -> {
                ByteArrayInputStream(bytes).use { input ->
                    PDDocument.load(input).use { document ->
                        PDFTextStripper().getText(document)
                    }
                }.trim().ifBlank {
                    throw IllegalArgumentException("PDF에서 읽을 수 있는 텍스트가 없습니다.")
                }
            }

            else -> throw IllegalArgumentException("현재 AI 요약은 텍스트와 PDF 파일만 지원합니다.")
        }
    }
}
