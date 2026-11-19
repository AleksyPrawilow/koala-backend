package com.koalagroup.koala.infrastructure.mail

import com.koalagroup.koala.domain.services.EmailService
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SmtpEmailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties
) : EmailService {

    private val logger = LoggerFactory.getLogger(SmtpEmailService::class.java)

    @Async
    override fun sendEmail(to: String, subject: String, body: String) {
        if (!mailProperties.enabled) {
            logger.info("Mail sending is disabled. Skipping email to='{}', subject='{}'", to, subject)
            return
        }

        val message = SimpleMailMessage().apply {
            from = mailProperties.from
            setTo(to)
            this.subject = subject
            text = body
        }

        try {
            mailSender.send(message)
            logger.info("Sent email to='{}', subject='{}'", to, subject)
        } catch (ex: MailException) {
            logger.error("Failed to send email to='{}', subject='{}': {}", to, subject, ex.message, ex)
        }
    }
}
