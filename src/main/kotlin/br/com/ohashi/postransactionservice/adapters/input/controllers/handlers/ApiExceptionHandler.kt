package br.com.ohashi.postransactionservice.adapters.input.controllers.handlers

import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiErrorResponse
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiValidationError
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import feign.FeignException
import feign.RetryableException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ExternalAuthorizationRejectedException::class)
    fun handleExternalAuthorizationRejected(
        exception: ExternalAuthorizationRejectedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        message = exception.message ?: "External authorization was rejected.",
        request = request
    )

    @ExceptionHandler(RetryableException::class)
    fun handleExternalAuthorizationTimeout(
        exception: RetryableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("External authorization timed out after retries", exception)

        return buildErrorResponse(
            status = HttpStatus.GATEWAY_TIMEOUT,
            message = "External authorization timed out after retry attempts.",
            request = request
        )
    }

    @ExceptionHandler(CallNotPermittedException::class)
    fun handleExternalAuthorizationCircuitBreakerOpen(
        exception: CallNotPermittedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("External authorization circuit breaker is open", exception)

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External authorization is temporarily unavailable because the circuit breaker is open.",
            request = request
        )
    }

    @ExceptionHandler(BulkheadFullException::class)
    fun handleExternalAuthorizationBulkheadFull(
        exception: BulkheadFullException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("External authorization bulkhead limit reached", exception)

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External authorization is temporarily unavailable because the concurrency limit was reached.",
            request = request
        )
    }

    @ExceptionHandler(FeignException.FeignServerException::class)
    fun handleExternalAuthorizationServerFailure(
        exception: FeignException.FeignServerException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.error(
            "External authorization failed with upstream server error status={}",
            exception.status(),
            exception
        )

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External authorization failed due to upstream server error status=${exception.status()}.",
            request = request
        )
    }

    @ExceptionHandler(FeignException.FeignClientException::class)
    fun handleExternalAuthorizationClientFailure(
        exception: FeignException.FeignClientException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.error(
            "External authorization failed with upstream client error status={}",
            exception.status(),
            exception
        )

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External authorization request was rejected by upstream with status=${exception.status()}.",
            request = request
        )
    }

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

    private fun buildErrorResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(
            ApiErrorResponse(
                timestamp = OffsetDateTime.now(),
                status = status.value(),
                error = status.name,
                message = message,
                path = request.requestURI,
                errors = emptyList()
            )
        )
}
