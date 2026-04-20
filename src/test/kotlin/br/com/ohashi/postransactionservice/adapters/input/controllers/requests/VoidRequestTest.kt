package br.com.ohashi.postransactionservice.adapters.input.controllers.requests

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import jakarta.validation.Validation
import org.junit.jupiter.api.Test

class VoidRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `should accept transaction id as the only identifier`() {
        val request = VoidRequest(transactionId = "txn-1")

        assertThat(request.hasValidIdentification).isEqualTo(true)
        assertThat(request.toCommand().transactionId).isEqualTo("txn-1")
        assertThat(request.toCommand().nsu).isEqualTo(null)
        assertThat(request.toCommand().terminalId).isEqualTo(null)
        assertThat(validator.validate(request)).hasSize(0)
    }

    @Test
    fun `should accept nsu and terminal id as identifiers`() {
        val request = VoidRequest(nsu = "nsu-1", terminalId = "terminal-1")

        assertThat(request.hasValidIdentification).isEqualTo(true)
        assertThat(request.toCommand().transactionId).isEqualTo(null)
        assertThat(request.toCommand().nsu).isEqualTo("nsu-1")
        assertThat(request.toCommand().terminalId).isEqualTo("terminal-1")
        assertThat(validator.validate(request)).hasSize(0)
    }

    @Test
    fun `should reject request without identifiers`() {
        val request = VoidRequest()

        assertThat(request.hasValidIdentification).isEqualTo(false)

        val violations = validator.validate(request).toList()

        assertThat(violations).hasSize(1)
        assertThat(violations.first().messageTemplate).isEqualTo("{message.void-request.identification.invalid}")
    }

    @Test
    fun `should reject request with mixed identifiers`() {
        val request = VoidRequest(
            transactionId = "txn-1",
            nsu = "nsu-1",
            terminalId = "terminal-1"
        )

        assertThat(request.hasValidIdentification).isEqualTo(false)

        val violations = validator.validate(request).toList()

        assertThat(violations).hasSize(1)
        assertThat(violations.first().messageTemplate).isEqualTo("{message.void-request.identification.invalid}")
    }

    @Test
    fun `should reject request with blank identifiers`() {
        val request = VoidRequest(
            transactionId = " ",
            nsu = "",
            terminalId = " "
        )

        assertThat(request.hasValidIdentification).isEqualTo(false)

        val command = request.toCommand()
        assertThat(command.transactionId).isEqualTo(null)
        assertThat(command.nsu).isEqualTo(null)
        assertThat(command.terminalId).isEqualTo(null)

        val violations = validator.validate(request).toList()

        assertThat(violations).hasSize(1)
        assertThat(violations.first().messageTemplate).isEqualTo("{message.void-request.identification.invalid}")
    }
}
