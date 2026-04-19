package br.com.ohashi.postransactionservice.application.ports.output

import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult

interface AuthorizeTransactionExternallyOutputPort {
    fun authorize(request: AuthorizeTransactionExternalRequest): AuthorizeTransactionExternalResult
}
