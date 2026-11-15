package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.api.dto.CreateUserDto
import com.koalagroup.koala.domain.exceptions.InvalidOrganiserCodeException
import com.koalagroup.koala.domain.services.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/auth")
@Tag(name = "Auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("register")
    @Operation(
        summary = "Register a new account",
        description = "Creates an account with the chosen role. The ADMIN role cannot be chosen, and the ORGANISER role " +
            "requires a valid one-time organiser code. Invalid data returns 400 with the details per field."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Account created."),
            ApiResponse(responseCode = "400", description = "Invalid data, role not allowed or invalid organiser code.")
        ]
    )
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