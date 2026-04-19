package br.com.ohashi.postransactionservice.adapters.input.controllers.requests

import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionCommand
import jakarta.validation.constraints.NotBlank

data class ConfirmRequest(
    @NotBlank(message = "{message.confirm-request.transaction-id.not-blank}")
    val transactionId: String
) {
    fun toCommand() = ConfirmTransactionCommand(
        transactionId = this.transactionId
    )
}
