package com.koalagroup.koala

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.repositories.AuditLogRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.security.web.FilterChainProxy
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional // every test is rolled back, so the seeded users never leak between tests
class AdminUserManagementTests {

    @Autowired
    lateinit var context: WebApplicationContext

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var userJpaRepository: PostgresUserJpaRepository

    @Autowired
    lateinit var auditLogRepository: AuditLogRepository

    @PersistenceContext
    lateinit var entityManager: EntityManager

    lateinit var mockMvc: MockMvc

    private lateinit var admin: UUID
    private lateinit var otherAdmin: UUID
    private lateinit var student: UUID
    private var authenticatedActor: UUID? = null

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters<DefaultMockMvcBuilder>(context.getBean(FilterChainProxy::class.java))
            .build()
        authenticatedActor = null

        admin = seed("Ada", "Admin", "ADMIN")
        otherAdmin = seed("Olek", "Admin", "ADMIN")
        student = seed("Stefan", "Student", "USER")
        entityManager.flush()
        entityManager.clear()
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun seed(firstName: String, lastName: String, roleName: String): UUID {
        val roleId = jdbc.queryForObject("SELECT id FROM roles WHERE name = ?", Long::class.java, roleName)!!
        return userJpaRepository.save(
            User(
                firstName = firstName,
                lastName = lastName,
                email = "${UUID.randomUUID()}@example.com",
                role = Role(roleId, roleName),
                passwordSalt = "-",
                passwordHash = "hash"
            ).toEntity()
        ).id!!
    }

