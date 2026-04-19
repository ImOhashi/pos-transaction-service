package br.com.ohashi.postransactionservice.shared.observability

object CorrelationIdContext {
    const val HEADER_NAME: String = "X-Correlation-Id"
    const val MDC_KEY: String = "correlationId"
}
