package com.echanneling;

import com.echanneling.controller.DddVivaController;
import com.echanneling.service.DddVivaService;
import com.echanneling.config.SecurityConfig;
import com.echanneling.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(DddVivaController.class)
@Import(SecurityConfig.class)
class DddVivaControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean DddVivaService service;
 @MockitoBean UserRepository users;
 DddVivaService.Member member;
 @BeforeEach void setup() {
  when(users.findByUsername(anyString())).thenAnswer(invocation -> {
   var user=new com.echanneling.entity.User();user.setUsername(invocation.getArgument(0));user.setActive(true);return Optional.of(user);
  });
  member=new DddVivaService.Member("IT25101985","Dhanukshan S.","Appointment","Appointment management",List.of("Appointment"),
   List.of(List.of("SELECT","Scheduled appointments","SELECT AppointmentID FROM dbo.Appointment;")),"procedure","EXEC dbo.usp_Viva_DoctorAppointments 6,'20260901','20260930';","trigger","valid","invalid",51043,"Completed visits cannot reopen.","IT25101985_Appointment.sql");
  when(service.member("IT25101985")).thenReturn(member);
  when(service.members()).thenReturn(List.of(member));
  when(service.sql(member,1)).thenReturn("SELECT AppointmentID FROM dbo.Appointment;");
  when(service.connection()).thenReturn(Map.of("DatabaseName","IT2140_MTR08_Part02_20260927","ServerName","SQLEXPRESS"));
  when(service.execute(member,1)).thenReturn(new DddVivaService.Result(List.of("AppointmentID"),List.of(List.of(6))));
 }
 @Test void administratorSeesNamedLiveDatabaseAndResults() throws Exception {
  mvc.perform(get("/admin/ddd-viva").with(user("admin").roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().string(containsString("IT2140_MTR08_Part02_20260927")))
   .andExpect(content().string(containsString("AppointmentID")));
  verify(service).execute(member,1);
 }
 @Test void patientCannotReadAssignmentData() throws Exception {
  mvc.perform(get("/admin/ddd-viva").with(user("patient").roles("PATIENT"))).andExpect(status().isForbidden());
  verifyNoInteractions(service);
 }
 @Test void anonymousUserMustSignIn() throws Exception {
  mvc.perform(get("/admin/ddd-viva")).andExpect(status().is3xxRedirection());
  verifyNoInteractions(service);
 }
 @Test void missingDatabaseShowsActionableMessage() throws Exception {
  when(service.connection()).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("sensitive connection text"));
  mvc.perform(get("/admin/ddd-viva").with(user("admin").roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().string(containsString("Cannot load the assignment database")))
   .andExpect(content().string(org.hamcrest.Matchers.not(containsString("sensitive connection text"))));
 }
 @Test void invalidStudentIsRejectedBeforeDatabaseAccess() throws Exception {
  when(service.member("invalid")).thenThrow(new IllegalArgumentException("Choose a listed student."));
  mvc.perform(get("/admin/ddd-viva?student=invalid").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
  verify(service,never()).connection();
 }
 @Test void downloadContainsAllSixParts() throws Exception {
  mvc.perform(get("/admin/ddd-viva/sql/IT25101985").with(user("admin").roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().string(containsString("PART 1:")))
   .andExpect(content().string(containsString("PART 6:")));
 }
}
