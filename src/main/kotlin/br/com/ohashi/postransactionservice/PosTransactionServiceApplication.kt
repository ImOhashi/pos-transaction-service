package br.com.ohashi.postransactionservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.boot.runApplication

@EnableFeignClients
@SpringBootApplication
class PosTransactionServiceApplication

fun main(args: Array<String>) {
    runApplication<PosTransactionServiceApplication>(*args)
}
