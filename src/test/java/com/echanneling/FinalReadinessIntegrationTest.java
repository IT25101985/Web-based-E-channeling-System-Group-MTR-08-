package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:finalready;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=true", "app.notifications.scheduling-enabled=false", "app.mail.enabled=true", "app.mail.from=carelink@example.test", "spring.mail.host=127.0.0.1", "spring.mail.username=", "spring.mail.password=", "spring.mail.properties.mail.smtp.auth=false", "spring.mail.properties.mail.smtp.starttls.enable=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class FinalReadinessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;
    @Autowired HospitalBranchRepository branches;
    @Autowired AppointmentRepository appointments;
    @Autowired InvoiceRepository invoices;
    @Autowired FeedbackRepository feedback;
    @Autowired NotificationRepository notifications;
    @Autowired BookingService bookings;
    @Autowired CheckoutService checkout;
    @Autowired UserService accounts;
    @Autowired FeedbackService reviews;
    @Autowired NotificationDeliveryService delivery;
    @Autowired UserDetailsService userDetails;
    private static final LocalSmtp smtp = new LocalSmtp();
    @DynamicPropertySource static void mailPort(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.port", smtp::port);
        if (Boolean.getBoolean("carelink.qa.sqlserver")) {
            java.util.Properties local = new java.util.Properties();
            try (var input = java.nio.file.Files.newInputStream(java.nio.file.Path.of("application-local.properties"))) { local.load(input); }
            catch (IOException e) { throw new UncheckedIOException(e); }
            registry.add("spring.datasource.url", () -> "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=CareLink_Final_QA_20261006;encrypt=true;trustServerCertificate=true;loginTimeout=10;");
            registry.add("spring.datasource.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
            registry.add("spring.datasource.username", () -> local.getProperty("spring.datasource.username", "sa"));
            registry.add("spring.datasource.password", () -> local.getProperty("spring.datasource.password", ""));
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.SQLServerDialect");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        }
    }
    @AfterAll static void closeSmtp() throws Exception { smtp.close(); }
    @BeforeEach void clearMail() { smtp.messages.clear(); }

    Appointment draft() {
        LocalDate day=LocalDate.now().plusDays(23);
        while(day.getDayOfWeek()==DayOfWeek.SUNDAY) day=day.plusDays(1);
        Appointment a=new Appointment(); a.setDoctor(doctors.findAll().get(0)); a.setHospitalBranch(branches.findAll().get(0));
        a.setAppointmentDate(day.atTime(11,0)); a.setContactEmail("patient@example.test"); a.setContactPhone("0771234567"); return a;
    }
    Appointment booking() { return bookings.book(users.findByUsername("dhanu").orElseThrow(),draft()); }
    Appointment completed() { Appointment a=booking();a.setStatus("COMPLETED");return appointments.saveAndFlush(a); }

    @Test void receptionPagesAndRoleBoundaries() throws Exception {
        mvc.perform(get("/reception/dashboard").with(user("reception").roles("RECEPTIONIST"))).andExpect(status().isOk());
        mvc.perform(get("/dashboard").with(user("reception").roles("RECEPTIONIST"))).andExpect(redirectedUrl("/reception/dashboard"));
        mvc.perform(get("/reception/dashboard").with(user("dhanu").roles("PATIENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").with(user("reception").roles("RECEPTIONIST"))).andExpect(status().isForbidden());
        mvc.perform(get("/doctor/records").with(user("reception").roles("RECEPTIONIST"))).andExpect(status().isForbidden());
        mvc.perform(post("/reception/book").with(user("reception").roles("RECEPTIONIST"))).andExpect(status().isForbidden());
    }
    @Test void receptionistBooksAndCollectsCashOnce() throws Exception {
        Appointment d=draft();User patient=users.findByUsername("dhanu").orElseThrow();long before=appointments.count();
        mvc.perform(post("/reception/book").with(csrf()).with(user("reception").roles("RECEPTIONIST"))
            .param("patientId",patient.getId().toString()).param("doctorId",d.getDoctor().getId().toString())
            .param("branchId",d.getHospitalBranch().getId().toString()).param("appointmentDate",d.getAppointmentDate().toString())
            .param("contactEmail",d.getContactEmail()).param("contactPhone",d.getContactPhone()))
            .andExpect(redirectedUrl("/reception/dashboard")).andExpect(flash().attributeExists("successMessage"));
        assertEquals(before+1,appointments.count());
        Appointment a=appointments.findByPatient(patient).stream().filter(x->d.getAppointmentDate().equals(x.getAppointmentDate())).findFirst().orElseThrow();
        for(int i=0;i<2;i++) mvc.perform(post("/reception/appointments/"+a.getId()+"/cash").with(csrf()).with(user("reception").roles("RECEPTIONIST")))
            .andExpect(redirectedUrl("/reception/dashboard")).andExpect(flash().attributeExists("successMessage"));
        Invoice inv=invoices.findByAppointment(a).orElseThrow();
        assertEquals("PAID",inv.getStatus());assertEquals("CASH",inv.getPaymentMethod());assertEquals("reception",inv.getCollectedBy());assertNotNull(inv.getCollectedAt());
        assertEquals(a.getDoctor().getConsultationFee(),inv.getAmount());
        assertEquals(1,invoices.findAll().stream().filter(x->x.getAppointment().getId().equals(a.getId())).count());
        mvc.perform(get("/invoice/download/"+inv.getId()).with(user("reception").roles("RECEPTIONIST"))).andExpect(status().isOk());
    }
    @Test void receptionistReschedulesAndCancelsWithSharedBookingPolicy() throws Exception {
        Appointment a=booking();LocalDateTime newTime=a.getAppointmentDate().plusHours(1);
        mvc.perform(post("/reception/book").with(csrf()).with(user("reception").roles("RECEPTIONIST"))
            .param("id",a.getId().toString()).param("patientId",a.getPatient().getId().toString()).param("doctorId",a.getDoctor().getId().toString())
            .param("branchId",a.getHospitalBranch().getId().toString()).param("appointmentDate",newTime.toString())
            .param("contactEmail",a.getContactEmail()).param("contactPhone",a.getContactPhone())).andExpect(flash().attributeExists("successMessage"));
        assertEquals(newTime,a.getAppointmentDate());
        mvc.perform(post("/reception/appointments/"+a.getId()+"/cancel").with(csrf()).with(user("reception").roles("RECEPTIONIST"))).andExpect(flash().attributeExists("successMessage"));
        assertEquals("CANCELLED",a.getStatus());
        assertTrue(notifications.findByAppointmentId(a.getId()).stream().anyMatch(n->n.getType()==Notification.NotificationType.CANCELLATION));
    }
    @Test void cashCannotOverwriteCardOrRefundedInvoices() {
        Appointment a=booking();Invoice invoice=checkout.checkout(a.getId(),"dhanu","card");
        assertThrows(IllegalArgumentException.class,()->checkout.collectCash(a.getId(),"reception"));
        assertEquals("CARD",invoice.getPaymentMethod());assertNull(invoice.getCollectedBy());
        invoice.setStatus("REFUNDED");assertThrows(IllegalArgumentException.class,()->checkout.collectCash(a.getId(),"reception"));
    }
    @Test void patientCannotCollectCash() throws Exception {
        Appointment a=booking();
        mvc.perform(post("/reception/appointments/"+a.getId()+"/cash").with(csrf()).with(user("dhanu").roles("PATIENT"))).andExpect(status().isForbidden());
        assertTrue(invoices.findByAppointment(a).isEmpty());
    }
    @Test void deactivatePreservesHistoryAndBlocksNewAndExistingSessions() throws Exception {
        Appointment a=completed();Invoice invoice=checkout.checkout(a.getId(),"dhanu","cash");Feedback review=reviews.submit(a.getId(),"dhanu",5,"");
        Long uid=a.getPatient().getId();
        mvc.perform(post("/patient/deactivate-account").with(csrf()).with(user("dhanu").roles("PATIENT"))).andExpect(redirectedUrl("/login?inactive"));
        assertFalse(users.findById(uid).orElseThrow().isActive());assertFalse(userDetails.loadUserByUsername("dhanu").isEnabled());
        assertTrue(appointments.existsById(a.getId()));assertTrue(invoices.existsById(invoice.getId()));assertTrue(feedback.existsById(review.getId()));
        mvc.perform(get("/patient/dashboard").with(user("dhanu").roles("PATIENT"))).andExpect(redirectedUrl("/login?inactive"));
        mvc.perform(post("/login").with(csrf()).param("username","dhanu").param("password","patient123")).andExpect(redirectedUrl("/login?error"));
    }
    @Test void adminCanReactivateButPatientCannotAndAdminIsProtected() throws Exception {
        User patient=users.findByUsername("dhanu").orElseThrow();accounts.setAccountActive(patient.getId(),false);
        mvc.perform(post("/admin/users/"+patient.getId()+"/active").param("active","true").with(csrf()).with(user("admin").roles("ADMIN"))).andExpect(redirectedUrl("/admin/users?updated"));
        assertTrue(patient.isActive());assertTrue(userDetails.loadUserByUsername("dhanu").isEnabled());
        mvc.perform(post("/admin/users/"+patient.getId()+"/active").param("active","false").with(csrf()).with(user("pavi").roles("PATIENT"))).andExpect(status().isForbidden());
        assertThrows(IllegalArgumentException.class,()->accounts.setAccountActive(users.findByUsername("admin").orElseThrow().getId(),false));
    }
    @Test void inactivePatientCannotBeBookedAtFrontDesk() {
        User p=users.findByUsername("dhanu").orElseThrow();accounts.setAccountActive(p.getId(),false);
        assertThrows(IllegalArgumentException.class,()->bookings.book(p,draft()));
    }
    @Test void feedbackLinksExactVisitAndRejectsDuplicateAndOtherPatient() throws Exception {
        Appointment a=completed();
        mvc.perform(post("/feedback/add").with(csrf()).with(user("pavi").roles("PATIENT")).param("appointmentId",a.getId().toString()).param("rating","5")).andExpect(status().isForbidden());
        mvc.perform(post("/feedback/add").with(csrf()).with(user("dhanu").roles("PATIENT")).param("appointmentId",a.getId().toString()).param("rating","4")).andExpect(flash().attributeExists("successMessage"));
        Feedback saved=feedback.findAll().stream().filter(f->f.getAppointment()!=null&&f.getAppointment().getId().equals(a.getId())).findFirst().orElseThrow();
        assertEquals(a.getDoctor().getId(),saved.getDoctor().getId());assertEquals("PENDING",saved.getModerationStatus());assertEquals("",saved.getComments());
        mvc.perform(post("/feedback/add").with(csrf()).with(user("dhanu").roles("PATIENT")).param("appointmentId",a.getId().toString()).param("rating","5")).andExpect(flash().attributeExists("errorMessage"));
        assertEquals(1,feedback.findAll().stream().filter(f->f.getAppointment()!=null&&f.getAppointment().getId().equals(a.getId())).count());
    }
    @Test void feedbackRequiresCompletedVisitAndValidRating() {
        Appointment a=booking();assertThrows(IllegalArgumentException.class,()->reviews.submit(a.getId(),"dhanu",5,""));
        a.setStatus("COMPLETED");assertThrows(IllegalArgumentException.class,()->reviews.submit(a.getId(),"dhanu",6,""));
    }
    @Test void realSmtpTransportDeliversOutboxMessageToLocalServer() throws Exception {
        Appointment a=booking();
        Notification n=notifications.findByAppointmentId(a.getId()).stream().filter(x->x.getRecipient().equals("patient@example.test")).findFirst().orElseThrow();
        delivery.deliver(n.getId());
        String message=smtp.messages.poll(5,TimeUnit.SECONDS);
        assertNotNull(message,"Local SMTP server must receive the complete message");
        assertTrue(message.contains("patient@example.test"));assertTrue(message.contains("Subject:"));
        assertEquals(Notification.NotificationStatus.SENT,n.getStatus());assertEquals(1,n.getAttempts());assertNotNull(n.getSentAt());
        delivery.deliver(n.getId());assertNull(smtp.messages.poll(100,TimeUnit.MILLISECONDS));
    }
    @Test void administratorCanCreateReceptionAccountUsingFactory() throws Exception {
        mvc.perform(post("/admin/users/add").with(csrf()).with(user("admin").roles("ADMIN"))
            .param("fullName","Second Receptionist").param("username","receptiontwo").param("email","desk2@example.test")
            .param("password","Reception@456").param("phoneNo","0771234567").param("address","Front desk").param("role","ROLE_RECEPTIONIST"))
            .andExpect(redirectedUrl("/admin/users?user_added"));
        assertEquals("ROLE_RECEPTIONIST",users.findByUsername("receptiontwo").orElseThrow().getRole());
    }

    /** Minimal loopback SMTP sink: no external mail or credentials are used. */
    static class LocalSmtp implements AutoCloseable {
        final ServerSocket server;
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        LocalSmtp() {
            try { server=new ServerSocket(0,20,InetAddress.getByName("127.0.0.1")); }
            catch(IOException e) { throw new UncheckedIOException(e); }
            Thread worker=new Thread(()->{
                while(!server.isClosed()) try(Socket socket=server.accept()) { handle(socket); }
                catch(IOException e) { if(!server.isClosed()) throw new UncheckedIOException(e); }
            },"carelink-test-smtp");worker.setDaemon(true);worker.start();
        }
        int port() { return server.getLocalPort(); }
        void handle(Socket socket) throws IOException {
            socket.setSoTimeout(5000);
            var in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            var out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);
            out.print("220 localhost test SMTP\r\n");out.flush();
            boolean data=false;StringBuilder body=new StringBuilder();String line;
            while((line=in.readLine())!=null) {
                if(data) {
                    if(line.equals(".")) { messages.add(body.toString());data=false;out.print("250 queued\r\n"); }
                    else body.append(line).append('\n');
                } else if(line.startsWith("DATA")) { data=true;out.print("354 end with dot\r\n"); }
                else if(line.startsWith("QUIT")) { out.print("221 bye\r\n");out.flush();break; }
                else out.print("250 OK\r\n");
                out.flush();
            }
        }
        public void close() throws IOException { server.close(); }
    }
}
