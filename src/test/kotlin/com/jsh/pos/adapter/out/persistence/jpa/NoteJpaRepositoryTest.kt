package com.jsh.pos.adapter.out.persistence.jpa

import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Instant

@DataJpaTest
class NoteJpaRepositoryTest {

    @Autowired
    private lateinit var noteJpaRepository: NoteJpaRepository

    @Test
    fun `save and load stored pdf file note`() {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        noteJpaRepository.save(
            NoteJpaEntity(
                id = "note-file-1",
                ownerUsername = "pos-admin",
                title = "architecture",
                content = "업로드된 PDF 파일 'architecture.pdf' 입니다. 상세 화면에서 다운로드해 확인하세요.",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                bookmarked = false,
                originalFileName = "architecture.pdf",
                fileContentType = "application/pdf",
                hasStoredFile = true,
                fileBytes = pdfBytes,
                createdAt = Instant.parse("2026-04-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
            ),
        )

        val found = noteJpaRepository.findById("note-file-1").orElseThrow()

        assertEquals("architecture.pdf", found.originalFileName)
        assertEquals("application/pdf", found.fileContentType)
        assertTrue(found.hasStoredFile)
        assertTrue(found.fileBytes?.contentEquals(pdfBytes) == true)
    }

    @Test
    fun `save and load regular text note without stored file`() {
        noteJpaRepository.save(
            NoteJpaEntity(
                id = "note-text-1",
                ownerUsername = "pos-admin",
                title = "plain note",
                content = "본문",
                visibility = Visibility.PRIVATE,
                tags = setOf("kotlin"),
                bookmarked = false,
                originalFileName = null,
                fileContentType = null,
                hasStoredFile = false,
                fileBytes = null,
                createdAt = Instant.parse("2026-04-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
            ),
        )

        val found = noteJpaRepository.findById("note-text-1").orElseThrow()

        assertNull(found.originalFileName)
        assertNull(found.fileContentType)
        assertEquals(false, found.hasStoredFile)
        assertNull(found.fileBytes)
    }
}

