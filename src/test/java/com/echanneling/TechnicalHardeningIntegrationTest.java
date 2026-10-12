package com.echanneling;
import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import com.echanneling.strategy.NotificationStrategy;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:hardening;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop",
 "app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=true","app.recovery.base-url=https://carelink.example"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class TechnicalHardeningIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired DoctorRepository doctors;
 @Autowired HospitalBranchRepository branches;
 @Autowired AppointmentRepository appointments;
 @Autowired AvailabilitySlotRepository slots;
 @Autowired InvoiceRepository invoices;
 @Autowired InvoiceAuditRepository audit;
 @Autowired MedicalRecordRepository records;
 @Autowired PasswordResetRepository resets;
 @Autowired NotificationRepository messages;
 @Autowired BookingService bookings;
 @Autowired ClinicSessionService clinics;
 @Autowired AppointmentService visits;
 @Autowired CheckoutService checkout;
 @Autowired UserService accounts;
 @Autowired PasswordRecoveryService recovery;
 @Autowired InvoiceService billing;
 @Autowired NotificationDeliveryService delivery;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
 @MockitoBean(name="emailNotificationStrategy") NotificationStrategy transport;
 User patient() { return users.findByUsername("dhanu").orElseThrow(); }
 Appointment draft(Doctor doctor,LocalDate date,String nic) {
  HospitalBranch branch=branches.findAll().get(0);
  if(slots.findByDoctorAndSlotDateAndIsActive(doctor,date,true).isEmpty())
   clinics.save(null,doctor.getId(),branch.getId(),date,LocalTime.of(9,0),LocalTime.of(12,0));
  Appointment a=new Appointment();a.setDoctor(doctor);a.setHospitalBranch(branch);a.setAppointmentDate(date.atTime(10,0));
  a.setContactEmail("patient@example.test");a.setContactPhone("0771234567");
  if(nic!=null) { a.setBookingFor("other");a.setPatientName("Relative");a.setRecipientNic(nic); }
  return a;
 }
 Appointment booking() { return bookings.book(patient(),draft(doctors.findAll().get(0),LocalDate.now().plusDays(45),null)); }

 @Test void paidCancelledVisitCanBeHiddenWithoutDestroyingItsReceipt() throws Exception {
  Appointment a=booking();Invoice receipt=checkout.checkout(a.getId(),"dhanu","cash");checkout.collectCash(a.getId(),"reception");
  visits.cancel(a.getId());
  mvc.perform(post("/patient/appointment/"+a.getId()+"/delete").with(csrf()).with(user("dhanu").roles("PATIENT"))).andExpect(status().is3xxRedirection());
  assertTrue(appointments.findById(a.getId()).orElseThrow().isHiddenByPatient());
  assertEquals("PAID",invoices.findById(receipt.getId()).orElseThrow().getStatus());
  assertFalse(visits.patientHistory(patient(),0).getContent().stream().anyMatch(x->x.getId().equals(a.getId())));
  mvc.perform(get("/invoice/download/"+receipt.getId()).with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk());
 }
 @Test void deactivatingDoctorPreservesPatientRecordsAndInvoices() {
  Appointment a=booking();Invoice invoice=checkout.checkout(a.getId(),"dhanu","cash");
  MedicalRecord record=new MedicalRecord();record.setAppointment(a);record.setPatient(patient());record.setDiagnosis("Historical diagnosis");records.saveAndFlush(record);
  accounts.deleteUser(a.getDoctor().getUser());
  assertFalse(users.findById(a.getDoctor().getUser().getId()).orElseThrow().isActive());
  assertTrue(appointments.existsById(a.getId()));assertTrue(invoices.existsById(invoice.getId()));assertTrue(records.existsById(record.getId()));
 }
 @Test void futureVisitCannotBeCheckedInOrCompleted() throws Exception {
  Appointment a=booking();assertThrows(IllegalArgumentException.class,()->visits.checkIn(a.getId()));
  mvc.perform(post("/doctor/prescribe/"+a.getId()).with(csrf()).with(user(a.getDoctor().getUser().getUsername()).roles("DOCTOR"))
    .param("diagnosis","Diagnosis").param("medication","Medicine").param("dosage","Once").param("duration","1 day").param("price","0"))
    .andExpect(redirectedUrl("/doctor/appointments?error=already_processed"));
  assertEquals("SCHEDULED",a.getStatus());assertTrue(records.findByAppointment(a).isEmpty());
 }
 @Test void adminCannotPublishDoctorAtTwoHospitalsAtTheSameTime() throws Exception {
  Doctor doctor=doctors.findAll().get(0);var existing=slots.findByDoctor(doctor).get(0);
  Long other=branches.findAll().stream().filter(b->!b.getId().equals(existing.getHospitalBranch().getId())).findFirst().orElseThrow().getId();
  long before=slots.count();
  mvc.perform(post("/admin/doctors/"+doctor.getId()+"/update").with(csrf()).with(user("admin").roles("ADMIN")).param("branchId",other.toString()))
    .andExpect(redirectedUrl("/admin/doctors?error"));
  assertEquals(before,slots.count());
 }
 @Test void samePatientCannotOverlapDifferentDoctorsButAnotherRelativeCan() {
  LocalDate day=LocalDate.now().plusDays(45);var ds=doctors.findAll();
  bookings.book(patient(),draft(ds.get(0),day,null));
  assertThrows(IllegalArgumentException.class,()->bookings.book(patient(),draft(ds.get(1),day,null)));
  assertNotNull(bookings.book(patient(),draft(ds.get(1),day,"197012345678")).getId());
 }
 @Test void familyIdentityConflictsAcrossDifferentAccountOwners() {
  LocalDate day=LocalDate.now().plusDays(45);var ds=doctors.findAll();
  bookings.book(patient(),draft(ds.get(0),day,"197012345678"));
  assertThrows(IllegalArgumentException.class,()->bookings.book(users.findByUsername("pavi").orElseThrow(),draft(ds.get(1),day,"197012345678")));
 }
 @Test void adjacentVisitsAreAllowedAndRescheduleExcludesItself() {
  Appointment a=booking();Appointment change=draft(a.getDoctor(),a.getAppointmentDate().toLocalDate(),null);
  change.setId(a.getId());change.setAppointmentDate(a.getAppointmentDate().plusHours(1));
  assertEquals(a.getId(),bookings.book(patient(),change).getId());
  Appointment adjacent=draft(doctors.findAll().get(1),a.getAppointmentDate().toLocalDate(),null);
  adjacent.setAppointmentDate(a.getAppointmentDate().minusHours(1));
  assertNotNull(bookings.book(patient(),adjacent).getId());
 }
 @Test void registrationCannotInventIdentityOrAcceptPipeNic() throws Exception {
  for(String nic:new String[]{"","991234567|"}) {
   mvc.perform(post("/register").with(csrf()).param("username","invalidnic").param("password","Password@123")
    .param("fullName","Patient").param("email","invalidnic@example.test").param("phoneNo","0771234567").param("address","Colombo").param("nic",nic))
    .andExpect(redirectedUrl("/register?error=invalid_nic"));
   assertTrue(users.findByUsername("invalidnic").isEmpty());
  }
 }
 @Test void passwordPolicyAndEmailUniquenessApplyToAllNewAccounts() throws Exception {
  assertFalse(PasswordPolicy.valid("1234567"));assertFalse(PasswordPolicy.valid("Ã©".repeat(37)));
  assertTrue(PasswordPolicy.valid("Ã©".repeat(36)));assertFalse(PasswordPolicy.valid("        "));
  mvc.perform(post("/register").with(csrf()).param("username","sameemail").param("password","Password@123").param("fullName","Patient")
   .param("email","  "+patient().getEmail().toUpperCase(Locale.ROOT)+"  ").param("phoneNo","0771234567").param("address","Colombo").param("nic","991234567V"))
   .andExpect(redirectedUrl("/register?error=duplicate_email"));
  assertTrue(users.findByUsername("sameemail").isEmpty());
 }
 @Test void monetaryValuesAreExactAndRoundingIsExplicit() {
  assertEquals(new BigDecimal("0.30"),Money.amount(new BigDecimal("0.10")).add(Money.amount(new BigDecimal("0.20"))));
  assertEquals(new BigDecimal("12.35"),Money.amount(new BigDecimal("12.345")));
  Invoice i=new Invoice();i.setAmount(new BigDecimal("123.45"));assertEquals(new BigDecimal("123.45"),i.getAmount());
 }
 @Test void csvQuotesDelimitersAndNeutralizesFormulas() throws Exception {
  User p=patient();p.setFullName("=1+2,\"quoted\"\nline");users.saveAndFlush(p);
  var result=mvc.perform(get("/admin/export").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn();
  assertTrue(result.getResponse().getContentAsString().contains("\"'=1+2,\"\"quoted\"\"\nline\""));
  assertEquals("\"'+94123456789\"",CsvExport.cell("+94123456789"));
 }
 @Test void patientHistoryIsPagedWithoutExposingOtherAccounts() throws Exception {
  for(int n=0;n<25;n++) {
   Appointment a=new Appointment();a.setDoctor(doctors.findAll().get(0));a.setPatient(patient());a.setHospitalBranch(branches.findAll().get(0));
   a.setAppointmentDate(LocalDateTime.now().minusDays(n+1));a.setStatus("CANCELLED");appointments.save(a);
  }
  appointments.flush();
  var page=visits.patientHistory(patient(),0);assertEquals(20,page.getNumberOfElements());assertTrue(page.hasNext());
  assertTrue(page.getContent().stream().allMatch(a->a.getPatient().getId().equals(patient().getId())));
  mvc.perform(get("/patient/dashboard").param("page","1").with(user("dhanu").roles("PATIENT"))).andExpect(status().isOk());
 }
 @Test void refundIsAuditedAndCannotBeUndoneOrDeleted() {
  Appointment a=booking();Invoice i=checkout.checkout(a.getId(),"dhanu","cash");checkout.collectCash(a.getId(),"reception");
  long before=audit.count();billing.changeStatus(i.getId(),"REFUNDED","admin");
  assertEquals("REFUNDED",i.getStatus());assertEquals(before+1,audit.count());
  assertThrows(IllegalArgumentException.class,()->billing.changeStatus(i.getId(),"PAID","admin"));
  assertThrows(IllegalArgumentException.class,()->billing.deleteInvoice(i.getId()));
 }
 @Test void resetLinksAreHashedSingleUseAndDoNotRevealAccountExistence() throws Exception {
  mvc.perform(post("/forgot-password").with(csrf()).param("email",patient().getEmail())).andExpect(redirectedUrl("/forgot-password?requested"));
  Notification n=messages.findAll().stream().filter(x->"CareLink password reset".equals(x.getSubject())).findFirst().orElseThrow();
  String token=n.getMessage().split("token=")[1].split("\\s")[0];PasswordReset reset=resets.findById(patient().getId()).orElseThrow();
  assertNotEquals(token,reset.getTokenHash());assertEquals(64,reset.getTokenHash().length());
  recovery.reset(token,"ChangedPassword@123");assertTrue(encoder.matches("ChangedPassword@123",patient().getPassword()));
  assertThrows(IllegalArgumentException.class,()->recovery.reset(token,"AnotherPassword@123"));
  mvc.perform(post("/forgot-password").with(csrf()).param("email","unknown@example.test")).andExpect(redirectedUrl("/forgot-password?requested"));
 }
 @Test void expiredResetIsRejectedAndRequestsAreThrottled() {
  recovery.request(patient().getEmail());long before=messages.count();recovery.request(patient().getEmail());assertEquals(before,messages.count());
  Notification n=messages.findAll().stream().filter(x->"CareLink password reset".equals(x.getSubject())).findFirst().orElseThrow();
  String token=n.getMessage().split("token=")[1].split("\\s")[0];resets.findById(patient().getId()).orElseThrow().setExpiresAt(LocalDateTime.now().minusMinutes(1));
  assertThrows(IllegalArgumentException.class,()->recovery.reset(token,"ChangedPassword@123"));
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void networkDeliveryReleasesTransactionAndLeaseBlocksAnotherWorker() {
  Notification n=new Notification("outbox@example.test","Test delivery",Notification.NotificationType.SYSTEM,Notification.NotificationStatus.PENDING);
  n.setSubject("Outbox test");messages.saveAndFlush(n);
  java.util.concurrent.atomic.AtomicInteger count=new java.util.concurrent.atomic.AtomicInteger();
  doAnswer(invocation->{
   assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
   count.incrementAndGet();
   delivery.deliver(n.getId()); // second worker sees the committed lease and cannot send again
   return null;
  }).when(transport).sendNotification(anyString(),anyString(),anyString());
  try {
   delivery.deliver(n.getId());var sent=messages.findById(n.getId()).orElseThrow();
   assertEquals(Notification.NotificationStatus.SENT,sent.getStatus());assertEquals(1,count.get());assertNull(sent.getDeliveryToken());
  } finally { messages.deleteById(n.getId()); }
 }
 @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
 void concurrentFamilyBookingsAcrossAccountsReserveOneAttendee() throws Exception {
  var ds=doctors.findAll();LocalDate day=LocalDate.now().plusDays(51);
  Appointment first=draft(ds.get(0),day,"196012345678"),second=draft(ds.get(1),day,"196012345678");
  User owner1=users.findByUsername("dhanu").orElseThrow(),owner2=users.findByUsername("pavi").orElseThrow();
  // Precreate the identity lock so this case isolates conflict serialization.
  var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
  tx.executeWithoutResult(s->attendeeLocks.acquire("nic:196012345678"));
  ExecutorService workers=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
  try {
   Callable<Appointment> one=()->{start.await();try{return bookings.book(owner1,first);}catch(IllegalArgumentException ex){return null;}};
   Callable<Appointment> two=()->{start.await();try{return bookings.book(owner2,second);}catch(IllegalArgumentException ex){return null;}};
   Future<Appointment> a=workers.submit(one),b=workers.submit(two);start.countDown();
   Appointment ar=a.get(30,TimeUnit.SECONDS),br=b.get(30,TimeUnit.SECONDS);
   assertEquals(1,(ar==null?0:1)+(br==null?0:1));
   if(ar!=null) visits.cancel(ar.getId());if(br!=null) visits.cancel(br.getId());
  } finally {workers.shutdownNow();}
 }
 @Test void productionRejectsUnsafeDatabaseAndNonHttpsRecovery() {
  var env=new org.springframework.mock.env.MockEnvironment().withProperty("spring.datasource.url","jdbc:sqlserver://db;encrypt=true;trustServerCertificate=true")
   .withProperty("spring.datasource.username","carelink").withProperty("app.recovery.base-url","https://carelink.example");
  assertThrows(IllegalStateException.class,()->new com.echanneling.config.ProductionConfigGuard(env));
  env.withProperty("spring.datasource.url","jdbc:sqlserver://db;encrypt=true;trustServerCertificate=false");
  assertDoesNotThrow(()->new com.echanneling.config.ProductionConfigGuard(env));
  env.withProperty("spring.datasource.username","sa");assertThrows(IllegalStateException.class,()->new com.echanneling.config.ProductionConfigGuard(env));
  env.withProperty("spring.datasource.username","carelink").withProperty("app.recovery.base-url","http://carelink.example");
  assertThrows(IllegalStateException.class,()->new com.echanneling.config.ProductionConfigGuard(env));
 }
 @Test void decimalAmountsSurviveDatabaseRoundTrip() {
  Appointment a=booking();Invoice i=checkout.checkout(a.getId(),"dhanu","cash");i.setAmount(new BigDecimal("123.45"));invoices.saveAndFlush(i);
  entityManager.clear();assertEquals(new BigDecimal("123.45"),invoices.findById(i.getId()).orElseThrow().getAmount());
 }
 @Test void disabledCardSimulationCannotCreatePaidInvoices() {
  Appointment a=booking();
  org.springframework.test.util.ReflectionTestUtils.setField(checkout,"demoCards",false);
  try { assertThrows(IllegalArgumentException.class,()->checkout.checkout(a.getId(),"dhanu","card"));assertTrue(invoices.findByAppointment(a).isEmpty()); }
  finally { org.springframework.test.util.ReflectionTestUtils.setField(checkout,"demoCards",true); }
 }
 @Test void deletedAccountsCannotContinueUsingOldSessions() throws Exception {
  mvc.perform(get("/patient/dashboard").with(user("missing-account").roles("PATIENT"))).andExpect(redirectedUrl("/login?inactive"));
 }
 @Autowired jakarta.persistence.EntityManager entityManager;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
 @Autowired AttendeeLockService attendeeLocks;
}
