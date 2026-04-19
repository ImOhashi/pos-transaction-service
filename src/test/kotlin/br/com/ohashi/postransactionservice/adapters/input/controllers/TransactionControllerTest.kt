package br.com.ohashi.postransactionservice.adapters.input.controllers

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.AuthorizeRequest
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class TransactionControllerTest {

    @MockK
    private lateinit var authorizeTransactionInputPort: AuthorizeTransactionInputPort

    @Test
    fun `should call use case and return response body`() {
        val commandSlot = slot<AuthorizeTransactionCommand>()
        every {
            authorizeTransactionInputPort.authorize(capture(commandSlot))
        } returns AuthorizeTransactionResult(
            nsu = "nsu-1",
            amount = BigDecimal("11.00"),
            terminalId = "terminal-1",
            transactionId = "txn-1"
        )

        val response = TransactionController(authorizeTransactionInputPort).authorize(
            AuthorizeRequest(
                nsu = "nsu-1",
                amount = BigDecimal("11.00"),
                terminalId = "terminal-1"
            )
        )

        assertThat(commandSlot.captured.nsu).isEqualTo("nsu-1")
        assertThat(commandSlot.captured.amount).isEqualTo(BigDecimal("11.00"))
        assertThat(commandSlot.captured.terminalId).isEqualTo("terminal-1")
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body?.nsu).isEqualTo("nsu-1")
        assertThat(response.body?.amount).isEqualTo(BigDecimal("11.00"))
        assertThat(response.body?.terminalId).isEqualTo("terminal-1")
        assertThat(response.body?.transactionId).isEqualTo("txn-1")
        verify(exactly = 1) { authorizeTransactionInputPort.authorize(any()) }
    }
}
