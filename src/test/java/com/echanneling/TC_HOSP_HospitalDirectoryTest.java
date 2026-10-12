package com.echanneling;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** FR-02: Hospital Directory & Room Management | TC_HOSP_001-010 | Pannila P.H.I.U. IT25101975 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tc_hosp;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.seed-demo=true","app.notifications.scheduling-enabled=false","app.mail.enabled=false"})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional @TestMethodOrder(MethodOrderer.DisplayName.class)
class TC_HOSP_HospitalDirectoryTest {
    @Autowired MockMvc mvc;
    @Autowired HospitalBranchService hospitalBranchService;
    @Autowired HospitalBranchRepository hospitalBranchRepository;
    @Autowired AvailabilitySlotRepository slots;
    @MockitoBean JavaMailSender mail;

    /** TC_HOSP_001 - Search Branch by Location */
    @Test @DisplayName("TC_HOSP_001 - Search Branch by Location")
    void TC_HOSP_001_searchBranchByLocation() {
        List<HospitalBranch> result = hospitalBranchService.searchByArea("Colombo");
        assertNotNull(result);
    }

    /** TC_HOSP_002 - Search Doctor by Specialty via API */
    @Test @DisplayName("TC_HOSP_002 - Search Doctor by Specialty via API")
    void TC_HOSP_002_searchDoctorBySpecialty() throws Exception {
        HospitalBranch b = hospitalBranchRepository.findAll().get(0);
        mvc.perform(get("/api/branches/" + b.getId() + "/doctors"))
            .andExpect(status().isOk());
    }

    /** TC_HOSP_003 - No Results for Unknown City */
    @Test @DisplayName("TC_HOSP_003 - No Results for Unknown City")
    void TC_HOSP_003_noResultsForUnknownCity() {
        List<HospitalBranch> result = hospitalBranchService.searchByArea("NonExistentCity12345");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /** TC_HOSP_004 - View All Hospital Branches */
    @Test @DisplayName("TC_HOSP_004 - View All Hospital Branches")
    void TC_HOSP_004_viewAllBranches() {
        assertFalse(hospitalBranchService.getAllBranches().isEmpty());
    }

    /** TC_HOSP_005 - Admin Add New Hospital Branch */
    @Test @DisplayName("TC_HOSP_005 - Admin Add New Hospital Branch")
    void TC_HOSP_005_adminAddNewBranch() throws Exception {
        long before = hospitalBranchRepository.count();
        mvc.perform(post("/admin/branches/add").with(csrf()).with(user("admin").roles("ADMIN"))
            .param("branchName","CareLink Kandy").param("area","Kandy")
            .param("address","50 Temple Road, Kandy").param("contactNumber","0812223344"))
            .andExpect(status().is3xxRedirection());
        assertEquals(before + 1, hospitalBranchRepository.count());
    }

    /** TC_HOSP_006 - Admin Edit Existing Branch */
    @Test @DisplayName("TC_HOSP_006 - Admin Edit Existing Branch")
    void TC_HOSP_006_adminEditExistingBranch() throws Exception {
        HospitalBranch b = hospitalBranchRepository.findAll().get(0);
        mvc.perform(post("/admin/branches/" + b.getId() + "/update").with(csrf()).with(user("admin").roles("ADMIN"))
            .param("branchName", b.getBranchName()).param("area", b.getArea())
            .param("address", b.getAddress()).param("contactNumber","0999999999"))
            .andExpect(status().is3xxRedirection());
        assertEquals("0999999999", hospitalBranchRepository.findById(b.getId()).orElseThrow().getContactNumber());
    }

    /** TC_HOSP_007 - Delete Branch Without Appointments */
    @Test @DisplayName("TC_HOSP_007 - Delete Branch Without Appointments")
    void TC_HOSP_007_deleteBranchWithoutAppointments() {
        HospitalBranch fresh = new HospitalBranch();
        fresh.setBranchName("Temp Branch"); fresh.setArea("Galle");
        fresh.setAddress("1 Beach Road"); fresh.setContactNumber("0912345678");
        fresh = hospitalBranchRepository.save(fresh);
        Long id = fresh.getId();
        hospitalBranchService.deleteBranch(id);
        assertFalse(hospitalBranchRepository.findById(id).isPresent());
    }

    /** TC_HOSP_008 - Delete Branch With Active Sessions Blocked */
    @Test @DisplayName("TC_HOSP_008 - Delete Branch With Active Sessions Blocked")
    void TC_HOSP_008_deleteBranchWithActiveSessionsBlocked() {
        HospitalBranch b = slots.findAll().stream()
            .map(AvailabilitySlot::getHospitalBranch).findFirst().orElse(null);
        if (b != null) {
            Long id = b.getId();
            assertThrows(IllegalArgumentException.class, () -> hospitalBranchService.deleteBranch(id));
        }
    }

    /** TC_HOSP_009 - Fetch Doctors at Branch via REST API */
    @Test @DisplayName("TC_HOSP_009 - Fetch Doctors at Branch via REST API")
    void TC_HOSP_009_fetchDoctorsAtBranchViaApi() throws Exception {
        HospitalBranch b = hospitalBranchRepository.findAll().get(0);
        mvc.perform(get("/api/branches/" + b.getId() + "/doctors")).andExpect(status().isOk());
    }

    /** TC_HOSP_010 - Patient Cannot Access Admin Branch Panel */
    @Test @DisplayName("TC_HOSP_010 - Patient Cannot Access Admin Branch Panel")
    void TC_HOSP_010_patientCannotAccessBranchAdminPanel() throws Exception {
        mvc.perform(get("/admin/branches").with(user("dhanu").roles("PATIENT")))
            .andExpect(status().isForbidden());
    }
}
