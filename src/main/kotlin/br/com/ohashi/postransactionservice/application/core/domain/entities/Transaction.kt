package br.com.ohashi.postransactionservice.application.core.domain.entities

import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import java.math.BigDecimal
import java.time.Instant

data class Transaction(
    val transactionId: String,
    val terminalId: String,
    val nsu: String,
    val amount: BigDecimal,
    var status: TransactionStatus,
    val createdAt: Instant
)