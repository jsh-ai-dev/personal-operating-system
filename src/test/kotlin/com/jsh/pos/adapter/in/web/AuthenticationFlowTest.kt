package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.infrastructure.security.AuthRemoteLoginService
import com.jsh.pos.infrastructure.security.Mk1AuthCookieNames
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.hamcrest.Matchers.endsWith
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@SpringBootTest(
    properties = [
        "pos.auth.jwt.secret=test-jwt-secret-test-jwt-secret-1234",
    ],
)
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var authRemoteLoginService: AuthRemoteLoginService

    private val jwtSecret = "test-jwt-secret-test-jwt-secret-1234"

    @BeforeEach
    fun resetMock() {
        whenever(authRemoteLoginService.login(any(), any())).thenReturn(null)
    }

    @Test
    fun `GET login returns login page`() {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk)
            .andExpect(view().name("auth/login"))
    }

    @Test
    fun `GET notes redirects to login when unauthenticated`() {
        mockMvc.perform(get("/notes"))
            .andExpect(status().is3xxRedirection)
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login")))
    }

    @Test
    fun `POST login redirects to notes when auth service returns token`() {
        whenever(authRemoteLoginService.login("a@b.com", "secret")).thenReturn("fake-access-token")

        mockMvc.perform(
            post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "a@b.com")
                .param("password", "secret"),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/notes")))
    }

    @Test
    fun `POST login redirects to login with error when auth service returns null`() {
        mockMvc.perform(
            post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "a@b.com")
                .param("password", "wrong"),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login?error")))
    }

    @Test
    fun `GET login redirects to notes when already authenticated via jwt cookie`() {
        val token = buildJwt("93b4f470-7f26-4a3d-a3f7-1e0cc5485af1")
        mockMvc.perform(
            get("/login").cookie(Cookie(Mk1AuthCookieNames.ACCESS_TOKEN, token)),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/notes")))
    }

    @Test
    fun `POST logout moves user to login page`() {
        val token = buildJwt("93b4f470-7f26-4a3d-a3f7-1e0cc5485af1")
        mockMvc.perform(
            post("/logout")
                .with(csrf())
                .cookie(Cookie(Mk1AuthCookieNames.ACCESS_TOKEN, token)),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login?logout")))
    }

    @Test
    fun `GET notes api returns 401 when unauthenticated`() {
        mockMvc.perform(get("/api/v1/notes"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET notes api returns 200 when bearer token is valid`() {
        val key = SecretKeySpec(jwtSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val token = Jwts.builder()
            .setSubject("93b4f470-7f26-4a3d-a3f7-1e0cc5485af1")
            .setExpiration(Date(System.currentTimeMillis() + 3600_000L))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        mockMvc.perform(
            get("/api/v1/notes")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
    }

    private fun buildJwt(subject: String): String {
        val key = SecretKeySpec(jwtSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        return Jwts.builder()
            .setSubject(subject)
            .setExpiration(Date(System.currentTimeMillis() + 3600_000L))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
}
