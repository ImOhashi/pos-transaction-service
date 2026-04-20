package br.com.ohashi.postransactionservice.shared.exceptions

class InvalidTransactionStateException(override val message: String) : RuntimeException(message)
