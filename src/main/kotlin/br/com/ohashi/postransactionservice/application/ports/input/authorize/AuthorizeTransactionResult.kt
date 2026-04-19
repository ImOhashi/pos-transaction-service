package br.com.ohashi.postransactionservice.application.ports.input.authorize

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import java.math.BigDecimal

data class AuthorizeTransactionResult(
    val nsu: String,
    val amount: BigDecimal,
    val terminalId: String,
    val transactionId: String
) {
    companion object {
        fun mountByTransaction(transaction: Transaction) = AuthorizeTransactionResult(
            nsu = transaction.nsu,
            amount = transaction.amount,
            terminalId = transaction.terminalId,
            transactionId = transaction.transactionId
        )
    }
}