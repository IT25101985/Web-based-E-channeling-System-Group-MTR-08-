package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** FR-01: Patient Management & BCrypt Authentication | TC_PAT_001-010 | Kulasuriya K.A.T.N. IT25101972 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc_pat;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional @TestMethodOrder(MethodOrderer.DisplayName.class)
class TC_PAT_PatientManagementTest {
    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean JavaMailSender mail;

    /** TC_PAT_001 - Valid Patient Registration */
    @Test @DisplayName("TC_PAT_001 - Valid Patient Registration")
    void TC_PAT_001_validPatientRegistration() throws Exception {
        mvc.perform(post("/register").with(csrf())
            .param("username","newpatient01").param("password","Pass@1234")
            .param("fullName","New Patient One").param("email","newpatient01@mail.com")
            .param("address","123 Main Street").param("nic","991234567V").param("phoneNo","0771234567"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
    }

    /** TC_PAT_002 - Duplicate NIC Registration Rejected */
    @Test @DisplayName("TC_PAT_002 - Duplicate NIC Registration Rejected")
    void TC_PAT_002_duplicateNicRegistrationRejected() throws Exception {
        mvc.perform(post("/register").with(csrf())
            .param("username","nic1").param("password","Pass@1234")
            .param("fullName","NIC One").param("email","nic1@mail.com")
            .param("address","10 Lake Rd").param("nic","991234567V").param("phoneNo","0771111111"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/register").with(csrf())
            .param("username","nic2").param("password","Pass@5678")
            .param("fullName","NIC Two").param("email","nic2@mail.com")
            .param("address","20 Hill Rd").param("nic","991234567V").param("phoneNo","0772222222"))
            .andExpect(status().is3xxRedirection());
    }

    /** TC_PAT_003 - Invalid Email Format Validation */
    @Test @DisplayName("TC_PAT_003 - Invalid Email Format Validation")
    void TC_PAT_003_invalidEmailFormatRejected() throws Exception {
        mvc.perform(post("/register").with(csrf())
            .param("username","bademail").param("password","Pass@1234")
            .param("fullName","Bad Email").param("email","invalidemail")
            .param("address","5 Test Lane").param("nic","881111111V").param("phoneNo","0773333333"))
            .andExpect(status().is3xxRedirection());
    }

    /** TC_PAT_004 - Successful Login with BCrypt Password */
    @Test @DisplayName("TC_PAT_004 - Successful Login with BCrypt Password")
    void TC_PAT_004_successfulLoginWithBcryptPassword() throws Exception {
        mvc.perform(post("/login").with(csrf())
            .param("username","dhanu").param("password","patient123"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
    }

    /** TC_PAT_005 - Invalid Login Credentials Rejected */
    @Test @DisplayName("TC_PAT_005 - Invalid Login Credentials Rejected")
    void TC_PAT_005_invalidLoginCredentialsRejected() throws Exception {
        mvc.perform(post("/login").with(csrf())
            .param("username","dhanu").param("password","wrongpassword"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));
    }

    /** TC_PAT_006 - BCrypt Password Encoding Verification */
    @Test @DisplayName("TC_PAT_006 - BCrypt Password Encoding Verification")
    void TC_PAT_006_passwordStoredAsBcryptHash() {
        String pw = userRepository.findByUsername("dhanu").orElseThrow().getPassword();
        assertTrue(pw.startsWith("$2a$") || pw.startsWith("$2b$"), "Must be BCrypt hash, not plaintext");
    }

    /** TC_PAT_007 - Profile Update Success */
    @Test @DisplayName("TC_PAT_007 - Profile Update Success")
    void TC_PAT_007_profileUpdateSuccess() throws Exception {
        mvc.perform(post("/patient/profile").with(csrf()).with(user("dhanu").roles("PATIENT"))
            .param("fullName","Dhanukshan Updated").param("address","456 Updated St").param("phoneNo","0779876543"))
            .andExpect(status().is3xxRedirection());
        assertEquals("Dhanukshan Updated", userRepository.findByUsername("dhanu").orElseThrow().getFullName());
    }

    /** TC_PAT_008 - Unauthenticated Access Blocked */
    @Test @DisplayName("TC_PAT_008 - Unauthenticated Access Blocked")
    void TC_PAT_008_unauthenticatedPatientDashboardBlocked() throws Exception {
        mvc.perform(get("/patient/dashboard"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    /** TC_PAT_009 - Patient Cannot Access Admin Endpoints (RBAC) */
    @Test @DisplayName("TC_PAT_009 - Patient Cannot Access Admin Endpoints (RBAC)")
    void TC_PAT_009_patientCannotAccessAdminEndpoints() throws Exception {
        mvc.perform(get("/admin/dashboard").with(user("dhanu").roles("PATIENT")))
            .andExpect(status().isForbidden());
    }

    /** TC_PAT_010 - Duplicate Username Registration Rejected */
    @Test @DisplayName("TC_PAT_010 - Duplicate Username Registration Rejected")
    void TC_PAT_010_duplicateUsernameRejected() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            User dup = new Patient();
            dup.setUsername("dhanu"); dup.setPassword("TestPass@1");
            dup.setFullName("Dup"); dup.setEmail("dup@mail.com"); dup.setAddress("1 St");
            userService.saveUser(dup);
        });
        assertTrue(ex.getMessage().contains("Username already exists"));
    }
}
