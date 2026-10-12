package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import com.echanneling.strategy.SmsNotificationStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:reviewfix;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=true", "app.notifications.scheduling-enabled=false", "app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class ReviewFixIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;
    @Autowired HospitalBranchRepository branches;
    @Autowired AppointmentRepository appointments;
    @Autowired AvailabilitySlotRepository sessions;
    @Autowired InvoiceRepository invoices;
    @Autowired MedicalRecordRepository records;
    @Autowired NotificationRepository notifications;
    @Autowired BookingService bookings;
    @Autowired CheckoutService checkout;
    @Autowired UserService accounts;
    @Autowired AppointmentService visits;
    @Autowired PasswordEncoder encoder;
    @Autowired PdfService pdf;

    User patient() { return users.findByUsername("dhanu").orElseThrow(); }
    Appointment draft() {
        LocalDate date=LocalDate.now().plusDays(20);
        while(date.getDayOfWeek()==DayOfWeek.SUNDAY) date=date.plusDays(1);
        Appointment a=new Appointment();
        a.setDoctor(doctors.findAll().get(0)); a.setHospitalBranch(branches.findAll().get(0));
        a.setAppointmentDate(date.atTime(10,0)); a.setContactEmail("patient@example.test"); a.setContactPhone("0771234567");
        return a;
    }
    Appointment family() {
        Appointment a=draft(); a.setBookingFor("other"); a.setPatientName("Family Member");
        a.setRecipientNic("198012345678"); return bookings.book(patient(),a);
    }

    @Test void familyIdentitySurvivesPersistenceRescheduleAndClinicalDocuments() throws Exception {
        Appointment a=family(); Long owner=a.getPatient().getId();
        appointments.flush();
        assertEquals("Family Member",a.getPatientName()); assertEquals(owner,patient().getId());
        assertTrue(notifications.findByAppointmentId(a.getId()).stream().allMatch(n->n.getMessage().contains("Family Member")));
        Appointment change=draft(); change.setId(a.getId()); change.setAppointmentDate(a.getAppointmentDate().plusHours(1));
        bookings.book(patient(),change); appointments.flush();
        assertEquals("Family Member",a.getPatientName()); assertEquals("198012345678",a.getRecipientNic());
        String doctor=a.getDoctor().getUser().getUsername();
        mvc.perform(get("/doctor/consultations").param("appointmentId",a.getId().toString()).with(user(doctor).roles("DOCTOR")))
            .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("patient"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Family Member")));
        a.setAppointmentDate(LocalDateTime.now()); visits.checkIn(a.getId());
        mvc.perform(post("/doctor/prescribe/"+a.getId()).with(csrf()).with(user(doctor).roles("DOCTOR"))
            .param("diagnosis","Family diagnosis").param("medication","Test").param("dosage","Test").param("duration","1 day").param("price","0"))
            .andExpect(redirectedUrl("/doctor/appointments?success"));
        MedicalRecord record=records.findByAppointment(a).orElseThrow();
        assertEquals("Family Member",record.getPatientName());
        var reader=new com.itextpdf.text.pdf.PdfReader(pdf.generateMedicalRecordPDF(record));
        assertTrue(com.itextpdf.text.pdf.parser.PdfTextExtractor.getTextFromPage(reader,1).contains("Family Member")); reader.close();
        var response=mvc.perform(get("/patient/dashboard").with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk()).andReturn();
        Object latest=response.getModelAndView().getModel().get("latestRecord");
        assertNotEquals(record,latest,"Family records must not become the account owner's latest personal health record");
    }

    @Test void familyNameAndNicAreRequiredAndCannotBeMassAssignedToOwner() throws Exception {
        Appointment a=draft(); a.setBookingFor("other");
        assertThrows(IllegalArgumentException.class,()->bookings.book(patient(),a));
        a.setPatientName("Family Member");
        assertThrows(IllegalArgumentException.class,()->bookings.book(patient(),a));
        a.setRecipientNic("invalid");
        assertThrows(IllegalArgumentException.class,()->bookings.book(patient(),a));
        assertEquals("dhanu",patient().getUsername());
    }

    @Test void bookingFormPersistsRecipientAndCarriesCardChoiceToCheckout() throws Exception {
        Appointment d=draft();
        var result=mvc.perform(post("/patient/book-appointment").with(csrf()).with(user("dhanu").roles("PATIENT"))
            .param("doctor.id",d.getDoctor().getId().toString()).param("hospitalBranch.id",d.getHospitalBranch().getId().toString())
            .param("appointmentDate",d.getAppointmentDate().toString()).param("contactEmail",d.getContactEmail()).param("contactPhone",d.getContactPhone())
            .param("bookingFor","other").param("patientName","Family Member").param("recipientNic","198012345678").param("settlementType","card")
            .param("bookedConsultationFee","1").param("patient.id","99999"))
            .andExpect(status().is3xxRedirection()).andReturn();
        Appointment a=appointments.findByPatient(patient()).stream().filter(x->d.getAppointmentDate().equals(x.getAppointmentDate())).findFirst().orElseThrow();
        assertEquals("/pay?appointmentId="+a.getId(),result.getResponse().getRedirectedUrl());
        assertEquals("card",a.getSettlementType()); assertEquals("Family Member",a.getPatientName());
        assertEquals(d.getDoctor().getConsultationFee(),a.getBookedConsultationFee());
        assertTrue(invoices.findByAppointment(a).isEmpty(),"Choosing card must not simulate payment before checkout confirmation");
        mvc.perform(get("/pay").param("appointmentId",a.getId().toString()).with(user("dhanu").roles("PATIENT")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"card\" selected=\"selected\"")));
    }

    @Test void cashCheckoutRemainsUnpaidUntilReceptionCollects() {
        Appointment d=draft(); d.setSettlementType("cash"); Appointment a=bookings.book(patient(),d);
        Invoice invoice=checkout.checkout(a.getId(),"dhanu","cash");
        assertEquals("UNPAID",invoice.getStatus());
        checkout.collectCash(a.getId(),"reception"); assertEquals("PAID",invoice.getStatus());
    }

    @Test void checkInAllowsPrescriptionAndCannotCompleteOrReopenVisit() throws Exception {
        Appointment a=bookings.book(patient(),draft());
        a.setAppointmentDate(LocalDateTime.now());
        mvc.perform(post("/admin/appointments/"+a.getId()+"/checkin").with(csrf()).with(user("admin").roles("ADMIN"))).andExpect(status().is3xxRedirection());
        assertEquals("CHECKED_IN",a.getStatus()); assertTrue(records.findByAppointment(a).isEmpty());
        String doctor=a.getDoctor().getUser().getUsername();
        mvc.perform(get("/doctor/appointments").with(user(doctor).roles("DOCTOR"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/doctor/prescribe/"+a.getId())));
        mvc.perform(post("/doctor/prescribe/"+a.getId()).with(csrf()).with(user(doctor).roles("DOCTOR"))
            .param("diagnosis","Diagnosis").param("medication","Test").param("dosage","Test").param("duration","1 day").param("price","0"))
            .andExpect(redirectedUrl("/doctor/appointments?success"));
        assertEquals("COMPLETED",a.getStatus()); assertTrue(records.findByAppointment(a).isPresent());
        assertThrows(IllegalArgumentException.class,()->visits.checkIn(a.getId()));
    }

    @Test void inactiveDoctorIsHiddenAndCannotReceiveBookings() throws Exception {
        Appointment d=draft(); accounts.setAccountActive(d.getDoctor().getUser().getId(),false);
        assertTrue(bookings.availableSlots(d.getDoctor().getId(),d.getHospitalBranch().getId(),d.getAppointmentDate().toLocalDate(),null).isEmpty());
        assertThrows(IllegalArgumentException.class,()->bookings.book(patient(),d));
        mvc.perform(get("/api/branches/"+d.getHospitalBranch().getId()+"/doctors")).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == "+d.getDoctor().getId()+")]").isEmpty());
        var result=mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        var visible=(java.util.List<Doctor>)result.getModelAndView().getModel().get("doctors");
        assertTrue(visible.stream().noneMatch(x->x.getId().equals(d.getDoctor().getId())));
    }

    @Test void feeIsFixedAtBookingAndSurvivesReschedule() {
        Appointment a=bookings.book(patient(),draft()); java.math.BigDecimal agreed=a.getBookedConsultationFee();
        a.getDoctor().setConsultationFee(agreed.add(new java.math.BigDecimal("750.00")));
        Appointment change=draft();change.setId(a.getId());change.setAppointmentDate(a.getAppointmentDate().plusHours(1));
        bookings.book(patient(),change);
        assertEquals(agreed,a.getPayableFee());
        assertEquals(agreed,checkout.checkout(a.getId(),"dhanu","cash").getAmount());
    }

    @Test void passwordPrefixIsAlwaysEncodedForRegistrationAndReset() throws Exception {
        String raw="$2a$ThisIsPlainText";
        mvc.perform(post("/register").with(csrf()).param("username","prefixuser").param("password",raw)
            .param("fullName","Prefix User").param("email","prefix@example.test").param("phoneNo","0771234567")
            .param("address","Colombo").param("nic","198012345678"))
            .andExpect(redirectedUrl("/login?registered"));
        User u=users.findByUsername("prefixuser").orElseThrow();
        assertNotEquals(raw,u.getPassword()); assertTrue(encoder.matches(raw,u.getPassword()));
        String reset="$2b$NewRawPassword";
        mvc.perform(post("/admin/users/"+u.getId()+"/reset-password").with(csrf()).with(user("admin").roles("ADMIN"))
            .param("newPassword",reset)).andExpect(redirectedUrl("/admin/users?reset_success"));
        assertTrue(encoder.matches(reset,u.getPassword()));
        mvc.perform(post("/login").with(csrf()).param("username","prefixuser").param("password",reset))
            .andExpect(redirectedUrl("/dashboard"));
    }

    @Test void profileEditWithoutPasswordPreservesExistingHash() throws Exception {
        String hash=patient().getPassword();
        mvc.perform(post("/patient/profile").with(csrf()).with(user("dhanu").roles("PATIENT"))
            .param("fullName",patient().getFullName()).param("address","New Address").param("phoneNo","0771234567"))
            .andExpect(redirectedUrl("/patient/dashboard?section=profile&success"));
        assertEquals(hash,patient().getPassword());
    }

    @Test void homepageUsesPublishedUnoccupiedTodaySlots() throws Exception {
        Appointment d=draft(); Doctor doctor=d.getDoctor();
        sessions.findByDoctorAndSlotDateAndIsActive(doctor,LocalDate.now(),true).forEach(s->s.setIsActive(false));
        assertFalse(bookings.todayAvailability().containsKey(doctor.getId()));
        AvailabilitySlot slot=new AvailabilitySlot(); slot.setDoctor(doctor);slot.setHospitalBranch(d.getHospitalBranch());
        slot.setSlotDate(LocalDate.now());slot.setStartTime(LocalTime.MIDNIGHT);slot.setEndTime(LocalTime.of(23,0));
        slot.setIsActive(true);slot.setMaxPatients(23);slot.setBookedCount(0);sessions.saveAndFlush(slot);
        var free=bookings.availableSlots(doctor.getId(),d.getHospitalBranch().getId(),LocalDate.now(),null);
        Map<Long,BookingService.TodayAvailability> actual=bookings.todayAvailability();
        if(free.isEmpty()) assertFalse(actual.containsKey(doctor.getId()));
        else assertEquals(LocalDateTime.parse(free.get(0)),actual.get(doctor.getId()).time());
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Next: Today 05:00 PM"))));
    }

    @Test void smsDoesNotPretendToSendAndPublicCopyStatesLimitation() throws Exception {
        assertThrows(UnsupportedOperationException.class,()->new SmsNotificationStrategy().sendNotification("0771234567","Test","Test"));
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SMS is not enabled")));
    }

    @Test void wrongDayCheckInReturnsUsefulMessageWithoutChangingAppointment() throws Exception {
        Appointment a=bookings.book(patient(),draft());
        for (java.time.LocalDate day : java.util.List.of(
                com.echanneling.patterns.ClinicClock.getInstance().today().plusDays(1),
                com.echanneling.patterns.ClinicClock.getInstance().today().minusDays(1))) {
            a.setAppointmentDate(day.atTime(10,0)); appointments.flush();
            mvc.perform(get("/admin/appointments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                    "action=\"/admin/appointments/"+a.getId()+"/checkin\""))));
            mvc.perform(post("/admin/appointments/"+a.getId()+"/checkin").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/appointments"))
                .andExpect(flash().attribute("errorMessage","Check-in is available only on the appointment date."));
            assertEquals("SCHEDULED",a.getStatus());
            assertTrue(records.findByAppointment(a).isEmpty());
        }
    }

    @Test void missingAppointmentCheckInReturnsToListWithClearMessage() throws Exception {
        mvc.perform(post("/admin/appointments/9223372036854775807/checkin").with(csrf()).with(user("admin").roles("ADMIN")))
            .andExpect(redirectedUrl("/admin/appointments"))
            .andExpect(flash().attribute("errorMessage","Appointment not found."));
    }
}
