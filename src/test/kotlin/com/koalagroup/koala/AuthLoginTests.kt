package com.koalagroup.koala

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.security.web.FilterChainProxy
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional
class AuthLoginTests {

    @Autowired
    lateinit var context: WebApplicationContext

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var userJpaRepository: PostgresUserJpaRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @PersistenceContext
    lateinit var entityManager: EntityManager

    private lateinit var mockMvc: MockMvc
    private val password = "Password1!"

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters<DefaultMockMvcBuilder>(context.getBean(FilterChainProxy::class.java))
            .build()
    }

    private fun seedUser(roleName: String, status: UserStatus = UserStatus.ACTIVE): String {
        val email = "${roleName.lowercase()}-${java.util.UUID.randomUUID()}@example.com"
        val roleId = jdbc.queryForObject("SELECT id FROM roles WHERE name = ?", Long::class.java, roleName)!!
        userJpaRepository.save(
            User(
                firstName = "Test",
                lastName = "User",
                email = email,
                role = Role(roleId, roleName),
                passwordSalt = "-",
                passwordHash = passwordEncoder.encode(password) ?: error("Password encoding failed"),
                status = status
            ).toEntity()
        )
        entityManager.flush()
        entityManager.clear()
        return email
    }

    private fun login(email: String, suppliedPassword: String = password) =
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$suppliedPassword"}""")
                .with(csrf())
        )

    @Test
    fun `login returns an HttpOnly JWT cookie that authenticates admin requests`() {
        val email = seedUser("ADMIN")
        val result = login(email).andExpect(status().isOk).andReturn()
        val cookie = assertNotNull(result.response.getCookie("AUTH_TOKEN"))

        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.secure)
        assertTrue(cookie.path == "/")
        assertTrue(cookie.maxAge == 900)
        assertTrue(result.response.getHeader("Set-Cookie")!!.contains("SameSite=Lax"))

        mockMvc.perform(get("/api/admin/users").cookie(cookie))
            .andExpect(status().isOk)
    }

    @Test
    fun `non-admin JWT cannot access admin endpoints`() {
        val email = seedUser("USER")
        val cookie = assertNotNull(login(email).andExpect(status().isOk).andReturn().response.getCookie("AUTH_TOKEN"))

        mockMvc.perform(get("/api/admin/users").cookie(cookie))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `invalid credentials and inactive accounts are rejected without issuing cookies`() {
        val activeEmail = seedUser("USER")
        val inactiveEmail = seedUser("USER", UserStatus.INACTIVE)

        val invalidPassword = login(activeEmail, "wrong-password").andExpect(status().isUnauthorized).andReturn()
        val inactiveAccount = login(inactiveEmail).andExpect(status().isUnauthorized).andReturn()

        assertTrue(invalidPassword.response.getCookie("AUTH_TOKEN") == null)
        assertTrue(inactiveAccount.response.getCookie("AUTH_TOKEN") == null)
    }

    @Test
    fun `csrf token endpoint returns a token required for login`() {
        val email = seedUser("USER")
        val csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk)
            .andReturn()
        val csrfToken = Regex("\"token\":\"([^\"]+)\"")
            .find(csrfResult.response.contentAsString)!!
            .groupValues[1]
        val csrfCookieHeader = assertNotNull(
            csrfResult.response.getHeader("Set-Cookie"),
            "body=${csrfResult.response.contentAsString}; " +
                "cookies=${csrfResult.response.cookies.map { it.name to it.value }}"
        )
        val csrfCookieValue = csrfCookieHeader.substringAfter("XSRF-TOKEN=").substringBefore(";")
        val csrfCookie = Cookie("XSRF-TOKEN", csrfCookieValue)

        assertTrue(csrfCookie.value == csrfToken)
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken)
        ).andExpect(status().isOk)
    }

    @Test
    fun `localhost cors preflight allows credentials and csrf headers`() {
        mockMvc.perform(
            options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
    }
}