    /** Makes [id] the administrator performing the requests (the principal name is the user id). */
    private fun actAs(id: UUID) {
        authenticatedActor = id
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                id.toString(),
                null,
                listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
            )
    }

    private fun reload(id: UUID): User {
        entityManager.flush()
        entityManager.clear()
        return userJpaRepository.findById(id).orElseThrow().toDomain()
    }

    private fun audits(id: UUID) = run {
        entityManager.flush()
        entityManager.clear()
        auditLogRepository.findByTargetUserId(id)
    }

    private fun changeRole(id: Any, role: String): ResultActions =
        mockMvc.perform(
            patch("/api/admin/users/{id}/role", id).contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":"$role"}""")
                .with(user(authenticatedActor?.toString() ?: "test").roles("ADMIN"))
                .with(csrf())
        )

    private fun changeStatus(id: Any, status: String): ResultActions =
        mockMvc.perform(
            patch("/api/admin/users/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"$status"}""")
                .with(user(authenticatedActor?.toString() ?: "test").roles("ADMIN"))
                .with(csrf())
        )

    private fun deleteUser(id: Any): ResultActions =
        mockMvc.perform(
            delete("/api/admin/users/{id}", id)
                .with(user(authenticatedActor?.toString() ?: "test").roles("ADMIN"))
                .with(csrf())
        )

    private fun ResultActions.body(): String = andReturn().response.contentAsString

    // ---- role

    @Test
    fun `changes the role, saves it and writes an audit entry`() {
        actAs(admin)

        val body = changeRole(student, "ORGANISER").andExpect(status().isOk).body()

        assertTrue(body.contains("\"role\":\"ORGANISER\""), body)
        assertEquals("ORGANISER", reload(student).role.name)

        val log = audits(student).single()
        assertEquals(AuditAction.ROLE_CHANGED, log.action)
        assertEquals(admin, log.actorId)
        assertEquals(student, log.targetUserId)
        assertEquals(mapOf("role" to "USER"), log.oldValue)
        assertEquals(mapOf("role" to "ORGANISER"), log.newValue)
        assertTrue(log.createdAt != null)
    }

    @Test
    fun `role name is case-insensitive and an unchanged role writes nothing`() {
        changeRole(student, "user").andExpect(status().isOk)

        assertEquals("USER", reload(student).role.name)
        assertTrue(audits(student).isEmpty())
    }

    @Test
    fun `rejects an unknown or blank role`() {
        changeRole(student, "SUPERHERO").andExpect(status().isBadRequest)
        changeRole(student, "").andExpect(status().isBadRequest)
        assertEquals("USER", reload(student).role.name)
    }

    @Test
    fun `an administrator cannot change their own role`() {
        actAs(admin)

        changeRole(admin, "USER").andExpect(status().isForbidden)

        assertEquals("ADMIN", reload(admin).role.name)
    }

    @Test
    fun `the last active administrator cannot be demoted`() {
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", otherAdmin)
        entityManager.clear()

        changeRole(admin, "USER").andExpect(status().isConflict)

        assertEquals("ADMIN", reload(admin).role.name)
    }

    @Test
    fun `an administrator can be demoted while another active one remains`() {
        actAs(admin)

        changeRole(otherAdmin, "USER").andExpect(status().isOk)

        assertEquals("USER", reload(otherAdmin).role.name)
    }

    // ---- status

    @Test
    fun `blocks and unblocks an account, logging both changes`() {
        actAs(admin)

        val blocked = changeStatus(student, "BLOCKED").andExpect(status().isOk).body()
        assertTrue(blocked.contains("\"status\":\"BLOCKED\""), blocked)
        assertEquals(UserStatus.BLOCKED, reload(student).status)

        changeStatus(student, "ACTIVE").andExpect(status().isOk)
        assertEquals(UserStatus.ACTIVE, reload(student).status)

        val logs = audits(student)
        assertEquals(2, logs.size)
        assertTrue(logs.all { it.action == AuditAction.STATUS_CHANGED && it.actorId == admin })
        assertTrue(logs.any { it.oldValue == mapOf("status" to "ACTIVE") && it.newValue == mapOf("status" to "BLOCKED") })
        assertTrue(logs.any { it.oldValue == mapOf("status" to "BLOCKED") && it.newValue == mapOf("status" to "ACTIVE") })
    }

    @Test
    fun `the actor is empty in the audit entry while principal is not a UUID`() {
        changeStatus(student, "BLOCKED").andExpect(status().isOk)

        assertNull(audits(student).single().actorId)
    }

    @Test
    fun `an unchanged status writes no audit entry`() {
        changeStatus(student, "ACTIVE").andExpect(status().isOk)

        assertTrue(audits(student).isEmpty())
    }

    @Test
    fun `rejects an invalid status`() {
        changeStatus(student, "DELETED").andExpect(status().isBadRequest)
        mockMvc.perform(
            patch("/api/admin/users/{id}/status", student).contentType(MediaType.APPLICATION_JSON).content("{}")
                .with(user("test").roles("ADMIN"))
                .with(csrf())
        ).andExpect(status().isBadRequest)
        assertEquals(UserStatus.ACTIVE, reload(student).status)
    }

    @Test
    fun `an administrator cannot change their own status`() {
        actAs(admin)

        changeStatus(admin, "BLOCKED").andExpect(status().isForbidden)

        assertEquals(UserStatus.ACTIVE, reload(admin).status)
    }

    @Test
    fun `the last active administrator cannot be blocked`() {
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", otherAdmin)
        entityManager.clear()

        changeStatus(admin, "BLOCKED").andExpect(status().isConflict)

        assertEquals(UserStatus.ACTIVE, reload(admin).status)
    }

    // ---- delete

    @Test
    fun `delete deactivates the account instead of removing it`() {
        actAs(admin)

        deleteUser(student).andExpect(status().isNoContent)

        assertEquals(UserStatus.INACTIVE, reload(student).status)
        val log = audits(student).single()
        assertEquals(AuditAction.STATUS_CHANGED, log.action)
        assertEquals(mapOf("status" to "INACTIVE"), log.newValue)
    }

    @Test
    fun `deleting twice is harmless and logged once`() {
        deleteUser(student).andExpect(status().isNoContent)
        deleteUser(student).andExpect(status().isNoContent)

        assertEquals(1, audits(student).size)
    }

    @Test
    fun `an administrator cannot delete themselves and the last administrator cannot be deleted`() {
        actAs(admin)
        deleteUser(admin).andExpect(status().isForbidden)

        SecurityContextHolder.clearContext()
        authenticatedActor = null
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", otherAdmin)
        entityManager.clear()
        deleteUser(admin).andExpect(status().isConflict)

        assertEquals(UserStatus.ACTIVE, reload(admin).status)
    }

    // ---- not found / bad id

    @Test
    fun `unknown user gives 404 and a malformed id gives 400`() {
        val unknown = UUID.randomUUID()
        changeRole(unknown, "USER").andExpect(status().isNotFound)
        changeStatus(unknown, "BLOCKED").andExpect(status().isNotFound)
        deleteUser(unknown).andExpect(status().isNotFound)

        changeRole("not-a-uuid", "USER").andExpect(status().isBadRequest)
        val body = changeStatus(unknown, "BLOCKED").andExpect(status().isNotFound).body()
        assertTrue(body.contains("\"status\":404"), body)
    }

    // ---- registration cannot grant the ADMIN role

    @Test
    fun `registration cannot be used to become an administrator`() {
        val adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE name = 'ADMIN'", Long::class.java)!!
        val email = "${UUID.randomUUID()}@example.com"

        mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                """{"email":"$email","password":"Password1","firstName":"Eve","lastName":"Hacker",
                    "role":{"id":$adminRoleId,"name":"ADMIN"}}"""
            ).with(csrf())
        ).andExpect(status().isBadRequest)

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Int::class.java, email))
    }
}
