package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note

/**
 * 노트 북마크 유스케이스 계약입니다.
 *
 * 북마크란?
 * - 자주 보고 싶은 노트에 "즐겨찾기" 표시를 하는 기능입니다.
 * - 노트 내용 자체를 수정하지 않으므로 UpdateNoteUseCase와 분리합니다.
 *
 * 왜 별도 유스케이스로 분리했나?
 * - 단일 책임 원칙(SRP): "북마크 ON/OFF"는 "내용 수정"과 다른 비즈니스 의도
 * - 나중에 북마크 상태 변경 시 알림이나 정렬 기준을 바꿀 때 여기만 수정하면 됨
 */
interface BookmarkNoteUseCase {

    /**
     * 노트에 북마크를 등록합니다.
     *
     * @param id 북마크할 노트의 ID
     * @return 북마크가 적용된 노트 (노트가 없으면 null)
     */
    fun bookmark(id: String): Note?

    /**
     * 노트의 북마크를 해제합니다.
     *
     * @param id 북마크를 해제할 노트의 ID
     * @return 북마크가 해제된 노트 (노트가 없으면 null)
     */
    fun unbookmark(id: String): Note?
}

