package com.echanneling.patterns.decorator;

public record TemplateMessage(String text) implements NotificationMessage {
    public String render() { return text; }
}
