package br.com.ohashi.postransactionservice.adapters.input.controllers.handlers

import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiErrorResponse
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiValidationError
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = exception.bindingResult.fieldErrors.map(::toValidationError)

        return ResponseEntity.badRequest().body(
            ApiErrorResponse(
                timestamp = OffsetDateTime.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.name,
                message = "Request validation failed.",
                path = request.requestURI,
                errors = errors
            )
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = exception.constraintViolations.map {
            ApiValidationError(
                field = it.propertyPath.toString(),
                message = it.message
            )
        }

        return ResponseEntity.badRequest().body(
            ApiErrorResponse(
                timestamp = OffsetDateTime.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.name,
                message = "Request validation failed.",
                path = request.requestURI,
                errors = errors
            )
        )
    }

    private fun toValidationError(fieldError: FieldError): ApiValidationError =
        ApiValidationError(
            field = fieldError.field,
            message = fieldError.defaultMessage ?: "Invalid value."
        )
}
