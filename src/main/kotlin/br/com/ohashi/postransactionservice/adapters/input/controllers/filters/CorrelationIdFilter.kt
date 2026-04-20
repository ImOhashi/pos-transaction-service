package br.com.ohashi.postransactionservice.adapters.input.controllers.filters

import br.com.ohashi.postransactionservice.shared.observability.CorrelationIdContext
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class CorrelationIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId: String = getCorrelationIdValue(request)
        val baggage: Baggage = Baggage.current()
            .toBuilder()
            .put(CorrelationIdContext.BAGGAGE_KEY, correlationId)
            .build()
        val baggageScope: Scope = baggage.storeInContext(Context.current()).makeCurrent()

        try {
            MDC.put(CorrelationIdContext.MDC_KEY, correlationId)
            Span.current().setAttribute(CorrelationIdContext.SPAN_ATTRIBUTE_KEY, correlationId)

            request.setAttribute(CorrelationIdContext.MDC_KEY, correlationId)
            response.setHeader(CorrelationIdContext.HEADER_NAME, correlationId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY)
            baggageScope.close()
        }
    }

    private fun getCorrelationIdValue(request: HttpServletRequest): String =
        request.getHeader(CorrelationIdContext.HEADER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
}
