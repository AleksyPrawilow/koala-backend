package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.api.dto.CreateUserDto
import com.koalagroup.koala.domain.exceptions.InvalidOrganiserCodeException
import com.koalagroup.koala.domain.services.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/auth")
class AuthController(
    private val authService: AuthService
) {
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