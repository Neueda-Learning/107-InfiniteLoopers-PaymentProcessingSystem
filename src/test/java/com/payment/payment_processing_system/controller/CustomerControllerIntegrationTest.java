package com.payment.payment_processing_system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_processing_system.dto.CustomerAccountResponse;
import com.payment.payment_processing_system.dto.CustomerListItemResponse;
import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.CurrencyType;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.exception.CustomerNotFoundException;
import com.payment.payment_processing_system.exception.TransactionNotFoundException;
import com.payment.payment_processing_system.service.CustomerService;
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
class CustomerControllerIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("GET /api/customers should return all customers")
    void getAllCustomers_shouldReturnList() throws Exception {
        List<CustomerListItemResponse> response = List.of(
                new CustomerListItemResponse(1L, "Alice")
        );

        when(customerService.getAllCustomers()).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("id").asLong()).isEqualTo(1L);
        assertThat(body.get(0).path("customerName").asText()).isEqualTo("Alice");
        assertThat(body.get(0).has("email")).isFalse();
        assertThat(body.get(0).has("phoneNumber")).isFalse();
    }

    @Test
    @DisplayName("GET /api/customers/{customerId}/accounts should return active accounts")
    void getCustomerAccounts_shouldReturnActiveAccounts() throws Exception {
        List<CustomerAccountResponse> response = List.of(
                new CustomerAccountResponse(
                        10L,
                        "100000000001",
                        "HDFC",
                        "HDFC0005678",
                        new BigDecimal("50000.00"),
                        CurrencyType.INR,
                        true
                )
        );

        when(customerService.getActiveAccountsByCustomerId(1L)).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/1/accounts");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("accountId").asLong()).isEqualTo(10L);
        assertThat(body.get(0).path("accountNumber").asText()).isEqualTo("100000000001");
        assertThat(body.get(0).path("currency").asText()).isEqualTo("INR");
        assertThat(body.get(0).path("isActive").asBoolean()).isTrue();
        assertThat(body.get(0).has("upiPin")).isFalse();
    }

    @Test
    @DisplayName("GET /api/customers/{customerId}/accounts should return 404 when customer is missing")
    void getCustomerAccounts_whenCustomerMissing_shouldReturnNotFound() throws Exception {
        when(customerService.getActiveAccountsByCustomerId(404L))
                .thenThrow(new CustomerNotFoundException("Customer not found with ID: 404"));

        HttpResponse<String> httpResponse = get("/api/customers/404/accounts");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("CUSTOMER_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Customer not found with ID: 404");
    }

    @Test
    @DisplayName("GET /api/customers/{customerId}/accounts should return 400 for invalid customer ID")
    void getCustomerAccounts_whenInvalidCustomerId_shouldReturnBadRequest() throws Exception {
        HttpResponse<String> httpResponse = get("/api/customers/abc/accounts");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("GET /api/customers/accounts should return active accounts by email")
    void getCustomerAccountsByIdentifier_shouldReturnActiveAccounts() throws Exception {
        List<CustomerAccountResponse> response = List.of(
                new CustomerAccountResponse(
                        10L,
                        "100000000001",
                        "HDFC",
                        "HDFC0005678",
                        new BigDecimal("50000.00"),
                        CurrencyType.USD,
                        true
                )
        );

        when(customerService.getActiveAccountsByCustomerIdentifier(null, "alice@example.com", null)).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/accounts?email=alice@example.com");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("accountId").asLong()).isEqualTo(10L);
        assertThat(body.get(0).path("currency").asText()).isEqualTo("USD");
        assertThat(body.get(0).path("upiPin").isMissingNode()).isTrue();
    }

    @Test
    @DisplayName("GET /api/customers/accounts should return 400 when identifier is missing")
    void getCustomerAccountsByIdentifier_whenIdentifierMissing_shouldReturnBadRequest() throws Exception {
        when(customerService.getActiveAccountsByCustomerIdentifier(null, null, null))
                .thenThrow(new IllegalArgumentException("Provide at least one identifier: customerName, email, or phoneNumber"));

        HttpResponse<String> httpResponse = get("/api/customers/accounts");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(400);
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("GET /api/customers/accounts should use priority (email > phone > name) when multiple identifiers provided")
    void getCustomerAccountsByIdentifier_withMultipleIdentifiers_shouldUsePriority() throws Exception {
        List<CustomerAccountResponse> response = List.of(
                new CustomerAccountResponse(
                        10L,
                        "100000000001",
                        "HDFC",
                        "HDFC0005678",
                        new BigDecimal("50000.00"),
                        CurrencyType.GBP,
                        true
                )
        );

        when(customerService.getActiveAccountsByCustomerIdentifier("David Miller", "david@example.com", "9876506234"))
                .thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/accounts?customerName=David%20Miller&email=david@example.com&phoneNumber=9876506234");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("currency").asText()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("GET /api/customers/{customerId} should return customer details")
    void getCustomerById_shouldReturnCustomer() throws Exception {
        CustomerResponse response = CustomerResponse.builder()
                .customerId(1L)
                .customerName("Alice")
                .email("alice@example.com")
                .phoneNumber("9999999999")
                .accountNumber("100000000001")
                .ifscCode("HDFC0005678")
                .bankName("HDFC")
                .balance(new BigDecimal("50000.00"))
                .currency(CurrencyType.INR)
                .build();

        when(customerService.getCustomerById(1L)).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/1");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.path("customerId").asLong()).isEqualTo(1L);
        assertThat(body.path("customerName").asText()).isEqualTo("Alice");
        assertThat(body.path("currency").asText()).isEqualTo("INR");
    }

    @Test
    @DisplayName("GET /api/customers/{customerId} should return 404 when customer is missing")
    void getCustomerById_whenMissing_shouldReturnNotFound() throws Exception {
        when(customerService.getCustomerById(404L))
                .thenThrow(new CustomerNotFoundException("Customer not found with ID: 404"));

        HttpResponse<String> httpResponse = get("/api/customers/404");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("CUSTOMER_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Customer not found with ID: 404");
        assertThat(body.path("path").asText()).isEqualTo("/api/customers/404");
    }

    @Test
    @DisplayName("GET /api/customers/account/{accountNumber} should return customer by account")
    void getCustomerByAccountNumber_shouldReturnCustomer() throws Exception {
        CustomerResponse response = CustomerResponse.builder()
                .customerId(2L)
                .customerName("Bob")
                .email("bob@example.com")
                .phoneNumber("8888888888")
                .accountNumber("100000000002")
                .ifscCode("HDFC0005678")
                .bankName("HDFC")
                .balance(new BigDecimal("30000.00"))
                .currency(CurrencyType.EUR)
                .build();

        when(customerService.getCustomerByAccountNumber("100000000002")).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/account/100000000002");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.path("customerId").asLong()).isEqualTo(2L);
        assertThat(body.path("accountNumber").asText()).isEqualTo("100000000002");
        assertThat(body.path("currency").asText()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("GET /api/customers/{accountNumber}/transactions should return transaction history")
    void getTransactionHistory_shouldReturnTransactions() throws Exception {
        List<TransactionResponse> response = List.of(
                TransactionResponse.builder()
                        .transactionId("TXN-HIST-1")
                        .senderAccountNumber("100000000001")
                        .receiverAccountNumber("100000000002")
                        .amount(new BigDecimal("125.00"))
                        .paymentStatus("COMPLETED")
                        .build()
        );

        when(customerService.getTransactionHistory("100000000001")).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/100000000001/transactions");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).path("transactionId").asText()).isEqualTo("TXN-HIST-1");
    }

    @Test
    @DisplayName("GET /api/customers/{accountNumber}/transactions should return 404 when account is missing")
    void getTransactionHistory_whenAccountMissing_shouldReturnNotFound() throws Exception {
        when(customerService.getTransactionHistory("404"))
                .thenThrow(new AccountNotFoundException("Account not found with number: 404"));

        HttpResponse<String> httpResponse = get("/api/customers/404/transactions");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Account not found with number: 404");
        assertThat(body.path("path").asText()).isEqualTo("/api/customers/404/transactions");
    }

    @Test
    @DisplayName("GET /api/customers/transaction/{transactionId} should return transaction details")
    void getTransactionDetails_shouldReturnTransaction() throws Exception {
        TransactionResponse response = TransactionResponse.builder()
                .transactionId("TXN-DET-1")
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .amount(new BigDecimal("350.00"))
                .paymentStatus("COMPLETED")
                .build();

        when(customerService.getTransactionDetails("TXN-DET-1")).thenReturn(response);

        HttpResponse<String> httpResponse = get("/api/customers/transaction/TXN-DET-1");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(body.path("transactionId").asText()).isEqualTo("TXN-DET-1");
        assertThat(body.path("paymentStatus").asText()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("GET /api/customers/transaction/{transactionId} should return 404 when transaction is missing")
    void getTransactionDetails_whenMissing_shouldReturnNotFound() throws Exception {
        when(customerService.getTransactionDetails("TXN-404"))
                .thenThrow(new TransactionNotFoundException("Transaction not found with id: TXN-404"));

        HttpResponse<String> httpResponse = get("/api/customers/transaction/TXN-404");
        JsonNode body = objectMapper.readTree(httpResponse.body());

        assertThat(httpResponse.statusCode()).isEqualTo(404);
        assertThat(body.path("errorCode").asText()).isEqualTo("PAYMENT_NOT_FOUND");
        assertThat(body.path("message").asText()).isEqualTo("Transaction not found with id: TXN-404");
        assertThat(body.path("path").asText()).isEqualTo("/api/customers/transaction/TXN-404");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

