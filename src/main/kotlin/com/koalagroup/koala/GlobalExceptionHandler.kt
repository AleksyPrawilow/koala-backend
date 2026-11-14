package com.koalagroup.koala

import com.koalagroup.koala.domain.exceptions.ForbiddenOperationException
import com.koalagroup.koala.domain.exceptions.InvalidQueryException
import com.koalagroup.koala.domain.exceptions.InvalidRoleException
import com.koalagroup.koala.domain.exceptions.LastAdminException
import com.koalagroup.koala.domain.exceptions.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }

        return ResponseEntity.badRequest().body(
            mapOf(
                "status" to 400,
                "error" to "Validation Failed",
                "fields" to fieldErrors
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.badRequest().body(
            mapOf(
                "status" to 400,
                "error" to "Bad Request",
                "message" to "Request body is missing or malformed JSON."
            )
        )
    }

    @ExceptionHandler(InvalidQueryException::class)
    fun handleInvalidQuery(ex: InvalidQueryException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.badRequest().body(
            mapOf(
                "status" to 400,
                "error" to "Bad Request",
                "message" to (ex.message ?: "Invalid query.")
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleParameterTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.badRequest().body(
            mapOf(
                "status" to 400,
                "error" to "Bad Request",
                "message" to "Invalid value '${ex.value}' for parameter '${ex.name}'."
            )
        )
    }

    @ExceptionHandler(InvalidRoleException::class)
    fun handleInvalidRole(ex: InvalidRoleException): ResponseEntity<Map<String, Any>> =
        error(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<Map<String, Any>> =
        error(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbiddenOperation(ex: ForbiddenOperationException): ResponseEntity<Map<String, Any>> =
        error(HttpStatus.FORBIDDEN, ex.message)

    @ExceptionHandler(LastAdminException::class)
    fun handleLastAdmin(ex: LastAdminException): ResponseEntity<Map<String, Any>> =
        error(HttpStatus.CONFLICT, ex.message)

    private fun error(status: HttpStatus, message: String?): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(status).body(
            mapOf(
                "status" to status.value(),
                "error" to status.reasonPhrase,
                "message" to (message ?: status.reasonPhrase)
            )
        )
    }
}