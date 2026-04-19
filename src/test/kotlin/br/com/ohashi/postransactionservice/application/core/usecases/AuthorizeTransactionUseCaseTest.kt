package br.com.ohashi.postransactionservice.application.core.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizationStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockKExtension::class)
class AuthorizeTransactionUseCaseTest {

    @MockK
    private lateinit var findTransactionByNsuAndTerminalIdOutputPort: FindTransactionByNsuAndTerminalIdOutputPort

    @MockK
    private lateinit var authorizeTransactionExternallyOutputPort: AuthorizeTransactionExternallyOutputPort

    @RelaxedMockK
    private lateinit var saveTransactionOutputPort: SaveTransactionOutputPort

    private lateinit var useCase: AuthorizeTransactionUseCase

    @BeforeEach
    fun setUp() {
        clearMocks(
            findTransactionByNsuAndTerminalIdOutputPort,
            authorizeTransactionExternallyOutputPort,
            saveTransactionOutputPort
        )
        useCase = AuthorizeTransactionUseCase(
            findTransactionByNsuAndTerminalIdOutputPort = findTransactionByNsuAndTerminalIdOutputPort,
            authorizeTransactionExternallyOutputPort = authorizeTransactionExternallyOutputPort,
            saveTransactionOutputPort = saveTransactionOutputPort
        )
    }

    @Test
    fun `should return existing transaction without external authorization or persistence`() {
        val existingTransaction = Transaction(
            transactionId = "txn-123",
            terminalId = "terminal-1",
            nsu = "nsu-1",
            amount = BigDecimal("10.00"),
            status = TransactionStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-04-18T10:15:30Z")
        )
        every {
            findTransactionByNsuAndTerminalIdOutputPort.find("nsu-1", "terminal-1")
        } returns existingTransaction

        val result = useCase.authorize(
            AuthorizeTransactionCommand(
                terminalId = "terminal-1",
                nsu = "nsu-1",
                amount = BigDecimal("10.00")
            )
        )

        assertThat(result.transactionId).isEqualTo("txn-123")
        assertThat(result.amount).isEqualTo(BigDecimal("10.00"))

        verify(exactly = 1) { findTransactionByNsuAndTerminalIdOutputPort.find("nsu-1", "terminal-1") }
        verify(exactly = 0) { authorizeTransactionExternallyOutputPort.authorize(any()) }
        verify(exactly = 0) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should persist transaction after external authorization`() {
        val requestSlot = slot<AuthorizeTransactionExternalRequest>()
        val transactionSlot = slot<Transaction>()
        every {
            findTransactionByNsuAndTerminalIdOutputPort.find("nsu-2", "terminal-2")
        } returns null
        every {
            authorizeTransactionExternallyOutputPort.authorize(capture(requestSlot))
        } returns AuthorizeTransactionExternalResult(
            transactionId = "txn-999",
            result = AuthorizationStatus.AUTHORIZED,
            approved = true,
            message = "approved"
        )
        every {
            saveTransactionOutputPort.save(capture(transactionSlot))
        } answers { firstArg() }

        val result = useCase.authorize(
            AuthorizeTransactionCommand(
                terminalId = "terminal-2",
                nsu = "nsu-2",
                amount = BigDecimal("25.50")
            )
        )

        assertThat(requestSlot.captured.terminalId).isEqualTo("terminal-2")
        assertThat(requestSlot.captured.nsu).isEqualTo("nsu-2")
        assertThat(requestSlot.captured.amount).isEqualTo(BigDecimal("25.50"))
        assertThat(transactionSlot.captured.transactionId).isEqualTo("txn-999")
        assertThat(transactionSlot.captured.status).isEqualTo(TransactionStatus.AUTHORIZED)
        assertThat(transactionSlot.captured.createdAt).isNotNull()
        assertThat(result.transactionId).isEqualTo("txn-999")
        assertThat(result.terminalId).isEqualTo("terminal-2")

        verify(exactly = 1) { authorizeTransactionExternallyOutputPort.authorize(any()) }
        verify(exactly = 1) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should throw when external authorization is not authorized`() {
        every {
            findTransactionByNsuAndTerminalIdOutputPort.find("nsu-3", "terminal-3")
        } returns null
        every {
            authorizeTransactionExternallyOutputPort.authorize(any())
        } returns AuthorizeTransactionExternalResult(
            transactionId = "txn-1000",
            result = AuthorizationStatus.NON_AUTHORIZED,
            approved = false,
            message = "denied"
        )

        val exception = runCatching {
            useCase.authorize(
                AuthorizeTransactionCommand(
                    terminalId = "terminal-3",
                    nsu = "nsu-3",
                    amount = BigDecimal("30.00")
                )
            )
        }.exceptionOrNull() ?: error("Expected exception")

        assertThat(exception).isInstanceOf(ExternalAuthorizationRejectedException::class)
        assertThat(exception.message).isEqualTo("External authorization was rejected with result=NON_AUTHORIZED.")
        verify(exactly = 1) { authorizeTransactionExternallyOutputPort.authorize(any()) }
        verify(exactly = 0) { saveTransactionOutputPort.save(any()) }
    }
}
