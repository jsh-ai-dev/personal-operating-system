package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.infrastructure.config.AuthServiceProperties
import com.jsh.pos.infrastructure.security.Mk1AuthCookieNames
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.client.RestClient

@Controller
class AuthRemoteLogoutController(
    private val props: AuthServiceProperties,
) {

    private val client: RestClient = RestClient.builder()
        .baseUrl(props.baseUrl.trimEnd('/'))
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(2_000)
                setReadTimeout(2_000)
            },
        )
        .build()

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): String {
        val token = request.cookies
            ?.firstOrNull { it.name == Mk1AuthCookieNames.ACCESS_TOKEN }
            ?.value
        if (!token.isNullOrBlank()) {
            try {
                client.post()
                    .uri("/api/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .toBodilessEntity()
            } catch (_: Exception) {
                // 쿠키는 항상 삭제
            }
        }
        val expired = Cookie(Mk1AuthCookieNames.ACCESS_TOKEN, "")
        expired.maxAge = 0
        expired.path = "/"
        response.addCookie(expired)
        return "redirect:/login?logout"
    }
}
