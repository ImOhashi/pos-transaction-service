package br.com.ohashi.postransactionservice.adapters.output.gateway

import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalConfirmRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalVoidRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalAuthorizeResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalConfirmResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalVoidResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
    name = "externalTransactionsClient",
    url = "\${external.client.base-url}"
)
interface ExternalTransactionsFeignClient {

    @PostMapping("/external/transactions/authorize")
    fun authorize(
        @RequestBody request: ExternalAuthorizeRequest
    ): ExternalAuthorizeResponse

    @PostMapping("/external/transactions/confirm")
    fun confirm(
        @RequestBody request: ExternalConfirmRequest
    ): ExternalConfirmResponse

    @PostMapping("/external/transactions/void")
    fun voidTransaction(
        @RequestBody request: ExternalVoidRequest
    ): ExternalVoidResponse
}