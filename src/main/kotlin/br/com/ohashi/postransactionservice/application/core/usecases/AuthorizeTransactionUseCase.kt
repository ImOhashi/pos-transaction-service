package br.com.ohashi.postransactionservice.application.core.usecases

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionResult
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import br.com.ohashi.postransactionservice.shared.LoggableClass
import java.time.Instant

class AuthorizeTransactionUseCase(
    private val findTransactionByNsuAndTerminalIdOutputPort: FindTransactionByNsuAndTerminalIdOutputPort,
    private val authorizeTransactionExternallyOutputPort: AuthorizeTransactionExternallyOutputPort,
    private val saveTransactionOutputPort: SaveTransactionOutputPort
) : AuthorizeTransactionInputPort, LoggableClass() {

    override fun authorize(authorizeTransactionCommand: AuthorizeTransactionCommand): AuthorizeTransactionResult {
        logger.info(
            "Starting transaction authorization to nsu=${authorizeTransactionCommand.nsu} " +
                    "and terminalId=${authorizeTransactionCommand.terminalId}"
        )

        val existingTransaction: Transaction? = findExistingTransaction(
            nsu = authorizeTransactionCommand.nsu,
            terminalId = authorizeTransactionCommand.terminalId
        )

        if (existingTransaction != null) {
            logger.info(
                "Returning stored transactionId=${existingTransaction.transactionId} " +
                        "with status=${existingTransaction.status}"
            )
            return AuthorizeTransactionResult.mountByTransaction(existingTransaction)
        }

        logger.info("No existing transaction found for authorization request")

        val externalAuthorization = authorizeExternally(authorizeTransactionCommand)
        val persistedTransaction = saveAuthorizedTransaction(authorizeTransactionCommand, externalAuthorization)

        return AuthorizeTransactionResult.mountByTransaction(persistedTransaction)
    }

    private fun findExistingTransaction(nsu: String, terminalId: String): Transaction? =
        findTransactionByNsuAndTerminalIdOutputPort.find(nsu = nsu, terminalId = terminalId)

    private fun authorizeExternally(command: AuthorizeTransactionCommand): AuthorizeTransactionExternalResult =
        authorizeTransactionExternallyOutputPort.authorize(
            request = AuthorizeTransactionExternalRequest(
                terminalId = command.terminalId,
                nsu = command.nsu,
                amount = command.amount
            )
        )

    private fun saveAuthorizedTransaction(
        command: AuthorizeTransactionCommand,
        externalAuthorization: AuthorizeTransactionExternalResult
    ): Transaction =
        saveTransactionOutputPort.save(
            Transaction(
                transactionId = externalAuthorization.transactionId,
                terminalId = command.terminalId,
                nsu = command.nsu,
                amount = command.amount,
                status = TransactionStatus.AUTHORIZED,
                createdAt = Instant.now()
            )
        )
}
