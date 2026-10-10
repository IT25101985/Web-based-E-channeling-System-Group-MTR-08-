package com.echanneling.service;

import org.springframework.stereotype.Component;

/**
 * Strategy Pattern - Concrete Strategy: Cash Payment
 */
@Component
public class CashPayment implements Payment {
    @Override
    public String execute(Double amount) {
        System.out.println("Processing CASH payment of Rs. " + amount);
        return "Cash payment of Rs. " + amount + " processed successfully.";
    }
}
