package kz.aqyldykundelik.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    data class ErrorBody(
        val timestamp: String = Instant.now().toString(),
        val status: Int,
        val error: String,
        val message: String?,
        val path: String? = null
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorBody> {
        val msg = e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        val body = ErrorBody(status = 400, error = "Bad Request", message = msg)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleIllegal(e: RuntimeException): ResponseEntity<ErrorBody> {
        val body = ErrorBody(status = 400, error = "Bad Request", message = e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ErrorBody> {
        val body = ErrorBody(status = 404, error = "Not Found", message = e.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(e: ResponseStatusException): ResponseEntity<ErrorBody> {
        val status = e.statusCode.value()
        val body = ErrorBody(
            status = status,
            error = e.reason ?: e.statusCode.toString(),
            message = e.reason
        )
        return ResponseEntity.status(e.statusCode).body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorBody> {
        val body = ErrorBody(status = 500, error = "Internal Server Error", message = e.message)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}
