package br.com.ohashi.postransactionservice.application.ports.output.requests

import java.math.BigDecimal

data class AuthorizeTransactionExternalRequest(
    val terminalId: String,
    val nsu: String,
    val amount: BigDecimal
)