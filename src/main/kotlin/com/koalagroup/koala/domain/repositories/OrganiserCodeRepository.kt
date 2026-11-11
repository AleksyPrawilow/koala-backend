package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.OrganiserCode

interface OrganiserCodeRepository {
    fun useCode(code: String): OrganiserCode?
}