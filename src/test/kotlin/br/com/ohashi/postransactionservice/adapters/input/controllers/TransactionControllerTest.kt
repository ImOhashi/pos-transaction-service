package br.com.ohashi.postransactionservice.adapters.input.controllers

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.AuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.ConfirmRequest
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.VoidRequest
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionInputPort
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class TransactionControllerTest {

    @MockK
    private lateinit var authorizeTransactionInputPort: AuthorizeTransactionInputPort

    @MockK
    private lateinit var confirmTransactionInputPort: ConfirmTransactionInputPort

    @MockK
    private lateinit var voidTransactionInputPort: VoidTransactionInputPort

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

        val response = TransactionController(
            authorizeTransactionInputPort,
            confirmTransactionInputPort,
            voidTransactionInputPort
        ).authorize(
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

    @Test
    fun `should call confirm use case and return no content`() {
        val commandSlot = slot<ConfirmTransactionCommand>()
        every {
            confirmTransactionInputPort.confirm(capture(commandSlot))
        } just runs

        val response = TransactionController(
            authorizeTransactionInputPort,
            confirmTransactionInputPort,
            voidTransactionInputPort
        ).confirm(
            ConfirmRequest(transactionId = "txn-1")
        )

        assertThat(commandSlot.captured.transactionId).isEqualTo("txn-1")
        assertThat(response.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { confirmTransactionInputPort.confirm(any()) }
    }

    @Test
    fun `should call void use case by transaction id and return no content`() {
        val commandSlot = slot<VoidTransactionCommand>()
        every {
            voidTransactionInputPort.voidTransaction(capture(commandSlot))
        } just runs

        val response = TransactionController(
            authorizeTransactionInputPort,
            confirmTransactionInputPort,
            voidTransactionInputPort
        ).voidTransaction(
            VoidRequest(transactionId = "txn-1")
        )

        assertThat(commandSlot.captured.transactionId).isEqualTo("txn-1")
        assertThat(commandSlot.captured.nsu).isEqualTo(null)
        assertThat(commandSlot.captured.terminalId).isEqualTo(null)
        assertThat(response.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { voidTransactionInputPort.voidTransaction(any()) }
    }

    @Test
    fun `should call void use case by nsu and terminal id and return no content`() {
        val commandSlot = slot<VoidTransactionCommand>()
        every {
            voidTransactionInputPort.voidTransaction(capture(commandSlot))
        } just runs

        val response = TransactionController(
            authorizeTransactionInputPort,
            confirmTransactionInputPort,
            voidTransactionInputPort
        ).voidTransaction(
            VoidRequest(nsu = "nsu-1", terminalId = "terminal-1")
        )

        assertThat(commandSlot.captured.transactionId).isEqualTo(null)
        assertThat(commandSlot.captured.nsu).isEqualTo("nsu-1")
        assertThat(commandSlot.captured.terminalId).isEqualTo("terminal-1")
        assertThat(response.statusCode.value()).isEqualTo(204)
        verify(exactly = 1) { voidTransactionInputPort.voidTransaction(any()) }
    }
}
