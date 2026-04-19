package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.shared.LoggableClass
import org.springframework.stereotype.Component

@Component
class SaveTransactionRepositoryAdapter(
    private val transactionRepository: TransactionRepository
) : SaveTransactionOutputPort, LoggableClass() {

    override fun save(transaction: Transaction): Transaction {
        logger.info(
            "Persisting transaction transactionId=${transaction.transactionId} " +
                "with status=${transaction.status}"
        )

        val savedTransaction = transactionRepository.save(transaction.toEntity())

        logger.info("Transaction persisted transactionId=${savedTransaction.transactionId}")

        return savedTransaction.toDomain()
    }

    private fun Transaction.toEntity() = TransactionEntity(
        transactionId = transactionId,
        terminalId = terminalId,
        nsu = nsu,
        amount = amount,
        status = status,
        createdAt = createdAt
    )

    private fun TransactionEntity.toDomain() = Transaction(
        transactionId = transactionId,
        terminalId = terminalId,
        nsu = nsu,
        amount = amount,
        status = status,
        createdAt = createdAt
    )
}
