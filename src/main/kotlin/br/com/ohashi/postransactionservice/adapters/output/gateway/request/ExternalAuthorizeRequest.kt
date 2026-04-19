package br.com.ohashi.postransactionservice.adapters.output.gateway.request

import java.math.BigDecimal

data class ExternalAuthorizeRequest(
    val terminalId: String,
    val nsu: String,
    val amount: BigDecimal
)