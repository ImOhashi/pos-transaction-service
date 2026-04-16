package br.com.ohashi.postransactionservice.infrastructure

import br.com.ohashi.postransactionservice.application.core.usecases.AuthorizeTransactionUseCase
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfiguration {

    @Bean
    fun authorizeTransactionUseCase(): AuthorizeTransactionInputPort =
        AuthorizeTransactionUseCase()
}