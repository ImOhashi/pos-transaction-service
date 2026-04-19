package br.com.ohashi.postransactionservice.infrastructure

import br.com.ohashi.postransactionservice.application.core.usecases.AuthorizeTransactionUseCase
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfiguration {

    @Bean
    fun authorizeTransactionUseCase(
        findTransactionByNsuAndTerminalIdOutputPort: FindTransactionByNsuAndTerminalIdOutputPort,
        authorizeTransactionExternallyOutputPort: AuthorizeTransactionExternallyOutputPort,
        saveTransactionOutputPort: SaveTransactionOutputPort
    ): AuthorizeTransactionInputPort =
        AuthorizeTransactionUseCase(
            findTransactionByNsuAndTerminalIdOutputPort = findTransactionByNsuAndTerminalIdOutputPort,
            authorizeTransactionExternallyOutputPort = authorizeTransactionExternallyOutputPort,
            saveTransactionOutputPort = saveTransactionOutputPort
        )
}
