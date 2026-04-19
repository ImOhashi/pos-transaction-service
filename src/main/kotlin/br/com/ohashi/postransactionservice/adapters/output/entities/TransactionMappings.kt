package br.com.ohashi.postransactionservice.adapters.output.entities

import br.com.ohashi.postransactionservice.application.core.domain.entities.Transaction

fun TransactionEntity.toDomain() = Transaction(
    transactionId = transactionId,
    terminalId = terminalId,
    nsu = nsu,
    amount = amount,
    status = status,
    createdAt = createdAt
)

fun Transaction.toEntity() = TransactionEntity(
    transactionId = transactionId,
    terminalId = terminalId,
    nsu = nsu,
    amount = amount,
    status = status,
    createdAt = createdAt
)
