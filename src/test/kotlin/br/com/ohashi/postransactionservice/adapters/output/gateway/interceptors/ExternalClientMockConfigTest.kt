package br.com.ohashi.postransactionservice.adapters.output.gateway.interceptors

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import br.com.ohashi.postransactionservice.shared.properties.ExternalMockProperties
import feign.RequestTemplate
import org.junit.jupiter.api.Test

class ExternalClientMockConfigTest {

    private val properties = ExternalMockProperties(
        scenarios = ExternalMockProperties.Scenarios(
            authorize = "authorize-success",
            confirm = "confirm-success",
            void = "void-success"
        )
    )

    private val interceptor = ExternalClientMockConfig(properties).mockScenarioRequestInterceptor()

    @Test
    fun `should add authorize scenario header when path matches`() {
        val template = RequestTemplate().uri("/external/transactions/authorize")

        interceptor.apply(template)

        assertThat(template.headers()["X-Mock-Scenario"]?.single()).isEqualTo("authorize-success")
    }

    @Test
    fun `should not add scenario header when path is not mapped`() {
        val template = RequestTemplate().uri("/external/transactions/refund")

        interceptor.apply(template)

        assertThat(template.headers()["X-Mock-Scenario"]).isNull()
    }

    @Test
    fun `should add confirm scenario header when path matches`() {
        val template = RequestTemplate().uri("/external/transactions/confirm")

        interceptor.apply(template)

        assertThat(template.headers()["X-Mock-Scenario"]?.single()).isEqualTo("confirm-success")
    }

    @Test
    fun `should add void scenario header when path matches`() {
        val template = RequestTemplate().uri("/external/transactions/void")

        interceptor.apply(template)

        assertThat(template.headers()["X-Mock-Scenario"]?.single()).isEqualTo("void-success")
    }

    @Test
    fun `should not add scenario header when configured value is blank`() {
        val blankProperties = ExternalMockProperties(
            scenarios = ExternalMockProperties.Scenarios(authorize = "   ")
        )
        val blankInterceptor = ExternalClientMockConfig(blankProperties).mockScenarioRequestInterceptor()
        val template = RequestTemplate().uri("/external/transactions/authorize")

        blankInterceptor.apply(template)

        assertThat(template.headers()["X-Mock-Scenario"]).isNull()
    }
}
