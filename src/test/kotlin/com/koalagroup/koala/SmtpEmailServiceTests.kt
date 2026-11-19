package com.koalagroup.koala

import com.koalagroup.koala.infrastructure.mail.MailProperties
import com.koalagroup.koala.infrastructure.mail.SmtpEmailService
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.io.InputStream

class SmtpEmailServiceTests {

    private class RecordingMailSender(
        private val shouldThrow: Boolean = false
    ) : JavaMailSender {
        val sentMessages = mutableListOf<SimpleMailMessage>()

        override fun send(simpleMessage: SimpleMailMessage) {
            if (shouldThrow) {
                throw MailSendException("Simulated SMTP failure")
            }
            sentMessages.add(simpleMessage)
        }

        override fun send(vararg simpleMessages: SimpleMailMessage) {
            simpleMessages.forEach { send(it) }
        }

        override fun createMimeMessage(): MimeMessage = throw UnsupportedOperationException()
        override fun createMimeMessage(contentStream: InputStream): MimeMessage = throw UnsupportedOperationException()
        override fun send(mimeMessage: MimeMessage) = throw UnsupportedOperationException()
        override fun send(vararg mimeMessages: MimeMessage) = throw UnsupportedOperationException()
    }

    @Test
    fun `sends simple mail message when enabled`() {
        val mailSender = RecordingMailSender()
        val properties = MailProperties(enabled = true, from = "no-reply@koala.local")
        val service = SmtpEmailService(mailSender, properties)

        service.sendEmail("student@example.com", "Witaj w KOALA", "Twoje konto zostało utworzone.")

        assertEquals(1, mailSender.sentMessages.size)
        val sent = mailSender.sentMessages.single()
        assertEquals("no-reply@koala.local", sent.from)
        assertArrayEquals(arrayOf("student@example.com"), sent.to)
        assertEquals("Witaj w KOALA", sent.subject)
        assertEquals("Twoje konto zostało utworzone.", sent.text)
    }

    @Test
    fun `skips sending and does not invoke JavaMailSender when disabled`() {
        val mailSender = RecordingMailSender(shouldThrow = true)
        val properties = MailProperties(enabled = false, from = "no-reply@koala.local")
        val service = SmtpEmailService(mailSender, properties)

        assertDoesNotThrow {
            service.sendEmail("student@example.com", "Witaj w KOALA", "Treść")
        }

        assertTrue(mailSender.sentMessages.isEmpty())
    }

    @Test
    fun `catches MailException without rethrowing`() {
        val mailSender = RecordingMailSender(shouldThrow = true)
        val properties = MailProperties(enabled = true, from = "no-reply@koala.local")
        val service = SmtpEmailService(mailSender, properties)

        assertDoesNotThrow {
            service.sendEmail("student@example.com", "Witaj w KOALA", "Treść")
        }

        assertTrue(mailSender.sentMessages.isEmpty())
    }
}
