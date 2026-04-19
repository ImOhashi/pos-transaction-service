package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalConfirmRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalConfirmResponse
import br.com.ohashi.postransactionservice.application.ports.output.ConfirmTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.ConfirmTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.ConfirmationStatus
import br.com.ohashi.postransactionservice.shared.LoggableClass
import org.springframework.stereotype.Component

@Component
class ConfirmTransactionExternallyAdapter(
    private val externalAuthorizationGateway: ExternalAuthorizationGateway
) : ConfirmTransactionExternallyOutputPort, LoggableClass() {

    override fun confirm(request: ConfirmTransactionExternalRequest): ConfirmationStatus {
        logger.info("Sending external confirmation request for transactionId=${request.transactionId}")

        val response: ExternalConfirmResponse = externalAuthorizationGateway.confirm(request.toExternalRequest())

        logger.info(
            "Received external confirmation response result=${response.result} " +
                "transactionId=${response.transactionId}"
        )

        return ConfirmationStatus.from(response.result)
    }

    private fun ConfirmTransactionExternalRequest.toExternalRequest() = ExternalConfirmRequest(
        transactionId = transactionId
    )
}
