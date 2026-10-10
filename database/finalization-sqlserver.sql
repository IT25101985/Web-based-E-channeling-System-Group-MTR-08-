-- Additive migration for the existing CareLink SQL Server database.
-- Back up the database before applying. Historical reviews keep NULL appointment_id.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.users','active') IS NULL ALTER TABLE dbo.users ADD active bit NULL;
IF COL_LENGTH('dbo.feedback','appointment_id') IS NULL ALTER TABLE dbo.feedback ADD appointment_id bigint NULL;
IF COL_LENGTH('dbo.invoice','payment_method') IS NULL ALTER TABLE dbo.invoice ADD payment_method nvarchar(255) NULL;
IF COL_LENGTH('dbo.invoice','collected_by') IS NULL ALTER TABLE dbo.invoice ADD collected_by nvarchar(255) NULL;
IF COL_LENGTH('dbo.invoice','collected_at') IS NULL ALTER TABLE dbo.invoice ADD collected_at datetime2(6) NULL;
COMMIT TRANSACTION;
