package br.com.ohashi.postransactionservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PosTransactionServiceApplication

fun main(args: Array<String>) {
    runApplication<PosTransactionServiceApplication>(*args)
}
