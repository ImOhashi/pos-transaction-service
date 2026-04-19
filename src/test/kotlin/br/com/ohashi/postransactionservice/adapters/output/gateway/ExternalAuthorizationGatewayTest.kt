package br.com.ohashi.postransactionservice.adapters.output.gateway

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalAuthorizeResponse
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
}
