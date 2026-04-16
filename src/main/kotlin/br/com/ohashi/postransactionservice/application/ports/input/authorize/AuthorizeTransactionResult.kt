package br.com.ohashi.postransactionservice.application.ports.input.authorize

import java.math.BigDecimal

data class AuthorizeTransactionResult(
    val nsu: String,
    val amount: BigDecimal,
    val terminalId: String,
    val transactionId: String
)