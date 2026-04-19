package br.com.ohashi.postransactionservice.adapters.input.controllers.responses

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiErrorResponse
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error.ApiValidationError
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class ResponseModelsCoverageTest {

    @Test
    fun `should expose authorize response fields`() {
        val response = AuthorizeResponse(
            AuthorizeTransactionResult(
                nsu = "nsu-1",
                amount = BigDecimal("10.00"),
                terminalId = "terminal-1",
                transactionId = "txn-1"
            )
        )

        assertThat(response.nsu).isEqualTo("nsu-1")
        assertThat(response.amount).isEqualTo(BigDecimal("10.00"))
        assertThat(response.terminalId).isEqualTo("terminal-1")
        assertThat(response.transactionId).isEqualTo("txn-1")
    }

    @Test
    fun `should expose api error response fields`() {
        val timestamp = OffsetDateTime.parse("2026-04-18T16:30:00-03:00")
        val validationError = ApiValidationError(field = "nsu", message = "required")
        val response = ApiErrorResponse(
            timestamp = timestamp,
            status = 400,
            error = "BAD_REQUEST",
            message = "invalid request",
            path = "/v1/pos/transactions/authorize",
            errors = listOf(validationError)
        )

        assertThat(response.timestamp).isEqualTo(timestamp)
        assertThat(response.status).isEqualTo(400)
        assertThat(response.error).isEqualTo("BAD_REQUEST")
        assertThat(response.message).isEqualTo("invalid request")
        assertThat(response.path).isEqualTo("/v1/pos/transactions/authorize")
        assertThat(response.errors.first().field).isEqualTo("nsu")
        assertThat(response.errors.first().message).isEqualTo("required")
    }
}
