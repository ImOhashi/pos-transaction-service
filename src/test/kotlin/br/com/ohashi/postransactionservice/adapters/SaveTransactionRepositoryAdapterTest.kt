package br.com.ohashi.postransactionservice.adapters

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockKExtension::class)
class SaveTransactionRepositoryAdapterTest {

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `should map domain transaction to entity and return saved domain`() {
        val entitySlot = slot<TransactionEntity>()
        every { transactionRepository.save(capture(entitySlot)) } answers { firstArg() }

        val transaction = Transaction(
            transactionId = "txn-1",
            terminalId = "terminal-1",
            nsu = "nsu-1",
            amount = BigDecimal("19.90"),
            status = TransactionStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-04-18T12:00:00Z")
        )

        val result = SaveTransactionRepositoryAdapter(transactionRepository).save(transaction)

        assertThat(entitySlot.captured.transactionId).isEqualTo("txn-1")
        assertThat(entitySlot.captured.amount).isEqualTo(BigDecimal("19.90"))
        assertThat(result.terminalId).isEqualTo("terminal-1")
        assertThat(result.status).isEqualTo(TransactionStatus.AUTHORIZED)
    }
}
