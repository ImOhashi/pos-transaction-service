package br.com.ohashi.postransactionservice.adapters.input.controllers

import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.AuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.ConfirmRequest
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.VoidRequest
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.AuthorizeResponse
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionInputPort
import br.com.ohashi.postransactionservice.shared.LoggableClass
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/pos/transactions"])
class TransactionController(
    private val authorizeTransactionInputPort: AuthorizeTransactionInputPort,
    private val confirmTransactionInputPort: ConfirmTransactionInputPort,
    private val voidTransactionInputPort: VoidTransactionInputPort
) : LoggableClass() {

    @PostMapping("/authorize")
    fun authorize(@RequestBody @Valid authorizeRequest: AuthorizeRequest): ResponseEntity<AuthorizeResponse> =
        withMDC(
            "nsu" to authorizeRequest.nsu,
            "terminalId" to authorizeRequest.terminalId
        ) {
            logger.info(
                "Receiving a request to authorize transaction for the nsu=${authorizeRequest.nsu} " +
                        "and terminalId=${authorizeRequest.terminalId}"
            )
            val result: AuthorizeTransactionResult = authorizeTransactionInputPort.authorize(
                authorizeTransactionCommand = authorizeRequest.toCommand()
            )

            return ResponseEntity.ok(
                AuthorizeResponse(authorizeTransactionResult = result)
            )
        }

    @PostMapping("/confirm")
    fun confirm(@RequestBody @Valid confirmRequest: ConfirmRequest): ResponseEntity<Void> =
        withMDC("transactionId" to confirmRequest.transactionId) {
            confirmTransactionInputPort.confirm(
                confirmTransactionCommand = confirmRequest.toCommand()
            )

            return ResponseEntity.noContent().build()
        }

    @PostMapping("/void")
    fun voidTransaction(@RequestBody @Valid voidRequest: VoidRequest): ResponseEntity<Void> =
        withMDC(
            "transactionId" to voidRequest.transactionId,
            "nsu" to voidRequest.nsu,
            "terminalId" to voidRequest.terminalId
        ) {
            voidTransactionInputPort.voidTransaction(
                voidTransactionCommand = voidRequest.toCommand()
            )

            return ResponseEntity.noContent().build()
        }
}
