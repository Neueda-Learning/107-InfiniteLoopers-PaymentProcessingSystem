package com.payment.payment_processing_system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.exception.RetryLimitExceededException;
import com.payment.payment_processing_system.exception.TransactionNotFoundException;
import com.payment.payment_processing_system.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /api/payments/send should return 201 for a new payment")
    void sendMoney_whenNewPayment_shouldReturnCreated() throws Exception {
        PaymentRequest request = validRequest();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN-NEW-100")
                .paymentStatus("COMPLETED")
                .message("Payment processed successfully")
                .idempotentReplay(false)
                .amount(new BigDecimal("10.00"))
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .transactionTime(LocalDateTime.of(2026, 8, 5, 9, 30, 0))
                .build();

        when(paymentService.sendMoney(any(PaymentRequest.class), isNull())).thenReturn(response);

        HttpResponse<String> httpResponse = postJson("/api/payments/send", objectMapper.writeValueAsString(request), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(201);
        assertThat(body.path("transactionId").asText()).isEqualTo("TXN-NEW-100");
        assertThat(body.path("idempotentReplay").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("POST /api/payments/send should return 200 for idempotent replay")
    void sendMoney_whenIdempotentReplay_shouldReturnOk() throws Exception {
        PaymentRequest request = validRequest();

        PaymentResponse response = PaymentResponse.builder()
                .transactionId("TXN-NEW-100")
                .paymentStatus("COMPLETED")
                .message("Idempotent replay")
                .idempotentReplay(true)
                .amount(new BigDecimal("10.00"))
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .transactionTime(LocalDateTime.of(2026, 8, 5, 9, 30, 0))
                .build();

        when(paymentService.sendMoney(any(PaymentRequest.class), eq("IDEMP-001"))).thenReturn(response);

        HttpResponse<String> httpResponse = postJson("/api/payments/send", objectMapper.writeValueAsString(request), "IDEMP-001");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.path("transactionId").asText()).isEqualTo("TXN-NEW-100");
        assertThat(body.path("idempotentReplay").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("POST /api/payments/send should return 400 when request is invalid")
    void sendMoney_whenValidationFails_shouldReturnBadRequest() throws Exception {
        String invalidPayload = """
                {
                  "senderAccountNumber": "",
                  "receiverAccountNumber": "100000000002",
                  "receiverIfscCode": "HDFC0005678",
                  "amount": 0,
                  "upiPin": "12"
                }
                """;

        HttpResponse<String> httpResponse = postJson("/api/payments/send", invalidPayload, null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/send");
    }

    @Test
    @DisplayName("GET /api/payments/{id} should return 404 when transaction is missing")
    void getTransaction_whenMissing_shouldReturnNotFound() throws Exception {
        when(paymentService.getTransaction("TXN-404"))
                .thenThrow(new TransactionNotFoundException("Transaction not found with id: TXN-404"));

        HttpResponse<String> httpResponse = get("/api/payments/TXN-404");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("PAYMENT_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Transaction not found with id: TXN-404");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/TXN-404");
    }

    @Test
    @DisplayName("POST /api/payments/retry/{id} should return 429 when retry limit exceeded")
    void retryPayment_whenRetryLimitExceeded_shouldReturnTooManyRequests() throws Exception {
        when(paymentService.retryPayment("TXN-RETRY-001"))
                .thenThrow(new RetryLimitExceededException("Retry limit exceeded for TXN-RETRY-001"));

        HttpResponse<String> httpResponse = postJson("/api/payments/retry/TXN-RETRY-001", null, null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(429);
        assertThat(body.path("errorCode").asText()).isEqualTo("RETRY_LIMIT_EXCEEDED");
        assertThat(body.path("message").asText()).isEqualTo("Retry limit exceeded for TXN-RETRY-001");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/retry/TXN-RETRY-001");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String payload, String idempotencyKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }

        if (payload != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private PaymentRequest validRequest() {
        return PaymentRequest.builder()
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .receiverIfscCode("HDFC0005678")
                .amount(new BigDecimal("10.00"))
                .description("integration-test")
                .upiPin("1234")
                .build();
    }
}





