package br.com.ohashi.postransactionservice.adapters.input.controllers.filters

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import br.com.ohashi.postransactionservice.shared.observability.CorrelationIdContext
import io.opentelemetry.api.baggage.Baggage
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(MockKExtension::class)
class CorrelationIdFilterTest {

    @MockK
    private lateinit var filterChain: FilterChain

    @Test
    fun `should reuse correlation id from request header`() {
        var baggageValueDuringChain: String? = null
        every { filterChain.doFilter(any<ServletRequest>(), any<ServletResponse>()) } returns Unit
        every { filterChain.doFilter(any<ServletRequest>(), any<ServletResponse>()) } answers {
            baggageValueDuringChain = Baggage.current().getEntryValue(CorrelationIdContext.BAGGAGE_KEY)
        }
        val request = MockHttpServletRequest().apply {
            addHeader(CorrelationIdContext.HEADER_NAME, "corr-123")
        }
        val response = MockHttpServletResponse()

        CorrelationIdFilter().doFilter(request, response, filterChain)

        assertThat(response.getHeader(CorrelationIdContext.HEADER_NAME)).isEqualTo("corr-123")
        assertThat(request.getAttribute(CorrelationIdContext.MDC_KEY) as String).isEqualTo("corr-123")
        assertThat(baggageValueDuringChain).isEqualTo("corr-123")
        assertThat(Baggage.current().getEntryValue(CorrelationIdContext.BAGGAGE_KEY)).isEqualTo(null)
        verify(exactly = 1) { filterChain.doFilter(any<ServletRequest>(), any<ServletResponse>()) }
    }

    @Test
    fun `should generate correlation id when request header is blank`() {
        every { filterChain.doFilter(any<ServletRequest>(), any<ServletResponse>()) } returns Unit
        val request = MockHttpServletRequest().apply {
            addHeader(CorrelationIdContext.HEADER_NAME, " ")
        }
        val response = MockHttpServletResponse()

        CorrelationIdFilter().doFilter(request, response, filterChain)

        assertThat(response.getHeader(CorrelationIdContext.HEADER_NAME)).isNotNull()
        assertThat(request.getAttribute(CorrelationIdContext.MDC_KEY) as String).isEqualTo(
            response.getHeader(CorrelationIdContext.HEADER_NAME)
        )
    }
}
