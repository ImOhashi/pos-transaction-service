package br.com.ohashi.postransactionservice.application.ports.input.authorize

interface AuthorizeTransactionInputPort {
    fun authorize(authorizeTransactionCommand: AuthorizeTransactionCommand): AuthorizeTransactionResult
}