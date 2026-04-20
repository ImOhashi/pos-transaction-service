package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.entities.toDomain
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.shared.LoggableClass
import br.com.ohashi.postransactionservice.shared.observability.TracingSupport.inSpan
import org.springframework.stereotype.Component

@Component
class FindTransactionByNsuAndTerminalIdRepositoryAdapter(
    private val transactionRepository: TransactionRepository
) : FindTransactionByNsuAndTerminalIdOutputPort, LoggableClass() {

    override fun find(nsu: String, terminalId: String): Transaction? {
        return inSpan(
            name = "repository.findTransactionByNsuAndTerminalId",
            "db.system" to "postgresql",
            "db.operation" to "SELECT",
            "transaction.nsu" to nsu,
            "transaction.terminalId" to terminalId
        ) {
            logger.info("Searching existing transaction by nsu=$nsu and terminalId=$terminalId")

            transactionRepository.findByTerminalIdAndNsu(terminalId = terminalId, nsu = nsu)
                .map { entity ->
                    logger.info(
                        "Found existing transaction transactionId=${entity.transactionId} " +
                                "with status=${entity.status}"
                    )

                    entity.toDomain()
                }
                .orElseGet {
                    logger.info("No transaction found by nsu=$nsu and terminalId=$terminalId")
                    null
                }
        }
    }
}
