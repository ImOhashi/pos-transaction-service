package br.com.ohashi.postransactionservice.application.core.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizationStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class DomainModelsCoverageTest {

    @Test
    fun `should expose transaction fields and allow status mutation`() {
        val transaction = Transaction(
            transactionId = "txn-1",
            terminalId = "terminal-1",
            nsu = "nsu-1",
            amount = BigDecimal("10.00"),
            status = TransactionStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-04-18T18:00:00Z")
        )

        val transactionWithStatus = transaction.copy(status = TransactionStatus.CONFIRMED)

        assertThat(transactionWithStatus.transactionId).isEqualTo("txn-1")
        assertThat(transactionWithStatus.terminalId).isEqualTo("terminal-1")
        assertThat(transactionWithStatus.nsu).isEqualTo("nsu-1")
        assertThat(transactionWithStatus.amount).isEqualTo(BigDecimal("10.00"))
        assertThat(transactionWithStatus.status).isEqualTo(TransactionStatus.CONFIRMED)
    }

    @Test
    fun `should expose transaction entity fields and allow status mutation`() {
        val entity = TransactionEntity(
            transactionId = "txn-2",
            terminalId = "terminal-2",
            nsu = "nsu-2",
            amount = BigDecimal("20.00"),
            status = TransactionStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-04-18T18:10:00Z")
        )

        entity.status = TransactionStatus.VOIDED

        assertThat(entity.transactionId).isEqualTo("txn-2")
        assertThat(entity.terminalId).isEqualTo("terminal-2")
        assertThat(entity.nsu).isEqualTo("nsu-2")
        assertThat(entity.amount).isEqualTo(BigDecimal("20.00"))
        assertThat(entity.status).isEqualTo(TransactionStatus.VOIDED)
    }

    @Test
    fun `should expose external authorization result fields`() {
        val result = AuthorizeTransactionExternalResult(
            transactionId = "txn-3",
            result = AuthorizationStatus.AUTHORIZED,
            approved = true,
            message = "approved"
        )

        assertThat(result.transactionId).isEqualTo("txn-3")
        assertThat(result.result).isEqualTo(AuthorizationStatus.AUTHORIZED)
        assertThat(result.approved).isEqualTo(true)
        assertThat(result.message).isEqualTo("approved")
    }
}
