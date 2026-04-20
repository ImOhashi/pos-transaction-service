package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.entities.toDomain
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByTransactionIdOutputPort
import br.com.ohashi.postransactionservice.shared.LoggableClass
import br.com.ohashi.postransactionservice.shared.observability.TracingSupport.inSpan
import br.com.ohashi.postransactionservice.shared.exceptions.TransactionNotFoundException
import org.springframework.stereotype.Component

@Component
class FindTransactionByTransactionIdRepositoryAdapter(
    private val transactionRepository: TransactionRepository
) : FindTransactionByTransactionIdOutputPort, LoggableClass() {

    override fun find(transactionId: String): Transaction {
        return inSpan(
            name = "repository.findTransactionByTransactionId",
            "db.system" to "postgresql",
            "db.operation" to "SELECT",
            "transaction.transactionId" to transactionId
        ) {
            logger.info("Searching existing transaction by transactionId=$transactionId")

            transactionRepository.findByTransactionId(transactionId)
                .map { entity ->
                    logger.info(
                        "Found transaction transactionId=${entity.transactionId} with status=${entity.status}"
                    )

                    entity.toDomain()
                }
                .orElseThrow {
                    logger.warn("Transaction not found by transactionId=$transactionId")
                    TransactionNotFoundException("Transaction not found for transactionId=$transactionId")
                }
        }
    }
}
