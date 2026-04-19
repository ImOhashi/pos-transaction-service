package br.com.ohashi.postransactionservice.application.ports.input.void

data class VoidTransactionCommand(
    val transactionId: String? = null,
    val nsu: String? = null,
    val terminalId: String? = null
)
