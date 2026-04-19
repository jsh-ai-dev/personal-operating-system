package com.jsh.pos.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pos.auth.service")
data class AuthServiceProperties(
    /** mk2 auth-service 베이스 URL (예: http://127.0.0.1:3002) */
    val baseUrl: String = "http://127.0.0.1:3002",
)
