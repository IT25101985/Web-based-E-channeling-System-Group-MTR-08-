package com.echanneling.patterns.observer;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Request-local subject: subscriptions cannot leak between patients or requests. */
public final class AppointmentEventSubject implements AppointmentSubject {
    private final Set<AppointmentObserver> observers = new LinkedHashSet<>();
    public void addObserver(AppointmentObserver observer) { observers.add(Objects.requireNonNull(observer)); }
    public void removeObserver(AppointmentObserver observer) { observers.remove(observer); }
    public void notifyObservers(AppointmentEvent event) {
        for (AppointmentObserver observer : new java.util.ArrayList<>(observers)) observer.update(event);
    }
}
