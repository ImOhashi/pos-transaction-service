package br.com.ohashi.postransactionservice.application.ports.output

import br.com.ohashi.postransactionservice.application.ports.output.requests.ConfirmTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.ConfirmationStatus

interface ConfirmTransactionExternallyOutputPort {
    fun confirm(request: ConfirmTransactionExternalRequest): ConfirmationStatus
}
