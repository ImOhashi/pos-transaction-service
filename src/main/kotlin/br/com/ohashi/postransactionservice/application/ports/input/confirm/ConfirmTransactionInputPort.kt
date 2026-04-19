package br.com.ohashi.postransactionservice.application.ports.input.confirm

interface ConfirmTransactionInputPort {
    fun confirm(confirmTransactionCommand: ConfirmTransactionCommand)
}