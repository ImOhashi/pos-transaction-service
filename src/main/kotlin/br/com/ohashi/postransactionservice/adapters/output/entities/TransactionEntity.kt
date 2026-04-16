package br.com.ohashi.postransactionservice.adapters.output.entities

import br.com.ohashi.postransactionservice.application.domain.enums.TransactionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "transactions")
class TransactionEntity(

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false, length = 50)
    val transactionId: String,

    @Column(name = "terminal_id", nullable = false, updatable = false, length = 50)
    val terminalId: String,

    @Column(name = "nsu", nullable = false, updatable = false, length = 50)
    val nsu: String,

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: TransactionStatus,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant
)