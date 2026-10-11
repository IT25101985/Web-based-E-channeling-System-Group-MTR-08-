package com.echanneling.patterns.observer;

public interface AppointmentSubject {
    void addObserver(AppointmentObserver observer);
    void removeObserver(AppointmentObserver observer);
    void notifyObservers(AppointmentEvent event);
}
