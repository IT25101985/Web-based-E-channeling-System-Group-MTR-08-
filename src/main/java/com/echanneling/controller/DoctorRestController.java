package com.echanneling.controller;
import com.echanneling.service.BookingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
@RestController
public class DoctorRestController {
    private final BookingService bookings;
    public DoctorRestController(BookingService bookings) { this.bookings = bookings; }
    @GetMapping("/api/doctors/{id}/slots")
    public List<String> slots(@PathVariable Long id, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required=false) Long excludeAppointmentId) {
        return bookings.availableSlots(id, date, excludeAppointmentId);
    }
}
