package com.koalagroup.koala.api

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

/** Resolves the user performing the current request. */
@Component
class CurrentActor {
    /**
     * Id of the authenticated user (the JWT subject is expected to be the user id), or null when nobody is
     * authenticated.
     */
    fun id(): UUID? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (authentication is AnonymousAuthenticationToken || !authentication.isAuthenticated) return null
        return runCatching { UUID.fromString(authentication.name) }.getOrNull()
    }
}
