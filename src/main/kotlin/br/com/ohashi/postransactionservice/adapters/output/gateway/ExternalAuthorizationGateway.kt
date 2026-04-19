package br.com.ohashi.postransactionservice.adapters.output.gateway

import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalConfirmRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalVoidRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalAuthorizeResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalConfirmResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalVoidResponse
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

    @Bulkhead(name = CONFIRM_EXTERNAL_RESILIENCE_NAME, type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = CONFIRM_EXTERNAL_RESILIENCE_NAME)
    @CircuitBreaker(name = CONFIRM_EXTERNAL_RESILIENCE_NAME)
    fun confirm(request: ExternalConfirmRequest): ExternalConfirmResponse =
        externalTransactionsFeignClient.confirm(request)

    @Bulkhead(name = VOID_EXTERNAL_RESILIENCE_NAME, type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = VOID_EXTERNAL_RESILIENCE_NAME)
    @CircuitBreaker(name = VOID_EXTERNAL_RESILIENCE_NAME)
    fun voidTransaction(request: ExternalVoidRequest): ExternalVoidResponse =
        externalTransactionsFeignClient.voidTransaction(request)

    companion object {
        const val AUTHORIZE_EXTERNAL_RESILIENCE_NAME = "externalAuthorize"
        const val CONFIRM_EXTERNAL_RESILIENCE_NAME = "externalConfirm"
        const val VOID_EXTERNAL_RESILIENCE_NAME = "externalVoid"
    }
}
