package br.com.ohashi.postransactionservice.application.ports.input.authorize

import java.math.BigDecimal

data class AuthorizeTransactionCommand(
    val nsu: String,
    val amount: BigDecimal,
    val terminalId: String
)