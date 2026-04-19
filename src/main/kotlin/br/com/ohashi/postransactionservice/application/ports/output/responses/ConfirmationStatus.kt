package br.com.ohashi.postransactionservice.application.ports.output.responses

enum class ConfirmationStatus {
    CONFIRMED,
    ALREADY_CONFIRMED,
    ERROR;

    companion object {
        fun from(value: String): ConfirmationStatus =
            values().firstOrNull { it.name == value } ?: ERROR
    }
}
