package com.jsh.pos.infrastructure.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import javax.crypto.spec.SecretKeySpec

/**
 * Bearer 헤더 또는 mk1 전용 httpOnly JWT 쿠키로 인증합니다.
 * principal은 auth-service와 동일하게 `sub`(사용자 UUID 문자열)입니다.
 */
class JwtAuthenticationFilter(
    private val jwtSecret: String,
    private val accessTokenCookieName: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val bearerToken = resolveBearerToken(request)

        if (bearerToken != null && jwtSecret.isNotBlank() && shouldAuthenticateWithJwt()) {
            try {
                val key = SecretKeySpec(jwtSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
                val claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(bearerToken)
                    .body

                val subject = claims.subject?.trim()
                if (!subject.isNullOrBlank()) {
                    val authentication = UsernamePasswordAuthenticationToken.authenticated(subject, null, emptyList())
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (_: JwtException) {
                SecurityContextHolder.clearContext()
            } catch (_: IllegalArgumentException) {
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader("Authorization")
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substringAfter("Bearer ").trim().ifBlank { null }
        }
        val cookieToken = request.cookies
            ?.firstOrNull { it.name == accessTokenCookieName }
            ?.value
            ?.trim()
            ?.ifBlank { null }
        return cookieToken
    }

    private fun shouldAuthenticateWithJwt(): Boolean {
        val existing = SecurityContextHolder.getContext().authentication
        return existing == null || !existing.isAuthenticated || existing is AnonymousAuthenticationToken
    }
}

