package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import br.com.ohashi.postransactionservice.shared.LoggableClass
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
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

        if (response.result != AUTHORIZED_RESULT) {
            throw ExternalAuthorizationRejectedException(
                "External authorization was rejected with result=${response.result}."
            )
        }

        return AuthorizeTransactionExternalResult(
            transactionId = response.transactionId,
            result = response.result,
            approved = true,
            message = response.message
        )
    }

    private fun AuthorizeTransactionExternalRequest.toExternalRequest() = ExternalAuthorizeRequest(
        terminalId = terminalId,
        nsu = nsu,
        amount = amount
    )

    companion object {
        private const val AUTHORIZED_RESULT = "AUTHORIZED"
    }
}
