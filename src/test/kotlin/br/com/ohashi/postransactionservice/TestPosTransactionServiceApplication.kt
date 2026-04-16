package br.com.ohashi.postransactionservice

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<PosTransactionServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
