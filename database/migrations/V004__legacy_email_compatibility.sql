-- Apply before starting a validate-only deployment of the legacy-email-compatible app.
-- Preserves accounts and emails; no account is treated as owner of a shared email.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.users','email_uniqueness_scope') IS NULL
 ALTER TABLE dbo.users ADD email_uniqueness_scope bigint NOT NULL CONSTRAINT DF_users_email_scope DEFAULT 0;
EXEC sp_executesql N'
 UPDATE u SET email_uniqueness_scope=u.id
 FROM dbo.users u
 WHERE EXISTS (SELECT 1 FROM dbo.users earlier WHERE earlier.id < u.id
   AND LOWER(LTRIM(RTRIM(earlier.email)))=LOWER(LTRIM(RTRIM(u.email))));
 UPDATE dbo.users SET email=LOWER(LTRIM(RTRIM(email))) WHERE email IS NOT NULL;
 IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.users'') AND name=''ux_users_email_scope'')
  CREATE UNIQUE INDEX ux_users_email_scope ON dbo.users(email,email_uniqueness_scope);';
COMMIT;
