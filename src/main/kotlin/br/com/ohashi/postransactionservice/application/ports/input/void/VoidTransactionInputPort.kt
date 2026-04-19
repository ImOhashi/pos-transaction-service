package br.com.ohashi.postransactionservice.application.ports.input.void

interface VoidTransactionInputPort {

    fun voidTransaction(voidTransactionCommand: VoidTransactionCommand)
}
