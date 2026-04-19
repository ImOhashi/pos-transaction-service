package br.com.ohashi.postransactionservice.application.ports.output

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction

interface FindTransactionByTransactionIdOutputPort {
    fun find(transactionId: String): Transaction
}