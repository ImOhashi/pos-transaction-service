package br.com.ohashi.postransactionservice.application.ports.output.responses

enum class VoidStatus {
    VOIDED,
    ERROR;

    companion object {
        fun from(value: String): VoidStatus =
            entries.firstOrNull { it.name == value } ?: ERROR
    }
}
