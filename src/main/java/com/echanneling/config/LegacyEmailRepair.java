package com.echanneling.config;

import org.springframework.jdbc.core.JdbcTemplate;

/** Keeps imported identities intact while enforcing email uniqueness for new accounts. */
public final class LegacyEmailRepair {
    private LegacyEmailRepair() {}

    public static int repair(JdbcTemplate jdbc) {
        var duplicates = jdbc.queryForList("""
            SELECT LOWER(LTRIM(RTRIM(email))) FROM users WHERE email IS NOT NULL
            GROUP BY LOWER(LTRIM(RTRIM(email))) HAVING COUNT(*) > 1
            """, String.class);
        for (String email : duplicates) {
            var ids = jdbc.queryForList("""
                SELECT id FROM users WHERE LOWER(LTRIM(RTRIM(email))) = ? ORDER BY id
                """, Long.class, email);
            // Keep one scope-0 reservation so even direct new inserts cannot reuse this email.
            // The other IDs only grandfather existing rows; no account is chosen as owner.
            for (Long id : ids.subList(1, ids.size()))
                jdbc.update("UPDATE users SET email_uniqueness_scope = ? WHERE id = ?", id, id);
        }
        jdbc.update("UPDATE users SET email = LOWER(LTRIM(RTRIM(email))) WHERE email IS NOT NULL");
        Boolean indexed = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection -> {
            try (var indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null,
                    connection.getMetaData().storesUpperCaseIdentifiers() ? "USERS" : "users", true, false)) {
                while (indexes.next())
                    if ("ux_users_email_scope".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            }
            return false;
        });
        if (!Boolean.TRUE.equals(indexed))
            jdbc.execute("CREATE UNIQUE INDEX ux_users_email_scope ON users(email, email_uniqueness_scope)");
        return duplicates.size();
    }
}
