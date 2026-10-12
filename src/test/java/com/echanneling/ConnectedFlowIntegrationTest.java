package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static com.echanneling.entity.Notification.NotificationType.*;
import static com.echanneling.entity.Notification.NotificationStatus.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:connected;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "app.seed-demo=true", "app.notifications.scheduling-enabled=false", "app.mail.enabled=true", "app.mail.from=care@example.com"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ConnectedFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;
    @Autowired HospitalBranchRepository branches;
    @Autowired AvailabilitySlotRepository slots;
    @Autowired AppointmentRepository appointments;
    @Autowired NotificationRepository records;
    @Autowired BookingService bookings;
    @Autowired ClinicSessionService clinics;
    @Autowired AppointmentService appointmentService;
    @Autowired NotificationService notifications;
    @Autowired NotificationDeliveryService delivery;
    @MockitoBean JavaMailSender mail;

    private Appointment draft() {
        LocalDate date=LocalDate.now().plusDays(21);
        while(date.getDayOfWeek()==DayOfWeek.SUNDAY)date=date.plusDays(1);
        Appointment a=new Appointment();a.setDoctor(doctors.findAll().get(0));a.setHospitalBranch(branches.findAll().get(0));
        a.setAppointmentDate(date.atTime(10,0));a.setContactEmail("patient@example.com");a.setContactPhone("0771234567");return a;
    }
    private Appointment book() { return bookings.book(users.findByUsername("dhanu").orElseThrow(),draft()); }
    @Test void bookingRequiresPublishedDoctorAtSelectedHospitalAndStoresBranch() throws Exception {
        Appointment a=draft();Long correct=a.getHospitalBranch().getId();
        mvc.perform(get("/api/branches/"+correct+"/doctors")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(a.getDoctor().getId())).andExpect(jsonPath("$[0].user").doesNotExist());
        a.setHospitalBranch(branches.findAll().get(1));
        assertTrue(bookings.availableSlots(a.getDoctor().getId(),a.getHospitalBranch().getId(),a.getAppointmentDate().toLocalDate(),null).isEmpty());
        assertThrows(IllegalArgumentException.class,()->bookings.book(users.findByUsername("dhanu").orElseThrow(),a));
        Appointment saved=book();assertEquals(correct,appointments.findById(saved.getId()).orElseThrow().getHospitalBranch().getId());
        assertEquals(2,records.findByAppointmentId(saved.getId()).size());
        assertTrue(notifications.history("dhanu").stream().anyMatch(n->n.getMessage().contains(saved.getHospitalBranch().getBranchName())));
    }
    @Test void browserBookingEndpointBindsHospitalAndSavesTwoNotifications() throws Exception {
        Appointment a=draft();long before=appointments.count();
        mvc.perform(post("/patient/book-appointment").with(user("dhanu").roles("PATIENT")).with(csrf())
            .param("doctor.id",a.getDoctor().getId().toString()).param("hospitalBranch.id",a.getHospitalBranch().getId().toString())
            .param("appointmentDate",a.getAppointmentDate().toString()).param("contactEmail",a.getContactEmail()).param("contactPhone",a.getContactPhone()))
            .andExpect(redirectedUrl("/patient/dashboard?section=history"));
        assertEquals(before+1,appointments.count());
    }
    @Test void sessionsRejectOverlapAcrossHospitalsAndProtectBookedSessions() {
        Appointment a=book();
        assertThrows(IllegalArgumentException.class,()->clinics.save(null,a.getDoctor().getId(),branches.findAll().get(1).getId(),a.getAppointmentDate().toLocalDate(),LocalTime.of(9,30),LocalTime.of(11,30)));
        var session=slots.findByDoctorAndSlotDateAndIsActive(a.getDoctor(),a.getAppointmentDate().toLocalDate(),true).get(0);
        assertThrows(IllegalArgumentException.class,()->clinics.delete(session.getId()));
    }
    @Test void adminCanCreateEditAndDeleteUnbookedSession() throws Exception {
        Appointment a=draft();String date=LocalDate.now().plusDays(50).toString();long count=slots.count();
        mvc.perform(post("/admin/sessions/save").with(user("admin").roles("ADMIN")).with(csrf())
            .param("doctorId",a.getDoctor().getId().toString()).param("branchId",a.getHospitalBranch().getId().toString())
            .param("date",date).param("start","09:00").param("end","12:00")).andExpect(status().is3xxRedirection());
        assertEquals(count+1,slots.count());var session=slots.findByDoctorAndSlotDateAndIsActive(a.getDoctor(),LocalDate.parse(date),true).get(0);
        clinics.save(session.getId(),a.getDoctor().getId(),a.getHospitalBranch().getId(),LocalDate.parse(date),LocalTime.of(13,0),LocalTime.of(15,0));
        assertEquals(LocalTime.of(13,0),slots.findById(session.getId()).orElseThrow().getStartTime());
        clinics.delete(session.getId());assertEquals(count,slots.count());
    }
    @Test void cancellationReleasesTimeAndReplacesUnsentConfirmations() {
        Appointment a=book();appointmentService.cancel(a.getId());
        assertTrue(bookings.availableSlots(a.getDoctor().getId(),a.getHospitalBranch().getId(),a.getAppointmentDate().toLocalDate(),null).contains(a.getAppointmentDate().toString()));
        var messages=records.findByAppointmentId(a.getId());
        assertEquals(2,messages.stream().filter(n->n.getType()==CANCELLATION && n.getStatus()==PENDING).count());
        assertEquals(2,messages.stream().filter(n->n.getType()==CONFIRMATION && n.getStatus()==CANCELLED).count());
    }
    @Test void reschedulingUpdatesAppointmentAndQueuesBothRecipients() {
        Appointment a=book();Appointment changed=draft();changed.setId(a.getId());changed.setAppointmentDate(a.getAppointmentDate().plusHours(1));
        bookings.book(a.getPatient(),changed);
        assertEquals(changed.getAppointmentDate(),appointments.findById(a.getId()).orElseThrow().getAppointmentDate());
        assertEquals(2,records.findByAppointmentId(a.getId()).stream().filter(n->n.getType()==RESCHEDULE && n.getStatus()==PENDING).count());
    }
    @Test void rolePagesRenderAndNotificationReadCannotCrossAccounts() throws Exception {
        Appointment a=book();Notification own=notifications.history("dhanu").stream().filter(n->n.getAppointment().getId().equals(a.getId())).findFirst().orElseThrow();
        for(String path:new String[]{"/admin/sessions","/admin/notifications","/admin/notifications/templates"})
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/patient/notifications").with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk());
        mvc.perform(get("/doctor/notifications").with(user(a.getDoctor().getUser().getUsername()).roles("DOCTOR"))).andExpect(status().isOk());
        mvc.perform(post("/patient/notifications/"+own.getId()+"/read").with(user("pavi").roles("PATIENT")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/patient/notifications/"+own.getId()+"/read").with(user("dhanu").roles("PATIENT")).with(csrf())).andExpect(status().is3xxRedirection());
        assertNotNull(records.findById(own.getId()).orElseThrow().getReadAt());
        mvc.perform(get("/admin/notifications/templates").with(user("dhanu").roles("PATIENT"))).andExpect(status().isForbidden());
    }
    @Test void emailIsMarkedSentOnlyAfterProviderAcceptsAndNotResent() {
        Notification n=records.findByAppointmentId(book().getId()).get(0);delivery.deliver(n.getId());delivery.deliver(n.getId());
        assertEquals(SENT,n.getStatus());assertNotNull(n.getSentAt());verify(mail,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void failedDeliveryRetriesThreeTimesAndCanBeRetriedByAdmin() {
        doThrow(new MailSendException("unavailable")).when(mail).send(any(SimpleMailMessage.class));
        Notification n=records.findByAppointmentId(book().getId()).get(0);
        delivery.deliver(n.getId());delivery.deliver(n.getId());verify(mail,times(1)).send(any(SimpleMailMessage.class));
        assertEquals(PENDING,n.getStatus());assertNull(n.getSentAt());
        for(int i=0;i<2;i++){n.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));delivery.deliver(n.getId());}
        assertEquals(FAILED,n.getStatus());assertEquals(3,n.getAttempts());assertNotNull(n.getDeliveryError());
        notifications.retry(n.getId());assertEquals(PENDING,n.getStatus());assertEquals(0,n.getAttempts());
    }
    @Test void disabledEmailPreservesPendingInboxWithoutPretendingToSend() {
        ReflectionTestUtils.setField(delivery,"enabled",false);
        try {Notification n=records.findByAppointmentId(book().getId()).get(0);delivery.deliver(n.getId());assertEquals(PENDING,n.getStatus());assertNull(n.getSentAt());verifyNoInteractions(mail);}
        finally {ReflectionTestUtils.setField(delivery,"enabled",true);}
    }
    @Test void reminderIsQueuedOncePerScheduleAndNotForCancelledAppointments() {
        Appointment a=book();a.setAppointmentDate(LocalDateTime.now().plusHours(2));appointments.saveAndFlush(a);
        notifications.queueReminder(a.getId());notifications.queueReminder(a.getId());
        assertEquals(2,records.findByAppointmentId(a.getId()).stream().filter(n->n.getType()==REMINDER).count());
        appointmentService.cancel(a.getId());notifications.queueReminder(a.getId());
        assertEquals(2,records.findByAppointmentId(a.getId()).stream().filter(n->n.getType()==REMINDER).count());
        assertTrue(records.findByAppointmentId(a.getId()).stream().filter(n->n.getType()==REMINDER).allMatch(n->n.getStatus()==CANCELLED));
    }
    @Test void templatesRenderRealAppointmentValuesAndRejectUnknownFields() {
        notifications.saveTemplate(CONFIRMATION,"Visit #{reference}","Hello {name}: {doctor} at {hospital}, {date}.");
        Appointment a=book();var n=notifications.history("dhanu").get(0);
        assertEquals("Visit #"+a.getId(),n.getSubject());assertTrue(n.getMessage().contains(a.getHospitalBranch().getBranchName()));assertFalse(n.getMessage().contains("{doctor}"));
        assertThrows(IllegalArgumentException.class,()->notifications.saveTemplate(REMINDER,"Subject","{unknown}"));
        notifications.resetTemplate(CONFIRMATION);assertEquals("CareLink | Appointment confirmed",notifications.template(CONFIRMATION).getSubject());
    }
    @Test void overdueConfirmationIsNotSentAfterApplicationDowntime() {
        Appointment a=book();Notification n=records.findByAppointmentId(a.getId()).get(0);
        a.setAppointmentDate(LocalDateTime.now().minusHours(1));appointments.saveAndFlush(a);
        delivery.deliver(n.getId());assertEquals(CANCELLED,n.getStatus());verifyNoInteractions(mail);
    }

}
