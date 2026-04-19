package com.jsh.pos.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jsh.pos.infrastructure.config.AuthServiceProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Service
class RestAuthRemoteLoginService(
    private val props: AuthServiceProperties,
    private val objectMapper: ObjectMapper,
) : AuthRemoteLoginService {

    private val client: RestClient = RestClient.builder()
        .baseUrl(props.baseUrl.trimEnd('/'))
        .build()

    override fun login(email: String, password: String): String? {
        val body = objectMapper.writeValueAsString(LoginBody(email.trim().lowercase(), password))
        return try {
            val raw = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String::class.java) ?: return null
            val res = objectMapper.readValue<LoginResponse>(raw)
            res.accessToken
        } catch (_: RestClientException) {
            null
        }
    }

    private data class LoginBody(val email: String, val password: String)

    private data class LoginResponse(val accessToken: String?)
}
