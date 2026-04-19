package br.com.ohashi.postransactionservice.shared.exceptions

class TransactionAlreadyExistsException(override val message: String?) : RuntimeException(message)