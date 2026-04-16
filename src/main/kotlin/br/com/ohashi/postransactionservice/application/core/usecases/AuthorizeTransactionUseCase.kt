package br.com.ohashi.postransactionservice.application.core.usecases

import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult

class AuthorizeTransactionUseCase : AuthorizeTransactionInputPort {
    override fun authorize(authorizeTransactionCommand: AuthorizeTransactionCommand): AuthorizeTransactionResult {
        TODO("Not yet implemented")
    }
}