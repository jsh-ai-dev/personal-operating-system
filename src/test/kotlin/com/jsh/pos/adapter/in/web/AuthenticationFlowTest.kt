package com.jsh.pos.adapter.`in`.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    @Autowired
    lateinit var mockMvc: MockMvc

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
            .andExpect(redirectedUrl("http://localhost/login"))
    }

    @Test
    fun `POST login redirects to notes when credentials are valid`() {
        mockMvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "pos-admin")
                .param("password", "pos-admin1234"),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/notes"))
    }

    @Test
    fun `POST login redirects to login with error when credentials are invalid`() {
        mockMvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "pos-admin")
                .param("password", "wrong-password"),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/login?error"))
    }

    @Test
    fun `GET login redirects to notes when already authenticated`() {
        mockMvc.perform(get("/login").with(user("pos-admin").roles("USER")))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/notes"))
    }

    @Test
    fun `POST logout moves user to login page`() {
        mockMvc.perform(
            post("/logout")
                .with(user("pos-admin").roles("USER"))
                .with(csrf()),
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/login?logout"))
    }
}

