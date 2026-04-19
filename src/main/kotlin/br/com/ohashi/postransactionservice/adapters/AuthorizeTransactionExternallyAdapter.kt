package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizationStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import br.com.ohashi.postransactionservice.shared.LoggableClass
import org.springframework.stereotype.Component

@Component
class AuthorizeTransactionExternallyAdapter(
    private val externalAuthorizationGateway: ExternalAuthorizationGateway
) : AuthorizeTransactionExternallyOutputPort, LoggableClass() {

    override fun authorize(request: AuthorizeTransactionExternalRequest): AuthorizeTransactionExternalResult {
        logger.info(
            "Sending external authorization request for nsu=${request.nsu} " +
                    "and terminalId=${request.terminalId}"
        )

        val response = externalAuthorizationGateway.authorize(request.toExternalRequest())

        logger.info(
            "Received external authorization response result=${response.result} " +
                    "transactionId=${response.transactionId}"
        )

        val authorizationStatus = AuthorizationStatus.valueOf(response.result)

        return AuthorizeTransactionExternalResult(
            transactionId = response.transactionId,
            result = authorizationStatus,
            approved = authorizationStatus == AuthorizationStatus.AUTHORIZED,
            message = response.message
        )
    }

    private fun AuthorizeTransactionExternalRequest.toExternalRequest() = ExternalAuthorizeRequest(
        terminalId = terminalId,
        nsu = nsu,
        amount = amount
    )
}
