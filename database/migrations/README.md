# Application schema updates

For existing databases, apply database/finalization-sqlserver.sql, database/review-fixes-sqlserver.sql, then V003__technical_hardening.sql with a migration account. V003 records its version and is safe to run again. Take a tested database backup before monetary type conversion. These migrations target the application schema, not the separate IT2140 viva schema.

V003 refuses duplicate normalized emails rather than merging accounts or inventing addresses. Resolve duplicates with their owners before proceeding. Existing dummy NIC/phone values cannot be safely corrected automatically; verify them with the patient.

Production uses ddl-auto=validate and disables startup data repairs. Grant the runtime account only required SELECT/INSERT/UPDATE permissions; schema DDL belongs to the deployment account. Use a trusted SQL Server certificate and a hostname matching it. The app refuses sa and unverified TLS in production.

Example connection: jdbc:sqlserver://db.example.org:1433;databaseName=echanneling_system;encrypt=true;trustServerCertificate=false;

Set CARELINK_DB_URL, CARELINK_DB_USER, CARELINK_DB_PASSWORD and CARELINK_PUBLIC_URL (an HTTPS origin), plus SMTP variables in application-prod.properties, then run the jar with --spring.profiles.active=prod. No real payment provider is configured: production supports counter cash only.

## Legacy shared emails (V004)

V004__legacy_email_compatibility.sql adds a database-managed scope that preserves imported accounts sharing an email. New accounts cannot reuse an existing email, and recovery is blocked for ambiguous addresses. The local schema-update startup performs this email repair automatically. V004 does not replace unrelated V003 schema changes; V003 still refuses shared emails, so resolve those verified addresses before using V003 unchanged. See docs/INTELLIJ_SQL_SERVER_STARTUP.md.
