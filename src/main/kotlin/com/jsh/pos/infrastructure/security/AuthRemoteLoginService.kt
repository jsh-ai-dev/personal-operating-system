package com.jsh.pos.infrastructure.security

/**
 * 중앙 auth-service에 로그인을 위임하고 액세스 토큰 문자열을 반환합니다.
 * 실패 시 null.
 */
fun interface AuthRemoteLoginService {
    fun login(email: String, password: String): String?
}
