package br.com.ohashi.postransactionservice.adapters

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.shared.exceptions.TransactionNotFoundException
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
class FindTransactionByTransactionIdRepositoryAdapterTest {

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `should return mapped domain transaction when repository finds entity`() {
        every {
            transactionRepository.findByTransactionId("txn-1")
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

        val result = FindTransactionByTransactionIdRepositoryAdapter(transactionRepository)
            .find("txn-1")

        assertThat(result.transactionId).isEqualTo("txn-1")
        assertThat(result.terminalId).isEqualTo("terminal-1")
        assertThat(result.status).isEqualTo(TransactionStatus.AUTHORIZED)
        verify(exactly = 1) { transactionRepository.findByTransactionId("txn-1") }
    }

    @Test
    fun `should throw transaction not found exception when repository does not find entity`() {
        every {
            transactionRepository.findByTransactionId("missing-txn")
        } returns Optional.empty()

        val failure = runCatching {
            FindTransactionByTransactionIdRepositoryAdapter(transactionRepository)
                .find("missing-txn")
        }.exceptionOrNull() ?: error("Expected exception")

        assertThat(failure).isInstanceOf(TransactionNotFoundException::class)
        assertThat(failure.message).isEqualTo("Transaction not found for transactionId=missing-txn")
    }
}
