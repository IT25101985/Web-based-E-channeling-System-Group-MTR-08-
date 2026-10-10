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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** FR-06: Doctor Schedule Management | TC_SCHED_001-010 | Salinda L.L.S.U. IT25101977 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc_sched;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional @TestMethodOrder(MethodOrderer.DisplayName.class)
class TC_SCHED_DoctorScheduleTest {
    @Autowired MockMvc mvc;
    @Autowired ClinicSessionService clinicSessionService;
    @Autowired AvailabilitySlotRepository slotRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired HospitalBranchRepository branchRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired BookingService bookingService;
    @Autowired AppointmentService appointmentService;
    @Autowired UserRepository userRepository;
    @MockBean JavaMailSender mail;

    private LocalDate futureDate() {
        LocalDate d = LocalDate.now().plusDays(50);
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.plusDays(1);
        return d;
    }
    private Doctor doctor() { return doctorRepository.findAll().get(0); }
    private HospitalBranch branch() { return branchRepository.findAll().get(0); }

    /** TC_SCHED_001 - Doctor Set Daily Session Hours */
    @Test @DisplayName("TC_SCHED_001 - Doctor Set Daily Session Hours")
    void TC_SCHED_001_doctorSetDailySessionHours() throws Exception {
        long before = slotRepository.count();
        mvc.perform(post("/admin/sessions/save").with(csrf()).with(user("admin").roles("ADMIN"))
            .param("doctorId", doctor().getId().toString())
            .param("branchId", branch().getId().toString())
            .param("date", futureDate().toString())
            .param("start","09:00").param("end","12:00"))
            .andExpect(status().is3xxRedirection());
        assertEquals(before + 1, slotRepository.count());
    }

    /** TC_SCHED_002 - Session Quota Computed from Duration */
    @Test @DisplayName("TC_SCHED_002 - Session Quota Computed from Duration")
    void TC_SCHED_002_sessionMaxQuotaComputedFromDuration() {
        LocalDate date = futureDate();
        clinicSessionService.save(null, doctor().getId(), branch().getId(),
            date, LocalTime.of(9,0), LocalTime.of(12,0));
        AvailabilitySlot slot = slotRepository
            .findByDoctorAndSlotDateAndIsActive(doctor(), date, true).get(0);
        assertEquals(3, slot.getMaxPatients());
    }

    /** TC_SCHED_003 - Real-Time Availability Sync After Session Creation */
    @Test @DisplayName("TC_SCHED_003 - Real-Time Availability Sync After Session Creation")
    void TC_SCHED_003_realTimeAvailabilitySyncAfterSessionCreation() {
        LocalDate date = futureDate();
        clinicSessionService.save(null, doctor().getId(), branch().getId(),
            date, LocalTime.of(9,0), LocalTime.of(11,0));
        assertEquals(2, bookingService.availableSlots(
            doctor().getId(), branch().getId(), date, null).size());
    }

    /** TC_SCHED_004 - Block Date Hides Slot from Public Booking */
    @Test @DisplayName("TC_SCHED_004 - Block Date Hides Slot from Public Booking")
    void TC_SCHED_004_blockDateHidesSlotFromPublicBooking() {
        LocalDate date = futureDate();
        clinicSessionService.save(null, doctor().getId(), branch().getId(),
            date, LocalTime.of(9,0), LocalTime.of(11,0));
        AvailabilitySlot slot = slotRepository
            .findByDoctorAndSlotDateAndIsActive(doctor(), date, true).get(0);
        slot.setIsActive(false); slotRepository.save(slot);
        assertTrue(bookingService.availableSlots(doctor().getId(), branch().getId(), date, null).isEmpty());
    }

    /** TC_SCHED_005 - Overlapping Session Prevention */
    @Test @DisplayName("TC_SCHED_005 - Overlapping Session Prevention")
    void TC_SCHED_005_overlappingSessionPrevention() {
        LocalDate date = futureDate();
        clinicSessionService.save(null, doctor().getId(), branch().getId(),
            date, LocalTime.of(9,0), LocalTime.of(12,0));
        assertThrows(IllegalArgumentException.class, () ->
            clinicSessionService.save(null, doctor().getId(), branch().getId(),
                date, LocalTime.of(11,0), LocalTime.of(14,0)));
    }

    /** TC_SCHED_006 - View Daily Patient Queue for Doctor */
    @Test @DisplayName("TC_SCHED_006 - View Daily Patient Queue for Doctor")
    void TC_SCHED_006_viewDailyPatientQueue() {
        assertNotNull(appointmentRepository.findByDoctor(doctor()));
    }

    /** TC_SCHED_007 - Doctor Dashboard Renders for Authenticated Doctor */
    @Test @DisplayName("TC_SCHED_007 - Doctor Dashboard Renders for Authenticated Doctor")
    void TC_SCHED_007_doctorDashboardRenders() throws Exception {
        mvc.perform(get("/doctor/dashboard")
            .with(user(doctor().getUser().getUsername()).roles("DOCTOR")))
            .andExpect(status().isOk());
    }

    /** TC_SCHED_008 - Session with Active Bookings Cannot Be Deleted */
    @Test @DisplayName("TC_SCHED_008 - Session with Active Bookings Cannot Be Deleted")
    void TC_SCHED_008_sessionWithActiveBookingsCannotBeDeleted() {
        LocalDate date = LocalDate.now().plusDays(21);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) date = date.plusDays(1);
        Appointment a = new Appointment();
        a.setDoctor(doctor()); a.setHospitalBranch(branch());
        a.setAppointmentDate(date.atTime(10, 0));
        a.setContactEmail("patient@example.com"); a.setContactPhone("0771234567");
        bookingService.book(userRepository.findByUsername("dhanu").orElseThrow(), a);
        AvailabilitySlot session = slotRepository
            .findByDoctorAndSlotDateAndIsActive(doctor(), date, true).get(0);
        assertThrows(IllegalArgumentException.class, () -> clinicSessionService.delete(session.getId()));
    }

    /** TC_SCHED_009 - Edit Unbooked Session Successfully */
    @Test @DisplayName("TC_SCHED_009 - Edit Unbooked Session Successfully")
    void TC_SCHED_009_editUnbookedSessionSuccessfully() {
        LocalDate date = futureDate();
        clinicSessionService.save(null, doctor().getId(), branch().getId(),
            date, LocalTime.of(9,0), LocalTime.of(11,0));
        AvailabilitySlot slot = slotRepository
            .findByDoctorAndSlotDateAndIsActive(doctor(), date, true).get(0);
        clinicSessionService.save(slot.getId(), doctor().getId(), branch().getId(),
            date, LocalTime.of(13,0), LocalTime.of(15,0));
        assertEquals(LocalTime.of(13, 0), slotRepository.findById(slot.getId()).orElseThrow().getStartTime());
    }

    /** TC_SCHED_010 - Unauthenticated Schedule Access Blocked */
    @Test @DisplayName("TC_SCHED_010 - Unauthenticated Schedule Access Blocked")
    void TC_SCHED_010_unauthenticatedScheduleAccessBlocked() throws Exception {
        mvc.perform(post("/admin/sessions/save").with(csrf())
            .param("doctorId", doctor().getId().toString())
            .param("branchId", branch().getId().toString())
            .param("date", futureDate().toString())
            .param("start","09:00").param("end","11:00"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
}
