package br.com.ohashi.postransactionservice.adapters.input.controllers

import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.AuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.input.controllers.responses.AuthorizeResponse
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
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
    private val authorizeTransactionInputPort: AuthorizeTransactionInputPort
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
}