package com.jsh.pos.infrastructure.security

import com.jsh.pos.adapter.out.persistence.jpa.UserJpaEntity
import com.jsh.pos.adapter.out.persistence.jpa.UserJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AuthSeedInitializer(
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${pos.auth.seed.username:pos-admin}")
    private val seedUsername: String,
    @Value("\${pos.auth.seed.password:pos-admin1234}")
    private val seedPassword: String,
    @Value("\${pos.auth.seed.role:ROLE_USER}")
    private val seedRole: String,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        val existingUser = userJpaRepository.findByUsername(seedUsername)
        if (existingUser == null) {
            userJpaRepository.save(
                UserJpaEntity(
                    username = seedUsername,
                    passwordHash = passwordEncoder.encode(seedPassword),
                    role = seedRole,
                    enabled = true,
                ),
            )
            return
        }

        // 초기 계정(seed) 설정이 바뀌면 서버 재기동 시 반영되도록 최소 동기화합니다.
        val needsUpdate =
            !passwordEncoder.matches(seedPassword, existingUser.passwordHash) ||
                existingUser.role != seedRole ||
                !existingUser.enabled

        if (needsUpdate) {
            existingUser.passwordHash = passwordEncoder.encode(seedPassword)
            existingUser.role = seedRole
            existingUser.enabled = true
            userJpaRepository.save(existingUser)
        }
    }
}



