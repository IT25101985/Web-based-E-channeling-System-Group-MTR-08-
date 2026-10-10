package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** FR-03: Appointment Booking Engine & Concurrency Lock | TC_BOOK_001-010 | Dhanukshan S. IT25101985 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc_book;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional @TestMethodOrder(MethodOrderer.DisplayName.class)
class TC_BOOK_AppointmentBookingTest {
    @Autowired MockMvc mvc;
    @Autowired BookingService bookingService;
    @Autowired AppointmentService appointmentService;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired UserRepository userRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired HospitalBranchRepository branchRepository;
    @MockBean JavaMailSender mail;

    private Appointment draft() {
        LocalDate date = LocalDate.now().plusDays(21);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) date = date.plusDays(1);
        Appointment a = new Appointment();
        a.setDoctor(doctorRepository.findAll().get(0));
        a.setHospitalBranch(branchRepository.findAll().get(0));
        a.setAppointmentDate(date.atTime(10, 0));
        a.setContactEmail("patient@example.com");
        a.setContactPhone("0771234567");
        return a;
    }
    private Appointment book() {
        return bookingService.book(userRepository.findByUsername("dhanu").orElseThrow(), draft());
    }

    /** TC_BOOK_001 - Select Doctor and Time Slot */
    @Test @DisplayName("TC_BOOK_001 - Select Doctor and Time Slot")
    void TC_BOOK_001_selectDoctorAndTimeSlot() {
        Appointment d = draft();
        List<String> slots = bookingService.availableSlots(
            d.getDoctor().getId(), d.getHospitalBranch().getId(),
            d.getAppointmentDate().toLocalDate(), null);
        assertFalse(slots.isEmpty(), "Available slots must exist for seeded session");
        assertTrue(slots.contains(d.getAppointmentDate().toString()));
    }

    /** TC_BOOK_002 - Slot Unavailable After First Booking (Concurrency Lock) */
    @Test @DisplayName("TC_BOOK_002 - Slot Unavailable After First Booking (Concurrency Lock)")
    void TC_BOOK_002_slotLockedAfterFirstBooking() {
        book();
        assertThrows(IllegalArgumentException.class, () -> {
            User pavi = userRepository.findByUsername("pavi").orElseThrow();
            bookingService.book(pavi, draft());
        });
    }

    /** TC_BOOK_003 - Successful Booking Creates SCHEDULED Appointment */
    @Test @DisplayName("TC_BOOK_003 - Successful Booking Creates SCHEDULED Appointment")
    void TC_BOOK_003_successfulBookingCreatesScheduledAppointment() {
        Appointment saved = book();
        assertNotNull(saved.getId());
        assertEquals("SCHEDULED", saved.getStatus());
    }

    /** TC_BOOK_004 - Browser Booking Form Creates Appointment Row */
    @Test @DisplayName("TC_BOOK_004 - Browser Booking Form Creates Appointment Row")
    void TC_BOOK_004_browserBookingFormCreatesAppointmentRow() throws Exception {
        Appointment d = draft();
        long before = appointmentRepository.count();
        mvc.perform(post("/patient/book-appointment").with(user("dhanu").roles("PATIENT")).with(csrf())
            .param("doctor.id", d.getDoctor().getId().toString())
            .param("hospitalBranch.id", d.getHospitalBranch().getId().toString())
            .param("appointmentDate", d.getAppointmentDate().toString())
            .param("contactEmail", d.getContactEmail())
            .param("contactPhone", d.getContactPhone()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/patient/dashboard?section=history"));
        assertEquals(before + 1, appointmentRepository.count());
    }

    /** TC_BOOK_005 - Booking with Non-Existent Doctor Rejected */
    @Test @DisplayName("TC_BOOK_005 - Booking with Non-Existent Doctor Rejected")
    void TC_BOOK_005_bookingWithNonExistentDoctorRejected() {
        Appointment bad = draft();
        Doctor ghost = new Doctor(); ghost.setId(99999L); bad.setDoctor(ghost);
        assertThrows(Exception.class, () ->
            bookingService.book(userRepository.findByUsername("dhanu").orElseThrow(), bad));
    }

    /** TC_BOOK_006 - Past Date Booking Rejected */
    @Test @DisplayName("TC_BOOK_006 - Past Date Booking Rejected")
    void TC_BOOK_006_pastDateBookingRejected() {
        Appointment past = draft();
        past.setAppointmentDate(LocalDateTime.now().minusDays(1).withSecond(0).withNano(0));
        assertThrows(IllegalArgumentException.class, () ->
            bookingService.book(userRepository.findByUsername("dhanu").orElseThrow(), past));
    }

    /** TC_BOOK_007 - Cancel Appointment Frees Slot */
    @Test @DisplayName("TC_BOOK_007 - Cancel Appointment Frees Slot")
    void TC_BOOK_007_cancelAppointmentFreesSlot() {
        Appointment a = book();
        String slot = a.getAppointmentDate().toString();
        appointmentService.cancel(a.getId());
        assertEquals("CANCELLED", appointmentRepository.findById(a.getId()).orElseThrow().getStatus());
        assertTrue(bookingService.availableSlots(
            a.getDoctor().getId(), a.getHospitalBranch().getId(),
            a.getAppointmentDate().toLocalDate(), null).contains(slot));
    }

    /** TC_BOOK_008 - Reschedule Appointment Updates Date */
    @Test @DisplayName("TC_BOOK_008 - Reschedule Appointment Updates Date")
    void TC_BOOK_008_rescheduleAppointmentUpdatesDate() {
        Appointment a = book();
        LocalDateTime newDate = a.getAppointmentDate().plusHours(1);
        Appointment reschedule = draft();
        reschedule.setId(a.getId());
        reschedule.setAppointmentDate(newDate);
        bookingService.book(a.getPatient(), reschedule);
        assertEquals(newDate, appointmentRepository.findById(a.getId()).orElseThrow().getAppointmentDate());
    }

    /** TC_BOOK_009 - Invalid Contact Email Rejected During Booking */
    @Test @DisplayName("TC_BOOK_009 - Invalid Contact Email Rejected During Booking")
    void TC_BOOK_009_invalidContactEmailRejected() {
        Appointment bad = draft(); bad.setContactEmail("not-an-email");
        assertThrows(IllegalArgumentException.class, () ->
            bookingService.book(userRepository.findByUsername("dhanu").orElseThrow(), bad));
    }

    /** TC_BOOK_010 - Unauthenticated Booking Endpoint Blocked */
    @Test @DisplayName("TC_BOOK_010 - Unauthenticated Booking Endpoint Blocked")
    void TC_BOOK_010_unauthenticatedBookingEndpointBlocked() throws Exception {
        Appointment d = draft();
        mvc.perform(post("/patient/book-appointment").with(csrf())
            .param("doctor.id", d.getDoctor().getId().toString())
            .param("hospitalBranch.id", d.getHospitalBranch().getId().toString())
            .param("appointmentDate", d.getAppointmentDate().toString())
            .param("contactEmail", d.getContactEmail())
            .param("contactPhone", d.getContactPhone()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
}
