package br.com.ohashi.postransactionservice.application.ports.output.responses

data class AuthorizeTransactionExternalResult(
    val transactionId: String,
    val result: AuthorizationStatus,
    val approved: Boolean,
    val message: String
)

enum class AuthorizationStatus {
    AUTHORIZED, NON_AUTHORIZED
}