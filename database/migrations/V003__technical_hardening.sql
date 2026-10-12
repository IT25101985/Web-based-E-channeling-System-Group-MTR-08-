-- Apply once using a migration account, before deploying the updated application.
-- Existing V001/V002 equivalents: finalization-sqlserver.sql and review-fixes-sqlserver.sql.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.app_schema_migrations','U') IS NULL
 CREATE TABLE dbo.app_schema_migrations (version int PRIMARY KEY, applied_at datetime2 NOT NULL DEFAULT SYSUTCDATETIME());
IF NOT EXISTS (SELECT 1 FROM dbo.app_schema_migrations WHERE version=3)
BEGIN
 IF EXISTS (SELECT LOWER(LTRIM(RTRIM(email))) FROM dbo.users GROUP BY LOWER(LTRIM(RTRIM(email))) HAVING COUNT(*)>1)
  THROW 51000, 'Duplicate account emails require administrator review. No identity data has been discarded.', 1;
 UPDATE dbo.users SET email=LOWER(LTRIM(RTRIM(email)));
 IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.users') AND name='ux_users_email')
  CREATE UNIQUE INDEX ux_users_email ON dbo.users(email);
 IF COL_LENGTH('dbo.appointment','hidden_by_patient') IS NULL
  ALTER TABLE dbo.appointment ADD hidden_by_patient bit NULL;
 EXEC sp_executesql N'UPDATE dbo.appointment SET hidden_by_patient=0 WHERE hidden_by_patient IS NULL;';
 ALTER TABLE dbo.doctor ALTER COLUMN consultation_fee decimal(19,2) NULL;
 ALTER TABLE dbo.appointment ALTER COLUMN booked_consultation_fee decimal(19,2) NULL;
 ALTER TABLE dbo.invoice ALTER COLUMN amount decimal(19,2) NULL;
 ALTER TABLE dbo.prescription ALTER COLUMN price decimal(19,2) NULL;
 IF COL_LENGTH('dbo.notifications','delivery_token') IS NULL ALTER TABLE dbo.notifications ADD delivery_token nvarchar(255) NULL;
 IF COL_LENGTH('dbo.notifications','delivery_lease_until') IS NULL ALTER TABLE dbo.notifications ADD delivery_lease_until datetime2(6) NULL;
 IF OBJECT_ID('dbo.attendee_lock','U') IS NULL CREATE TABLE dbo.attendee_lock (id varchar(64) PRIMARY KEY);
 IF OBJECT_ID('dbo.password_reset','U') IS NULL CREATE TABLE dbo.password_reset (
  user_id bigint PRIMARY KEY, token_hash varchar(64) UNIQUE,
  expires_at datetime2(6), requested_at datetime2(6), used_at datetime2(6));
 IF OBJECT_ID('dbo.invoice_audit','U') IS NULL CREATE TABLE dbo.invoice_audit (
  id bigint IDENTITY PRIMARY KEY, invoice_id bigint NOT NULL REFERENCES dbo.invoice(id),
  old_status nvarchar(255), new_status nvarchar(255), changed_by nvarchar(255), changed_at datetime2(6));
 IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.appointment') AND name='ix_appointment_doctor_time')
  CREATE INDEX ix_appointment_doctor_time ON dbo.appointment(doctor_id,appointment_date) INCLUDE(status);
 IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.appointment') AND name='ix_appointment_patient_time')
  EXEC sp_executesql N'CREATE INDEX ix_appointment_patient_time ON dbo.appointment(patient_id,appointment_date) INCLUDE(status,hidden_by_patient);';
 IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.appointment') AND name='ix_appointment_recipient_time')
  CREATE INDEX ix_appointment_recipient_time ON dbo.appointment(recipient_nic,appointment_date) INCLUDE(status);
 INSERT INTO dbo.app_schema_migrations(version) VALUES(3);
END;
COMMIT;
