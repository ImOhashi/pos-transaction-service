package br.com.ohashi.postransactionservice.shared.observability

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import org.springframework.stereotype.Component

object TracingSupport {
    private const val INSTRUMENTATION_NAME = "br.com.ohashi.postransactionservice"

    @Volatile
    private var openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()

    fun setOpenTelemetry(openTelemetry: OpenTelemetry) {
        this.openTelemetry = openTelemetry
    }

    fun <T> inSpan(
        name: String,
        vararg attributes: Pair<String, Any?>,
        block: (Span) -> T
    ): T = inSpan(name, SpanKind.INTERNAL, attributes.toList(), block)

    fun <T> inClientSpan(
        name: String,
        vararg attributes: Pair<String, Any?>,
        block: (Span) -> T
    ): T = inSpan(name, SpanKind.CLIENT, attributes.toList(), block)

    private fun <T> inSpan(
        name: String,
        kind: SpanKind,
        attributes: List<Pair<String, Any?>>,
        block: (Span) -> T
    ): T {
        val span = openTelemetry.getTracer(INSTRUMENTATION_NAME)
            .spanBuilder(name)
            .setSpanKind(kind)
            .startSpan()

        attributes.forEach { (key, value) ->
            span.setAttributeSafely(key, value)
        }

        val scope = span.makeCurrent()

        return try {
            block(span)
        } catch (exception: Exception) {
            span.recordException(exception)
            span.setStatus(StatusCode.ERROR)
            throw exception
        } finally {
            scope.close()
            span.end()
        }
    }

    fun Span.setAttributeSafely(key: String, value: Any?) {
        when (value) {
            null -> Unit
            is String -> setAttribute(key, value)
            is Long -> setAttribute(key, value)
            is Int -> setAttribute(key, value.toLong())
            is Double -> setAttribute(key, value)
            is Float -> setAttribute(key, value.toDouble())
            is Boolean -> setAttribute(key, value)
            else -> setAttribute(key, value.toString())
        }
    }
}

@Component
class TracingSupportInitializer(
    openTelemetry: OpenTelemetry
) {
    init {
        TracingSupport.setOpenTelemetry(openTelemetry)
    }
}
