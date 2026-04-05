package com.jsh.pos.adapter.out.persistence.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class UserJpaRepositoryTest {

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Test
    fun `findByUsername returns user when username exists`() {
        userJpaRepository.save(
            UserJpaEntity(
                username = "db-user-1",
                passwordHash = "hashed-password",
                role = "ROLE_USER",
                enabled = true,
            ),
        )

        val found = userJpaRepository.findByUsername("db-user-1")

        assertNotNull(found)
        assertEquals("db-user-1", found!!.username)
        assertEquals("ROLE_USER", found.role)
        assertEquals(true, found.enabled)
    }

    @Test
    fun `findByUsername returns null when username is missing`() {
        val found = userJpaRepository.findByUsername("missing-user")

        assertNull(found)
    }
}

