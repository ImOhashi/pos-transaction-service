package br.com.ohashi.postransactionservice.application.core.usecases

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionCommand
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByTransactionIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.VoidTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.VoidTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.VoidStatus
import br.com.ohashi.postransactionservice.shared.LoggableClass
import br.com.ohashi.postransactionservice.shared.exceptions.ExternalAuthorizationRejectedException
import br.com.ohashi.postransactionservice.shared.exceptions.TransactionNotFoundException

class VoidTransactionUseCase(
    private val findTransactionByTransactionIdOutputPort: FindTransactionByTransactionIdOutputPort,
    private val findTransactionByNsuAndTerminalIdOutputPort: FindTransactionByNsuAndTerminalIdOutputPort,
    private val voidTransactionExternallyOutputPort: VoidTransactionExternallyOutputPort,
    private val saveTransactionOutputPort: SaveTransactionOutputPort
) : VoidTransactionInputPort, LoggableClass() {

    override fun voidTransaction(voidTransactionCommand: VoidTransactionCommand) {
        logger.info(
            "Starting transaction void flow for transactionId=${voidTransactionCommand.transactionId} " +
                    "nsu=${voidTransactionCommand.nsu} terminalId=${voidTransactionCommand.terminalId}"
        )

        val transaction: Transaction = findTransaction(voidTransactionCommand)
        logger.info(
            "Transaction loaded for void with transactionId=${transaction.transactionId} " +
                    "and status=${transaction.status}"
        )

        if (transaction.status == TransactionStatus.VOIDED) {
            logger.info(
                "Transaction already voided for transactionId=${transaction.transactionId}"
            )
            return
        }

        logger.info("Sending external void for transactionId=${transaction.transactionId}")
        val voidStatus: VoidStatus = voidTransactionExternallyOutputPort.voidTransaction(
            VoidTransactionExternalRequest(transactionId = transaction.transactionId)
        )
        logger.info(
            "External void returned status=$voidStatus for transactionId=${transaction.transactionId}"
        )

        ensureAcceptedVoidStatus(voidStatus)

        logger.info("Persisting voided transaction for transactionId=${transaction.transactionId}")
        saveTransactionOutputPort.save(
            transaction = transaction.copy(
                status = TransactionStatus.VOIDED
            )
        )
        logger.info("Transaction void flow finished for transactionId=${transaction.transactionId}")
    }

    private fun findTransaction(voidTransactionCommand: VoidTransactionCommand): Transaction =
        voidTransactionCommand.transactionId?.let { transactionId ->
            findTransactionByTransactionIdOutputPort.find(transactionId = transactionId)
        } ?: run {
            val nsu = requireNotNull(voidTransactionCommand.nsu)
            val terminalId = requireNotNull(voidTransactionCommand.terminalId)

            findTransactionByNsuAndTerminalIdOutputPort.find(nsu = nsu, terminalId = terminalId)
                ?: throw TransactionNotFoundException(
                    "Transaction not found for nsu=$nsu and terminalId=$terminalId"
                )
        }

    private fun ensureAcceptedVoidStatus(voidStatus: VoidStatus) {
        if (voidStatus == VoidStatus.VOIDED) {
            return
        }

        logger.warn("External void rejected with result=$voidStatus")
        throw ExternalAuthorizationRejectedException(
            "External void was rejected with result=$voidStatus."
        )
    }
}
