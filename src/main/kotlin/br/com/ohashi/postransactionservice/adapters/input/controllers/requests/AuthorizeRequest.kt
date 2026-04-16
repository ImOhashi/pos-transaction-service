package br.com.ohashi.postransactionservice.adapters.input.controllers.requests

import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionCommand
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class AuthorizeRequest(
    @field:NotBlank(message = "{message.authorize-request.nsu.not-blank}")
    @field:Size(max = 20, message = "{message.authorize-request.nsu.size}")
    val nsu: String,

    @field:Positive(message = "{message.authorize-request.amount.positive}")
    @field:Digits(integer = 10, fraction = 2, message = "{message.authorize-request.amount.digits}")
    val amount: BigDecimal,

    @field:NotBlank(message = "{message.authorize-request.terminal-id.not-blank}")
    val terminalId: String
) {
    fun toCommand(): AuthorizeTransactionCommand = AuthorizeTransactionCommand(
        nsu = this.nsu,
        amount = this.amount,
        terminalId = this.terminalId
    )
}
