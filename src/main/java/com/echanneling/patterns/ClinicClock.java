package com.echanneling.patterns;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Singleton: one immutable clinic clock, independent of the server's timezone. */
public final class ClinicClock {
    private static final ClinicClock INSTANCE = new ClinicClock();
    private final ZoneId zone = ZoneId.of("Asia/Colombo");
    private ClinicClock() {}
    public static ClinicClock getInstance() { return INSTANCE; }
    public LocalDateTime now() { return LocalDateTime.now(zone); }
    public LocalDate today() { return LocalDate.now(zone); }
    public ZoneId zone() { return zone; }
}
