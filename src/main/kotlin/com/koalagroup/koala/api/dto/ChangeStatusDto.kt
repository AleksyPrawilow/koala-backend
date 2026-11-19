package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class ChangeStatusDto(
    @field:NotNull(message = "Status is required.")
    @field:Schema(description = "New account status. BLOCKED is a reversible block, INACTIVE is a deactivation (soft delete), ACTIVE unblocks or reactivates the account.", example = "BLOCKED")
    var status: UserStatus?
)
