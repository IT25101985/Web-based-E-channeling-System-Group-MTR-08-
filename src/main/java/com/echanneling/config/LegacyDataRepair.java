package com.echanneling.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Preserve legacy records while making account lookup unambiguous. */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.legacy-repair.enabled", havingValue="true", matchIfMissing=true)
@Order(-100)
public class LegacyDataRepair implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(LegacyDataRepair.class);
    private final JdbcTemplate jdbc;
    public LegacyDataRepair(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    @Transactional
    public void run(String... args) {
        int duplicateEmails = LegacyEmailRepair.repair(jdbc);
        if (duplicateEmails > 0)
            log.warn("Preserved {} legacy shared email addresses. Email-based password recovery is blocked for ambiguous addresses; usernames and account records remain available.", duplicateEmails);
        for (String name : jdbc.queryForList("SELECT username FROM users GROUP BY username HAVING COUNT(*) > 1", String.class)) {
            var ids = jdbc.queryForList("SELECT id FROM users WHERE username = ? ORDER BY id", Long.class, name);
            for (Long id : ids.subList(1, ids.size())) {
                String repaired = name.substring(0, Math.min(name.length(), 24)) + "_legacy_" + id;
                while (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, repaired) > 0) repaired += "_";
                jdbc.update("UPDATE users SET username = ? WHERE id = ?", repaired, id);
                log.warn("Duplicate username repaired: record {} now uses {}. All related records were preserved.", id, repaired);
            }
        }
        jdbc.update("""
            UPDATE appointment SET booked_consultation_fee = COALESCE(
                (SELECT MAX(i.amount) FROM invoice i WHERE i.appointment_id = appointment.id),
                (SELECT d.consultation_fee FROM doctor d WHERE d.id = appointment.doctor_id))
            WHERE booked_consultation_fee IS NULL
            """);
        // Old imports used the base discriminator for patient/admin accounts.
        jdbc.update("UPDATE users SET user_type = 'PATIENT' WHERE role = 'ROLE_PATIENT' AND user_type = 'User'");
        jdbc.update("UPDATE users SET user_type = 'ADMIN' WHERE role = 'ROLE_ADMIN' AND user_type = 'User'");
        Boolean indexed = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection -> {
            try (var indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null,
                    connection.getMetaData().storesUpperCaseIdentifiers() ? "USERS" : "users", true, false)) {
                while (indexes.next()) if ("ux_users_username".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
            return false;
        });
        if (!Boolean.TRUE.equals(indexed)) jdbc.execute("CREATE UNIQUE INDEX ux_users_username ON users(username)");

        // Ensure notifications table check constraints permit CANCELLED status and all NotificationType values
        try {
            String dbName = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
            if (dbName != null && dbName.toLowerCase().contains("microsoft")) {
                jdbc.execute("""
                    DECLARE @sql NVARCHAR(MAX) = N'';
                    SELECT @sql += N'ALTER TABLE dbo.notifications DROP CONSTRAINT ' + QUOTENAME(cc.name) + N'; '
                    FROM sys.check_constraints cc
                    JOIN sys.columns col ON cc.parent_object_id = col.object_id AND cc.parent_column_id = col.column_id
                    WHERE cc.parent_object_id = OBJECT_ID('dbo.notifications')
                      AND (
                          (col.name = 'status' AND cc.definition NOT LIKE '%CANCELLED%')
                          OR (col.name = 'type' AND cc.definition NOT LIKE '%RESCHEDULE%')
                      );

                    IF @sql <> N''
                    BEGIN
                        EXEC sp_executesql @sql;
                    END;

                    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.notifications') AND name = 'CK_notifications_status')
                        ALTER TABLE dbo.notifications ADD CONSTRAINT CK_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'));

                    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.notifications') AND name = 'CK_notifications_type')
                        ALTER TABLE dbo.notifications ADD CONSTRAINT CK_notifications_type CHECK (type IN ('CONFIRMATION', 'RESCHEDULE', 'REMINDER', 'CANCELLATION', 'SYSTEM', 'GENERAL'));
                """);
            }
        } catch (Exception ex) {
            log.warn("Notification check constraint check skipped or already configured: {}", ex.getMessage());
        }
    }
}
