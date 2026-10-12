package com.echanneling.service;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** One booking policy shared by the availability API and appointment writes. */
@Service
public class BookingService {
    @org.springframework.beans.factory.annotation.Value("${app.payments.demo-card-enabled:false}") private boolean demoCards;
    @org.springframework.beans.factory.annotation.Autowired private AttendeeLockService attendeeLocks;
    @org.springframework.beans.factory.annotation.Autowired private UserRepository users;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final NotificationService notifications;
    private final AvailabilitySlotRepository sessions;
    private final HospitalBranchRepository branches;
    public BookingService(DoctorRepository doctors, AppointmentRepository appointments, NotificationService notifications, AvailabilitySlotRepository sessions, HospitalBranchRepository branches) {
        this.doctors = doctors;
        this.appointments = appointments;
        this.notifications = notifications;
        this.sessions = sessions; this.branches = branches;
    }

    public List<String> availableSlots(Long doctorId, Long branchId, LocalDate date, Long excludeId) {
        Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isBookable(doctor) || branchId == null || date == null || date.isBefore(com.echanneling.patterns.ClinicClock.getInstance().today())) return List.of();
        List<Appointment> booked = appointments.findByDoctorAndAppointmentDateBetween(doctor, date.atStartOfDay(), date.atTime(LocalTime.MAX));
        return slotsFrom(sessions.findByDoctorAndSlotDateAndIsActive(doctor, date, true), booked, branchId, date, excludeId);
    }
    private List<String> slotsFrom(List<AvailabilitySlot> published, List<Appointment> booked, Long branchId, LocalDate date, Long excludeId) {
        Set<String> result = new TreeSet<>();
        for (AvailabilitySlot session : published) {
            if (!session.getHospitalBranch().getId().equals(branchId)) continue;
            for (LocalDateTime time=date.atTime(session.getStartTime()); !time.plusHours(1).isAfter(date.atTime(session.getEndTime())); time=time.plusHours(1)) {
                LocalDateTime slot=time;
                if (slot.isAfter(com.echanneling.patterns.ClinicClock.getInstance().now()) && booked.stream().noneMatch(a ->
                        !Objects.equals(a.getId(), excludeId) && !"CANCELLED".equals(a.getStatus())
                        && a.getAppointmentDate().isBefore(slot.plusHours(1)) && a.getAppointmentDate().plusHours(1).isAfter(slot)))
                    result.add(slot.toString());
            }
        }
        return new ArrayList<>(result);
    }

    @Transactional
    public Appointment book(User patient, Appointment submitted) {
        if (patient == null || !patient.isActive() || !"ROLE_PATIENT".equals(patient.getRole()))
            throw new IllegalArgumentException("Choose an active patient account.");
        if (submitted.getDoctor() == null || submitted.getDoctor().getId() == null || submitted.getAppointmentDate() == null || submitted.getHospitalBranch() == null || submitted.getHospitalBranch().getId() == null)
            throw new IllegalArgumentException("Choose a hospital, doctor, date and available time.");
        patient = users.findLockedById(patient.getId()).orElseThrow();
        if (!patient.isActive()) throw new IllegalArgumentException("This patient account is inactive.");
        Appointment original = submitted.getId() == null ? null : appointments.findById(submitted.getId()).orElseThrow();
        if (original != null && (original.getPatient() == null || !original.getPatient().getId().equals(patient.getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String identityNic = original != null ? original.getRecipientNic() : submitted.isForSomeoneElse() ? submitted.getRecipientNic() : patient instanceof Patient p ? p.getNic() : null;
        String identity = identityNic == null || identityNic.isBlank() ? "account:" + patient.getId() : "nic:" + identityNic.trim().toUpperCase(Locale.ROOT);
        attendeeLocks.acquire(identity);
        // Lock the doctor's database row until commit, including concurrent requests on other app instances.
        Doctor doctor = doctors.findForBooking(submitted.getDoctor().getId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor is no longer available."));
        if (!isBookable(doctor)) throw new IllegalArgumentException("This doctor is not currently accepting appointments.");
        Appointment booking = new Appointment();
        if (submitted.getId() != null) {
            booking = appointments.findLockedById(submitted.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (booking.getPatient() == null || !Objects.equals(booking.getPatient().getId(), patient.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (!"SCHEDULED".equals(booking.getStatus())) throw new IllegalArgumentException("Only scheduled appointments can be rescheduled.");
            if (booking.getDoctor() != null && !Objects.equals(booking.getDoctor().getId(), doctor.getId()))
                throw new IllegalArgumentException("Keep the same doctor when rescheduling. Cancel and make a new booking to change doctors.");
        }
        HospitalBranch branch=branches.findById(submitted.getHospitalBranch().getId()).orElseThrow(()->new IllegalArgumentException("Hospital is no longer available."));
        if (submitted.getAppointmentDate().getSecond()!=0 || submitted.getAppointmentDate().getNano()!=0)
            throw new IllegalArgumentException("Choose an exact appointment time.");
        String time = submitted.getAppointmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        if (!availableSlots(doctor.getId(), branch.getId(), submitted.getAppointmentDate().toLocalDate(), booking.getId()).contains(time))
            throw new IllegalArgumentException("That time is unavailable. Please choose another slot.");
        if (submitted.getContactEmail() == null || !submitted.getContactEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid contact email.");
        if (submitted.getContactPhone() == null || !submitted.getContactPhone().matches("^\\+?[0-9\\s\\-()]{9,20}$"))
            throw new IllegalArgumentException("Enter a valid contact phone number.");
        if (!Set.of("self", "other").contains(submitted.getBookingFor()))
            throw new IllegalArgumentException("Choose who this booking is for.");
        if ("card".equals(submitted.getSettlementType()) && !demoCards) throw new IllegalArgumentException("Choose counter cash; card payments are unavailable.");
        if (submitted.getSettlementType() != null && !Set.of("cash", "card").contains(submitted.getSettlementType()))
            throw new IllegalArgumentException("Choose card demonstration or counter cash.");
        if (submitted.getId() == null) {
            booking.setBookingFor(submitted.getBookingFor());
            if (submitted.isForSomeoneElse()) {
                String name = submitted.getPatientName();
                String nic = submitted.getRecipientNic();
                if (name == null || name.isBlank() || name.length() > 150)
                    throw new IllegalArgumentException("Enter the attending patient's full name (150 characters maximum).");
                if (nic == null || !nic.trim().matches("(?:[0-9]{9}[xXvV]|[0-9]{12})"))
                    throw new IllegalArgumentException("Enter the attending patient's valid NIC.");
                booking.setPatientName(name.trim());
                booking.setRecipientNic(nic.trim());
            } else {
                booking.setPatientName(patient.getFullName());
                if (patient instanceof Patient p) booking.setRecipientNic(p.getNic());
            }
            java.math.BigDecimal fee = doctor.getConsultationFee();
            if (fee == null || fee.signum() <= 0)
                throw new IllegalArgumentException("The doctor must publish a valid consultation fee before booking.");
            booking.setBookedConsultationFee(fee);
            booking.setSettlementType(submitted.getSettlementType() == null ? "cash" : submitted.getSettlementType());
        }
        String attendingNic = booking.getRecipientNic() == null ? null : booking.getRecipientNic().trim().toUpperCase(Locale.ROOT);
        if (attendingNic != null) booking.setRecipientNic(attendingNic);
        if (appointments.attendeeConflicts(attendingNic, patient.getId(), !booking.isForSomeoneElse(),
                submitted.getAppointmentDate().minusHours(1), submitted.getAppointmentDate().plusHours(1))
                .stream().anyMatch(a -> !Objects.equals(a.getId(), submitted.getId())))
            throw new IllegalArgumentException("The attending patient already has an appointment at this time.");
        booking.setDoctor(doctor);
        booking.setHospitalBranch(branch);
        booking.setPatient(patient);
        booking.setAppointmentDate(submitted.getAppointmentDate());
        booking.setContactEmail(submitted.getContactEmail().trim());
        booking.setContactPhone(submitted.getContactPhone().trim());
        booking.setStatus("SCHEDULED");
        Appointment saved = appointments.save(booking);
        notifications.appointmentEvent(saved, submitted.getId()==null ? Notification.NotificationType.CONFIRMATION : Notification.NotificationType.RESCHEDULE);
        return saved;
    }

    public static boolean isBookable(Doctor doctor) {
        return doctor.getUser() != null && doctor.getUser().isActive()
                && "ROLE_DOCTOR".equals(doctor.getUser().getRole());
    }

    public record TodayAvailability(LocalDateTime time, String hospital, String area) {}

    public Map<Long, TodayAvailability> todayAvailability() {
        Map<Long, TodayAvailability> result = new HashMap<>();
        LocalDate today = com.echanneling.patterns.ClinicClock.getInstance().today();
        List<AvailabilitySlot> todaySessions=sessions.findBySlotDateAndIsActive(today,true);
        Map<Long,List<Appointment>> byDoctor=appointments.findByAppointmentDateBetween(today.atStartOfDay(),today.atTime(LocalTime.MAX))
            .stream().collect(java.util.stream.Collectors.groupingBy(a -> a.getDoctor().getId()));
        for (AvailabilitySlot session : todaySessions) {
            if (!isBookable(session.getDoctor())) continue;
            List<String> free = slotsFrom(List.of(session), byDoctor.getOrDefault(session.getDoctor().getId(),List.of()), session.getHospitalBranch().getId(), today, null);
            if (free.isEmpty()) continue;
            LocalDateTime time = LocalDateTime.parse(free.get(0));
            Long id = session.getDoctor().getId();
            if (!result.containsKey(id) || time.isBefore(result.get(id).time()))
                result.put(id, new TodayAvailability(time, session.getHospitalBranch().getBranchName(), session.getHospitalBranch().getArea()));
        }
        return result;
    }
}
