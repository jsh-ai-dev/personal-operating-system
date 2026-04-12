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
 * API Bearer 토큰 인증 필터입니다.
 *
 * 주의:
 * - JWT 사용 시 principal은 Nest와 동일하게 sub(UUID)를 사용합니다.
 * - 기존 폼 로그인 principal(사용자명)과 혼용되므로 ownerUsername 마이그레이션 시 값 체계를 통일해야 합니다.
 */
class JwtAuthenticationFilter(
    private val jwtSecret: String,
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
        val authorization = request.getHeader("Authorization") ?: return null
        if (!authorization.startsWith("Bearer ")) {
            return null
        }
        return authorization.substringAfter("Bearer ").trim().ifBlank { null }
    }

    private fun shouldAuthenticateWithJwt(): Boolean {
        val existing = SecurityContextHolder.getContext().authentication
        return existing == null || !existing.isAuthenticated || existing is AnonymousAuthenticationToken
    }
}

