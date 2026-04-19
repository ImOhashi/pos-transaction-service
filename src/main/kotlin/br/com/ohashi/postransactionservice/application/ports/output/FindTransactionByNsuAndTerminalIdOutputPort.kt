package br.com.ohashi.postransactionservice.application.ports.output

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction

interface FindTransactionByNsuAndTerminalIdOutputPort {
    fun find(nsu: String, terminalId: String): Transaction?
}