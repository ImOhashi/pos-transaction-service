package br.com.ohashi.postransactionservice.application.core.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByTransactionIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.VoidTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.VoidTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.VoidStatus
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import br.com.ohashi.postransactionservice.shared.exceptions.TransactionNotFoundException
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
class VoidTransactionUseCaseTest {

    @MockK
    private lateinit var findTransactionByTransactionIdOutputPort: FindTransactionByTransactionIdOutputPort

    @MockK
    private lateinit var findTransactionByNsuAndTerminalIdOutputPort: FindTransactionByNsuAndTerminalIdOutputPort

    @MockK
    private lateinit var voidTransactionExternallyOutputPort: VoidTransactionExternallyOutputPort

    @RelaxedMockK
    private lateinit var saveTransactionOutputPort: SaveTransactionOutputPort

    private lateinit var useCase: VoidTransactionUseCase

    @BeforeEach
    fun setUp() {
        clearMocks(
            findTransactionByTransactionIdOutputPort,
            findTransactionByNsuAndTerminalIdOutputPort,
            voidTransactionExternallyOutputPort,
            saveTransactionOutputPort
        )
        useCase = VoidTransactionUseCase(
            findTransactionByTransactionIdOutputPort = findTransactionByTransactionIdOutputPort,
            findTransactionByNsuAndTerminalIdOutputPort = findTransactionByNsuAndTerminalIdOutputPort,
            voidTransactionExternallyOutputPort = voidTransactionExternallyOutputPort,
            saveTransactionOutputPort = saveTransactionOutputPort
        )
    }

    @Test
    fun `should return idempotent success when transaction is already voided`() {
        every {
            findTransactionByTransactionIdOutputPort.find("txn-1")
        } returns transaction(transactionId = "txn-1", status = TransactionStatus.VOIDED)

        useCase.voidTransaction(VoidTransactionCommand(transactionId = "txn-1"))

        verify(exactly = 1) { findTransactionByTransactionIdOutputPort.find("txn-1") }
        verify(exactly = 0) { voidTransactionExternallyOutputPort.voidTransaction(any()) }
        verify(exactly = 0) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should void externally and persist transaction as voided by transaction id`() {
        val requestSlot = slot<VoidTransactionExternalRequest>()
        val transactionSlot = slot<Transaction>()
        every {
            findTransactionByTransactionIdOutputPort.find("txn-2")
        } returns transaction(transactionId = "txn-2", status = TransactionStatus.CONFIRMED)
        every {
            voidTransactionExternallyOutputPort.voidTransaction(capture(requestSlot))
        } returns VoidStatus.VOIDED
        every {
            saveTransactionOutputPort.save(capture(transactionSlot))
        } answers { firstArg() }

        useCase.voidTransaction(VoidTransactionCommand(transactionId = "txn-2"))

        assertThat(requestSlot.captured.transactionId).isEqualTo("txn-2")
        assertThat(transactionSlot.captured.transactionId).isEqualTo("txn-2")
        assertThat(transactionSlot.captured.status).isEqualTo(TransactionStatus.VOIDED)
        verify(exactly = 1) { voidTransactionExternallyOutputPort.voidTransaction(any()) }
        verify(exactly = 1) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should void transaction found by nsu and terminal id`() {
        every {
            findTransactionByNsuAndTerminalIdOutputPort.find("nsu-1", "terminal-1")
        } returns transaction(transactionId = "txn-3", status = TransactionStatus.CONFIRMED)
        every {
            voidTransactionExternallyOutputPort.voidTransaction(any())
        } returns VoidStatus.ALREADY_VOIDED
        every {
            saveTransactionOutputPort.save(any())
        } answers { firstArg() }

        useCase.voidTransaction(
            VoidTransactionCommand(
                nsu = "nsu-1",
                terminalId = "terminal-1"
            )
        )

        verify(exactly = 1) { findTransactionByNsuAndTerminalIdOutputPort.find("nsu-1", "terminal-1") }
        verify(exactly = 1) {
            voidTransactionExternallyOutputPort.voidTransaction(
                VoidTransactionExternalRequest(transactionId = "txn-3")
            )
        }
        verify(exactly = 1) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should throw when transaction is not found by nsu and terminal id`() {
        every {
            findTransactionByNsuAndTerminalIdOutputPort.find("nsu-404", "terminal-404")
        } returns null

        val exception = kotlin.runCatching {
            useCase.voidTransaction(
                VoidTransactionCommand(
                    nsu = "nsu-404",
                    terminalId = "terminal-404"
                )
            )
        }.exceptionOrNull()

        assertThat(exception is TransactionNotFoundException).isEqualTo(true)
        assertThat(exception?.message).isEqualTo(
            "Transaction not found for nsu=nsu-404 and terminalId=terminal-404"
        )
    }

    @Test
    fun `should throw when external void is not accepted`() {
        every {
            findTransactionByTransactionIdOutputPort.find("txn-4")
        } returns transaction(transactionId = "txn-4", status = TransactionStatus.CONFIRMED)
        every {
            voidTransactionExternallyOutputPort.voidTransaction(any())
        } returns VoidStatus.ERROR

        val exception = kotlin.runCatching {
            useCase.voidTransaction(VoidTransactionCommand(transactionId = "txn-4"))
        }.exceptionOrNull()

        assertThat(exception is ExternalAuthorizationRejectedException).isEqualTo(true)
        assertThat(exception?.message).isEqualTo("External void was rejected with result=ERROR.")
        verify(exactly = 0) { saveTransactionOutputPort.save(any()) }
    }

    private fun transaction(transactionId: String, status: TransactionStatus) = Transaction(
        transactionId = transactionId,
        terminalId = "terminal-1",
        nsu = "nsu-1",
        amount = BigDecimal("10.00"),
        status = status,
        createdAt = Instant.parse("2026-04-18T10:15:30Z")
    )
}
