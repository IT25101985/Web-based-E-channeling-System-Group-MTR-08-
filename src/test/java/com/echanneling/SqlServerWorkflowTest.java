package com.echanneling;
import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import com.echanneling.patterns.ClinicClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.seed-demo=false","app.legacy-repair.enabled=false",
 "app.notifications.scheduling-enabled=false","app.mail.enabled=false","spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("sqlserver") @AutoConfigureMockMvc @Transactional
@EnabledIfSystemProperty(named="carelink.sqlserver",matches="true")
class SqlServerWorkflowTest {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired DoctorRepository doctors;
 @Autowired HospitalBranchRepository branches;
 @Autowired AppointmentRepository appointments;
 @Autowired InvoiceRepository invoices;
 @Autowired MedicalRecordRepository records;
 @Autowired NotificationRepository notifications;
 @Autowired ClinicSessionService clinics;
 @Autowired BookingService bookings;
 @Autowired CheckoutService checkout;
 @Autowired FeedbackService feedback;
 @Autowired jakarta.persistence.EntityManager em;

 @Test void bookingToPrescriptionAndFeedbackWithRollback() throws Exception {
  User patient=users.findByRole("ROLE_PATIENT").stream().filter(User::isActive).findFirst().orElseThrow();
  Doctor doctor=doctors.findAll().stream().filter(d->d.getUser()!=null && d.getUser().isActive()
      && d.getConsultationFee()!=null && d.getConsultationFee().signum()>0).findFirst().orElseThrow();
  HospitalBranch branch=branches.findAll().get(0);
  String admin=users.findByRole("ROLE_ADMIN").stream().filter(User::isActive).findFirst().orElseThrow().getUsername();
  LocalDate day=ClinicClock.getInstance().today().plusYears(5);
  clinics.save(null,doctor.getId(),branch.getId(),day,LocalTime.of(13,0),LocalTime.of(15,0));
  Appointment draft=new Appointment();draft.setDoctor(doctor);draft.setHospitalBranch(branch);
  draft.setAppointmentDate(day.atTime(13,0));draft.setContactEmail("workflow-check@example.test");draft.setContactPhone("0771234567");
  Appointment a=bookings.book(patient,draft);appointments.flush();
  assertEquals("SCHEDULED",a.getStatus());
  assertFalse(notifications.findByAppointmentId(a.getId()).isEmpty());
  mvc.perform(post("/admin/appointments/"+a.getId()+"/checkin").with(csrf()).with(user(admin).roles("ADMIN")))
      .andExpect(redirectedUrl("/admin/appointments"))
      .andExpect(flash().attribute("errorMessage","Check-in is available only on the appointment date."));
  Invoice invoice=checkout.checkout(a.getId(),patient.getUsername(),"cash");
  assertEquals("UNPAID",invoice.getStatus());
  checkout.collectCash(a.getId(),admin);assertEquals("PAID",invoice.getStatus());
  // Advance only this rollback-only test fixture to its visit day; never edit a real booking.
  a.setAppointmentDate(ClinicClock.getInstance().today().atTime(13,0));appointments.flush();
  mvc.perform(post("/admin/appointments/"+a.getId()+"/checkin").with(csrf()).with(user(admin).roles("ADMIN")))
      .andExpect(redirectedUrl("/admin/appointments")).andExpect(flash().attributeExists("successMessage"));
  assertEquals("CHECKED_IN",a.getStatus());
  mvc.perform(get("/doctor/prescribe/"+a.getId()).with(user(doctor.getUser().getUsername()).roles("DOCTOR")))
      .andExpect(status().isOk());
  mvc.perform(post("/doctor/prescribe/"+a.getId()).with(csrf()).with(user(doctor.getUser().getUsername()).roles("DOCTOR"))
      .param("diagnosis","Workflow verification fixture").param("medication","Test fixture only")
      .param("dosage","Test").param("duration","Test").param("price","0"))
      .andExpect(redirectedUrl("/doctor/appointments?success"));
  assertEquals("COMPLETED",a.getStatus());assertTrue(records.findByAppointment(a).isPresent());
  Feedback review=feedback.submit(a.getId(),patient.getUsername(),5,"Workflow test fixture");
  feedback.approveFeedback(review.getId());assertEquals("APPROVED",review.getModerationStatus());
  em.flush();
  mvc.perform(get("/invoice/download/"+invoice.getId()).with(user(patient.getUsername()).roles("PATIENT")))
      .andExpect(status().isOk());
 }
}
