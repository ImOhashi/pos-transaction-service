package br.com.ohashi.postransactionservice.adapters.output.gateway.interceptors

import br.com.ohashi.postransactionservice.shared.properties.ExternalMockProperties
import feign.RequestInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local", "dev")
@EnableConfigurationProperties(ExternalMockProperties::class)
class ExternalClientMockConfig(
    private val properties: ExternalMockProperties
) {

    @Bean
    fun mockScenarioRequestInterceptor(): RequestInterceptor =
        RequestInterceptor { template ->
            resolveScenario(template.path())?.let { scenario ->
                template.header(MOCK_SCENARIO_HEADER, scenario)
            }
        }

    private fun resolveScenario(path: String): String? {
        val scenario = when {
            path.startsWith(AUTHORIZE_PATH) -> properties.scenarios.authorize
            path.startsWith(CONFIRM_PATH) -> properties.scenarios.confirm
            path.startsWith(VOID_PATH) -> properties.scenarios.void
            else -> null
        }

        return if (scenario.isNullOrBlank()) null else scenario
    }

    companion object {
        private const val MOCK_SCENARIO_HEADER = "X-Mock-Scenario"
        private const val AUTHORIZE_PATH = "/external/transactions/authorize"
        private const val CONFIRM_PATH = "/external/transactions/confirm"
        private const val VOID_PATH = "/external/transactions/void"
    }
}
