package br.com.ohashi.postransactionservice.application.ports.output.requests

data class VoidTransactionExternalRequest(
    val transactionId: String
)
