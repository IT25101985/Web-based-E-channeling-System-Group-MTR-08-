package com.echanneling.controller;
import com.echanneling.service.BookingService;
import com.echanneling.repository.AvailabilitySlotRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.*;
@RestController
public class DoctorRestController {
    private final BookingService bookings;
    private final AvailabilitySlotRepository sessions;
    public DoctorRestController(BookingService bookings, AvailabilitySlotRepository sessions) { this.bookings=bookings; this.sessions=sessions; }
    @GetMapping("/api/doctors/{id}/slots")
    public List<String> slots(@PathVariable Long id,@RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required=false) Long excludeAppointmentId) {
        return bookings.availableSlots(id,branchId,date,excludeAppointmentId);
    }
    public record DoctorOption(Long id,String name,String specialization,Double fee) {}
    @GetMapping("/api/branches/{id}/doctors")
    public List<DoctorOption> doctors(@PathVariable Long id) {
        Map<Long,DoctorOption> result=new TreeMap<>();
        sessions.findByHospitalBranchIdAndIsActiveTrueAndSlotDateGreaterThanEqual(id,LocalDate.now()).stream()
            .filter(s->s.getSlotDate().atTime(s.getEndTime()).isAfter(java.time.LocalDateTime.now()))
            .forEach(s->{var d=s.getDoctor();result.put(d.getId(),new DoctorOption(d.getId(),d.getName(),d.getSpecialization(),d.getConsultationFee()));});
        return new ArrayList<>(result.values());
    }
}
