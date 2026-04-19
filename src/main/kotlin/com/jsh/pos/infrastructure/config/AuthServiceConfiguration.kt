package com.jsh.pos.infrastructure.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AuthServiceProperties::class)
class AuthServiceConfiguration
