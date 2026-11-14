package com.koalagroup.koala.domain.entities

enum class UserStatus {
    /** Normal account - the default for every new user. */
    ACTIVE,

    /** Temporarily blocked by an administrator. Reversible - unblocking restores ACTIVE. */
    BLOCKED,

    /** Deactivated account (soft delete). The row and related data are kept for consistency. */
    INACTIVE
}
