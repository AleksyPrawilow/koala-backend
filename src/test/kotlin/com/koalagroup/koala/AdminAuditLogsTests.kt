package com.koalagroup.koala

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional // every test is rolled back, so the seeded entries never leak between tests
class AdminAuditLogsTests {

    @Autowired
    lateinit var context: WebApplicationContext

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var userJpaRepository: PostgresUserJpaRepository

    @PersistenceContext
    lateinit var entityManager: EntityManager

    lateinit var mockMvc: MockMvc

    private lateinit var admin: UUID
    private lateinit var otherAdmin: UUID
    private lateinit var student: UUID
    private lateinit var guardian: UUID

    private val t1 = Instant.parse("2026-11-01T10:00:00Z")
    private val t2 = Instant.parse("2026-11-02T10:00:00Z")
    private val t3 = Instant.parse("2026-11-03T10:00:00Z")
    private val t4 = Instant.parse("2026-11-04T10:00:00Z")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

        admin = seedUser("Ada", "ADMIN")
        otherAdmin = seedUser("Olek", "ADMIN")
        student = seedUser("Stefan", "USER")
        guardian = seedUser("Gosia", "GUARDIAN")
        entityManager.flush()

        // oldest to newest
        insertLog(admin, student, "ROLE_CHANGED", """{"role":"USER"}""", """{"role":"ORGANISER"}""", t1)
        insertLog(admin, student, "STATUS_CHANGED", """{"status":"ACTIVE"}""", """{"status":"BLOCKED"}""", t2)
        insertLog(otherAdmin, guardian, "STATUS_CHANGED", """{"status":"ACTIVE"}""", """{"status":"INACTIVE"}""", t3)
        insertLog(otherAdmin, student, "ROLE_CHANGED", """{"role":"ORGANISER"}""", """{"role":"USER"}""", t4)
    }

    private fun seedUser(firstName: String, roleName: String): UUID {
        val roleId = jdbc.queryForObject("SELECT id FROM roles WHERE name = ?", Long::class.java, roleName)!!
        return userJpaRepository.save(
            User(
                firstName = firstName,
                lastName = "Test",
                email = "${UUID.randomUUID()}@example.com",
                role = Role(roleId, roleName),
                passwordSalt = "-",
                passwordHash = "hash"
            ).toEntity()
        ).id!!
    }

    private fun insertLog(actor: UUID, target: UUID, action: String, old: String, new: String, createdAt: Instant) {
        jdbc.update(
            "INSERT INTO audit_logs (actor_id, target_user_id, action, old_value, new_value, created_at) " +
                "VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?)",
            actor, target, action, old, new, Timestamp.from(createdAt)
        )
    }

    private fun list(vararg params: Pair<String, String>): ResultActions =
        mockMvc.perform(get("/api/admin/audit-logs").apply { params.forEach { (name, value) -> param(name, value) } })

    private fun ResultActions.body(): String = andReturn().response.contentAsString

    /** The creation times of the returned entries, in the returned order. */
    private fun ResultActions.times(): List<Instant> =
        Regex("\"createdAt\":\"([^\"]+)\"").findAll(body()).map { Instant.parse(it.groupValues[1]) }.toList()

    @Test
    fun `returns entries newest first with the paging metadata`() {
        val response = list().andExpect(status().isOk)

        val body = response.body()
        assertTrue(body.contains("\"totalElements\":4"), body)
        assertTrue(body.contains("\"totalPages\":1"), body)
        assertEquals(listOf(t4, t3, t2, t1), response.times())
    }

    @Test
    fun `entry contains the administrator, the user, the action and the old and new value`() {
        val body = list("userId" to student.toString(), "action" to "ROLE_CHANGED", "actorId" to admin.toString())
            .andExpect(status().isOk).body()

        assertTrue(body.contains("\"actorId\":\"$admin\""), body)
        assertTrue(body.contains("\"targetUserId\":\"$student\""), body)
        assertTrue(body.contains("\"action\":\"ROLE_CHANGED\""), body)
        assertTrue(body.contains("\"oldValue\":{\"role\":\"USER\"}"), body)
        assertTrue(body.contains("\"newValue\":{\"role\":\"ORGANISER\"}"), body)
    }

    @Test
    fun `sorts oldest first on request`() {
        assertEquals(listOf(t1, t2, t3, t4), list("sort" to "createdAt,asc").times())
    }

    @Test
    fun `filters by user`() {
        assertEquals(listOf(t4, t2, t1), list("userId" to student.toString()).times())
        assertEquals(listOf(t3), list("userId" to guardian.toString()).times())
    }

    @Test
    fun `filters by administrator`() {
        assertEquals(listOf(t2, t1), list("actorId" to admin.toString()).times())
        assertEquals(listOf(t4, t3), list("actorId" to otherAdmin.toString()).times())
    }

    @Test
    fun `filters by action`() {
        assertEquals(listOf(t4, t1), list("action" to "ROLE_CHANGED").times())
        assertEquals(listOf(t3, t2), list("action" to "STATUS_CHANGED").times())
    }

    @Test
    fun `filters by date range, inclusive on both ends`() {
        assertEquals(listOf(t3, t2), list("from" to t2.toString(), "to" to t3.toString()).times())
        assertEquals(listOf(t4, t3), list("from" to t3.toString()).times())
        assertEquals(listOf(t2, t1), list("to" to t2.toString()).times())
    }

    @Test
    fun `combines filters`() {
        assertEquals(
            listOf(t4),
            list("userId" to student.toString(), "actorId" to otherAdmin.toString(), "from" to t3.toString()).times()
        )
        assertTrue(list("userId" to guardian.toString(), "action" to "ROLE_CHANGED").times().isEmpty())
    }

    @Test
    fun `paginates results`() {
        val first = list("size" to "3", "page" to "0").andExpect(status().isOk)
        assertEquals(listOf(t4, t3, t2), first.times())
        assertTrue(first.body().contains("\"totalPages\":2"))

        assertEquals(listOf(t1), list("size" to "3", "page" to "1").times())
    }

    @Test
    fun `keeps the entry after the user is removed and empties the reference`() {
        jdbc.update("DELETE FROM users WHERE id = ?", guardian)
        entityManager.clear()

        val body = list("action" to "STATUS_CHANGED", "to" to t3.toString(), "from" to t3.toString())
            .andExpect(status().isOk).body()

        assertTrue(body.contains("\"targetUserId\":null"), body)
        assertTrue(body.contains("\"newValue\":{\"status\":\"INACTIVE\"}"), body)
    }

    @Test
    fun `rejects invalid parameters with 400`() {
        list("size" to "0").andExpect(status().isBadRequest)
        list("size" to "100001").andExpect(status().isBadRequest)
        list("page" to "-1").andExpect(status().isBadRequest)
        list("action" to "DELETED").andExpect(status().isBadRequest)
        list("userId" to "not-a-uuid").andExpect(status().isBadRequest)
        list("from" to "yesterday").andExpect(status().isBadRequest)
        list("from" to t3.toString(), "to" to t1.toString()).andExpect(status().isBadRequest)
        list("sort" to "action").andExpect(status().isBadRequest)
        list("sort" to "createdAt,sideways").andExpect(status().isBadRequest)
    }
}
