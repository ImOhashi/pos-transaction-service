package br.com.ohashi.postransactionservice.adapters.input.controllers.requests

import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionCommand
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size

data class VoidRequest(
    @field:Size(max = 50, message = "{message.void-request.transaction-id.size}")
    val transactionId: String? = null,

    @field:Size(max = 20, message = "{message.void-request.nsu.size}")
    val nsu: String? = null,

    @field:Size(max = 50, message = "{message.void-request.terminal-id.size}")
    val terminalId: String? = null
) {
    @get:AssertTrue(message = "{message.void-request.identification.invalid}")
    val hasValidIdentification: Boolean
        get() {
            val hasTransactionId = !transactionId.isNullOrBlank()
            val hasNsu = !nsu.isNullOrBlank()
            val hasTerminalId = !terminalId.isNullOrBlank()

            return (hasTransactionId && !hasNsu && !hasTerminalId) ||
                (!hasTransactionId && hasNsu && hasTerminalId)
        }

    fun toCommand() = VoidTransactionCommand(
        transactionId = transactionId?.takeIf { it.isNotBlank() },
        nsu = nsu?.takeIf { it.isNotBlank() },
        terminalId = terminalId?.takeIf { it.isNotBlank() }
    )
}
