package com.echanneling.patterns.decorator;

import com.echanneling.entity.User;
import java.util.Objects;

/** Adds recipient personalization to any message component. */
public final class RecipientMessageDecorator extends MessageDecorator {
    private final User recipient;
    public RecipientMessageDecorator(NotificationMessage wrapped, User recipient) {
        super(wrapped); this.recipient = recipient;
    }
    public String render() {
        return wrapped.render().replace("{name}", Objects.toString(recipient.getFullName(), recipient.getUsername()));
    }
}
