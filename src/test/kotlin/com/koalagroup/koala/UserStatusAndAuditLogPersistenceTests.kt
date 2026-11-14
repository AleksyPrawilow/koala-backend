package com.koalagroup.koala

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.repositories.AuditLogRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.jdbc.core.queryForObject
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional // each test is rolled back; also keeps the lazy `role` association loadable
class UserStatusAndAuditLogPersistenceTests {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var userJpaRepository: PostgresUserJpaRepository

    @Autowired
    lateinit var auditLogRepository: AuditLogRepository

    private fun userRole(): Role = Role(
        id = jdbc.queryForObject<Long>("SELECT id FROM roles WHERE name = 'USER'")!!,
        name = "USER"
    )

    private fun newUser(email: String = "${UUID.randomUUID()}@example.com") = User(
        firstName = "Jan",
        lastName = "Kowalski",
        email = email,
        role = userRole(),
        passwordSalt = "-",
        passwordHash = "hash"
    )

    private fun insertUser(): UUID = userJpaRepository.saveAndFlush(newUser().toEntity()).id!!

    /** Forces a real round trip to the database instead of reading from the Hibernate session cache. */
    private fun reloadUser(id: UUID): User {
        entityManager.flush()
        entityManager.clear()
        return userJpaRepository.findById(id).orElseThrow().toDomain()
    }

    @Test
    fun `new user is ACTIVE and gets timestamps`() {
        val id = insertUser()

        val saved = reloadUser(id)

        assertEquals(UserStatus.ACTIVE, saved.status)
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `status is persisted and updated_at changes`() {
        val id = insertUser()
        val before = reloadUser(id)

        userJpaRepository.save(before.copy(status = UserStatus.BLOCKED).toEntity())

        val after = reloadUser(id)
        assertEquals(UserStatus.BLOCKED, after.status)
        assertEquals(before.createdAt, after.createdAt)
        assertTrue(after.updatedAt!! >= before.updatedAt!!)
    }

    @Test
    fun `database defaults status to ACTIVE and rejects unknown values`() {
        val id = insertUser()
        assertEquals("ACTIVE", jdbc.queryForObject<String>("SELECT status FROM users WHERE id = ?", id))

        assertFailsWith<DataIntegrityViolationException> {
            jdbc.update("UPDATE users SET status = 'DELETED' WHERE id = ?", id)
        }
    }

    @Test
    fun `audit log round-trips JSON values`() {
        val actor = insertUser()
        val target = insertUser()

        val created = auditLogRepository.create(
            AuditLog(
                actorId = actor,
                targetUserId = target,
                action = AuditAction.ROLE_CHANGED,
                oldValue = mapOf("role" to "USER"),
                newValue = mapOf("role" to "ORGANISER")
            )
        )
        assertNotNull(created.id)
        assertNotNull(created.createdAt)

        entityManager.flush()
        entityManager.clear()

        val found = auditLogRepository.findByTargetUserId(target)
        assertEquals(1, found.size)
        assertEquals(AuditAction.ROLE_CHANGED, found[0].action)
        assertEquals(actor, found[0].actorId)
        assertEquals(mapOf("role" to "USER"), found[0].oldValue)
        assertEquals(mapOf("role" to "ORGANISER"), found[0].newValue)
    }

    @Test
    fun `deleting a user keeps audit entries and nulls the reference`() {
        val actor = insertUser()
        val target = insertUser()
        auditLogRepository.create(
            AuditLog(
                actorId = actor,
                targetUserId = target,
                action = AuditAction.STATUS_CHANGED,
                oldValue = mapOf("status" to "ACTIVE"),
                newValue = mapOf("status" to "BLOCKED")
            )
        )

        jdbc.update("DELETE FROM users WHERE id = ?", target)

        val row = jdbc.queryForMap("SELECT actor_id, target_user_id FROM audit_logs WHERE actor_id = ?", actor)
        assertEquals(actor, row["actor_id"])
        assertNull(row["target_user_id"])
    }
}
