package br.com.ohashi.postransactionservice.adapters.input.controllers.responses.error

import java.time.OffsetDateTime

data class ApiErrorResponse(
    val timestamp: OffsetDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val errors: List<ApiValidationError>
)
