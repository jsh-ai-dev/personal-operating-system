package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.domain.note.Note
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import com.jsh.pos.domain.note.Visibility
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Controller
@RequestMapping("/notes")
class NotePageController(
    private val createNoteUseCase: CreateNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val bookmarkNoteUseCase: BookmarkNoteUseCase,
    private val getBookmarkedNotesUseCase: GetBookmarkedNotesUseCase,
    private val getAllNotesUseCase: GetAllNotesUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "false") bookmarkedOnly: Boolean,
        @RequestParam(defaultValue = "recent") sort: String = "recent",
        model: Model,
        authentication: Authentication? = null,
    ): String {
        val currentUsername = currentUsername(authentication)
        val normalizedKeyword = keyword?.trim().orEmpty()
        val notes = when {
            normalizedKeyword.isNotBlank() -> searchNotesUseCase.search(SearchNotesUseCase.Command(normalizedKeyword))
            bookmarkedOnly -> getBookmarkedNotesUseCase.getBookmarked()
            else -> getAllNotesUseCase.getAll()
        }
        val ownedNotes = notes.filter { it.ownerUsername == currentUsername }
        val normalizedSort = normalizeSort(sort)
        val sortedNotes = sortNotes(ownedNotes, normalizedSort)

        model.addAttribute("notes", sortedNotes)
        model.addAttribute("keyword", normalizedKeyword)
        model.addAttribute("bookmarkedOnly", bookmarkedOnly)
        model.addAttribute("sort", normalizedSort)
        model.addAttribute("tagsDisplayById", sortedNotes.associate { it.id to formatTags(it.tags) })
        model.addAttribute("createdAtDisplayById", sortedNotes.associate { it.id to formatDateTime(it.createdAt) })
        model.addAttribute("updatedAtDisplayById", sortedNotes.associate { it.id to formatDateTime(it.updatedAt) })
        return "notes/list"
    }

    @GetMapping("/new")
    fun newForm(model: Model): String {
        model.addAttribute("form", NoteForm())
        model.addAttribute("mode", "create")
        return "notes/form"
    }

    @PostMapping
    fun create(
        @Valid @ModelAttribute("form") form: NoteForm,
        bindingResult: BindingResult,
        model: Model,
        authentication: Authentication? = null,
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create")
            return "notes/form"
        }

        val created = createNoteUseCase.create(
            CreateNoteUseCase.Command(
                ownerUsername = currentUsername(authentication),
                title = form.title,
                content = form.content,
                visibility = form.visibility,
                tags = parseTags(form.tagsText),
            ),
        )

        return "redirect:/notes/${created.id}"
    }

    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        model.addAttribute("note", note)
        model.addAttribute("tagsDisplay", formatTags(note.tags))
        return "notes/detail"
    }

    @GetMapping("/{id}/edit")
    fun editForm(
        @PathVariable id: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        model.addAttribute(
            "form",
            NoteForm(
                title = note.title,
                content = note.content,
                visibility = note.visibility,
                tagsText = note.tags.joinToString(", "),
            ),
        )
        model.addAttribute("mode", "edit")
        model.addAttribute("noteId", id)
        return "notes/form"
    }

    @PostMapping("/{id}/edit")
    fun edit(
        @PathVariable id: String,
        @Valid @ModelAttribute("form") form: NoteForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val existing = getNoteUseCase.getById(id)
        if (existing == null || !isOwnedByCurrentUser(existing, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit")
            model.addAttribute("noteId", id)
            return "notes/form"
        }

        val updated = updateNoteUseCase.updateById(
            id,
            UpdateNoteUseCase.Command(
                title = form.title,
                content = form.content,
                visibility = form.visibility,
                tags = parseTags(form.tagsText),
            ),
        )

        if (updated == null) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        return "redirect:/notes/${updated.id}"
    }

    @PostMapping("/{id}/delete")
    fun delete(
        @PathVariable id: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "삭제할 노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        val deleted = deleteNoteUseCase.deleteById(id)
        if (!deleted) {
            redirectAttributes.addFlashAttribute("message", "삭제할 노트를 찾을 수 없습니다.")
        }
        return "redirect:/notes"
    }

    @PostMapping("/{id}/bookmark")
    fun bookmark(
        @PathVariable id: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "북마크할 노트를 찾을 수 없습니다.")
            return "redirect:/notes/$id"
        }

        bookmarkNoteUseCase.bookmark(id)
            ?: redirectAttributes.addFlashAttribute("message", "북마크할 노트를 찾을 수 없습니다.")
        return "redirect:/notes/$id"
    }

    @PostMapping("/{id}/unbookmark")
    fun unbookmark(
        @PathVariable id: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes/$id"
        }

        bookmarkNoteUseCase.unbookmark(id)
            ?: redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
        return "redirect:/notes/$id"
    }

    private fun isOwnedByCurrentUser(note: Note, authentication: Authentication?): Boolean =
        note.ownerUsername == currentUsername(authentication)

    private fun currentUsername(authentication: Authentication?): String {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
        val isAuthenticated = auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken
        return if (isAuthenticated) auth.name else "anonymousUser"
    }

    private fun parseTags(tagsText: String): Set<String> =
        tagsText.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun formatTags(tags: Set<String>): String =
        tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sortedBy { it.lowercase() }
            .joinToString(", ")
            .ifBlank { "-" }

    private fun normalizeSort(sort: String): String =
        if (sort.equals("title", ignoreCase = true)) "title" else "recent"

    private fun sortNotes(notes: List<Note>, sort: String): List<Note> =
        when (sort) {
            "title" -> notes.sortedBy { it.title.lowercase(Locale.getDefault()) }
            else -> notes.sortedWith(compareByDescending<Note> { it.updatedAt }.thenByDescending { it.createdAt })
        }

    private fun formatDateTime(value: Instant): String = DISPLAY_DATE_FORMATTER.format(value)

    companion object {
        private val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withLocale(Locale.KOREA)
                .withZone(ZoneId.systemDefault())
    }
}

data class NoteForm(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String = "",
    @field:NotBlank(message = "본문은 필수입니다")
    val content: String = "",
    val visibility: Visibility = Visibility.PRIVATE,
    val tagsText: String = "",
)





