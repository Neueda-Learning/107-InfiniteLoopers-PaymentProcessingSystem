package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.enums.CurrencyType;
import com.payment.payment_processing_system.exception.UnsupportedExchangeRateException;
import com.payment.payment_processing_system.service.CurrencyConversionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Static exchange-rate based implementation of {@link CurrencyConversionService}.
 */
@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private static final int AMOUNT_SCALE = 4;
    private static final int CHARGE_SCALE = 4;
    private static final BigDecimal DOMESTIC_RATE = BigDecimal.ONE;
    private static final BigDecimal DOMESTIC_CHARGE_RATE = BigDecimal.ZERO;
    private static final BigDecimal INTERNATIONAL_CHARGE_RATE = new BigDecimal("0.02");

    private final Map<CurrencyType, Map<CurrencyType, BigDecimal>> exchangeRates;

    public CurrencyConversionServiceImpl() {
        this.exchangeRates = buildExchangeRates();
    }

    @Override
    public BigDecimal getExchangeRate(CurrencyType from, CurrencyType to) {
        validateCurrency(from, "from");
        validateCurrency(to, "to");

        if (from == to) {
            return DOMESTIC_RATE;
        }

        BigDecimal directRate = lookupRate(from, to);
        if (directRate != null) {
            return directRate;
        }

        BigDecimal reverseRate = lookupRate(to, from);
        if (reverseRate != null) {
            return BigDecimal.ONE.divide(reverseRate, 10, RoundingMode.HALF_UP);
        }

        throw new UnsupportedExchangeRateException(
                "Unsupported exchange rate for currency pair: " + from + " -> " + to);
    }

    @Override
    public BigDecimal convertAmount(BigDecimal amount, CurrencyType from, CurrencyType to) {
        validateAmount(amount);
        BigDecimal exchangeRate = getExchangeRate(from, to);
        return amount.multiply(exchangeRate).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTransferCharge(BigDecimal amount, CurrencyType from, CurrencyType to) {
        validateAmount(amount);
        validateCurrency(from, "from");
        validateCurrency(to, "to");

        if (from == to) {
            return DOMESTIC_CHARGE_RATE.setScale(CHARGE_SCALE, RoundingMode.HALF_UP);
        }

        return amount.multiply(INTERNATIONAL_CHARGE_RATE).setScale(CHARGE_SCALE, RoundingMode.HALF_UP);
    }

    private Map<CurrencyType, Map<CurrencyType, BigDecimal>> buildExchangeRates() {
        Map<CurrencyType, Map<CurrencyType, BigDecimal>> rates = new EnumMap<>(CurrencyType.class);

        addRate(rates, CurrencyType.USD, CurrencyType.INR, new BigDecimal("87"));
        addRate(rates, CurrencyType.INR, CurrencyType.USD, new BigDecimal("0.0115"));
        addRate(rates, CurrencyType.EUR, CurrencyType.INR, new BigDecimal("100"));
        addRate(rates, CurrencyType.GBP, CurrencyType.INR, new BigDecimal("115"));
        addRate(rates, CurrencyType.EUR, CurrencyType.USD, new BigDecimal("1.17"));
        addRate(rates, CurrencyType.GBP, CurrencyType.USD, new BigDecimal("1.35"));

        return rates;
    }

    private void addRate(Map<CurrencyType, Map<CurrencyType, BigDecimal>> rates,
                         CurrencyType from,
                         CurrencyType to,
                         BigDecimal rate) {
        rates.computeIfAbsent(from, key -> new EnumMap<>(CurrencyType.class)).put(to, rate);
    }

    private BigDecimal lookupRate(CurrencyType from, CurrencyType to) {
        Map<CurrencyType, BigDecimal> fromRates = exchangeRates.get(from);
        return fromRates != null ? fromRates.get(to) : null;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
    }

    private void validateCurrency(CurrencyType currency, String parameterName) {
        if (currency == null) {
            throw new IllegalArgumentException("Currency parameter '" + parameterName + "' must not be null");
        }
    }
}

