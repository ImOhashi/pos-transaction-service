package br.com.ohashi.postransactionservice.application.ports.output.requests

data class ConfirmTransactionExternalRequest(
    val transactionId: String
)
