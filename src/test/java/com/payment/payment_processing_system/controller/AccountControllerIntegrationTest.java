package com.payment.payment_processing_system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_processing_system.dto.AccountResponse;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.service.AccountService;
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
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("GET /api/accounts/{accountId} should return safe account details")
    void getAccountById_shouldReturnAccountDetails() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .accountNumber("100000000001")
                .bankName("HDFC Bank")
                .ifscCode("HDFC0005678")
                .balance(new BigDecimal("50000.00"))
                .customerName("Alice")
                .email("alice@example.com")
                .currency(null)
                .build();

        when(accountService.getAccountById(1L)).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/accounts/1");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.path("accountNumber").asText()).isEqualTo("100000000001");
        assertThat(body.path("customerName").asText()).isEqualTo("Alice");
        assertThat(body.path("email").asText()).isEqualTo("alice@example.com");
        assertThat(body.path("upiPin").isMissingNode() || body.path("upiPin").isNull()).isTrue();
        assertThat(body.path("currency").isNull()).isTrue();
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId} should return 404 when account is missing")
    void getAccountById_whenMissing_shouldReturnNotFound() throws Exception {
        when(accountService.getAccountById(404L))
                .thenThrow(new AccountNotFoundException("Account not found with ID: 404"));

        HttpResponse<String> httpResponse = get("/api/accounts/404");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Account not found with ID: 404");
        assertThat(body.path("path").asText()).isEqualTo("/api/accounts/404");
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId} should return 400 for invalid account ID")
    void getAccountById_whenInvalidAccountId_shouldReturnBadRequest() throws Exception {
        HttpResponse<String> httpResponse = get("/api/accounts/abc");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_FAILED");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

