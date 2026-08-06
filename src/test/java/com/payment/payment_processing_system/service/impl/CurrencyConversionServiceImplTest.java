package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.enums.CurrencyType;
import com.payment.payment_processing_system.exception.UnsupportedExchangeRateException;
import com.payment.payment_processing_system.service.CurrencyConversionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyConversionServiceImplTest {

    private final CurrencyConversionService currencyConversionService = new CurrencyConversionServiceImpl();

    @Test
    @DisplayName("getExchangeRate should return 1 for same currency")
    void getExchangeRate_whenSameCurrency_shouldReturnOne() {
        assertEquals(BigDecimal.ONE, currencyConversionService.getExchangeRate(CurrencyType.USD, CurrencyType.USD));
        assertEquals(BigDecimal.ONE, currencyConversionService.getExchangeRate(CurrencyType.INR, CurrencyType.INR));
    }

    @Test
    @DisplayName("getExchangeRate should return configured direct rate")
    void getExchangeRate_whenDirectRateExists_shouldReturnDirectRate() {
        assertEquals(new BigDecimal("87"), currencyConversionService.getExchangeRate(CurrencyType.USD, CurrencyType.INR));
        assertEquals(new BigDecimal("1.17"), currencyConversionService.getExchangeRate(CurrencyType.EUR, CurrencyType.USD));
    }

    @Test
    @DisplayName("getExchangeRate should generate reverse rate when only reverse mapping exists")
    void getExchangeRate_whenReverseRateExists_shouldGenerateReverseRate() {
        assertEquals(new BigDecimal("0.0086956522"), currencyConversionService.getExchangeRate(CurrencyType.INR, CurrencyType.GBP));
        assertEquals(new BigDecimal("0.8547008547"), currencyConversionService.getExchangeRate(CurrencyType.USD, CurrencyType.EUR));
    }

    @Test
    @DisplayName("convertAmount should convert using exchange rate with BigDecimal precision")
    void convertAmount_shouldConvertUsingExchangeRate() {
        BigDecimal converted = currencyConversionService.convertAmount(
                new BigDecimal("100.00"), CurrencyType.USD, CurrencyType.INR);

        assertEquals(new BigDecimal("8700.0000"), converted);
    }

    @Test
    @DisplayName("convertAmount should preserve amount for same currency")
    void convertAmount_whenSameCurrency_shouldPreserveAmount() {
        BigDecimal converted = currencyConversionService.convertAmount(
                new BigDecimal("99.99"), CurrencyType.EUR, CurrencyType.EUR);

        assertEquals(new BigDecimal("99.9900"), converted);
    }

    @Test
    @DisplayName("calculateTransferCharge should return zero for domestic transfer")
    void calculateTransferCharge_whenSameCurrency_shouldReturnZero() {
        BigDecimal charge = currencyConversionService.calculateTransferCharge(
                new BigDecimal("500.00"), CurrencyType.INR, CurrencyType.INR);

        assertEquals(new BigDecimal("0.0000"), charge);
    }

    @Test
    @DisplayName("calculateTransferCharge should return two percent for international transfer")
    void calculateTransferCharge_whenDifferentCurrency_shouldReturnTwoPercent() {
        BigDecimal charge = currencyConversionService.calculateTransferCharge(
                new BigDecimal("500.00"), CurrencyType.USD, CurrencyType.INR);

        assertEquals(new BigDecimal("10.0000"), charge);
    }

    @Test
    @DisplayName("getExchangeRate should throw when currency pair is unsupported")
    void getExchangeRate_whenPairUnsupported_shouldThrowException() {
        UnsupportedExchangeRateException exception = assertThrows(
                UnsupportedExchangeRateException.class,
                () -> currencyConversionService.getExchangeRate(CurrencyType.EUR, CurrencyType.GBP)
        );

        assertEquals("Unsupported exchange rate for currency pair: EUR -> GBP", exception.getMessage());
    }
}

