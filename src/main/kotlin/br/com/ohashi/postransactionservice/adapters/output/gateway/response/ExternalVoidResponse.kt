package br.com.ohashi.postransactionservice.adapters.output.gateway.response

data class ExternalVoidResponse(
    val result: String,
    val transactionId: String,
    val message: String
)
