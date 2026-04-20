package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.entities.TransactionEntity
import br.com.ohashi.postransactionservice.adapters.output.entities.toDomain
import br.com.ohashi.postransactionservice.adapters.output.entities.toEntity
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.shared.LoggableClass
import br.com.ohashi.postransactionservice.shared.observability.TracingSupport.inSpan
import org.springframework.stereotype.Component

@Component
class SaveTransactionRepositoryAdapter(
    private val transactionRepository: TransactionRepository
    ) : SaveTransactionOutputPort, LoggableClass() {

    override fun save(transaction: Transaction): Transaction {
        return inSpan(
            name = "repository.saveTransaction",
            "db.system" to "postgresql",
            "db.operation" to "UPSERT",
            "transaction.transactionId" to transaction.transactionId,
            "transaction.status" to transaction.status.name
        ) {
            logger.info(
                "Persisting transaction transactionId=${transaction.transactionId} " +
                    "with status=${transaction.status}"
            )

            val savedTransaction: TransactionEntity = transactionRepository.save(transaction.toEntity())

            logger.info("Transaction persisted transactionId=${savedTransaction.transactionId}")

            savedTransaction.toDomain()
        }
    }
}
