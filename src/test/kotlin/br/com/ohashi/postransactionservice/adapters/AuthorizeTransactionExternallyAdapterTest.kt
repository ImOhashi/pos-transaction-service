package br.com.ohashi.postransactionservice.adapters

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalAuthorizeRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalAuthorizeResponse
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class AuthorizeTransactionExternallyAdapterTest {

    @MockK
    private lateinit var externalAuthorizationGateway: ExternalAuthorizationGateway

    @Test
    fun `should map request and return authorized result`() {
        val requestSlot = slot<ExternalAuthorizeRequest>()
        every {
            externalAuthorizationGateway.authorize(capture(requestSlot))
        } returns ExternalAuthorizeResponse(
            transactionId = "txn-1",
            result = "AUTHORIZED",
            message = "approved"
        )

        val result = AuthorizeTransactionExternallyAdapter(externalAuthorizationGateway).authorize(
            AuthorizeTransactionExternalRequest(
                terminalId = "terminal-1",
                nsu = "nsu-1",
                amount = BigDecimal("12.30")
            )
        )

        assertThat(requestSlot.captured.terminalId).isEqualTo("terminal-1")
        assertThat(requestSlot.captured.nsu).isEqualTo("nsu-1")
        assertThat(requestSlot.captured.amount).isEqualTo(BigDecimal("12.30"))
        assertThat(result.transactionId).isEqualTo("txn-1")
        assertThat(result.result).isEqualTo("AUTHORIZED")
        assertThat(result.approved).isTrue()
        assertThat(result.message).isEqualTo("approved")
        verify(exactly = 1) { externalAuthorizationGateway.authorize(any()) }
    }

    @Test
    fun `should throw when external authorization is rejected`() {
        every {
            externalAuthorizationGateway.authorize(any())
        } returns ExternalAuthorizeResponse(
            transactionId = "txn-2",
            result = "DENIED",
            message = "denied"
        )

        val exception = runCatching {
            AuthorizeTransactionExternallyAdapter(externalAuthorizationGateway).authorize(
                AuthorizeTransactionExternalRequest(
                    terminalId = "terminal-2",
                    nsu = "nsu-2",
                    amount = BigDecimal("5.00")
                )
            )
        }.exceptionOrNull() ?: error("Expected exception")

        assertThat(exception).isInstanceOf(ExternalAuthorizationRejectedException::class)
        assertThat(exception.message).isEqualTo("External authorization was rejected with result=DENIED.")
    }
}
