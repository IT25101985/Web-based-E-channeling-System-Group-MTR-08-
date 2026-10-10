package com.echanneling.service;

/**
 * Strategy Pattern Implementation - Payment Interface
 * Different payment strategies (Card, Cash) implement this interface.
 * This allows runtime selection of payment method without changing client code.
 */
public interface Payment {
    // Strategy method - each concrete strategy implements this differently
    String execute(Double amount);

    // Legacy method kept for backward compatibility
    default void processPayment(Double amount) {
        execute(amount);
    }
}
