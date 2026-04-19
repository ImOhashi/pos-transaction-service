package br.com.ohashi.postransactionservice.infrastructure

import br.com.ohashi.postransactionservice.application.core.usecases.AuthorizeTransactionUseCase
import br.com.ohashi.postransactionservice.application.core.usecases.ConfirmTransactionUseCase
import br.com.ohashi.postransactionservice.application.core.usecases.VoidTransactionUseCase
import br.com.ohashi.postransactionservice.application.ports.input.authorize.AuthorizeTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.confirm.ConfirmTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.input.void.VoidTransactionInputPort
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.ConfirmTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByNsuAndTerminalIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.FindTransactionByTransactionIdOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.SaveTransactionOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.VoidTransactionExternallyOutputPort
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

    @Bean
    fun confirmTransactionUseCase(
        findTransactionByTransactionIdOutputPort: FindTransactionByTransactionIdOutputPort,
        confirmTransactionExternallyOutputPort: ConfirmTransactionExternallyOutputPort,
        saveTransactionOutputPort: SaveTransactionOutputPort
    ): ConfirmTransactionInputPort =
        ConfirmTransactionUseCase(
            findTransactionByTransactionIdOutputPort = findTransactionByTransactionIdOutputPort,
            confirmTransactionExternallyOutputPort = confirmTransactionExternallyOutputPort,
            saveTransactionOutputPort = saveTransactionOutputPort
        )

    @Bean
    fun voidTransactionUseCase(
        findTransactionByTransactionIdOutputPort: FindTransactionByTransactionIdOutputPort,
        findTransactionByNsuAndTerminalIdOutputPort: FindTransactionByNsuAndTerminalIdOutputPort,
        voidTransactionExternallyOutputPort: VoidTransactionExternallyOutputPort,
        saveTransactionOutputPort: SaveTransactionOutputPort
    ): VoidTransactionInputPort =
        VoidTransactionUseCase(
            findTransactionByTransactionIdOutputPort = findTransactionByTransactionIdOutputPort,
            findTransactionByNsuAndTerminalIdOutputPort = findTransactionByNsuAndTerminalIdOutputPort,
            voidTransactionExternallyOutputPort = voidTransactionExternallyOutputPort,
            saveTransactionOutputPort = saveTransactionOutputPort
        )
}
