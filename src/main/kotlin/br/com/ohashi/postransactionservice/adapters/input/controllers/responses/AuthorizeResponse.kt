package br.com.ohashi.postransactionservice.adapters.input.controllers.responses

import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
import java.math.BigDecimal

data class AuthorizeResponse(
    val nsu: String,
    val amount: BigDecimal,
    val terminalId: String,
    val transactionId: String
) {
    constructor(authorizeTransactionResult: AuthorizeTransactionResult) : this(
        nsu = authorizeTransactionResult.nsu,
        amount = authorizeTransactionResult.amount,
        terminalId = authorizeTransactionResult.terminalId,
        transactionId = authorizeTransactionResult.terminalId
    )
}