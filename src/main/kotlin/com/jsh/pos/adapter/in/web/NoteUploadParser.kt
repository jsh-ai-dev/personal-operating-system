package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.domain.note.Visibility
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import kotlin.text.Charsets

/**
 * .txt / .pdf 업로드 → [CreateNoteUseCase.Command] 변환 (MVC·REST 공통).
 *
 * - txt: 내용을 UTF-8로 읽어 본문으로 저장 (편집 가능한 일반 노트).
 * - pdf: 바이트·MIME 저장, 본문은 안내 문구(다운로드로 원본 확인).
 */
object NoteUploadParser {

    private enum class FileType {
        TEXT,
        PDF,
        UNSUPPORTED,
    }

    fun buildCommand(file: MultipartFile, ownerUsername: String): CreateNoteUseCase.Command {
        if (file.isEmpty) {
            throw IllegalArgumentException("파일을 선택해주세요.")
        }

        val originalName = file.originalFilename ?: ""
        val fileType = detectFileType(originalName)
        if (fileType == FileType.UNSUPPORTED) {
            throw IllegalArgumentException("현재 .txt 또는 .pdf 파일만 업로드할 수 있습니다.")
        }

        val contentType = file.contentType ?: ""
        if (!isAllowedMime(contentType, fileType)) {
            throw IllegalArgumentException("파일 형식이 올바르지 않습니다.")
        }

        return when (fileType) {
            FileType.TEXT -> {
                val title = originalName.substringBeforeLast(".").trim().ifBlank { originalName }
                val text = file.bytes.toString(Charsets.UTF_8)
                if (text.isBlank()) {
                    throw IllegalArgumentException("파일에 내용이 없습니다.")
                }
                CreateNoteUseCase.Command(
                    ownerUsername = ownerUsername,
                    title = title,
                    content = text,
                    visibility = Visibility.PRIVATE,
                    tags = emptySet(),
                    originalFileName = originalName,
                )
            }

            FileType.PDF -> {
                val title = originalName.trim()
                val fileBytes = file.bytes
                if (fileBytes.isEmpty()) {
                    throw IllegalArgumentException("파일에 내용이 없습니다.")
                }
                CreateNoteUseCase.Command(
                    ownerUsername = ownerUsername,
                    title = title,
                    content = "",
                    visibility = Visibility.PRIVATE,
                    tags = emptySet(),
                    originalFileName = originalName,
                    fileContentType = normalizeContentType(contentType, fileType),
                    fileBytes = fileBytes,
                )
            }

            FileType.UNSUPPORTED -> throw IllegalArgumentException("현재 .txt 또는 .pdf 파일만 업로드할 수 있습니다.")
        }
    }

    private fun detectFileType(fileName: String): FileType {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".txt") -> FileType.TEXT
            lowerName.endsWith(".pdf") -> FileType.PDF
            else -> FileType.UNSUPPORTED
        }
    }

    private fun isAllowedMime(contentType: String, fileType: FileType): Boolean {
        val normalized = contentType.lowercase()
        return when (fileType) {
            FileType.TEXT -> normalized.startsWith("text/") || normalized == "application/octet-stream"
            FileType.PDF -> normalized == "application/pdf" || normalized == "application/octet-stream"
            FileType.UNSUPPORTED -> false
        }
    }

    private fun normalizeContentType(contentType: String, fileType: FileType): String = when (fileType) {
        FileType.TEXT -> if (contentType.isBlank()) "text/plain" else contentType
        FileType.PDF -> if (contentType.isBlank() || contentType == "application/octet-stream") {
            "application/pdf"
        } else {
            contentType
        }

        FileType.UNSUPPORTED -> MediaType.APPLICATION_OCTET_STREAM_VALUE
    }
}
