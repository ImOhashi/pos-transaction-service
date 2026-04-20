package br.com.ohashi.postransactionservice.adapters.input.controllers.handlers

import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiErrorResponse
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiValidationError
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import br.com.ohashi.postransactionservice.shared.exceptions.InvalidTransactionStateException
import br.com.ohashi.postransactionservice.shared.exceptions.TransactionNotFoundException
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
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(TransactionNotFoundException::class)
    fun handleTransactionNotFound(
        exception: TransactionNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Transaction lookup failed: {}", exception.message)

        return buildErrorResponse(
            status = HttpStatus.NOT_FOUND,
            message = exception.message ?: "Transaction not found.",
            request = request
        )
    }

    @ExceptionHandler(ExternalAuthorizationRejectedException::class)
    fun handleExternalAuthorizationRejected(
        exception: ExternalAuthorizationRejectedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        message = exception.message ?: "External authorization was rejected.",
        request = request
    )

    @ExceptionHandler(InvalidTransactionStateException::class)
    fun handleInvalidTransactionState(
        exception: InvalidTransactionStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        message = exception.message ?: "Transaction state transition is invalid.",
        request = request
    )

    @ExceptionHandler(RetryableException::class)
    fun handleExternalAuthorizationTimeout(
        exception: RetryableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val operation = resolveExternalOperation(request)
        logger.error("External $operation timed out after retries", exception)

        return buildErrorResponse(
            status = HttpStatus.GATEWAY_TIMEOUT,
            message = "External $operation timed out after retry attempts.",
            request = request
        )
    }

    @ExceptionHandler(CallNotPermittedException::class)
    fun handleExternalAuthorizationCircuitBreakerOpen(
        exception: CallNotPermittedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val operation = resolveExternalOperation(request)
        logger.error("External $operation circuit breaker is open", exception)

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External $operation is temporarily unavailable because the circuit breaker is open.",
            request = request
        )
    }

    @ExceptionHandler(BulkheadFullException::class)
    fun handleExternalAuthorizationBulkheadFull(
        exception: BulkheadFullException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val operation = resolveExternalOperation(request)
        logger.error("External $operation bulkhead limit reached", exception)

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External $operation is temporarily unavailable because the concurrency limit was reached.",
            request = request
        )
    }

    @ExceptionHandler(FeignException.FeignServerException::class)
    fun handleExternalAuthorizationServerFailure(
        exception: FeignException.FeignServerException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val operation = resolveExternalOperation(request)
        logger.error(
            "External {} failed with upstream server error status={}",
            operation,
            exception.status(),
            exception
        )

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External $operation failed due to upstream server error status=${exception.status()}.",
            request = request
        )
    }

    @ExceptionHandler(FeignException.FeignClientException::class)
    fun handleExternalAuthorizationClientFailure(
        exception: FeignException.FeignClientException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val operation = resolveExternalOperation(request)
        logger.error(
            "External {} failed with upstream client error status={}",
            operation,
            exception.status(),
            exception
        )

        return buildErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = "External $operation request was rejected by upstream with status=${exception.status()}.",
            request = request
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = exception.bindingResult.fieldErrors.map(::toValidationError) +
            exception.bindingResult.globalErrors.map(::toValidationError)

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

    private fun toValidationError(objectError: ObjectError): ApiValidationError =
        ApiValidationError(
            field = objectError.objectName,
            message = objectError.defaultMessage ?: "Invalid value."
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

    private fun resolveExternalOperation(request: HttpServletRequest): String =
        when {
            request.requestURI.contains("/confirm") -> "confirmation"
            request.requestURI.contains("/void") -> "void"
            else -> "authorization"
        }
}
