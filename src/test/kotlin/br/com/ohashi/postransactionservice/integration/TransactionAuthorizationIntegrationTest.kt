package br.com.ohashi.postransactionservice.integration

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.TestcontainersConfiguration
import br.com.ohashi.postransactionservice.adapters.input.controllers.TransactionController
import br.com.ohashi.postransactionservice.adapters.input.controllers.requests.AuthorizeRequest
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizationStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(
    TestcontainersConfiguration::class,
    TransactionAuthorizationIntegrationTest.StubExternalAuthorizationConfiguration::class
)
class TransactionAuthorizationIntegrationTest {

    @Autowired
    private lateinit var transactionController: TransactionController

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM transactions")
        StubExternalAuthorizationConfiguration.counter.set(0)
    }

    @Test
    fun `should persist authorized transaction in postgres`() {
        val response = transactionController.authorize(
            AuthorizeRequest(
                nsu = "nsu-1",
                amount = BigDecimal("10.50"),
                terminalId = "terminal-1"
            )
        )

        val rows = jdbcTemplate.queryForList(
            "SELECT transaction_id, nsu, terminal_id, amount, status FROM transactions"
        )

        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(rows).hasSize(1)
        assertThat(rows.first()["transaction_id"]).isEqualTo("txn-1")
        assertThat(rows.first()["nsu"]).isEqualTo("nsu-1")
        assertThat(rows.first()["terminal_id"]).isEqualTo("terminal-1")
        assertThat(rows.first()["status"]).isEqualTo("AUTHORIZED")
    }

    @Test
    fun `should keep idempotency and avoid duplicate rows`() {
        val payload = AuthorizeRequest(
            nsu = "nsu-2",
            amount = BigDecimal("20.00"),
            terminalId = "terminal-2"
        )

        val firstResponse = transactionController.authorize(payload)
        val secondResponse = transactionController.authorize(payload)

        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long::class.java)

        assertThat(firstResponse.statusCode.value()).isEqualTo(200)
        assertThat(secondResponse.statusCode.value()).isEqualTo(200)
        assertThat(firstResponse.body?.transactionId).isEqualTo("txn-1")
        assertThat(secondResponse.body?.transactionId).isEqualTo("txn-1")
        assertThat(count).isEqualTo(1L)
    }

    @Test
    fun `should persist multiple different transactions in postgres`() {
        transactionController.authorize(
            AuthorizeRequest(
                nsu = "nsu-3",
                amount = BigDecimal("30.00"),
                terminalId = "terminal-3"
            )
        )
        transactionController.authorize(
            AuthorizeRequest(
                nsu = "nsu-4",
                amount = BigDecimal("40.00"),
                terminalId = "terminal-4"
            )
        )

        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long::class.java)

        assertThat(count).isEqualTo(2L)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class StubExternalAuthorizationConfiguration {

        companion object {
            val counter = AtomicInteger(0)
        }

        @Bean
        @Primary
        fun authorizeTransactionExternallyOutputPort(): AuthorizeTransactionExternallyOutputPort =
            object : AuthorizeTransactionExternallyOutputPort {
                override fun authorize(request: AuthorizeTransactionExternalRequest): AuthorizeTransactionExternalResult =
                    AuthorizeTransactionExternalResult(
                        transactionId = "txn-${counter.incrementAndGet()}",
                        result = AuthorizationStatus.AUTHORIZED,
                        approved = true,
                        message = "approved"
                    )
            }
    }
}
