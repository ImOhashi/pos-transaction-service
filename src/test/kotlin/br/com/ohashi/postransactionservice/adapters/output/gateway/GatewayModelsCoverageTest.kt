package br.com.ohashi.postransactionservice.adapters.output.gateway

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalConfirmRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.request.ExternalVoidRequest
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalConfirmResponse
import br.com.ohashi.postransactionservice.adapters.output.gateway.response.ExternalVoidResponse
import org.junit.jupiter.api.Test

class GatewayModelsCoverageTest {

    @Test
    fun `should expose external confirm models`() {
        val request = ExternalConfirmRequest(transactionId = "txn-1")
        val response = ExternalConfirmResponse(
            result = "CONFIRMED",
            transactionId = "txn-1",
            message = "ok"
        )

        assertThat(request.transactionId).isEqualTo("txn-1")
        assertThat(response.result).isEqualTo("CONFIRMED")
        assertThat(response.transactionId).isEqualTo("txn-1")
        assertThat(response.message).isEqualTo("ok")
    }

    @Test
    fun `should expose external void models`() {
        val request = ExternalVoidRequest(transactionId = "txn-2")
        val response = ExternalVoidResponse(
            result = "VOIDED",
            transactionId = "txn-2",
            message = "ok"
        )

        assertThat(request.transactionId).isEqualTo("txn-2")
        assertThat(response.result).isEqualTo("VOIDED")
        assertThat(response.transactionId).isEqualTo("txn-2")
        assertThat(response.message).isEqualTo("ok")
    }
}
