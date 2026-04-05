package com.jsh.pos.adapter.out.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByUsername(username: String): UserJpaEntity?
}

