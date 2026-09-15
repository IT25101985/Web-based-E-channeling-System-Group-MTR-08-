package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:portal;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=true"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PortalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired InvoiceRepository invoices;
    @Autowired MedicalRecordRepository records;
    @Autowired HospitalBranchRepository branches;
    @Autowired BookingService bookings;

    @Test void publicPagesRenderAndAnonymousCannotDownloadInvoices() throws Exception {
        for (String path : new String[]{"/", "/login", "/register", "/forgot-password"}) mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get("/invoice/download/1")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/patient/book-appointment").with(user("dhanu").roles("PATIENT"))).andExpect(status().isForbidden());
    }
    @Test void everyRolePageRendersFromDatabase() throws Exception {
        for (String path : new String[]{"dashboard","appointments","doctors","users","branches","invoices","reviews","notifications"})
            mvc.perform(get("/admin/"+path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        for (String path : new String[]{"dashboard","appointments","patients","records","consultations"})
            mvc.perform(get("/doctor/"+path).with(user("drsmith").roles("DOCTOR"))).andExpect(status().isOk());
        for (String section : new String[]{"dashboard","booking","history","profile"})
            mvc.perform(get("/patient/dashboard").param("section",section).with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk());
        mvc.perform(get("/admin/users").with(user("dhanu").roles("PATIENT"))).andExpect(status().isForbidden());
    }
    private Appointment newBooking() {
        LocalDate day=LocalDate.now().plusDays(21);
        while(day.getDayOfWeek()==DayOfWeek.SUNDAY)day=day.plusDays(1);
        Doctor doc=doctors.findAll().get(0);
        Appointment a=new Appointment();a.setDoctor(doc);a.setAppointmentDate(day.atTime(10,0));a.setContactEmail("patient@example.com");a.setContactPhone("0771234567");return a;
    }
    @Test void bookingPersistsAndRejectsDuplicatePastAndForeignReschedule() throws Exception {
        User patient=users.findByUsername("dhanu").orElseThrow();
        Appointment submitted=newBooking();
        Appointment saved=bookings.book(patient,submitted);
        assertEquals(patient.getId(),appointments.findById(saved.getId()).orElseThrow().getPatient().getId());
        assertThrows(IllegalArgumentException.class,()->bookings.book(patient,submitted));
        Appointment old=newBooking();old.setAppointmentDate(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,()->bookings.book(patient,old));
        submitted.setId(saved.getId());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->bookings.book(users.findByUsername("pavi").orElseThrow(),submitted));
    }
    @Test void checkoutIgnoresClientAmountAndIsIdempotentAndOwned() throws Exception {
        Appointment a=bookings.book(users.findByUsername("dhanu").orElseThrow(),newBooking());
        mvc.perform(get("/pay").param("appointmentId",a.getId().toString()).with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk());
        mvc.perform(post("/pay").with(csrf()).with(user("pavi").roles("PATIENT")).param("type","card").param("appointmentId",a.getId().toString())).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(post("/pay").with(csrf()).with(user("dhanu").roles("PATIENT")).param("type","card").param("amount","1").param("appointmentId",a.getId().toString())).andExpect(status().isOk());
        Invoice invoice=invoices.findByAppointment(a).orElseThrow();assertEquals(a.getDoctor().getConsultationFee(),invoice.getAmount());
        assertEquals(1,invoices.findAll().stream().filter(v->v.getAppointment().getId().equals(a.getId())).count());
        mvc.perform(get("/invoice/download/"+invoice.getId()).with(user("pavi").roles("PATIENT"))).andExpect(status().isForbidden());
        byte[] pdf=mvc.perform(get("/invoice/download/"+invoice.getId()).with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdf,0,4).equals("%PDF"));
    }
    @Test void prescriptionCreatesRecordAndCannotBeAccessedByAnotherDoctor() throws Exception {
        Appointment a=bookings.book(users.findByUsername("dhanu").orElseThrow(),newBooking());
        String owner=a.getDoctor().getUser().getUsername();
        mvc.perform(get("/doctor/prescribe/"+a.getId()).with(user(owner).roles("DOCTOR"))).andExpect(status().isOk());
        mvc.perform(post("/doctor/prescribe/"+a.getId()).with(user(owner).roles("DOCTOR")).with(csrf()).param("diagnosis","Follow-up assessment").param("medication","Test medication").param("dosage","As directed").param("duration","5 days").param("price","0")).andExpect(status().is3xxRedirection());
        MedicalRecord record=records.findByAppointment(a).orElseThrow();assertEquals("COMPLETED",a.getStatus());
        mvc.perform(get("/doctor/record/"+record.getId()+"/edit").with(user("unassigned").roles("DOCTOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/patient/appointment/"+a.getId()+"/record").with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk());
    }
    @Test void branchCrudPersists() throws Exception {
        long count=branches.count();
        mvc.perform(post("/admin/branches/add").with(user("admin").roles("ADMIN")).with(csrf()).param("branchName","QA Branch").param("area","Colombo").param("address","10 Test Road").param("contactNumber","0771234567")).andExpect(status().is3xxRedirection());
        assertEquals(count+1,branches.count());var branch=branches.findAll().stream().filter(b->"QA Branch".equals(b.getBranchName())).findFirst().orElseThrow();
        mvc.perform(post("/admin/branches/"+branch.getId()+"/update").with(user("admin").roles("ADMIN")).with(csrf()).param("branchName","Updated Branch").param("area","Kandy").param("address","11 Test Road").param("contactNumber","0771234567")).andExpect(status().is3xxRedirection());
        assertEquals("Updated Branch",branches.findById(branch.getId()).orElseThrow().getBranchName());
        mvc.perform(post("/admin/branches/"+branch.getId()+"/delete").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection());assertEquals(count,branches.count());
    }
    @Test void registrationCannotOverwriteAnAccountOrElevateRole() throws Exception {
        var original=users.findByUsername("admin").orElseThrow();String hash=original.getPassword();
        mvc.perform(post("/register").with(csrf()).param("id",original.getId().toString()).param("username","newpatient").param("password","Patient@123").param("fullName","New Patient").param("email","newpatient@example.com").param("nic","200012345678").param("phoneNo","0771234567").param("address","Colombo").param("role","ROLE_ADMIN")).andExpect(status().is3xxRedirection());
        assertEquals(hash,users.findByUsername("admin").orElseThrow().getPassword());assertEquals("ROLE_PATIENT",users.findByUsername("newpatient").orElseThrow().getRole());
    }
    @Test void cancellationDeletionCleansDependencies() throws Exception {
        Appointment a=bookings.book(users.findByUsername("dhanu").orElseThrow(),newBooking());
        mvc.perform(post("/patient/appointment/"+a.getId()+"/cancel").with(user("dhanu").roles("PATIENT")).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals("CANCELLED",a.getStatus());
        mvc.perform(post("/patient/appointment/"+a.getId()+"/delete").with(user("dhanu").roles("PATIENT")).with(csrf())).andExpect(status().is3xxRedirection());
        assertTrue(appointments.findById(a.getId()).isEmpty());
    }
    @Test void realLoginChecksTheStoredPassword() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username","admin").param("password","admin123")).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/login").with(csrf()).param("username","admin").param("password","incorrect")).andExpect(redirectedUrl("/login?error"));
    }
}
