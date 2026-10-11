package com.echanneling.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
public final class Money {
    private Money() {}
    public static BigDecimal amount(Number value) {
        if (value == null) return null;
        BigDecimal result = new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
        if (result.abs().compareTo(new BigDecimal("99999999999999999.99")) > 0)
            throw new IllegalArgumentException("Amount is too large.");
        return result;
    }
}
