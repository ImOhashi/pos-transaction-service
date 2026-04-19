package br.com.ohashi.postransactionservice.adapters

import br.com.ohashi.postransactionservice.adapters.output.entities.toDomain
import br.com.ohashi.postransactionservice.adapters.output.repositories.TransactionRepository
import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.shared.LoggableClass
import org.springframework.stereotype.Component

@Component
class FindTransactionByNsuAndTerminalIdRepositoryAdapter(
    private val transactionRepository: TransactionRepository
) : FindTransactionByNsuAndTerminalIdOutputPort, LoggableClass() {

    override fun find(nsu: String, terminalId: String): Transaction? {
        logger.info("Searching existing transaction by nsu=$nsu and terminalId=$terminalId")

        return transactionRepository.findByTerminalIdAndNsu(terminalId = terminalId, nsu = nsu)
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
