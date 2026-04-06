package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.SummarizeUseCase
import com.jsh.pos.application.port.out.AiSummaryException
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

/**
 * AI 요약 화면 컨트롤러입니다.
 *
 * 역할:
 * - GET  /summary        → 파일 업로드 화면 표시
 * - POST /summary/upload → 파일을 받아 요약 처리 후 결과 화면 표시
 *
 * 보안/검증 정책 (개발 단계):
 * - 허용 확장자: .txt, .pdf
 * - 허용 MIME: text 계열, application/pdf
 * - 최대 크기:  application.yaml의 spring.servlet.multipart 설정으로 통제
 *
 * 처리 방식: 동기 (파일이 작으므로 충분)
 */
@Controller
@RequestMapping("/summary")
class SummaryPageController(
    private val summarizeUseCase: SummarizeUseCase,
) {

    /** 파일 업로드 폼 화면을 반환합니다. */
    @GetMapping
    fun uploadForm(model: Model): String {
        model.addAttribute("selectedModelTier", DEFAULT_MODEL_TIER)
        return "summary/upload"
    }

    /**
     * 업로드된 파일을 텍스트로 읽어 AI 요약 결과를 화면에 표시합니다.
     *
     * @param file  업로드된 파일 (multipart/form-data)
     * @param model Thymeleaf 모델
     * @return      결과가 담긴 업로드 화면 (같은 템플릿, 결과 섹션 표시)
     */
    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(name = "modelTier", defaultValue = DEFAULT_MODEL_TIER) modelTier: String,
        model: Model,
    ): String {
        val normalizedModelTier = normalizeModelTier(modelTier)
        model.addAttribute("selectedModelTier", normalizedModelTier)

        // 1. 파일 비어있는지 확인
        if (file.isEmpty) {
            model.addAttribute("error", "파일을 선택해주세요.")
            return "summary/upload"
        }

        // 2. 확장자 검사: .txt/.pdf 만 허용
        val originalName = file.originalFilename ?: ""
        val fileType = detectFileType(originalName)
        if (fileType == FileType.UNSUPPORTED) {
            model.addAttribute("error", "현재 .txt 또는 .pdf 파일만 지원합니다.")
            return "summary/upload"
        }

        // 3. MIME 타입 검사
        //    확장자 위조(virus.exe -> virus.txt) 방어
        val contentType = file.contentType ?: ""
        if (!isAllowedMime(contentType, fileType)) {
            model.addAttribute("error", "파일 형식이 올바르지 않습니다. .txt 또는 .pdf 파일만 업로드할 수 있습니다.")
            return "summary/upload"
        }

        // 4. 파일 타입별 텍스트 추출
        val text = try {
            when (fileType) {
                FileType.TEXT -> file.bytes.toString(Charsets.UTF_8)
                FileType.PDF -> extractPdfText(file)
                FileType.UNSUPPORTED -> ""
            }
        } catch (e: Exception) {
            model.addAttribute("error", "파일을 읽는 중 오류가 발생했습니다: ${e.message}")
            return "summary/upload"
        }

        if (text.isBlank()) {
            model.addAttribute("error", "파일에서 읽을 수 있는 텍스트가 없습니다.")
            return "summary/upload"
        }

        // 5. AI 요약 호출
        val result = try {
            summarizeUseCase.summarize(
                SummarizeUseCase.Command(
                    text = text,
                    fileName = originalName,
                    modelTier = normalizedModelTier,
                )
            )
        } catch (e: AiSummaryException) {
            // API 키 미설정, 네트워크 오류 등
            model.addAttribute("error", e.message)
            return "summary/upload"
        } catch (e: IllegalArgumentException) {
            // 빈 텍스트 등 입력값 오류
            model.addAttribute("error", e.message)
            return "summary/upload"
        }

        // 6. 결과를 모델에 담아 같은 템플릿에서 결과 섹션 렌더링
        model.addAttribute("result", result)
        return "summary/upload"
    }

    private fun normalizeModelTier(modelTier: String): String {
        val normalized = modelTier.trim().lowercase()
        return if (normalized in ALLOWED_MODEL_TIERS) normalized else DEFAULT_MODEL_TIER
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

    private fun extractPdfText(file: MultipartFile): String = file.inputStream.use { input ->
        PDDocument.load(input).use { document ->
            PDFTextStripper().getText(document)
        }
    }

    companion object {
        private const val DEFAULT_MODEL_TIER = "flash"
        private val ALLOWED_MODEL_TIERS = setOf("flash", "pro")
    }

    private enum class FileType {
        TEXT,
        PDF,
        UNSUPPORTED,
    }
}

