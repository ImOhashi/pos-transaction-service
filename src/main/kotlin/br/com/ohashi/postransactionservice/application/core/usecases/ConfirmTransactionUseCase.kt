package br.com.ohashi.postransactionservice.application.core.usecases

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.output.ConfirmTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByTransactionIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.ConfirmTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.ConfirmationStatus
import br.com.ohashi.postransactionservice.shared.LoggableClass
import br.com.ohashi.postransactionservice.shared.observability.TracingSupport.inSpan
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import br.com.ohashi.postransactionservice.shared.exceptions.InvalidTransactionStateException

class ConfirmTransactionUseCase(
    private val findTransactionByTransactionIdOutputPort: FindTransactionByTransactionIdOutputPort,
    private val confirmTransactionExternallyOutputPort: ConfirmTransactionExternallyOutputPort,
    private val saveTransactionOutputPort: SaveTransactionOutputPort
    ) : ConfirmTransactionInputPort, LoggableClass() {

    override fun confirm(confirmTransactionCommand: ConfirmTransactionCommand) {
        inSpan(
            name = "usecase.confirmTransaction",
            "transaction.transactionId" to confirmTransactionCommand.transactionId
        ) {
            logger.info(
                "Starting transaction confirmation flow to " +
                        "transactionId=${confirmTransactionCommand.transactionId}"
            )

            val transaction: Transaction = findTransactionByTransactionIdOutputPort.find(
                transactionId = confirmTransactionCommand.transactionId
            )
            logger.info(
                "Transaction loaded for confirmation with transactionId=${transaction.transactionId} " +
                        "and status=${transaction.status}"
            )

            if (transaction.status == TransactionStatus.CONFIRMED) {
                logger.info(
                    "Transaction already confirmed for transactionId=${transaction.transactionId}"
                )
                return@inSpan
            }

            isVoidedTransactionStatus(
                status = transaction.status,
                transactionId = transaction.transactionId
            )

            logger.info("Sending external confirmation for transactionId=${transaction.transactionId}")
            val confirmationStatus: ConfirmationStatus = confirmTransactionExternallyOutputPort.confirm(
                ConfirmTransactionExternalRequest(transactionId = transaction.transactionId)
            )
            logger.info(
                "External confirmation returned status=$confirmationStatus " +
                        "for transactionId=${transaction.transactionId}"
            )

            ensureAcceptedConfirmationStatus(confirmationStatus)

            logger.info("Persisting confirmed transaction for transactionId=${transaction.transactionId}")
            saveTransactionOutputPort.save(
                transaction = transaction.copy(
                    status = TransactionStatus.CONFIRMED
                )
            )

            logger.info("Transaction confirmation flow finished for transactionId=${transaction.transactionId}")
        }
    }

    private fun isVoidedTransactionStatus(status: TransactionStatus, transactionId: String) {
        if (status == TransactionStatus.VOIDED) {
            logger.warn(
                "Transaction cannot be confirmed because it is voided for transactionId=${transactionId}"
            )
            throw InvalidTransactionStateException(
                "Transaction cannot be confirmed because it is VOIDED."
            )
        }
    }

    private fun ensureAcceptedConfirmationStatus(confirmationStatus: ConfirmationStatus) {
        if (confirmationStatus == ConfirmationStatus.CONFIRMED) {
            return
        }

        logger.warn("External confirmation rejected with result=$confirmationStatus")
        throw ExternalAuthorizationRejectedException(
            "External confirmation was rejected with result=$confirmationStatus."
        )
    }
}
