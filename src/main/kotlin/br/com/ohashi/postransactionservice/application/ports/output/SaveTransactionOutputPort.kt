package br.com.ohashi.postransactionservice.application.ports.output

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction

interface SaveTransactionOutputPort {
    fun save(transaction: Transaction): Transaction
}
