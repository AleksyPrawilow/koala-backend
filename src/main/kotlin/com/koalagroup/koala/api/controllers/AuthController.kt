package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.api.dto.CreateUserDto
import com.koalagroup.koala.api.dto.LoginRequest
import com.koalagroup.koala.api.dto.toLoginResponse
import com.koalagroup.koala.domain.exceptions.InvalidOrganiserCodeException
import com.koalagroup.koala.domain.services.AuthService
import com.koalagroup.koala.infrastructure.security.JwtCookie
import com.koalagroup.koala.infrastructure.security.JwtProperties
import com.koalagroup.koala.infrastructure.security.JwtTokenService
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseCookie
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtTokenService: JwtTokenService,
    private val jwtProperties: JwtProperties,
    private val csrfTokenRepository: CookieCsrfTokenRepository
) {
    @GetMapping("csrf")
    fun csrfToken(
        @RequestAttribute("_csrf") csrfToken: CsrfToken,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Map<String, String> {
        csrfTokenRepository.saveToken(csrfToken, request, response)
        return mapOf("token" to csrfToken.token)
    }

    @PostMapping("login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<Any> {
        val user = authService.authenticate(request.email, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf(
                    "status" to HttpStatus.UNAUTHORIZED.value(),
                    "error" to HttpStatus.UNAUTHORIZED.reasonPhrase,
                    "message" to "Invalid email or password."
                )
            )

        val cookie = ResponseCookie.from(JwtCookie.NAME, jwtTokenService.createToken(user))
            .httpOnly(true)
            .secure(jwtProperties.cookieSecure)
            .sameSite(jwtProperties.cookieSameSite)
            .path("/")
            .maxAge(jwtProperties.expiration)
            .build()

        return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(user.toLoginResponse())
    }

    @PostMapping("register")
    fun registerUser(@Valid @RequestBody request: CreateUserDto) {
        authService.registerUser(request)
    }

    @ExceptionHandler(InvalidOrganiserCodeException::class)
    fun handleInvalidOrganiserCode(ex: InvalidOrganiserCodeException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.badRequest().body(
            mapOf(
                "status" to 403,
                "error" to "Unauthorized",
                "message" to ex.localizedMessage
            )
        )
    }
}