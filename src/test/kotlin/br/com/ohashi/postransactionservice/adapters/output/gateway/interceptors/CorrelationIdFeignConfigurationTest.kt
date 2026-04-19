package br.com.ohashi.postransactionservice.adapters.output.gateway.interceptors

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import br.com.ohashi.postransactionservice.shared.observability.CorrelationIdContext
import feign.RequestTemplate
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class CorrelationIdFeignConfigurationTest {

    private val interceptor = CorrelationIdFeignConfiguration().correlationIdRequestInterceptor()

    @Test
    fun `should add correlation id header from mdc`() {
        val template = RequestTemplate()
        MDC.put(CorrelationIdContext.MDC_KEY, "corr-123")

        try {
            interceptor.apply(template)
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY)
        }

        assertThat(template.headers()[CorrelationIdContext.HEADER_NAME]?.single()).isEqualTo("corr-123")
    }

    @Test
    fun `should not add header when mdc is blank`() {
        val template = RequestTemplate()
        MDC.put(CorrelationIdContext.MDC_KEY, " ")

        try {
            interceptor.apply(template)
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY)
        }

        assertThat(template.headers()[CorrelationIdContext.HEADER_NAME]).isNull()
    }

    @Test
    fun `should not add header when mdc does not contain correlation id`() {
        val template = RequestTemplate()
        MDC.remove(CorrelationIdContext.MDC_KEY)

        interceptor.apply(template)

        assertThat(template.headers()[CorrelationIdContext.HEADER_NAME]).isNull()
    }
}
