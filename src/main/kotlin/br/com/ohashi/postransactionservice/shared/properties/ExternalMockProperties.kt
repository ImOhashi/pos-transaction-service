package br.com.ohashi.postransactionservice.shared.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external.mock")
data class ExternalMockProperties(
    val scenarios: Scenarios = Scenarios()
) {
    data class Scenarios(
        val authorize: String? = null,
        val confirm: String? = null,
        val void: String? = null
    )
}
