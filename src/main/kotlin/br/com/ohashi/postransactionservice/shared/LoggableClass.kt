package br.com.ohashi.postransactionservice.shared

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

abstract class LoggableClass() {
    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    protected inline fun <T> withMDC(
        vararg keyValues: Pair<String, Any>,
        action: () -> T
    ) = keyValues.forEach { (key, value) -> MDC.put(key, value as String) }.let {
        try {
            action()
        } finally {
            keyValues.forEach { (key, _) -> MDC.remove(key) }
        }
    }
}