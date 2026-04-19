package br.com.ohashi.postransactionservice.application.ports.output.responses

data class AuthorizeTransactionExternalResult(
    val transactionId: String,
    val result: String,
    val approved: Boolean,
    val message: String
)