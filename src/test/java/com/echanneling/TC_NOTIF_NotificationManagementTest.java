package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.entity.Notification.NotificationStatus;
import com.echanneling.entity.Notification.NotificationType;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** FR-04: Notification Management | TC_NOTIF_001-010 | Ariyawansha N.S.K. IT25101980 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc_notif;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=true","app.mail.from=care@example.com"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional @TestMethodOrder(MethodOrderer.DisplayName.class)
class TC_NOTIF_NotificationManagementTest {
    @Autowired MockMvc mvc;
    @Autowired BookingService bookingService;
    @Autowired AppointmentService appointmentService;
    @Autowired NotificationService notificationService;
    @Autowired NotificationDeliveryService deliveryService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired UserRepository userRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired HospitalBranchRepository branchRepository;
    @MockitoBean JavaMailSender mail;

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

    /** TC_NOTIF_001 - In-App Booking Confirmation Notification Created */
    @Test @DisplayName("TC_NOTIF_001 - In-App Booking Confirmation Notification Created")
    void TC_NOTIF_001_inAppBookingConfirmationCreated() {
        Appointment a = book();
        List<Notification> notifs = notificationRepository.findByAppointmentId(a.getId());
        assertTrue(notifs.stream().anyMatch(n -> n.getType() == NotificationType.CONFIRMATION));
    }

    /** TC_NOTIF_002 - Automated Email Sent After Booking */
    @Test @DisplayName("TC_NOTIF_002 - Automated Email Sent After Booking")
    void TC_NOTIF_002_automatedEmailSentAfterBooking() {
        Notification n = notificationRepository.findByAppointmentId(book().getId()).get(0);
        deliveryService.deliver(n.getId());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
    }

    /** TC_NOTIF_003 - Cancellation Notification Queued */
    @Test @DisplayName("TC_NOTIF_003 - Cancellation Notification Queued")
    void TC_NOTIF_003_cancellationNotificationQueued() {
        Appointment a = book();
        appointmentService.cancel(a.getId());
        long count = notificationRepository.findByAppointmentId(a.getId()).stream()
            .filter(n -> n.getType() == NotificationType.CANCELLATION && n.getStatus() == NotificationStatus.PENDING)
            .count();
        assertEquals(2, count);
    }

    /** TC_NOTIF_004 - Notification Log History Accessible */
    @Test @DisplayName("TC_NOTIF_004 - Notification Log History Accessible")
    void TC_NOTIF_004_notificationLogHistory() throws Exception {
        book();
        mvc.perform(get("/patient/notifications").with(user("dhanu").roles("PATIENT")))
            .andExpect(status().isOk());
        assertFalse(notificationService.history("dhanu").isEmpty());
    }

    /** TC_NOTIF_005 - Mark Notification as Read */
    @Test @DisplayName("TC_NOTIF_005 - Mark Notification as Read")
    void TC_NOTIF_005_markNotificationAsRead() throws Exception {
        Appointment a = book();
        Notification own = notificationService.history("dhanu").stream()
            .filter(n -> n.getAppointment() != null && n.getAppointment().getId().equals(a.getId()))
            .findFirst().orElseThrow();
        mvc.perform(post("/patient/notifications/" + own.getId() + "/read")
            .with(user("pavi").roles("PATIENT")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/patient/notifications/" + own.getId() + "/read")
            .with(user("dhanu").roles("PATIENT")).with(csrf())).andExpect(status().is3xxRedirection());
        assertNotNull(notificationRepository.findById(own.getId()).orElseThrow().getReadAt());
    }

    /** TC_NOTIF_006 - Reminder Queued 24h Before Session */
    @Test @DisplayName("TC_NOTIF_006 - Reminder Queued 24h Before Session")
    void TC_NOTIF_006_reminderQueuedOnceFor24hWindow() {
        Appointment a = book();
        a.setAppointmentDate(LocalDateTime.now().plusHours(2));
        appointmentRepository.saveAndFlush(a);
        notificationService.queueReminder(a.getId());
        notificationService.queueReminder(a.getId());
        long count = notificationRepository.findByAppointmentId(a.getId()).stream()
            .filter(n -> n.getType() == NotificationType.REMINDER).count();
        assertEquals(2, count);
    }

    /** TC_NOTIF_007 - Already-Sent Notification Not Delivered Again */
    @Test @DisplayName("TC_NOTIF_007 - Already-Sent Notification Not Delivered Again")
    void TC_NOTIF_007_alreadySentNotificationNotDeliveredAgain() {
        Notification n = notificationRepository.findByAppointmentId(book().getId()).get(0);
        deliveryService.deliver(n.getId());
        deliveryService.deliver(n.getId());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        assertEquals(NotificationStatus.SENT, n.getStatus());
    }

    /** TC_NOTIF_008 - Admin Manual Notification Saved as PENDING */
    @Test @DisplayName("TC_NOTIF_008 - Admin Manual Notification Saved as PENDING")
    void TC_NOTIF_008_adminManualNotificationSavedAsPending() {
        long before = notificationRepository.count();
        notificationService.saveManual(null, "admin@example.com",
            "Cash Reconciliation Alert", "LKR 25000 settled", NotificationType.GENERAL);
        assertEquals(before + 1, notificationRepository.count());
        assertEquals(NotificationStatus.PENDING, notificationService.getAllNotifications().get(0).getStatus());
    }

    /** TC_NOTIF_009 - Failed Delivery Retries 3 Times then FAILED */
    @Test @DisplayName("TC_NOTIF_009 - Failed Delivery Retries 3 Times then FAILED")
    void TC_NOTIF_009_failedDeliveryRetriesThreeTimes() {
        doThrow(new org.springframework.mail.MailSendException("unavailable"))
            .when(mail).send(any(SimpleMailMessage.class));
        Notification n = notificationRepository.findByAppointmentId(book().getId()).get(0);
        deliveryService.deliver(n.getId());
        assertEquals(NotificationStatus.PENDING, n.getStatus());
        for (int i = 0; i < 2; i++) {
            n.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
            deliveryService.deliver(n.getId());
        }
        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals(3, n.getAttempts());
        notificationService.retry(n.getId());
        assertEquals(NotificationStatus.PENDING, n.getStatus());
        assertEquals(0, n.getAttempts());
    }

    /** TC_NOTIF_010 - Malformed Email Payload Handled Gracefully */
    @Test @DisplayName("TC_NOTIF_010 - Malformed Email Payload Handled Gracefully")
    void TC_NOTIF_010_malformedEmailPayloadHandled() {
        assertThrows(IllegalArgumentException.class, () ->
            notificationService.saveManual(null, "broken@@mail",
                "Subject", "Body", NotificationType.GENERAL));
    }
}
