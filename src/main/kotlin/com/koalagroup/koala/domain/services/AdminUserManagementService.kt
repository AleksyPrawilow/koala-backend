package com.koalagroup.koala.domain.services

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.exceptions.ForbiddenOperationException
import com.koalagroup.koala.domain.exceptions.InvalidRoleException
import com.koalagroup.koala.domain.exceptions.LastAdminException
import com.koalagroup.koala.domain.exceptions.UserNotFoundException
import com.koalagroup.koala.domain.repositories.AuditLogRepository
import com.koalagroup.koala.domain.repositories.RoleRepository
import com.koalagroup.koala.domain.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Admin operations on existing accounts. Every change is saved together with its audit log entry
 * in one transaction. [actorId] is the administrator performing the action (null while there is
 * no authentication yet).
 */
@Service
class AdminUserManagementService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val auditLogRepository: AuditLogRepository
) {
    @Transactional
    fun changeRole(actorId: UUID?, userId: UUID, roleName: String): User {
        val user = getUser(userId)
        val newRole = roleRepository.findByName(roleName.trim().uppercase())
            ?: throw InvalidRoleException("Role '$roleName' does not exist.")

        if (actorId == userId) throw ForbiddenOperationException("Administrators cannot change their own role.")
        if (user.role.id == newRole.id) return user
        if (user.role.name == Role.ADMIN && user.status == UserStatus.ACTIVE) ensureAnotherActiveAdmin()

        val updated = userRepository.update(user.copy(role = newRole))
        auditLogRepository.create(
            AuditLog(
                actorId = actorId,
                targetUserId = userId,
                action = AuditAction.ROLE_CHANGED,
                oldValue = mapOf("role" to user.role.name),
                newValue = mapOf("role" to newRole.name)
            )
        )
        return updated
    }

    /** Covers blocking, unblocking, deactivating and reactivating an account. */
    @Transactional
    fun changeStatus(actorId: UUID?, userId: UUID, status: UserStatus): User {
        val user = getUser(userId)

        if (actorId == userId) throw ForbiddenOperationException("Administrators cannot change their own status.")
        if (user.status == status) return user
        if (user.role.name == Role.ADMIN && user.status == UserStatus.ACTIVE) ensureAnotherActiveAdmin()

        val updated = userRepository.update(user.copy(status = status))
        auditLogRepository.create(
            AuditLog(
                actorId = actorId,
                targetUserId = userId,
                action = AuditAction.STATUS_CHANGED,
                oldValue = mapOf("status" to user.status.name),
                newValue = mapOf("status" to status.name)
            )
        )
        return updated
    }

    /** "Deleting" an account is a soft delete: the data is kept and the status becomes INACTIVE. */
    @Transactional
    fun deactivate(actorId: UUID?, userId: UUID): User = changeStatus(actorId, userId, UserStatus.INACTIVE)

    private fun getUser(userId: UUID): User =
        userRepository.findById(userId) ?: throw UserNotFoundException("User $userId was not found.")

    /** The affected account is an active administrator, so at least one other has to remain. */
    private fun ensureAnotherActiveAdmin() {
        if (userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw LastAdminException("This is the last active administrator and cannot be demoted, blocked or deactivated.")
        }
    }
}
