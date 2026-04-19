package com.jsh.pos.infrastructure.security

object Mk1AuthCookieNames {
    /** mk2 auth-service에서 받은 JWT를 Thymeleaf 요청에 실어 나르는 httpOnly 쿠키 */
    const val ACCESS_TOKEN: String = "mk1_pos_jwt"
}
