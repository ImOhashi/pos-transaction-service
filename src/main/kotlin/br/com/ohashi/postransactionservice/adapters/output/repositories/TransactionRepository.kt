package br.com.ohashi.postransactionservice.adapters.output.repositories

import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional


interface TransactionRepository : JpaRepository<TransactionEntity, String> {

    fun findByTerminalIdAndNsu(terminalId: String, nsu: String): Optional<TransactionEntity>

    fun findByTransactionId(transactionId: String): Optional<TransactionEntity>
}