package com.jsh.pos.infrastructure.security

import com.jsh.pos.adapter.out.persistence.jpa.UserJpaRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class DbUserDetailsService(
    private val userJpaRepository: UserJpaRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userJpaRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다: $username")

        return User(
            user.username,
            user.passwordHash,
            user.enabled,
            true,
            true,
            true,
            listOf(SimpleGrantedAuthority(user.role)),
        )
    }
}

