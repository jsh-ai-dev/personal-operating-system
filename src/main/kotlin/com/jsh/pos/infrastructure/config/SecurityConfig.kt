package com.jsh.pos.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf {
                it.ignoringRequestMatchers("/api/**")
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/", "/login", "/hello", "/css/**", "/js/**", "/error").permitAll()
                    .requestMatchers("/api/**").permitAll()
                    .requestMatchers("/notes/**").authenticated()
                    .anyRequest().permitAll()
            }
            .formLogin {
                it
                    .loginPage("/login")
                    .defaultSuccessUrl("/notes", false)
                    .failureUrl("/login?error")
                    .permitAll()
            }
            .logout {
                it
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            }

        return http.build()
    }
}


