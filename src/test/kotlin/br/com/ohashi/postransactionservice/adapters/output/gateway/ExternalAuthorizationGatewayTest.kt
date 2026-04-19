package br.com.ohashi.postransactionservice.adapters.output.gateway

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalConfirmRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalVoidRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalAuthorizeResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalConfirmResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalVoidResponse
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class ExternalAuthorizationGatewayTest {

    @MockK
    private lateinit var externalTransactionsFeignClient: ExternalTransactionsFeignClient

    @Test
    fun `should delegate authorization to feign client`() {
        val request = ExternalAuthorizeRequest(
            terminalId = "terminal-1",
            nsu = "nsu-1",
            amount = BigDecimal("15.00")
        )
        every {
            externalTransactionsFeignClient.authorize(request)
        } returns ExternalAuthorizeResponse(
            transactionId = "txn-1",
            result = "AUTHORIZED",
            message = "approved"
        )

        val response = ExternalAuthorizationGateway(externalTransactionsFeignClient).authorize(request)

        assertThat(response.transactionId).isEqualTo("txn-1")
        verify(exactly = 1) { externalTransactionsFeignClient.authorize(request) }
    }

    @Test
    fun `should delegate confirmation to feign client`() {
        val request = ExternalConfirmRequest(transactionId = "txn-2")
        every {
            externalTransactionsFeignClient.confirm(request)
        } returns ExternalConfirmResponse(
            transactionId = "txn-2",
            result = "CONFIRMED",
            message = "approved"
        )

        val response = ExternalAuthorizationGateway(externalTransactionsFeignClient).confirm(request)

        assertThat(response.transactionId).isEqualTo("txn-2")
        verify(exactly = 1) { externalTransactionsFeignClient.confirm(request) }
    }

    @Test
    fun `should delegate void to feign client`() {
        val request = ExternalVoidRequest(transactionId = "txn-3")
        every {
            externalTransactionsFeignClient.voidTransaction(request)
        } returns ExternalVoidResponse(
            transactionId = "txn-3",
            result = "VOIDED",
            message = "ok"
        )

        val response = ExternalAuthorizationGateway(externalTransactionsFeignClient).voidTransaction(request)

        assertThat(response.transactionId).isEqualTo("txn-3")
        verify(exactly = 1) { externalTransactionsFeignClient.voidTransaction(request) }
    }
}
