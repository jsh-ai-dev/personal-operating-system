package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.infrastructure.security.AuthRemoteLoginService
import com.jsh.pos.infrastructure.security.Mk1AuthCookieNames
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class AuthRemoteLoginController(
    private val authRemoteLogin: AuthRemoteLoginService,
    @param:Value("\${pos.auth.token-cookie-max-age-sec:604800}")
    private val tokenCookieMaxAgeSec: Long,
) {

    @PostMapping("/login", consumes = [org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun login(
        @RequestParam("email") email: String,
        @RequestParam("password") password: String,
        response: HttpServletResponse,
    ): String {
        val token = authRemoteLogin.login(email, password)
        if (token.isNullOrBlank()) {
            return "redirect:/login?error"
        }
        val cookie = ResponseCookie.from(Mk1AuthCookieNames.ACCESS_TOKEN, token)
            .httpOnly(true)
            .path("/")
            .maxAge(tokenCookieMaxAgeSec)
            .sameSite("Lax")
            .secure(false)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return "redirect:/notes"
    }
}
