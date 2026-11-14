package com.koalagroup.koala

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
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
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional // every test is rolled back, so the seeded users never leak between tests
class AdminUsersListTests {

    @Autowired
    lateinit var context: WebApplicationContext

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var userJpaRepository: PostgresUserJpaRepository

    lateinit var mockMvc: MockMvc

    // All seeded e-mails share this domain, so every request is scoped to it with a search token
    // and the tests stay independent of any other data in the database.
    private val domain = "adm-test.example"

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

        seed("Alice", "Nowak", "alice.nowak@$domain", "USER", UserStatus.ACTIVE)
        seed("Bob", "Nowak", "bob.nowak@$domain", "GUARDIAN", UserStatus.BLOCKED)
        seed("Carol", "Kowalska", "carol.kowalska@$domain", "ORGANISER", UserStatus.ACTIVE)
        seed("Dan", "Zielinski", "dan_z@$domain", "USER", UserStatus.INACTIVE)
        // Start every request from a clean session so users and roles are really loaded from the database.
        entityManager.flush()
        entityManager.clear()
    }

    private fun seed(firstName: String, lastName: String, email: String, roleName: String, status: UserStatus) {
        val roleId = jdbc.queryForObject<Long>("SELECT id FROM roles WHERE name = ?", roleName)!!
        userJpaRepository.save(
            User(
                firstName = firstName,
                lastName = lastName,
                email = email,
                role = Role(roleId, roleName),
                passwordSalt = "-",
                passwordHash = "hash",
                status = status
            ).toEntity()
        )
    }

    /** Calls the endpoint; a given `search` is combined (AND) with the domain token that scopes the data. */
    private fun list(vararg params: Pair<String, String>): ResultActions {
        val query = params.toMap().toMutableMap()
        query["search"] = listOfNotNull(domain, query["search"]).joinToString(" ")
        return mockMvc.perform(
            get("/api/admin/users").apply { query.forEach { (name, value) -> param(name, value) } }
        )
    }

    private fun ResultActions.body(): String = andReturn().response.contentAsString

    private fun ResultActions.emails(): List<String> =
        Regex("\"email\":\"([^\"]+)\"").findAll(body()).map { it.groupValues[1].removeSuffix("@$domain") }.toList()

    @Test
    fun `returns all users with the paging metadata and default sort by last name`() {
        val response = list().andExpect(status().isOk)

        val body = response.body()
        assertTrue(body.contains("\"totalElements\":4"), body)
        assertTrue(body.contains("\"totalPages\":1"), body)
        assertTrue(body.contains("\"page\":0"), body)
        assertTrue(body.contains("\"size\":20"), body)
        // lastName asc: Kowalska, Nowak, Nowak, Zielinski (ties broken by id, so Alice/Bob may swap)
        val emails = response.emails()
        assertEquals("carol.kowalska", emails.first())
        assertEquals("dan_z", emails.last())
    }

    @Test
    fun `item contains name, e-mail, role and status`() {
        val body = list("search" to "carol").andExpect(status().isOk).body()

        assertTrue(body.contains("\"firstName\":\"Carol\""), body)
        assertTrue(body.contains("\"lastName\":\"Kowalska\""), body)
        assertTrue(body.contains("\"role\":\"ORGANISER\""), body)
        assertTrue(body.contains("\"status\":\"ACTIVE\""), body)
    }

    @Test
    fun `search is case-insensitive and matches first name, last name and e-mail`() {
        assertEquals(listOf("alice.nowak"), list("search" to "ALICE").emails())
        assertEquals(2, list("search" to "nowak").emails().size)
        assertEquals(listOf("carol.kowalska"), list("search" to "kowalska").emails())
        assertEquals(listOf("bob.nowak"), list("search" to "bob.nowak@").emails())
    }

    @Test
    fun `search with several words requires all of them to match`() {
        assertEquals(listOf("alice.nowak"), list("search" to "alice nowak").emails())
        assertTrue(list("search" to "alice kowalska").emails().isEmpty())
    }

    @Test
    fun `search treats LIKE wildcards literally`() {
        assertEquals(listOf("dan_z"), list("search" to "dan_z").emails())
        assertTrue(list("search" to "d_n").emails().isEmpty())
        assertTrue(list("search" to "%").emails().isEmpty())
    }

    @Test
    fun `filters by role`() {
        assertEquals(setOf("alice.nowak", "dan_z"), list("role" to "USER").emails().toSet())
        assertEquals(listOf("bob.nowak"), list("role" to "guardian").emails())
        assertTrue(list("role" to "NO_SUCH_ROLE").emails().isEmpty())
    }

    @Test
    fun `filters by status`() {
        assertEquals(setOf("alice.nowak", "carol.kowalska"), list("status" to "ACTIVE").emails().toSet())
        assertEquals(listOf("bob.nowak"), list("status" to "BLOCKED").emails())
        assertEquals(listOf("dan_z"), list("status" to "INACTIVE").emails())
    }

    @Test
    fun `combines search, role and status filters`() {
        assertEquals(
            listOf("alice.nowak"),
            list("search" to "nowak", "role" to "USER", "status" to "ACTIVE").emails()
        )
        assertTrue(list("search" to "nowak", "role" to "ORGANISER").emails().isEmpty())
    }

    @Test
    fun `paginates results`() {
        val first = list("size" to "3", "page" to "0").andExpect(status().isOk)
        assertEquals(3, first.emails().size)
        assertTrue(first.body().contains("\"totalElements\":4"))
        assertTrue(first.body().contains("\"totalPages\":2"))

        val second = list("size" to "3", "page" to "1").andExpect(status().isOk)
        assertEquals(1, second.emails().size)

        val all = first.emails() + second.emails()
        assertEquals(4, all.toSet().size, "pages must not overlap")
    }

    @Test
    fun `sorts by the requested field and direction`() {
        assertEquals("dan_z", list("sort" to "lastName,desc").emails().first())
        assertEquals("alice.nowak", list("sort" to "email").emails().first())
        assertEquals("dan_z", list("sort" to "email,desc").emails().first())
        // roles ascending: GUARDIAN (bob), ORGANISER (carol), USER (alice, dan)
        assertEquals("bob.nowak", list("sort" to "role,asc").emails().first())
        // statuses ascending: ACTIVE, ACTIVE, BLOCKED, INACTIVE
        assertEquals("dan_z", list("sort" to "status,asc").emails().last())
    }

    @Test
    fun `rejects invalid parameters with 400`() {
        list("size" to "0").andExpect(status().isBadRequest)
        list("size" to "101").andExpect(status().isBadRequest)
        list("page" to "-1").andExpect(status().isBadRequest)
        list("page" to "abc").andExpect(status().isBadRequest)
        list("status" to "DELETED").andExpect(status().isBadRequest)
        list("sort" to "passwordHash").andExpect(status().isBadRequest)
        list("sort" to "lastName,sideways").andExpect(status().isBadRequest)
    }

    @Test
    fun `error response has a readable message`() {
        val body = list("sort" to "passwordHash").andExpect(status().isBadRequest).body()

        assertTrue(body.contains("\"status\":400"), body)
        assertTrue(body.contains("Cannot sort by 'passwordHash'"), body)
    }
}
