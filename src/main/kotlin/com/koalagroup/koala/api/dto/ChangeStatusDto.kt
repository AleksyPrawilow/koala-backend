package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.UserStatus
import jakarta.validation.constraints.NotNull

data class ChangeStatusDto(
    @field:NotNull(message = "Status is required.")
    var status: UserStatus?
)
