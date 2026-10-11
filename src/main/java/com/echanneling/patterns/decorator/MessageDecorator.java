package com.echanneling.patterns.decorator;

import java.util.Objects;

public abstract class MessageDecorator implements NotificationMessage {
    protected final NotificationMessage wrapped;
    protected MessageDecorator(NotificationMessage wrapped) { this.wrapped = Objects.requireNonNull(wrapped); }
}
