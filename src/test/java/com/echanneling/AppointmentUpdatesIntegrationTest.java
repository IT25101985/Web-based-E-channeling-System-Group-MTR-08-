package com.echanneling;
import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import com.echanneling.patterns.ClinicClock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Appointment module regressions - Dhanukshan S., IT25101985. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:appointment_updates;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class AppointmentUpdatesIntegrationTest {
 @Autowired BookingService booking;
 @Autowired AppointmentService visits;
 @Autowired CheckoutService checkout;
 @Autowired UserRepository users;
 @Autowired DoctorRepository doctors;
 @Autowired HospitalBranchRepository branches;
 @Autowired AppointmentRepository appointments;
 @Autowired InvoiceRepository invoices;
 @Autowired NotificationRepository notifications;
 @Autowired MockMvc mvc;
 @MockitoBean JavaMailSender mail;
 User patient(){return users.findByUsername("dhanu").orElseThrow();}
 Appointment draft(Doctor doctor,String nic){
  LocalDate date=ClinicClock.getInstance().today().plusDays(21);
  while(date.getDayOfWeek()==DayOfWeek.SUNDAY)date=date.plusDays(1);
  Appointment a=new Appointment();a.setDoctor(doctor);a.setHospitalBranch(branches.findAll().get(0));
  a.setAppointmentDate(date.atTime(10,0));a.setContactEmail("family@example.test");a.setContactPhone("0771234567");
  if(nic!=null){a.setBookingFor("other");a.setPatientName("Family Patient");a.setRecipientNic(nic);}
  return a;
 }
 @Test void familyBookingPreservesIdentityAndBothObserversQueueMessages(){
  Appointment a=booking.book(patient(),draft(doctors.findAll().get(0),"200012345678"));
  appointments.flush();assertEquals("Family Patient",appointments.findById(a.getId()).orElseThrow().getPatientName());
  assertEquals("200012345678",a.getRecipientNic());
  var messages=notifications.findByAppointmentId(a.getId());assertEquals(2,messages.size());
  assertTrue(messages.stream().allMatch(n->n.getMessage().contains("Family Patient")));
  assertTrue(messages.stream().anyMatch(n->"family@example.test".equals(n.getRecipient())));
  assertTrue(messages.stream().anyMatch(n->n.getRecipientUser().getId().equals(a.getDoctor().getUser().getId())));
 }
 @Test void sameAttendeeCannotOverlapAcrossDoctorsAndAccounts(){
  booking.book(patient(),draft(doctors.findAll().get(0),"200012345678"));
  User other=users.findByUsername("pavi").orElseThrow();
  assertThrows(IllegalArgumentException.class,()->booking.book(other,draft(doctors.findAll().get(1),"200012345678")));
 }
 @Test void checkoutUsesAgreedFeeAfterDoctorPriceChanges(){
  Doctor doctor=doctors.findAll().get(0);Appointment a=booking.book(patient(),draft(doctor,null));
  BigDecimal agreed=a.getBookedConsultationFee();doctor.setConsultationFee(new BigDecimal("9999.00"));doctors.saveAndFlush(doctor);
  Invoice receipt=checkout.checkout(a.getId(),patient().getUsername(),"cash");
  assertEquals(0,agreed.compareTo(receipt.getAmount()));assertEquals("UNPAID",receipt.getStatus());
 }
 @Test void archiveRetainsReceiptAndHidesOnlyPatientHistory(){
  Appointment a=booking.book(patient(),draft(doctors.findAll().get(0),null));
  Invoice receipt=checkout.checkout(a.getId(),patient().getUsername(),"cash");
  visits.cancel(a.getId());visits.deleteAppointment(a);appointments.flush();
  assertTrue(invoices.findById(receipt.getId()).isPresent());
  assertTrue(appointments.findById(a.getId()).isPresent());
  assertFalse(visits.patientHistory(patient(),0).stream().anyMatch(x->x.getId().equals(a.getId())));
 }
 @Test void inactiveDoctorHasNoBookableSlots(){
  Doctor doctor=doctors.findAll().get(0);Appointment a=draft(doctor,null);
  doctor.getUser().setActive(false);users.saveAndFlush(doctor.getUser());
  assertTrue(booking.availableSlots(doctor.getId(),a.getHospitalBranch().getId(),a.getAppointmentDate().toLocalDate(),null).isEmpty());
  assertThrows(IllegalArgumentException.class,()->booking.book(patient(),a));
 }
 @Test void browserBookingBindsFamilyDetailsAndContinuesToCheckout() throws Exception {
  Appointment a=draft(doctors.findAll().get(0),"200012345678");
  mvc.perform(post("/patient/book-appointment").with(csrf()).with(user("dhanu").roles("PATIENT"))
   .param("doctor.id",a.getDoctor().getId().toString()).param("hospitalBranch.id",a.getHospitalBranch().getId().toString())
   .param("appointmentDate",a.getAppointmentDate().toString()).param("contactEmail",a.getContactEmail()).param("contactPhone",a.getContactPhone())
   .param("bookingFor","other").param("patientName","Family Patient").param("recipientNic","200012345678").param("settlementType","cash"))
   .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/pay?appointmentId=*"));
 }
}
