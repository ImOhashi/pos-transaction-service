package br.com.ohashi.postransactionservice.application.ports.input.confirm

data class ConfirmTransactionCommand(
    val transactionId: String
)
