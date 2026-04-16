package br.com.ohashi.postransactionservice.adapters.input.controllers.responses

import java.math.BigDecimal

data class AuthorizeResponse (
    val nsu: String,
    val amount: BigDecimal,
    val terminalId: String,
    val transactionId: String
)