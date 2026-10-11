package com.echanneling;
import com.echanneling.entity.*;
import com.echanneling.patterns.ClinicClock;
import com.echanneling.patterns.decorator.*;
import com.echanneling.patterns.observer.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;
class DesignPatternsTest {
 @Test void concurrentCallersShareTheClinicClock() throws Exception {
  var executor=Executors.newFixedThreadPool(4);
  try {
   var tasks=new ArrayList<java.util.concurrent.Future<ClinicClock>>();
   for(int i=0;i<20;i++) tasks.add(executor.submit(ClinicClock::getInstance));
   for(var task:tasks) assertSame(ClinicClock.getInstance(),task.get());
   assertEquals(ZoneId.of("Asia/Colombo"),ClinicClock.getInstance().zone());
  } finally { executor.shutdownNow(); }
 }
 Appointment appointment() {
  User patient=new User();patient.setUsername("owner");patient.setFullName("Account Owner");
  User doctorUser=new User();doctorUser.setEmail("doctor@example.test");
  Doctor doctor=new Doctor();doctor.setName("Dr Test");doctor.setUser(doctorUser);
  Appointment a=new Appointment();a.setId(7L);a.setPatient(patient);a.setPatientName("Family Patient");
  a.setDoctor(doctor);a.setContactEmail("contact@example.test");a.setAppointmentDate(LocalDateTime.of(2026,11,20,10,0));
  return a;
 }
 @Test void observersUseCorrectRecipientsAndRemovalStopsUpdates() {
  var a=appointment();List<String> deliveries=new ArrayList<>();
  NotificationSink sink=(event,user,email,key)->deliveries.add(email+"|"+key);
  AppointmentSubject subject=new AppointmentEventSubject();
  AppointmentObserver patient=new PatientNotificationObserver(sink),doctor=new DoctorNotificationObserver(sink);
  subject.addObserver(patient);subject.addObserver(patient);subject.addObserver(doctor);
  var event=new AppointmentEvent(a,Notification.NotificationType.CONFIRMATION,"event-7");
  subject.notifyObservers(event);
  assertEquals(List.of("contact@example.test|event-7:patient","doctor@example.test|event-7:doctor"),deliveries);
  deliveries.clear();subject.removeObserver(patient);subject.notifyObservers(event);
  assertEquals(List.of("doctor@example.test|event-7:doctor"),deliveries);
  deliveries.clear();new AppointmentEventSubject().notifyObservers(event);assertTrue(deliveries.isEmpty());
 }
 @Test void messageDecoratorsComposeWithoutChangingBaseTemplate() {
  var a=appointment();NotificationMessage base=new TemplateMessage("Hello {name}, {patient} sees {doctor} at {hospital} on {date}, #{reference}.");
  NotificationMessage personalized=new RecipientMessageDecorator(base,a.getPatient());
  NotificationMessage complete=new AppointmentMessageDecorator(personalized,a);
  assertEquals("Hello Account Owner, Family Patient sees Dr Test at your hospital on 20 Nov 2026, 10:00, #7.",complete.render());
  assertTrue(base.render().contains("{name}"));assertTrue(personalized.render().contains("{doctor}"));
 }
}
