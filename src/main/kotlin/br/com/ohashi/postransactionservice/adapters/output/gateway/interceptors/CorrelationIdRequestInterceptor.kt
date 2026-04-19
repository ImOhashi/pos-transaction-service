package br.com.ohashi.postransactionservice.adapters.output.gateway.interceptors

import br.com.ohashi.postransactionservice.shared.observability.CorrelationIdContext
import feign.RequestInterceptor
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CorrelationIdFeignConfiguration {

    @Bean
    fun correlationIdRequestInterceptor(): RequestInterceptor =
        RequestInterceptor { template ->
            MDC.get(CorrelationIdContext.MDC_KEY)
                ?.takeIf { it.isNotBlank() }
                ?.let { correlationId ->
                    template.header(CorrelationIdContext.HEADER_NAME, correlationId)
                }
        }
}
