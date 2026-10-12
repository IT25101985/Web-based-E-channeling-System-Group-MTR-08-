package com.echanneling.service;

import org.springframework.stereotype.Component;

/**
 * Strategy Pattern - Concrete Strategy: Card Payment
 */
@Component
public class CardPayment implements Payment {
    @Override
    public String execute(java.math.BigDecimal amount) {
        System.out.println("Processing CARD payment of Rs. " + amount);
        return "Card payment of Rs. " + amount + " processed successfully.";
    }
}
