package br.com.ohashi.postransactionservice.shared

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.AuthorizeRequest
import br.com.ohashi.postransactionservice.shared.exceptions.TransactionAlreadyExistsException
import br.com.ohashi.postransactionservice.shared.observability.CorrelationIdContext
import br.com.ohashi.postransactionservice.shared.properties.ExternalMockProperties
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.math.BigDecimal
import kotlin.jvm.functions.Function0

class SharedCoverageTest {

    @Test
    fun `should expose authorize request fields and map to command`() {
        val request = AuthorizeRequest(
            nsu = "nsu-1",
            amount = BigDecimal("12.50"),
            terminalId = "terminal-1"
        )
        val command = request.toCommand()

        assertThat(request.nsu).isEqualTo("nsu-1")
        assertThat(request.amount).isEqualTo(BigDecimal("12.50"))
        assertThat(request.terminalId).isEqualTo("terminal-1")
        assertThat(command.nsu).isEqualTo("nsu-1")
        assertThat(command.amount).isEqualTo(BigDecimal("12.50"))
        assertThat(command.terminalId).isEqualTo("terminal-1")
    }

    @Test
    fun `should expose correlation id constants`() {
        assertThat(CorrelationIdContext.HEADER_NAME).isEqualTo("X-Correlation-Id")
        assertThat(CorrelationIdContext.MDC_KEY).isEqualTo("correlationId")
    }

    @Test
    fun `should expose external mock properties values`() {
        val properties = ExternalMockProperties(
            scenarios = ExternalMockProperties.Scenarios(
                authorize = "authorize-success",
                confirm = "confirm-success",
                void = "void-success"
            )
        )

        assertThat(properties.scenarios.authorize).isEqualTo("authorize-success")
        assertThat(properties.scenarios.confirm).isEqualTo("confirm-success")
        assertThat(properties.scenarios.void).isEqualTo("void-success")
    }

    @Test
    fun `should expose external mock properties defaults`() {
        val properties = ExternalMockProperties()

        assertThat(properties.scenarios.authorize).isEqualTo(null)
        assertThat(properties.scenarios.confirm).isEqualTo(null)
        assertThat(properties.scenarios.void).isEqualTo(null)
    }

    @Test
    fun `should expose transaction already exists exception message`() {
        val exception = TransactionAlreadyExistsException("already exists")

        assertThat(exception.message).isEqualTo("already exists")
    }

    @Test
    fun `should restore mdc values after executing action`() {
        val subject = object : LoggableClass() {
            fun execute(action: () -> String): String = withMDC(
                "correlationId" to "new-correlation",
                "nsu" to null
            ) { action() }
        }
        MDC.put("correlationId", "old-correlation")
        MDC.put("nsu", "old-nsu")

        try {
            val result = subject.execute {
                assertThat(MDC.get("correlationId")).isEqualTo("new-correlation")
                assertThat(MDC.get("nsu")).isEqualTo(null)
                "done"
            }

            assertThat(result).isEqualTo("done")
            assertThat(MDC.get("correlationId")).isEqualTo("old-correlation")
            assertThat(MDC.get("nsu")).isEqualTo("old-nsu")
        } finally {
            MDC.clear()
        }
    }

    @Test
    fun `should remove key when null value is provided and there is no previous value`() {
        val subject = object : LoggableClass() {}

        try {
            invokeWithMdc(subject, arrayOf("nsu" to null)) {
                assertThat(MDC.get("nsu")).isEqualTo(null)
                Unit
            }

            assertThat(MDC.get("nsu")).isEqualTo(null)
        } finally {
            MDC.clear()
        }
    }

    @Test
    fun `should restore previous mdc value when overriding existing entry`() {
        val subject = object : LoggableClass() {}
        MDC.put("terminalId", "terminal-1")

        try {
            invokeWithMdc(subject, arrayOf("terminalId" to "terminal-2")) {
                assertThat(MDC.get("terminalId")).isEqualTo("terminal-2")
                Unit
            }

            assertThat(MDC.get("terminalId")).isEqualTo("terminal-1")
        } finally {
            MDC.clear()
        }
    }

    private fun invokeWithMdc(
        subject: LoggableClass,
        keyValues: Array<Pair<String, Any?>>,
        action: () -> Unit
    ) {
        val method = LoggableClass::class.java.getDeclaredMethod(
            "withMDC",
            emptyArray<Pair<String, Any?>>()::class.java,
            Function0::class.java
        )
        method.isAccessible = true
        method.invoke(subject, keyValues, object : Function0<Unit> {
            override fun invoke() {
                action()
            }
        })
    }
}
