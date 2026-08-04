package com.payment.payment_processing_system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIdempotencyIntegrationTests {

    private static final String TEST_DESCRIPTION_PREFIX = "idempotency-test-";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTestData() {
        jdbcTemplate.update("DELETE FROM payment_transactions WHERE description LIKE ?", TEST_DESCRIPTION_PREFIX + "%");
        jdbcTemplate.update("UPDATE accounts SET balance = ? WHERE account_number = ?", new BigDecimal("50000.0000"), "100000000001");
        jdbcTemplate.update("UPDATE accounts SET balance = ? WHERE account_number = ?", new BigDecimal("30000.0000"), "100000000002");
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalTransaction() throws Exception {
        String idempotencyKey = "IDEMPTEST-" + UUID.randomUUID().toString().replace("-", "");
        String description = TEST_DESCRIPTION_PREFIX + "replay";
        String requestBody = paymentRequestJson("10.00", description);

        HttpResponse<String> firstResponse = sendPaymentRequest(idempotencyKey, requestBody);
        assertThat(firstResponse.statusCode()).isEqualTo(201);

        JsonNode firstJson = objectMapper.readTree(firstResponse.body());
        assertThat(firstJson.path("idempotentReplay").asBoolean()).isFalse();

        String firstTransactionId = extractTransactionId(firstJson);

        HttpResponse<String> secondResponse = sendPaymentRequest(idempotencyKey, requestBody);
        assertThat(secondResponse.statusCode()).isEqualTo(200);

        JsonNode secondJson = objectMapper.readTree(secondResponse.body());
        assertThat(secondJson.path("idempotentReplay").asBoolean()).isTrue();
        assertThat(secondJson.path("transactionId").asText()).isEqualTo(firstTransactionId);
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadReturnsConflict() throws Exception {
        String idempotencyKey = "IDEMPTEST-" + UUID.randomUUID().toString().replace("-", "");
        String description = TEST_DESCRIPTION_PREFIX + "conflict";

        HttpResponse<String> firstResponse = sendPaymentRequest(idempotencyKey, paymentRequestJson("15.00", description));
        assertThat(firstResponse.statusCode()).isEqualTo(201);
        assertThat(objectMapper.readTree(firstResponse.body()).path("idempotentReplay").asBoolean()).isFalse();

        HttpResponse<String> secondResponse = sendPaymentRequest(idempotencyKey, paymentRequestJson("25.00", description));
        assertThat(secondResponse.statusCode()).isEqualTo(409);
        assertThat(objectMapper.readTree(secondResponse.body()).path("message").asText())
                .isEqualTo("The provided idempotency key has already been used for a different payment request.");
    }

    private HttpResponse<String> sendPaymentRequest(String idempotencyKey, String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/payments/send"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String paymentRequestJson(String amount, String description) {
        return """
                {
                  "senderAccountNumber": "100000000001",
                  "receiverAccountNumber": "100000000002",
                  "receiverIfscCode": "HDFC0005678",
                  "amount": %s,
                  "description": "%s",
                  "upiPin": "1234"
                }
                """.formatted(amount, description);
    }

    private String extractTransactionId(JsonNode root) {
        String transactionId = root.path("transactionId").asText();
        assertThat(transactionId).isNotBlank();
        return transactionId;
    }
}



