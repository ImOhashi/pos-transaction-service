package br.com.ohashi.postransactionservice.adapters

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.gateway.ExternalAuthorizationGateway
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalVoidRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalVoidResponse
import br.com.ohashi.postransactionservice.application.ports.output.requests.VoidTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.VoidStatus
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VoidTransactionExternallyAdapterTest {

    @MockK
    private lateinit var externalAuthorizationGateway: ExternalAuthorizationGateway

    @Test
    fun `should map void request and return voided result`() {
        val requestSlot = slot<ExternalVoidRequest>()
        every {
            externalAuthorizationGateway.voidTransaction(capture(requestSlot))
        } returns ExternalVoidResponse(
            transactionId = "txn-1",
            result = "VOIDED",
            message = "ok"
        )

        val result = VoidTransactionExternallyAdapter(externalAuthorizationGateway).voidTransaction(
            VoidTransactionExternalRequest(transactionId = "txn-1")
        )

        assertThat(requestSlot.captured.transactionId).isEqualTo("txn-1")
        assertThat(result).isEqualTo(VoidStatus.VOIDED)
        verify(exactly = 1) { externalAuthorizationGateway.voidTransaction(any()) }
    }

    @Test
    fun `should treat already voided response as successful idempotency`() {
        every {
            externalAuthorizationGateway.voidTransaction(any())
        } returns ExternalVoidResponse(
            transactionId = "txn-2",
            result = "ALREADY_VOIDED",
            message = "idempotent"
        )

        val result = VoidTransactionExternallyAdapter(externalAuthorizationGateway).voidTransaction(
            VoidTransactionExternalRequest(transactionId = "txn-2")
        )

        assertThat(result).isEqualTo(VoidStatus.ALREADY_VOIDED)
    }

    @Test
    fun `should map unknown external void result to error`() {
        every {
            externalAuthorizationGateway.voidTransaction(any())
        } returns ExternalVoidResponse(
            transactionId = "txn-3",
            result = "INVALID_STATUS",
            message = "unexpected"
        )

        val result = VoidTransactionExternallyAdapter(externalAuthorizationGateway).voidTransaction(
            VoidTransactionExternalRequest(transactionId = "txn-3")
        )

        assertThat(result).isEqualTo(VoidStatus.ERROR)
    }
}
