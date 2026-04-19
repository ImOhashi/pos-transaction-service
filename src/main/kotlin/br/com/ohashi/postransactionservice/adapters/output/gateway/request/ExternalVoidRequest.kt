package br.com.ohashi.postransactionservice.adapters.output.gateway.request

data class ExternalVoidRequest(
    val transactionId: String
)