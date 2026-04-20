package br.com.ohashi.postransactionservice.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import br.com.ohashi.postransactionservice.TestcontainersConfiguration
import br.com.ohashi.postransactionservice.application.core.domain.enums.TransactionStatus
import br.com.ohashi.postransactionservice.application.ports.output.AuthorizeTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.ConfirmTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.VoidTransactionExternallyOutputPort
import br.com.ohashi.postransactionservice.application.ports.output.requests.AuthorizeTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.requests.ConfirmTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.requests.VoidTransactionExternalRequest
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizationStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.AuthorizeTransactionExternalResult
import br.com.ohashi.postransactionservice.application.ports.output.responses.ConfirmationStatus
import br.com.ohashi.postransactionservice.application.ports.output.responses.VoidStatus
import feign.Request
import feign.RequestTemplate
import feign.RetryableException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(
    TestcontainersConfiguration::class,
    TransactionFlowIntegrationTest.StubExternalTransactionsConfiguration::class
)
class TransactionFlowIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM transactions")
        StubExternalTransactionsConfiguration.reset()
    }

    @Test
    fun `should authorize and persist transaction`() {
        postJson(
            path = "/v1/pos/transactions/authorize",
            body = """
                {
                  "nsu": "nsu-1",
                  "amount": 10.50,
                  "terminalId": "terminal-1"
                }
            """.trimIndent()
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactionId").value("txn-1"))

        assertThat(countTransactions()).isEqualTo(1L)
        assertThat(statusOf("txn-1")).isEqualTo("AUTHORIZED")
        assertThat(StubExternalTransactionsConfiguration.authorizeCalls.get()).isEqualTo(1)
    }

    @Test
    fun `should keep authorize idempotency and avoid duplicate external call`() {
        val payload = """
            {
              "nsu": "nsu-2",
              "amount": 20.00,
              "terminalId": "terminal-2"
            }
        """.trimIndent()

        postJson("/v1/pos/transactions/authorize", payload)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactionId").value("txn-1"))

        postJson("/v1/pos/transactions/authorize", payload)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactionId").value("txn-1"))

        assertThat(countTransactions()).isEqualTo(1L)
        assertThat(StubExternalTransactionsConfiguration.authorizeCalls.get()).isEqualTo(1)
    }

    @Test
    fun `should not persist authorize transaction when external rejects`() {
        StubExternalTransactionsConfiguration.authorizeMode = StubExternalTransactionsConfiguration.AuthorizeMode.REJECTED

        postJson(
            "/v1/pos/transactions/authorize",
            """
                {
                  "nsu": "nsu-3",
                  "amount": 30.00,
                  "terminalId": "terminal-3"
                }
            """.trimIndent()
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("External authorization was rejected with result=NON_AUTHORIZED."))

        assertThat(countTransactions()).isEqualTo(0L)
    }

    @Test
    fun `should not persist authorize transaction on technical external failure`() {
        StubExternalTransactionsConfiguration.authorizeMode = StubExternalTransactionsConfiguration.AuthorizeMode.TIMEOUT

        postJson(
            "/v1/pos/transactions/authorize",
            """
                {
                  "nsu": "nsu-4",
                  "amount": 40.00,
                  "terminalId": "terminal-4"
                }
            """.trimIndent()
        )
            .andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.message").value("External authorization timed out after retry attempts."))

        assertThat(countTransactions()).isEqualTo(0L)
    }

    @Test
    fun `should confirm authorized transaction and persist confirmed status`() {
        insertTransaction(transactionId = "txn-confirm-1", status = TransactionStatus.AUTHORIZED)

        postJson("/v1/pos/transactions/confirm", """{"transactionId":"txn-confirm-1"}""")
            .andExpect(status().isNoContent)

        assertThat(statusOf("txn-confirm-1")).isEqualTo("CONFIRMED")
        assertThat(StubExternalTransactionsConfiguration.confirmCalls.get()).isEqualTo(1)
    }

    @Test
    fun `should keep confirm idempotency without external call`() {
        insertTransaction(transactionId = "txn-confirm-2", status = TransactionStatus.CONFIRMED)

        postJson("/v1/pos/transactions/confirm", """{"transactionId":"txn-confirm-2"}""")
            .andExpect(status().isNoContent)

        assertThat(statusOf("txn-confirm-2")).isEqualTo("CONFIRMED")
        assertThat(StubExternalTransactionsConfiguration.confirmCalls.get()).isEqualTo(0)
    }

    @Test
    fun `should reject confirm when transaction is voided`() {
        insertTransaction(transactionId = "txn-confirm-3", status = TransactionStatus.VOIDED)

        postJson("/v1/pos/transactions/confirm", """{"transactionId":"txn-confirm-3"}""")
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("Transaction cannot be confirmed because it is VOIDED."))

        assertThat(statusOf("txn-confirm-3")).isEqualTo("VOIDED")
        assertThat(StubExternalTransactionsConfiguration.confirmCalls.get()).isEqualTo(0)
    }

    @Test
    fun `should not persist confirm status on technical external failure`() {
        insertTransaction(transactionId = "txn-confirm-4", status = TransactionStatus.AUTHORIZED)
        StubExternalTransactionsConfiguration.confirmMode = StubExternalTransactionsConfiguration.ConfirmMode.TIMEOUT

        postJson("/v1/pos/transactions/confirm", """{"transactionId":"txn-confirm-4"}""")
            .andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.message").value("External confirmation timed out after retry attempts."))

        assertThat(statusOf("txn-confirm-4")).isEqualTo("AUTHORIZED")
    }

    @Test
    fun `should return not found when confirm transaction does not exist`() {
        postJson("/v1/pos/transactions/confirm", """{"transactionId":"txn-missing"}""")
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transaction not found for transactionId=txn-missing"))
    }

    @Test
    fun `should void transaction by transaction id and persist voided status`() {
        insertTransaction(transactionId = "txn-void-1", status = TransactionStatus.CONFIRMED)

        postJson("/v1/pos/transactions/void", """{"transactionId":"txn-void-1"}""")
            .andExpect(status().isNoContent)

        assertThat(statusOf("txn-void-1")).isEqualTo("VOIDED")
        assertThat(StubExternalTransactionsConfiguration.voidCalls.get()).isEqualTo(1)
    }

    @Test
    fun `should void transaction by nsu and terminal id`() {
        insertTransaction(
            transactionId = "txn-void-2",
            status = TransactionStatus.AUTHORIZED,
            nsu = "nsu-void-2",
            terminalId = "terminal-void-2"
        )

        postJson(
            "/v1/pos/transactions/void",
            """
                {
                  "nsu": "nsu-void-2",
                  "terminalId": "terminal-void-2"
                }
            """.trimIndent()
        )
            .andExpect(status().isNoContent)

        assertThat(statusOf("txn-void-2")).isEqualTo("VOIDED")
        assertThat(StubExternalTransactionsConfiguration.voidCalls.get()).isEqualTo(1)
    }

    @Test
    fun `should keep void idempotency without external call`() {
        insertTransaction(transactionId = "txn-void-3", status = TransactionStatus.VOIDED)

        postJson("/v1/pos/transactions/void", """{"transactionId":"txn-void-3"}""")
            .andExpect(status().isNoContent)

        assertThat(statusOf("txn-void-3")).isEqualTo("VOIDED")
        assertThat(StubExternalTransactionsConfiguration.voidCalls.get()).isEqualTo(0)
    }

    @Test
    fun `should not persist void status on technical external failure`() {
        insertTransaction(transactionId = "txn-void-4", status = TransactionStatus.CONFIRMED)
        StubExternalTransactionsConfiguration.voidMode = StubExternalTransactionsConfiguration.VoidMode.TIMEOUT

        postJson("/v1/pos/transactions/void", """{"transactionId":"txn-void-4"}""")
            .andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.message").value("External void timed out after retry attempts."))

        assertThat(statusOf("txn-void-4")).isEqualTo("CONFIRMED")
    }

    @Test
    fun `should return not found when void transaction does not exist`() {
        postJson(
            "/v1/pos/transactions/void",
            """
                {
                  "nsu": "nsu-missing",
                  "terminalId": "terminal-missing"
                }
            """.trimIndent()
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transaction not found for nsu=nsu-missing and terminalId=terminal-missing"))
    }

    @Test
    fun `should return validation error when void payload has invalid identification`() {
        postJson("/v1/pos/transactions/void", """{"transactionId":"txn-1","nsu":"nsu-1","terminalId":"terminal-1"}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0].field").value("hasValidIdentification"))
            .andExpect(jsonPath("$.errors[0].message").value("Inform transactionId or nsu with terminalId."))
    }

    private fun insertTransaction(
        transactionId: String,
        status: TransactionStatus,
        nsu: String = "nsu-$transactionId",
        terminalId: String = "terminal-$transactionId",
        amount: BigDecimal = BigDecimal("10.00")
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO transactions (transaction_id, nsu, terminal_id, amount, status, created_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """.trimIndent(),
            transactionId,
            nsu,
            terminalId,
            amount,
            status.name
        )
    }

    private fun countTransactions(): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long::class.java) ?: 0L

    private fun postJson(path: String, body: String): ResultActions =
        mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )

    private fun statusOf(transactionId: String): String =
        jdbcTemplate.queryForObject(
            "SELECT status FROM transactions WHERE transaction_id = ?",
            String::class.java,
            transactionId
        ) ?: error("Status not found for transactionId=$transactionId")

    @TestConfiguration(proxyBeanMethods = false)
    class StubExternalTransactionsConfiguration {

        enum class AuthorizeMode { SUCCESS, REJECTED, TIMEOUT }
        enum class ConfirmMode { CONFIRMED, ERROR, TIMEOUT }
        enum class VoidMode { VOIDED, ERROR, TIMEOUT }

        companion object {
            var authorizeMode: AuthorizeMode = AuthorizeMode.SUCCESS
            var confirmMode: ConfirmMode = ConfirmMode.CONFIRMED
            var voidMode: VoidMode = VoidMode.VOIDED

            val authorizeCalls = AtomicInteger(0)
            val confirmCalls = AtomicInteger(0)
            val voidCalls = AtomicInteger(0)

            fun reset() {
                authorizeMode = AuthorizeMode.SUCCESS
                confirmMode = ConfirmMode.CONFIRMED
                voidMode = VoidMode.VOIDED
                authorizeCalls.set(0)
                confirmCalls.set(0)
                voidCalls.set(0)
            }
        }

        @Bean
        @Primary
        fun authorizeTransactionExternallyOutputPort(): AuthorizeTransactionExternallyOutputPort =
            object : AuthorizeTransactionExternallyOutputPort {
                override fun authorize(request: AuthorizeTransactionExternalRequest): AuthorizeTransactionExternalResult {
                    authorizeCalls.incrementAndGet()
                    return when (authorizeMode) {
                        AuthorizeMode.SUCCESS -> AuthorizeTransactionExternalResult(
                            transactionId = "txn-${authorizeCalls.get()}",
                            result = AuthorizationStatus.AUTHORIZED,
                            approved = true,
                            message = "approved"
                        )

                        AuthorizeMode.REJECTED -> AuthorizeTransactionExternalResult(
                            transactionId = "txn-rejected",
                            result = AuthorizationStatus.NON_AUTHORIZED,
                            approved = false,
                            message = "denied"
                        )

                        AuthorizeMode.TIMEOUT -> throw retryableException("/external/transactions/authorize")
                    }
                }
            }

        @Bean
        @Primary
        fun confirmTransactionExternallyOutputPort(): ConfirmTransactionExternallyOutputPort =
            object : ConfirmTransactionExternallyOutputPort {
                override fun confirm(request: ConfirmTransactionExternalRequest): ConfirmationStatus {
                    confirmCalls.incrementAndGet()
                    return when (confirmMode) {
                        ConfirmMode.CONFIRMED -> ConfirmationStatus.CONFIRMED
                        ConfirmMode.ERROR -> ConfirmationStatus.ERROR
                        ConfirmMode.TIMEOUT -> throw retryableException("/external/transactions/confirm")
                    }
                }
            }

        @Bean
        @Primary
        fun voidTransactionExternallyOutputPort(): VoidTransactionExternallyOutputPort =
            object : VoidTransactionExternallyOutputPort {
                override fun voidTransaction(request: VoidTransactionExternalRequest): VoidStatus {
                    voidCalls.incrementAndGet()
                    return when (voidMode) {
                        VoidMode.VOIDED -> VoidStatus.VOIDED
                        VoidMode.ERROR -> VoidStatus.ERROR
                        VoidMode.TIMEOUT -> throw retryableException("/external/transactions/void")
                    }
                }
            }

        private fun retryableException(path: String): RetryableException =
            RetryableException(
                504,
                "timeout",
                Request.HttpMethod.POST,
                Date(),
                Request.create(
                    Request.HttpMethod.POST,
                    "http://localhost$path",
                    emptyMap(),
                    null,
                    RequestTemplate()
                )
            )
    }
}
