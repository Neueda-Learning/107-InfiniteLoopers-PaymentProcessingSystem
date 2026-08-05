package com.payment.payment_processing_system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_processing_system.dto.SupportDashboardResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.service.SupportService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SupportControllerIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @MockitoBean
    private SupportService supportService;

    @Test
    @DisplayName("GET /api/support/dashboard should return dashboard metrics")
    void getDashboard_shouldReturnMetrics() throws Exception {
        SupportDashboardResponse response = SupportDashboardResponse.builder()
                .totalCustomers(12L)
                .totalTransactions(50L)
                .successfulTransactions(45L)
                .failedTransactions(5L)
                .totalCreditAmount(new BigDecimal("15000.75"))
                .totalDebitAmount(new BigDecimal("15000.75"))
                .build();

        when(supportService.getDashboard()).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/support/dashboard");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.path("totalCustomers").asLong()).isEqualTo(12L);
        assertThat(body.path("totalTransactions").asLong()).isEqualTo(50L);
        assertThat(body.path("totalCreditAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("15000.75"));
    }

    @Test
    @DisplayName("GET /api/support/transactions should return all transactions")
    void getAllTransactions_shouldReturnList() throws Exception {
        List<TransactionResponse> response = List.of(
                TransactionResponse.builder()
                        .transactionId("TXN-100")
                        .senderAccountNumber("100000000001")
                        .receiverAccountNumber("100000000002")
                        .amount(new BigDecimal("200.00"))
                        .paymentStatus("COMPLETED")
                        .build()
        );

        when(supportService.getAllTransactions()).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/support/transactions");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("transactionId").asText()).isEqualTo("TXN-100");
        assertThat(body.get(0).path("paymentStatus").asText()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("GET /api/support/customer/{accountNumber} should return customer transactions")
    void getTransactionsByCustomer_whenAccountExists_shouldReturnList() throws Exception {
        List<TransactionResponse> response = List.of(
                TransactionResponse.builder()
                        .transactionId("TXN-CUST-1")
                        .senderAccountNumber("100000000001")
                        .receiverAccountNumber("100000000002")
                        .amount(new BigDecimal("99.99"))
                        .paymentStatus("COMPLETED")
                        .build()
        );

        when(supportService.getTransactionsByCustomer("100000000001")).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/support/customer/100000000001");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("transactionId").asText()).isEqualTo("TXN-CUST-1");
    }

    @Test
    @DisplayName("GET /api/support/customer/{accountNumber} should return 404 when account is missing")
    void getTransactionsByCustomer_whenAccountMissing_shouldReturnNotFound() throws Exception {
        when(supportService.getTransactionsByCustomer("404"))
                .thenThrow(new AccountNotFoundException("Account not found with number: 404"));

        HttpResponse<String> httpResponse = get("/api/support/customer/404");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Account not found with number: 404");
        assertThat(body.path("path").asText()).isEqualTo("/api/support/customer/404");
    }

    @Test
    @DisplayName("GET /api/support/status/{status} should return transactions by status")
    void getTransactionsByStatus_shouldReturnList() throws Exception {
        List<TransactionResponse> response = List.of(
                TransactionResponse.builder()
                        .transactionId("TXN-ST-1")
                        .senderAccountNumber("100000000003")
                        .receiverAccountNumber("100000000004")
                        .amount(new BigDecimal("555.00"))
                        .paymentStatus("COMPLETED")
                        .build()
        );

        when(supportService.getTransactionsByStatus(PaymentStatus.COMPLETED)).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/support/status/COMPLETED");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("transactionId").asText()).isEqualTo("TXN-ST-1");
        assertThat(body.get(0).path("paymentStatus").asText()).isEqualTo("COMPLETED");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

