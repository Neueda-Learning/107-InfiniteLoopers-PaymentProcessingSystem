package com.payment.payment_processing_system.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalExceptionHandlerIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("InvalidPaymentException should map to 400 VALIDATION_FAILED")
    void invalidPayment_shouldReturnValidationFailed() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), isNull()))
                .thenThrow(new InvalidPaymentException("Amount must be greater than zero"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.path("message").asText()).isEqualTo("Amount must be greater than zero");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/send");
    }

    @Test
    @DisplayName("InvalidPaymentException with transition message should map to INVALID_STATUS_TRANSITION")
    void invalidStatusTransition_shouldReturnSpecificErrorCode() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), isNull()))
                .thenThrow(new InvalidPaymentException("Invalid status transition from CREATED to COMPLETED"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/send");
    }

    @Test
    @DisplayName("PaymentValidationException should map to 400 VALIDATION_FAILED")
    void paymentValidation_shouldReturnBadRequest() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), isNull()))
                .thenThrow(new PaymentValidationException("Payment validation failed"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.path("message").asText()).isEqualTo("Payment validation failed");
    }

    @Test
    @DisplayName("InvalidUpiPinException should map to 401 INVALID_UPI_PIN")
    void invalidUpiPin_shouldReturnUnauthorized() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), isNull()))
                .thenThrow(new InvalidUpiPinException("Invalid UPI PIN"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(401);
        assertThat(body.path("errorCode").asText()).isEqualTo("INVALID_UPI_PIN");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/send");
    }

    @Test
    @DisplayName("DuplicatePaymentException should map to 409 DUPLICATE_PAYMENT")
    void duplicatePayment_shouldReturnConflict() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), eq("IDEMP-CONFLICT")))
                .thenThrow(new DuplicatePaymentException("Idempotency key already used"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), "IDEMP-CONFLICT");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(409);
        assertThat(body.path("errorCode").asText()).isEqualTo("DUPLICATE_PAYMENT");
        assertThat(body.path("message").asText()).isEqualTo("Idempotency key already used");
    }

    @Test
    @DisplayName("InsufficientBalanceException should map to 422 INSUFFICIENT_FUNDS")
    void insufficientFunds_shouldReturnUnprocessableEntity() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), isNull()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(422);
        assertThat(body.path("errorCode").asText()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/send");
    }

    @Test
    @DisplayName("MaxRetryExceededException should map to 429 RETRY_LIMIT_EXCEEDED")
    void maxRetryExceeded_shouldReturnTooManyRequests() throws Exception {
        when(paymentService.retryPayment("TXN-RETRY-429"))
                .thenThrow(new MaxRetryExceededException("Max retries reached"));

        HttpResponse<String> httpResponse = postNoBody("/api/payments/retry/TXN-RETRY-429");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(429);
        assertThat(body.path("errorCode").asText()).isEqualTo("RETRY_LIMIT_EXCEEDED");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/retry/TXN-RETRY-429");
    }

    @Test
    @DisplayName("PaymentFailedException should map to 502 PAYMENT_FAILED")
    void paymentFailed_shouldReturnBadGateway() throws Exception {
        when(paymentService.sendMoney(any(PaymentRequest.class), isNull()))
                .thenThrow(new PaymentFailedException("Bank gateway down"));

        HttpResponse<String> httpResponse = postJson("/api/payments/send", validRequestJson(), null);
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(502);
        assertThat(body.path("errorCode").asText()).isEqualTo("PAYMENT_FAILED");
        assertThat(body.path("message").asText()).isEqualTo("Bank gateway down");
    }

    @Test
    @DisplayName("Unhandled exception should map to 500 PROCESSING_ERROR")
    void unexpectedException_shouldReturnInternalServerError() throws Exception {
        when(paymentService.getTransaction("TXN-500"))
                .thenThrow(new RuntimeException("Unexpected crash"));

        HttpResponse<String> httpResponse = get("/api/payments/TXN-500");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(500);
        assertThat(body.path("errorCode").asText()).isEqualTo("PROCESSING_ERROR");
        assertThat(body.path("message").asText())
                .isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(body.path("path").asText()).isEqualTo("/api/payments/TXN-500");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postNoBody(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String payload, String idempotencyKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }

        builder.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String validRequestJson() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .receiverIfscCode("HDFC0005678")
                .amount(new BigDecimal("10.00"))
                .description("global-exception-test")
                .upiPin("1234")
                .build();
        return objectMapper.writeValueAsString(request);
    }
}

