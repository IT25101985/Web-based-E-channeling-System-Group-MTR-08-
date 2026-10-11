package com.echanneling.config;
@org.springframework.web.bind.annotation.ControllerAdvice
public class PaymentViewConfig {
    @org.springframework.beans.factory.annotation.Value("${app.payments.demo-card-enabled:false}") private boolean demoCards;
    @org.springframework.web.bind.annotation.ModelAttribute("demoCardEnabled")
    public boolean demoCardEnabled() { return demoCards; }
}
