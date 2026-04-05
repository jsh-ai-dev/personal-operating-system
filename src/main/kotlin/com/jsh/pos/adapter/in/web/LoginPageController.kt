package com.jsh.pos.adapter.`in`.web

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginPageController {

    @GetMapping("/login")
    fun login(authentication: Authentication?): String {
        val authenticated = authentication != null &&
            authentication.isAuthenticated &&
            authentication !is AnonymousAuthenticationToken

        return if (authenticated) {
            "redirect:/notes"
        } else {
            "auth/login"
        }
    }
}

