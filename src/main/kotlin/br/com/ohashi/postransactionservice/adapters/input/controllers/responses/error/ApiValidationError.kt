package br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error

data class ApiValidationError(
    val field: String,
    val message: String
)
