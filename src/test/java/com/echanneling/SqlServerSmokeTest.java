package com.echanneling;

import com.echanneling.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in read-only page verification against the installed SQL Server database. */
@SpringBootTest(properties="app.seed-demo=false")
@ActiveProfiles("sqlserver")
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named="carelink.sqlserver",matches="true")
class SqlServerSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Test void existingDatabaseRecordsRenderForAllRoles() throws Exception {
        assertFalse(users.findAll().isEmpty(), "The installed database must contain existing accounts");
        mvc.perform(get("/")).andExpect(status().isOk());
        for(String page:new String[]{"dashboard","appointments","doctors","users","branches","invoices","reviews","notifications"})
            mvc.perform(get("/admin/"+page).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        String patient=users.findByRole("ROLE_PATIENT").get(0).getUsername();
        var result=mvc.perform(get("/patient/dashboard").with(user(patient).roles("PATIENT"))).andExpect(status().isOk()).andReturn();
        assertTrue(result.getResponse().getContentAsString().contains("My care"));
        String doctor=users.findByRole("ROLE_DOCTOR").get(0).getUsername();
        for(String page:new String[]{"dashboard","appointments","patients","records","consultations"})
            mvc.perform(get("/doctor/"+page).with(user(doctor).roles("DOCTOR"))).andExpect(status().isOk());
    }
}
