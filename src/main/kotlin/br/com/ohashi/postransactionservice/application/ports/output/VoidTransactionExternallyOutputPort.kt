package br.com.ohashi.postransactionservice.application.ports.output

import br.com.ohashi.postransactionservice.application.ports.output.requests.VoidTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.VoidStatus

interface VoidTransactionExternallyOutputPort {
    fun voidTransaction(request: VoidTransactionExternalRequest): VoidStatus
}
