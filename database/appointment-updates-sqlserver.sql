-- Appointment module migration only; apply to the application database, not the viva database.
-- Take a database backup first. No account identities or historical invoices are deleted.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.appointment','booking_for') IS NULL
 ALTER TABLE dbo.appointment ADD booking_for nvarchar(255) NULL;
IF COL_LENGTH('dbo.appointment','recipient_nic') IS NULL
 ALTER TABLE dbo.appointment ADD recipient_nic nvarchar(255) NULL;
IF COL_LENGTH('dbo.appointment','settlement_type') IS NULL
 ALTER TABLE dbo.appointment ADD settlement_type nvarchar(255) NULL;
IF COL_LENGTH('dbo.appointment','booked_consultation_fee') IS NULL
 ALTER TABLE dbo.appointment ADD booked_consultation_fee decimal(19,2) NULL;
IF COL_LENGTH('dbo.appointment','hidden_by_patient') IS NULL
 ALTER TABLE dbo.appointment ADD hidden_by_patient bit NULL;
IF OBJECT_ID('dbo.attendee_lock','U') IS NULL
 CREATE TABLE dbo.attendee_lock (id varchar(64) NOT NULL PRIMARY KEY);
EXEC sp_executesql N'
UPDATE a SET booked_consultation_fee=COALESCE(
 (SELECT MAX(i.amount) FROM dbo.invoice i WHERE i.appointment_id=a.id),d.consultation_fee)
FROM dbo.appointment a LEFT JOIN dbo.doctor d ON d.id=a.doctor_id
WHERE a.booked_consultation_fee IS NULL;
UPDATE dbo.appointment SET hidden_by_patient=0 WHERE hidden_by_patient IS NULL;';
COMMIT TRANSACTION;
-- Existing rows with no recorded family identity require patient verification; never fabricate a NIC.
