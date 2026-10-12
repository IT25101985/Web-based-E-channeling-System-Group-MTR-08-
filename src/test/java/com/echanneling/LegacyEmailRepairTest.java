package com.echanneling;

import com.echanneling.config.LegacyEmailRepair;
import com.echanneling.entity.User;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyEmailRepairTest {
    @Test void preservesDuplicatesAndReservesTheirEmailAcrossRestarts() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:legacy_email_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE users(id bigint PRIMARY KEY, email varchar(254), email_uniqueness_scope bigint NOT NULL DEFAULT 0)");
        jdbc.update("INSERT INTO users(id,email) VALUES(1,' Shared@Example.test '),(2,'shared@example.test'),(3,'unique@example.test')");
        assertEquals(1, LegacyEmailRepair.repair(jdbc));
        assertEquals(1, LegacyEmailRepair.repair(jdbc));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email='shared@example.test'", Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO users(id,email) VALUES(4,'shared@example.test')"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO users(id,email) VALUES(5,'unique@example.test')"));
        jdbc.update("INSERT INTO users(id,email) VALUES(6,'new@example.test')");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO users(id,email) VALUES(7,'new@example.test')"));
    }

    @Test void ambiguousEmailNeverSelectsAnAccountOrIssuesReset() {
        UserRepository users = mock(UserRepository.class);
        User first = new User(); first.setId(1L);
        User second = new User(); second.setId(2L);
        when(users.findAllByEmailIgnoreCase("shared@example.test")).thenReturn(List.of(first,second));
        when(users.findByEmailIgnoreCase(anyString())).thenCallRealMethod();
        assertTrue(users.findByEmailIgnoreCase("shared@example.test").isEmpty());
        PasswordResetRepository resets = mock(PasswordResetRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        PasswordRecoveryService recovery = new PasswordRecoveryService(users,resets,notifications,mock(UserService.class));
        ReflectionTestUtils.setField(recovery,"mailEnabled",true);
        recovery.request("shared@example.test");
        verifyNoInteractions(resets,notifications);
        when(users.findAllByEmailIgnoreCase("unique@example.test")).thenReturn(List.of(first));
        assertEquals(first,users.findByEmailIgnoreCase("unique@example.test").orElseThrow());
    }
}
