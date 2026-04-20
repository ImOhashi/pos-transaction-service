package br.com.ohashi.postransactionservice.adapters

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalConfirmRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalConfirmResponse
import br.com.ohashi.postransactionservice.application.ports.output.requests.ConfirmTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.ConfirmationStatus
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ConfirmTransactionExternallyAdapterTest {

    @MockK
    private lateinit var externalAuthorizationGateway: ExternalAuthorizationGateway

    @Test
    fun `should map confirmation request and return confirmed result`() {
        val requestSlot = slot<ExternalConfirmRequest>()
        every {
            externalAuthorizationGateway.confirm(capture(requestSlot))
        } returns ExternalConfirmResponse(
            transactionId = "txn-1",
            result = "CONFIRMED",
            message = "approved"
        )

        val result = ConfirmTransactionExternallyAdapter(externalAuthorizationGateway).confirm(
            ConfirmTransactionExternalRequest(transactionId = "txn-1")
        )

        assertThat(requestSlot.captured.transactionId).isEqualTo("txn-1")
        assertThat(result).isEqualTo(ConfirmationStatus.CONFIRMED)
        verify(exactly = 1) { externalAuthorizationGateway.confirm(any()) }
    }

    @Test
    fun `should map unknown external confirmation result to error`() {
        every {
            externalAuthorizationGateway.confirm(any())
        } returns ExternalConfirmResponse(
            transactionId = "txn-3",
            result = "INVALID_STATUS",
            message = "unexpected"
        )

        val result = ConfirmTransactionExternallyAdapter(externalAuthorizationGateway).confirm(
            ConfirmTransactionExternalRequest(transactionId = "txn-3")
        )

        assertThat(result).isEqualTo(ConfirmationStatus.ERROR)
    }
}
