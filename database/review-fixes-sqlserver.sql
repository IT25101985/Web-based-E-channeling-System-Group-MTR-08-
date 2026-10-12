-- Additive migration for the application database (echanneling_system).
-- Do not run against the separate DDD viva database.
-- The development ddl-auto=update setting also adds these columns on startup.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.appointment','booking_for') IS NULL
    ALTER TABLE dbo.appointment ADD booking_for nvarchar(255) NULL;
IF COL_LENGTH('dbo.appointment','recipient_nic') IS NULL
    ALTER TABLE dbo.appointment ADD recipient_nic nvarchar(255) NULL;
IF COL_LENGTH('dbo.appointment','settlement_type') IS NULL
    ALTER TABLE dbo.appointment ADD settlement_type nvarchar(255) NULL;
IF COL_LENGTH('dbo.appointment','booked_consultation_fee') IS NULL
    ALTER TABLE dbo.appointment ADD booked_consultation_fee float NULL;
EXEC sp_executesql N'
UPDATE a SET booked_consultation_fee = COALESCE(
    (SELECT MAX(i.amount) FROM dbo.invoice i WHERE i.appointment_id = a.id),
    d.consultation_fee)
FROM dbo.appointment a LEFT JOIN dbo.doctor d ON d.id = a.doctor_id
WHERE a.booked_consultation_fee IS NULL;';
COMMIT TRANSACTION;
-- Status is stored as text by JPA. Any custom installation-specific CHECK constraint
-- must allow SCHEDULED, CHECKED_IN, COMPLETED and CANCELLED before enabling check-in.
