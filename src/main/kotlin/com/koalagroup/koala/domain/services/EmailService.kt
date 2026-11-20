package com.koalagroup.koala.domain.services

interface EmailService {
    fun sendEmail(to: String, subject: String, body: String)
}
