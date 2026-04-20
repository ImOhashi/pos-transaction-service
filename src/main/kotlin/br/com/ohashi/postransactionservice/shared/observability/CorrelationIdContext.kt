package br.com.ohashi.postransactionservice.shared.observability

object CorrelationIdContext {
    const val HEADER_NAME: String = "X-Correlation-Id"
    const val MDC_KEY: String = "correlationId"
    const val BAGGAGE_KEY: String = "correlationId"
    const val SPAN_ATTRIBUTE_KEY: String = "correlationId"
}
