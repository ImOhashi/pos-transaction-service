package br.com.ohashi.postransactionservice.application.core.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.output.ConfirmTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByTransactionIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.ConfirmTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.ConfirmationStatus
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
class ConfirmTransactionUseCaseTest {

    @MockK
    private lateinit var findTransactionByTransactionIdOutputPort: FindTransactionByTransactionIdOutputPort

    @MockK
    private lateinit var confirmTransactionExternallyOutputPort: ConfirmTransactionExternallyOutputPort

    @RelaxedMockK
    private lateinit var saveTransactionOutputPort: SaveTransactionOutputPort

    private lateinit var useCase: ConfirmTransactionUseCase

    @BeforeEach
    fun setUp() {
        clearMocks(
            findTransactionByTransactionIdOutputPort,
            confirmTransactionExternallyOutputPort,
            saveTransactionOutputPort
        )
        useCase = ConfirmTransactionUseCase(
            findTransactionByTransactionIdOutputPort = findTransactionByTransactionIdOutputPort,
            confirmTransactionExternallyOutputPort = confirmTransactionExternallyOutputPort,
            saveTransactionOutputPort = saveTransactionOutputPort
        )
    }

    @Test
    fun `should return idempotent success when transaction is already confirmed`() {
        every {
            findTransactionByTransactionIdOutputPort.find("txn-1")
        } returns transaction(transactionId = "txn-1", status = TransactionStatus.CONFIRMED)

        useCase.confirm(ConfirmTransactionCommand(transactionId = "txn-1"))

        verify(exactly = 1) { findTransactionByTransactionIdOutputPort.find("txn-1") }
        verify(exactly = 0) { confirmTransactionExternallyOutputPort.confirm(any()) }
        verify(exactly = 0) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should confirm externally and persist transaction as confirmed`() {
        val requestSlot = slot<ConfirmTransactionExternalRequest>()
        val transactionSlot = slot<Transaction>()
        every {
            findTransactionByTransactionIdOutputPort.find("txn-2")
        } returns transaction(transactionId = "txn-2", status = TransactionStatus.AUTHORIZED)
        every {
            confirmTransactionExternallyOutputPort.confirm(capture(requestSlot))
        } returns ConfirmationStatus.CONFIRMED
        every {
            saveTransactionOutputPort.save(capture(transactionSlot))
        } answers { firstArg() }

        useCase.confirm(ConfirmTransactionCommand(transactionId = "txn-2"))

        assertThat(requestSlot.captured.transactionId).isEqualTo("txn-2")
        assertThat(transactionSlot.captured.transactionId).isEqualTo("txn-2")
        assertThat(transactionSlot.captured.status).isEqualTo(TransactionStatus.CONFIRMED)
        verify(exactly = 1) { confirmTransactionExternallyOutputPort.confirm(any()) }
        verify(exactly = 1) { saveTransactionOutputPort.save(any()) }
    }

    @Test
    fun `should throw when external confirmation is not accepted`() {
        every {
            findTransactionByTransactionIdOutputPort.find("txn-3")
        } returns transaction(transactionId = "txn-3", status = TransactionStatus.AUTHORIZED)
        every {
            confirmTransactionExternallyOutputPort.confirm(any())
        } returns ConfirmationStatus.ERROR

        val exception = kotlin.runCatching {
            useCase.confirm(ConfirmTransactionCommand(transactionId = "txn-3"))
        }.exceptionOrNull()

        assertThat(exception is ExternalAuthorizationRejectedException).isEqualTo(true)
        assertThat(exception?.message).isEqualTo("External confirmation was rejected with result=ERROR.")
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
