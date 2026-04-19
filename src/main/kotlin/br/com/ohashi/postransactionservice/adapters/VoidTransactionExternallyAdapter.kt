package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalVoidRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalVoidResponse
import br.com.ohashi.postransactionservice.application.ports.output.VoidTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.VoidTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.VoidStatus
import br.com.ohashi.postransactionservice.shared.LoggableClass
import org.springframework.stereotype.Component

@Component
class VoidTransactionExternallyAdapter(
    private val externalAuthorizationGateway: ExternalAuthorizationGateway
) : VoidTransactionExternallyOutputPort, LoggableClass() {

    override fun voidTransaction(request: VoidTransactionExternalRequest): VoidStatus {
        logger.info("Sending external void request for transactionId=${request.transactionId}")

        val response: ExternalVoidResponse = externalAuthorizationGateway.voidTransaction(request.toExternalRequest())

        logger.info(
            "Received external void response result=${response.result} " +
                "transactionId=${response.transactionId}"
        )

        return VoidStatus.from(response.result)
    }

    private fun VoidTransactionExternalRequest.toExternalRequest() = ExternalVoidRequest(
        transactionId = transactionId
    )
}
