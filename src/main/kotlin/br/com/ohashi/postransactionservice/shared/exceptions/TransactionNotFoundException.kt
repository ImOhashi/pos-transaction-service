package br.com.ohashi.postransactionservice.shared.exceptions

class TransactionNotFoundException(override val message: String) : RuntimeException(message)
