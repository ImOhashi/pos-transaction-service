package br.com.ohashi.postransactionservice.shared.exceptions

class ExternalAuthorizationRejectedException(override val message: String) : RuntimeException(message)
