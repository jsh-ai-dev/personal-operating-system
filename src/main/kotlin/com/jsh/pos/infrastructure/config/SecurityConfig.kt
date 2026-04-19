package com.jsh.pos.infrastructure.config

import com.jsh.pos.infrastructure.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig {

    @Bean
    fun jwtAuthenticationFilter(
        @Value("\${pos.auth.jwt.secret:}") jwtSecret: String,
        @Value("\${pos.auth.token-cookie-name:mk1_pos_jwt}") accessTokenCookieName: String,
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(jwtSecret, accessTokenCookieName)

    @Bean
    @Order(1)
    fun apiSecurityFilterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/api/v1/notes/**").authenticated()
                    .anyRequest().permitAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .formLogin { it.disable() }
            .logout { it.disable() }

        return http.build()
    }

    @Bean
    @Order(2)
    fun webSecurityFilterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http
            // Thymeleaf 폼·로그아웃이 ${_csrf} 를 쓰므로 브라우저용 웹 체인에서는 CSRF 유지.
            // /api/** 는 Order(1) 체인에서 CSRF 비활성화.
            .csrf { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/", "/login", "/logout", "/hello", "/css/**", "/js/**", "/error").permitAll()
                    .requestMatchers("/notes/**").authenticated()
                    .requestMatchers("/summary/**").authenticated()
                    .anyRequest().permitAll()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/login"))
            }
            .formLogin { it.disable() }
            .logout { it.disable() }

        return http.build()
    }
}
