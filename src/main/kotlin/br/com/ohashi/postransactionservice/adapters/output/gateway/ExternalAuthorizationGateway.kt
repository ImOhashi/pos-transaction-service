package br.com.ohashi.postransactionservice.adapters.output.gateway

import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalAuthorizeResponse
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class ExternalAuthorizationGateway(
    private val externalTransactionsFeignClient: ExternalTransactionsFeignClient
) {

    @Bulkhead(name = AUTHORIZE_EXTERNAL_RESILIENCE_NAME, type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = AUTHORIZE_EXTERNAL_RESILIENCE_NAME)
    @CircuitBreaker(name = AUTHORIZE_EXTERNAL_RESILIENCE_NAME)
    fun authorize(request: ExternalAuthorizeRequest): ExternalAuthorizeResponse =
        externalTransactionsFeignClient.authorize(request)

    companion object {
        const val AUTHORIZE_EXTERNAL_RESILIENCE_NAME = "externalAuthorize"
    }
}
