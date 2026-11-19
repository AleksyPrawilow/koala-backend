package com.koalagroup.koala.domain.services

import com.koalagroup.koala.api.dto.CreateUserDto
import com.koalagroup.koala.domain.entities.OrganiserCode
import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.exceptions.InvalidOrganiserCodeException
import com.koalagroup.koala.domain.exceptions.InvalidRoleException
import com.koalagroup.koala.domain.exceptions.PasswordCreationFailureException
import com.koalagroup.koala.domain.repositories.OrganiserCodeRepository
import com.koalagroup.koala.domain.repositories.RoleRepository
import com.koalagroup.koala.domain.repositories.UserRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val organiserCodeRepository: OrganiserCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {
    fun registerUser(requestDto: CreateUserDto): User {
        val encodedPassword = passwordEncoder.encode(requestDto.password) ?: throw PasswordCreationFailureException("Password required")

        val availableRoles: List<Role> = roleRepository.getAll()

        if (requestDto.role !in availableRoles) {
            throw InvalidRoleException("Role ${requestDto.role.name} is not allowed")
        }

        if (requestDto.role.name == Role.ADMIN) {
            throw InvalidRoleException("Role ${requestDto.role.name} is not allowed")
        }

        if (requestDto.role.name == Role.ORGANISER) {
            val incomingOrganiserCode: String = requestDto.organiserCode ?: throw InvalidOrganiserCodeException("Organiser Code is required for organisers")
            organiserCodeRepository.useCode(incomingOrganiserCode) ?: throw InvalidOrganiserCodeException("The provided code was not found")
        }

        val user = User(
            firstName = requestDto.firstName,
            lastName = requestDto.lastName,
            email = requestDto.email,
            role = requestDto.role,
            passwordHash = encodedPassword,
            passwordSalt = "-",
        )
        val createdUser = userRepository.create(user)
        sendWelcomeEmail(createdUser)
        return createdUser
    }

    private fun sendWelcomeEmail(user: User) {
        val roleDisplayName = when (user.role.name) {
            "USER" -> "Uczeń"
            "GUARDIAN" -> "Opiekun"
            Role.ORGANISER -> "Organizator"
            else -> user.role.name
        }

        val subject = "Witamy w systemie KOALA – konto zostało utworzone"
        val body = """
            Cześć ${user.firstName},

            Twoje konto w systemie konkursu KOALA zostało pomyślnie utworzone.

            Przypisana rola: $roleDisplayName
            Adres e-mail: ${user.email}

            Pozdrawiamy,
            Zespół KOALA
        """.trimIndent()

        emailService.sendEmail(user.email, subject, body)
    }
}