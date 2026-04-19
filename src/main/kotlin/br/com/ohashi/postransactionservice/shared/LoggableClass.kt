package br.com.ohashi.postransactionservice.shared

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

abstract class LoggableClass() {
    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    protected inline fun <T> withMDC(
        vararg keyValues: Pair<String, Any?>,
        action: () -> T
    ): T {
        val previousValues = keyValues.associate { (key, _) -> key to MDC.get(key) }

        keyValues.forEach { (key, value) ->
            if (value == null) {
                MDC.remove(key)
            } else {
                MDC.put(key, value.toString())
            }
        }

        return try {
            action()
        } finally {
            previousValues.forEach { (key, previousValue) ->
                if (previousValue == null) {
                    MDC.remove(key)
                } else {
                    MDC.put(key, previousValue)
                }
            }
        }
    }
}
