package br.com.ohashi.postransactionservice.adapters.input.controllers.handlers

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import feign.RetryableException
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.core.MethodParameter
import java.net.URI
import java.util.Date

@ExtendWith(MockKExtension::class)
class ApiExceptionHandlerTest {

    @MockK
    private lateinit var constraintViolation: ConstraintViolation<Any>

    @MockK
    private lateinit var circuitBreaker: CircuitBreaker

    @MockK
    private lateinit var bulkhead: Bulkhead

    private val handler = ApiExceptionHandler()
    private val request = MockHttpServletRequest().apply { requestURI = "/v1/pos/transactions/authorize" }

    @Test
    fun `should map external authorization rejection to unprocessable entity`() {
        val response = handler.handleExternalAuthorizationRejected(
            ExternalAuthorizationRejectedException("rejected"),
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(422)
        assertThat(response.body?.message).isEqualTo("rejected")
        assertThat(response.body?.status).isEqualTo(422)
        assertThat(response.body?.error).isEqualTo("UNPROCESSABLE_ENTITY")
        assertThat(response.body?.path).isEqualTo("/v1/pos/transactions/authorize")
    }

    @Test
    fun `should use default rejection message when exception message is null`() {
        val exception = ExternalAuthorizationRejectedException("temporary")
        ExternalAuthorizationRejectedException::class.java.getDeclaredField("message")
            .apply { isAccessible = true }
            .set(exception, null)

        val response = handler.handleExternalAuthorizationRejected(
            exception,
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(422)
        assertThat(response.body?.message).isEqualTo("External authorization was rejected.")
    }

    @Test
    fun `should map retryable exception to gateway timeout`() {
        val exception = RetryableException(
            504,
            "timeout",
            Request.HttpMethod.POST,
            Date(),
            Request.create(
                Request.HttpMethod.POST,
                "http://localhost/external/transactions/authorize",
                emptyMap(),
                null,
                RequestTemplate()
            )
        )

        val response = handler.handleExternalAuthorizationTimeout(exception, request)

        assertThat(response.statusCode.value()).isEqualTo(504)
        assertThat(response.body?.message ?: "").contains("timed out")
    }

    @Test
    fun `should map circuit breaker open to service unavailable`() {
        every { circuitBreaker.name } returns "externalAuthorize"
        every { circuitBreaker.state } returns CircuitBreaker.State.OPEN
        every { circuitBreaker.circuitBreakerConfig } returns CircuitBreakerConfig.ofDefaults()

        val response = handler.handleExternalAuthorizationCircuitBreakerOpen(
            CallNotPermittedException.createCallNotPermittedException(circuitBreaker),
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(503)
    }

    @Test
    fun `should map bulkhead full to service unavailable`() {
        every { bulkhead.name } returns "externalAuthorize"
        every { bulkhead.bulkheadConfig } returns BulkheadConfig.custom().build()

        val response = handler.handleExternalAuthorizationBulkheadFull(
            BulkheadFullException.createBulkheadFullException(bulkhead),
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(503)
    }

    @Test
    fun `should map feign server exception to service unavailable`() {
        val response = handler.handleExternalAuthorizationServerFailure(
            FeignException.errorStatus(
                "authorize",
                feign.Response.builder()
                    .status(502)
                    .reason("bad gateway")
                    .request(
                        Request.create(
                            Request.HttpMethod.POST,
                            URI("http://localhost").toString(),
                            emptyMap(),
                            null,
                            RequestTemplate()
                        )
                    )
                    .build()
            ) as FeignException.FeignServerException,
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(503)
        assertThat(response.body?.message ?: "").contains("status=502")
    }

    @Test
    fun `should map feign client exception to service unavailable`() {
        val response = handler.handleExternalAuthorizationClientFailure(
            FeignException.errorStatus(
                "authorize",
                feign.Response.builder()
                    .status(400)
                    .reason("bad request")
                    .request(
                        Request.create(
                            Request.HttpMethod.POST,
                            URI("http://localhost").toString(),
                            emptyMap(),
                            null,
                            RequestTemplate()
                        )
                    )
                    .build()
            ) as FeignException.FeignClientException,
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(503)
        assertThat(response.body?.message ?: "").contains("status=400")
    }

    @Test
    fun `should map method argument validation error to bad request`() {
        val target = ValidationTarget()
        val bindingResult = BeanPropertyBindingResult(target, "validationTarget")
        bindingResult.addError(FieldError("validationTarget", "nsu", "must not be blank"))
        val exception = MethodArgumentNotValidException(
            MethodParameter(ValidationTargetController::class.java.getMethod("save", ValidationTarget::class.java), 0),
            bindingResult
        )

        val response = handler.handleMethodArgumentNotValid(exception, request)

        assertThat(response.statusCode.value()).isEqualTo(400)
        assertThat(response.body?.errors ?: emptyList()).hasSize(1)
        assertThat(response.body?.errors?.first()?.field).isEqualTo("nsu")
        assertThat(response.body?.errors?.first()?.message).isEqualTo("must not be blank")
    }

    @Test
    fun `should use default validation message when field error has no default message`() {
        val target = ValidationTarget()
        val bindingResult = BeanPropertyBindingResult(target, "validationTarget")
        bindingResult.addError(
            FieldError(
                "validationTarget",
                "nsu",
                null,
                false,
                emptyArray(),
                emptyArray(),
                null
            )
        )
        val exception = MethodArgumentNotValidException(
            MethodParameter(ValidationTargetController::class.java.getMethod("save", ValidationTarget::class.java), 0),
            bindingResult
        )

        val response = handler.handleMethodArgumentNotValid(exception, request)

        assertThat(response.statusCode.value()).isEqualTo(400)
        assertThat(response.body?.errors?.first()?.message).isEqualTo("Invalid value.")
    }

    @Test
    fun `should map constraint violation to bad request`() {
        every { constraintViolation.propertyPath.toString() } returns "authorize.nsu"
        every { constraintViolation.message } returns "must not be blank"

        val response = handler.handleConstraintViolation(
            ConstraintViolationException(setOf(constraintViolation)),
            request
        )

        assertThat(response.statusCode.value()).isEqualTo(400)
        assertThat(response.body?.errors ?: emptyList()).hasSize(1)
        assertThat(response.body?.errors?.first()?.field).isEqualTo("authorize.nsu")
        assertThat(response.body?.errors?.first()?.message).isEqualTo("must not be blank")
    }

    class ValidationTarget

    class ValidationTargetController {
        fun save(target: ValidationTarget) = target
    }
}
