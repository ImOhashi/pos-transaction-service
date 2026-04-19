package br.com.ohashi.postransactionservice.adapters.input.controllers.filters

import br.com.ohashi.postransactionservice.shared.observability.CorrelationIdContext
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

        try {
            MDC.put(CorrelationIdContext.MDC_KEY, correlationId)

            request.setAttribute(CorrelationIdContext.MDC_KEY, correlationId)
            response.setHeader(CorrelationIdContext.HEADER_NAME, correlationId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY)
        }
    }

    private fun getCorrelationIdValue(request: HttpServletRequest): String =
        request.getHeader(CorrelationIdContext.HEADER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
}
