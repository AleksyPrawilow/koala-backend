package com.koalagroup.koala

import com.koalagroup.koala.api.dto.CreateUserDto
import com.koalagroup.koala.domain.entities.OrganiserCode
import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.exceptions.InvalidOrganiserCodeException
import com.koalagroup.koala.domain.exceptions.InvalidRoleException
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.UserQuery
import com.koalagroup.koala.domain.repositories.OrganiserCodeRepository
import com.koalagroup.koala.domain.repositories.RoleRepository
import com.koalagroup.koala.domain.repositories.UserRepository
import com.koalagroup.koala.domain.services.AuthService
import com.koalagroup.koala.domain.services.EmailService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import java.util.UUID

class AuthServiceTests {

    private data class SentEmail(val to: String, val subject: String, val body: String)

    private class RecordingEmailService : EmailService {
        val sentEmails = mutableListOf<SentEmail>()
        override fun sendEmail(to: String, subject: String, body: String) {
            sentEmails.add(SentEmail(to, subject, body))
        }
    }

    private class FakeUserRepository(private val failOnCreate: Boolean = false) : UserRepository {
        val created = mutableListOf<User>()
        override fun create(user: User): User {
            if (failOnCreate) throw IllegalStateException("DB error")
            created.add(user)
            return user
        }

        override fun findByEmail(email: String): User? {
            TODO("Not yet implemented")
        }

        override fun search(query: UserQuery): PagedResult<User> = throw UnsupportedOperationException()
        override fun findById(id: UUID): User? = throw UnsupportedOperationException()
        override fun update(user: User): User = throw UnsupportedOperationException()
        override fun countByRoleAndStatus(roleName: String, status: UserStatus): Long = throw UnsupportedOperationException()
    }

    private val roles = listOf(
        Role(1L, "ADMIN"),
        Role(2L, "USER"),
        Role(3L, "ORGANISER"),
        Role(4L, "GUARDIAN")
    )

    private val roleRepository = object : RoleRepository {
        override fun getAll(): List<Role> = roles
        override fun findByName(name: String): Role? = roles.firstOrNull { it.name == name }
    }

    private fun organiserCodeRepository(validCode: String? = "AB12CD") = object : OrganiserCodeRepository {
        override fun useCode(code: String): OrganiserCode? =
            if (code == validCode) OrganiserCode(1L, code, true) else null
    }

    private val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Test
    fun `sends welcome email after successful student registration`() {
        val userRepo = FakeUserRepository()
        val emailService = RecordingEmailService()
        val service = AuthService(userRepo, roleRepository, organiserCodeRepository(), passwordEncoder, emailService)

        service.registerUser(
            CreateUserDto(
                email = "jan.kowalski@example.com",
                password = "Password123",
                firstName = "Jan",
                lastName = "Kowalski",
                role = Role(2L, "USER")
            )
        )

        assertEquals(1, userRepo.created.size)
        assertEquals(1, emailService.sentEmails.size)
        val email = emailService.sentEmails.single()
        assertEquals("jan.kowalski@example.com", email.to)
        assertEquals("Witamy w systemie KOALA – konto zostało utworzone", email.subject)
        assertTrue(email.body.contains("Cześć Jan,"))
        assertTrue(email.body.contains("Przypisana rola: Uczeń"))
        assertTrue(email.body.contains("Adres e-mail: jan.kowalski@example.com"))
    }

    @Test
    fun `does not send welcome email when ADMIN role is requested`() {
        val userRepo = FakeUserRepository()
        val emailService = RecordingEmailService()
        val service = AuthService(userRepo, roleRepository, organiserCodeRepository(), passwordEncoder, emailService)

        assertThrows<InvalidRoleException> {
            service.registerUser(
                CreateUserDto(
                    email = "admin@example.com",
                    password = "Password123",
                    firstName = "Admin",
                    lastName = "Test",
                    role = Role(1L, "ADMIN")
                )
            )
        }

        assertTrue(userRepo.created.isEmpty())
        assertTrue(emailService.sentEmails.isEmpty())
    }

    @Test
    fun `does not send welcome email when organiser code is invalid`() {
        val userRepo = FakeUserRepository()
        val emailService = RecordingEmailService()
        val service = AuthService(userRepo, roleRepository, organiserCodeRepository("VALID1"), passwordEncoder, emailService)

        assertThrows<InvalidOrganiserCodeException> {
            service.registerUser(
                CreateUserDto(
                    email = "org@example.com",
                    password = "Password123",
                    firstName = "Anna",
                    lastName = "Nowak",
                    role = Role(3L, "ORGANISER"),
                    organiserCode = "WRONG1"
                )
            )
        }

        assertTrue(userRepo.created.isEmpty())
        assertTrue(emailService.sentEmails.isEmpty())
    }

    @Test
    fun `does not send welcome email when user persistence fails`() {
        val userRepo = FakeUserRepository(failOnCreate = true)
        val emailService = RecordingEmailService()
        val service = AuthService(userRepo, roleRepository, organiserCodeRepository(), passwordEncoder, emailService)

        assertThrows<IllegalStateException> {
            service.registerUser(
                CreateUserDto(
                    email = "dup@example.com",
                    password = "Password123",
                    firstName = "Jan",
                    lastName = "Kowalski",
                    role = Role(2L, "USER")
                )
            )
        }

        assertTrue(emailService.sentEmails.isEmpty())
    }
}
