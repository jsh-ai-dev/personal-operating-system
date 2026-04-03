package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note

/**
 * 북마크된 노트 목록 조회 유스케이스 계약입니다.
 *
 * 왜 BookmarkNoteUseCase와 분리했나?
 * - 북마크 ON/OFF (Command)와 목록 조회 (Query)는 책임이 다릅니다.
 * - CQRS(Command Query Responsibility Segregation) 원칙:
 *   "상태를 변경하는 커맨드"와 "데이터를 조회하는 쿼리"를 분리
 * - 읽기 전용 기능은 나중에 캐시, 읽기 전용 DB 복제 등으로 최적화하기 쉬워짐
 */
interface GetBookmarkedNotesUseCase {

    /**
     * 북마크된 노트 목록을 최신 수정순으로 반환합니다.
     *
     * @return 북마크된 노트 목록 (없으면 빈 리스트)
     */
    fun getBookmarked(): List<Note>
}

