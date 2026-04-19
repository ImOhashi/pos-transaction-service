package br.com.ohashi.postransactionservice.adapters

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

@ExtendWith(MockKExtension::class)
class FindTransactionByNsuAndTerminalIdRepositoryAdapterTest {

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `should return mapped domain transaction when repository finds entity`() {
        every {
            transactionRepository.findByTerminalIdAndNsu("terminal-1", "nsu-1")
        } returns Optional.of(
            TransactionEntity(
                transactionId = "txn-1",
                terminalId = "terminal-1",
                nsu = "nsu-1",
                amount = BigDecimal("9.99"),
                status = TransactionStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-04-18T12:00:00Z")
            )
        )

        val result = FindTransactionByNsuAndTerminalIdRepositoryAdapter(transactionRepository)
            .find(nsu = "nsu-1", terminalId = "terminal-1")

        assertThat(result?.transactionId).isEqualTo("txn-1")
        assertThat(result?.status).isEqualTo(TransactionStatus.AUTHORIZED)
        verify(exactly = 1) { transactionRepository.findByTerminalIdAndNsu("terminal-1", "nsu-1") }
    }

    @Test
    fun `should return null when repository does not find entity`() {
        every {
            transactionRepository.findByTerminalIdAndNsu("terminal-2", "nsu-2")
        } returns Optional.empty()

        val result = FindTransactionByNsuAndTerminalIdRepositoryAdapter(transactionRepository)
            .find(nsu = "nsu-2", terminalId = "terminal-2")

        assertThat(result).isNull()
    }
}
