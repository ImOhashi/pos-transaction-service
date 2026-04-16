package br.com.ohashi.postransactionservice.adapters.input.controllers.filters

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
            MDC.put(CORRELATION_ID_ATTRIBUTE, correlationId)

            request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId)
            response.setHeader(CORRELATION_ID_HEADER, correlationId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_ATTRIBUTE)
        }
    }

    private fun getCorrelationIdValue(request: HttpServletRequest): String =
        request.getHeader(CORRELATION_ID_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

    companion object {
        private const val CORRELATION_ID_HEADER: String = "X-Correlation-Id"
        private const val CORRELATION_ID_ATTRIBUTE: String = "correlationId"
    }
}