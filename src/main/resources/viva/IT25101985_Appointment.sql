-- IT25101985 | Dhanukshan S.
-- MY FEATURE: Appointment management
-- MY TABLES: Appointment, Patient, DoctorBranch
-- Six complete parts. Shared dependencies are included for standalone setup.
-- SSMS: select one query/demo and press F5. Prepare DDL/DML before the 3-minute slot.
-- Do not connect the main JPA app to this schema by changing only databaseName.

/*
PART 1: EER DIAGRAM & DATABASE DESIGN
Open EER_Original.pdf in a separate tab and Schema_and_Viva_Guide.pdf beside SSMS.
Users(UserID PK, UserType, FirstName, LastName, Email UNIQUE, PasswordHash)
Patient(UserID PK, UserType, NIC UNIQUE, EmergencyContact)
Doctor(UserID PK, UserType, Specialization, LicenseNo UNIQUE, ConsultationFee)
Receptionist(UserID PK, UserType, WorkShift, CounterNo)
SystemAdmin(UserID PK, UserType, AdminLevel)
Each subtype (UserID,UserType) FK -> Users(UserID,UserType).
PatientContact(PatientID PK/FK -> Patient.UserID, ContactNumber PK)
HospitalBranch(BranchID PK, BranchName UNIQUE, Location)
DoctorBranch(DoctorID PK/FK -> Doctor.UserID, BranchID PK/FK -> HospitalBranch)
Appointment(AppointmentID PK, PatientID FK -> Patient.UserID,
 DoctorID + BranchID composite FK -> DoctorBranch, AppointmentAt, Status)
Invoice(InvoiceID PK, AppointmentID UNIQUE/FK -> Appointment,
 ReceptionistID FK -> Receptionist.UserID, Amount, Status)
Feedback(FeedbackID PK, AppointmentID + PatientID composite FK -> Appointment,
 Rating, Review)
Notification(NotificationID PK, UserID FK -> Users, AppointmentID FK -> Appointment,
 Title, Message, NotificationType, Status, SentAt, ReadAt)
ISA: one supertype table + four subtype tables; shared UserID is the subtype PK.
Disjointness: a constant subtype CHECK + composite FK enforces the declared role.
Total participation is NOT enforced by these FKs; use the completeness check.
The original EER uses a single line above d (partial specialization).
Name is split into atomic FirstName/LastName. Contact_Numbers becomes a table.
Doctor-Branch M:N becomes DoctorBranch. Date+Time becomes AppointmentAt.
Bill/Payment is named Invoice; its UNIQUE appointment FK implements at most one
invoice per appointment, NOT mandatory invoice existence on every appointment.
Notification is an added entity for the notification feature.
Filtered unique indexes prevent identical active doctor/patient start times;
they do NOT prevent arbitrary overlapping durations (no duration in this schema).
This DDD schema differs from the SE application's JPA physical schema.
*/

-- PART 2: TABLE CREATION (shared schema; highlight MY TABLES above)
USE master;
GO
IF DB_ID(N'IT2140_MTR08_Part02_20260927') IS NULL
    EXEC(N'CREATE DATABASE [IT2140_MTR08_Part02_20260927]');
GO
USE [IT2140_MTR08_Part02_20260927];
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Users (
  UserID int NOT NULL PRIMARY KEY,
  UserType varchar(12) NOT NULL,
  FirstName nvarchar(40) NOT NULL,
  LastName nvarchar(40) NOT NULL,
  Email varchar(100) NOT NULL UNIQUE,
  PasswordHash varchar(128) NOT NULL,
  CONSTRAINT CK_UserType CHECK (UserType IN
    ('PATIENT','DOCTOR','RECEPTIONIST','ADMIN')),
  CONSTRAINT UQ_UserType UNIQUE (UserID, UserType)
);
END;
GO

IF OBJECT_ID(N'dbo.Patient', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Patient (
  UserID int NOT NULL PRIMARY KEY,
  UserType varchar(12) NOT NULL DEFAULT 'PATIENT'
    CHECK (UserType = 'PATIENT'),
  NIC varchar(12) NOT NULL UNIQUE,
  EmergencyContact varchar(20) NOT NULL,
  CONSTRAINT FK_Patient_Users
    FOREIGN KEY (UserID, UserType)
    REFERENCES dbo.Users (UserID, UserType)
);
END;
GO

IF OBJECT_ID(N'dbo.Doctor', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Doctor (
  UserID int NOT NULL PRIMARY KEY,
  UserType varchar(12) NOT NULL DEFAULT 'DOCTOR'
    CHECK (UserType = 'DOCTOR'),
  Specialization nvarchar(60) NOT NULL,
  LicenseNo varchar(20) NOT NULL UNIQUE,
  ConsultationFee decimal(10,2) NOT NULL
    CHECK (ConsultationFee >= 0),
  CONSTRAINT FK_Doctor_Users
    FOREIGN KEY (UserID, UserType)
    REFERENCES dbo.Users (UserID, UserType)
);
END;
GO

IF OBJECT_ID(N'dbo.Receptionist', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Receptionist (
  UserID int NOT NULL PRIMARY KEY,
  UserType varchar(12) NOT NULL DEFAULT 'RECEPTIONIST'
    CHECK (UserType = 'RECEPTIONIST'),
  WorkShift varchar(10) NOT NULL
    CHECK (WorkShift IN ('MORNING','EVENING')),
  CounterNo int NOT NULL CHECK (CounterNo > 0),
  CONSTRAINT FK_Receptionist_Users
    FOREIGN KEY (UserID, UserType)
    REFERENCES dbo.Users (UserID, UserType)
);
END;
GO

IF OBJECT_ID(N'dbo.SystemAdmin', N'U') IS NULL
BEGIN
CREATE TABLE dbo.SystemAdmin (
  UserID int NOT NULL PRIMARY KEY,
  UserType varchar(12) NOT NULL DEFAULT 'ADMIN'
    CHECK (UserType = 'ADMIN'),
  AdminLevel int NOT NULL CHECK (AdminLevel BETWEEN 1 AND 3),
  CONSTRAINT FK_SystemAdmin_Users
    FOREIGN KEY (UserID, UserType)
    REFERENCES dbo.Users (UserID, UserType)
);
END;
GO

IF OBJECT_ID(N'dbo.PatientContact', N'U') IS NULL
BEGIN
CREATE TABLE dbo.PatientContact (
  PatientID int NOT NULL,
  ContactNumber varchar(20) NOT NULL,
  PRIMARY KEY (PatientID, ContactNumber),
  FOREIGN KEY (PatientID) REFERENCES dbo.Patient(UserID)
);
END;
GO

IF OBJECT_ID(N'dbo.HospitalBranch', N'U') IS NULL
BEGIN
CREATE TABLE dbo.HospitalBranch (
  BranchID int NOT NULL PRIMARY KEY,
  BranchName nvarchar(80) NOT NULL UNIQUE,
  Location nvarchar(100) NOT NULL
);
END;
GO

IF OBJECT_ID(N'dbo.DoctorBranch', N'U') IS NULL
BEGIN
CREATE TABLE dbo.DoctorBranch (
  DoctorID int NOT NULL,
  BranchID int NOT NULL,
  PRIMARY KEY (DoctorID, BranchID),
  FOREIGN KEY (DoctorID) REFERENCES dbo.Doctor(UserID),
  FOREIGN KEY (BranchID)
    REFERENCES dbo.HospitalBranch(BranchID)
);
END;
GO

IF OBJECT_ID(N'dbo.Appointment', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Appointment (
  AppointmentID int NOT NULL PRIMARY KEY,
  PatientID int NOT NULL,
  DoctorID int NOT NULL,
  BranchID int NOT NULL,
  AppointmentAt datetime2(0) NOT NULL,
  Status varchar(12) NOT NULL DEFAULT 'SCHEDULED'
    CHECK (Status IN ('SCHEDULED','COMPLETED','CANCELLED')),
  FOREIGN KEY (PatientID) REFERENCES dbo.Patient(UserID),
  FOREIGN KEY (DoctorID, BranchID)
    REFERENCES dbo.DoctorBranch(DoctorID, BranchID),
  CONSTRAINT UQ_AppointmentPatient
    UNIQUE (AppointmentID, PatientID)
);
END;
GO

IF OBJECT_ID(N'dbo.Invoice', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Invoice (
  InvoiceID int NOT NULL PRIMARY KEY,
  AppointmentID int NOT NULL UNIQUE,
  ReceptionistID int NOT NULL,
  Amount decimal(10,2) NOT NULL CHECK (Amount >= 0),
  Status varchar(10) NOT NULL DEFAULT 'UNPAID'
    CHECK (Status IN ('PAID','UNPAID','REFUNDED')),
  FOREIGN KEY (AppointmentID)
    REFERENCES dbo.Appointment(AppointmentID),
  FOREIGN KEY (ReceptionistID)
    REFERENCES dbo.Receptionist(UserID)
);
END;
GO

IF OBJECT_ID(N'dbo.Feedback', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Feedback (
  FeedbackID int NOT NULL PRIMARY KEY,
  AppointmentID int NOT NULL,
  PatientID int NOT NULL,
  Rating tinyint NOT NULL CHECK (Rating BETWEEN 1 AND 5),
  Review nvarchar(200) NOT NULL,
  FOREIGN KEY (AppointmentID, PatientID)
    REFERENCES dbo.Appointment(AppointmentID, PatientID)
);
END;
GO

IF OBJECT_ID(N'dbo.Notification', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Notification (
  NotificationID int NOT NULL PRIMARY KEY,
  UserID int NOT NULL,
  AppointmentID int NOT NULL,
  Title nvarchar(80) NOT NULL,
  Message nvarchar(250) NOT NULL,
  NotificationType varchar(12) NOT NULL CHECK
    (NotificationType IN ('CONFIRMATION','REMINDER',
                         'CANCELLATION')),
  Status varchar(10) NOT NULL DEFAULT 'PENDING'
    CHECK (Status IN ('PENDING','SENT','FAILED')),
  SentAt datetime2(0) NULL,
  ReadAt datetime2(0) NULL,
  FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
  FOREIGN KEY (AppointmentID)
    REFERENCES dbo.Appointment(AppointmentID),
  CHECK ((Status = 'SENT' AND SentAt IS NOT NULL)
      OR (Status IN ('PENDING','FAILED') AND SentAt IS NULL)),
  CHECK (ReadAt IS NULL OR
    (SentAt IS NOT NULL AND ReadAt >= SentAt))
);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Appointment') AND name = N'UX_Doctor_ActiveTime')
BEGIN
CREATE UNIQUE INDEX UX_Doctor_ActiveTime
  ON dbo.Appointment(DoctorID, AppointmentAt)
  WHERE Status <> 'CANCELLED';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Appointment') AND name = N'UX_Patient_ActiveTime')
BEGIN
CREATE UNIQUE INDEX UX_Patient_ActiveTime
  ON dbo.Appointment(PatientID, AppointmentAt)
  WHERE Status <> 'CANCELLED';
END;
GO


-- PART 3: SAMPLE DATA (shared dependencies; minimum 5 rows per table)
IF NOT EXISTS (SELECT 1 FROM dbo.Users)
BEGIN
INSERT INTO dbo.Users (
  UserID, UserType, FirstName,
  LastName, Email, PasswordHash)
VALUES
(1, N'PATIENT', N'Kamal',
 N'Perera', N'user01@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(2, N'PATIENT', N'Nimali',
 N'Silva', N'user02@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(3, N'PATIENT', N'Arun',
 N'Kumar', N'user03@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(4, N'PATIENT', N'Fathima',
 N'Ali', N'user04@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(5, N'PATIENT', N'Nuwan',
 N'Fernando', N'user05@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(6, N'DOCTOR', N'John',
 N'Smith', N'user06@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(7, N'DOCTOR', N'Sarah',
 N'Perera', N'user07@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(8, N'DOCTOR', N'Kannan',
 N'Shanmugam', N'user08@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(9, N'DOCTOR', N'Amal',
 N'Fernando', N'user09@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(10, N'DOCTOR', N'Nisha',
 N'Silva', N'user10@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(11, N'RECEPTIONIST', N'Ravi',
 N'Silva', N'user11@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(12, N'RECEPTIONIST', N'Anu',
 N'Perera', N'user12@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(13, N'RECEPTIONIST', N'Lina',
 N'Kumar', N'user13@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(14, N'RECEPTIONIST', N'Mala',
 N'Fernando', N'user14@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(15, N'RECEPTIONIST', N'Saman',
 N'Ali', N'user15@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(16, N'ADMIN', N'Admin', N'One', N'user16@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(17, N'ADMIN', N'Admin', N'Two', N'user17@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(18, N'ADMIN', N'Admin',
 N'Three', N'user18@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(19, N'ADMIN', N'Admin',
 N'Four', N'user19@example.test', N'DEMO_HASH_NOT_FOR_LOGIN'),
(20, N'ADMIN', N'Admin',
 N'Five', N'user20@example.test', N'DEMO_HASH_NOT_FOR_LOGIN');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Patient)
BEGIN
INSERT INTO dbo.Patient (
  UserID, UserType, NIC,
  EmergencyContact)
VALUES
(1, N'PATIENT', N'200000000001', N'0700000101'),
(2, N'PATIENT', N'200000000002', N'0700000102'),
(3, N'PATIENT', N'200000000003', N'0700000103'),
(4, N'PATIENT', N'200000000004', N'0700000104'),
(5, N'PATIENT', N'200000000005', N'0700000105');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Doctor)
BEGIN
INSERT INTO dbo.Doctor (
  UserID, UserType, Specialization,
  LicenseNo, ConsultationFee)
VALUES
(6, N'DOCTOR', N'Cardiology', N'SLMC-DEMO-001', 2500),
(7, N'DOCTOR', N'Pediatrics', N'SLMC-DEMO-002', 2000),
(8, N'DOCTOR', N'Neurology', N'SLMC-DEMO-003', 3000),
(9, N'DOCTOR', N'Dermatology', N'SLMC-DEMO-004', 1800),
(10, N'DOCTOR', N'General Medicine', N'SLMC-DEMO-005', 1500);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Receptionist)
BEGIN
INSERT INTO dbo.Receptionist (
  UserID, UserType, WorkShift,
  CounterNo)
VALUES
(11, N'RECEPTIONIST', N'MORNING', 1),
(12, N'RECEPTIONIST', N'EVENING', 2),
(13, N'RECEPTIONIST', N'MORNING', 3),
(14, N'RECEPTIONIST', N'EVENING', 4),
(15, N'RECEPTIONIST', N'MORNING', 5);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SystemAdmin)
BEGIN
INSERT INTO dbo.SystemAdmin (
  UserID, UserType, AdminLevel)
VALUES
(16, N'ADMIN', 1),
(17, N'ADMIN', 2),
(18, N'ADMIN', 3),
(19, N'ADMIN', 1),
(20, N'ADMIN', 2);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.PatientContact)
BEGIN
INSERT INTO dbo.PatientContact (
  PatientID, ContactNumber)
VALUES
(1, N'0700000001'),
(2, N'0700000002'),
(3, N'0700000003'),
(4, N'0700000004'),
(5, N'0700000005'),
(1, N'0700000021');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.HospitalBranch)
BEGIN
INSERT INTO dbo.HospitalBranch (
  BranchID, BranchName, Location)
VALUES
(1, N'Colombo Branch', N'Colombo'),
(2, N'Kandy Branch', N'Kandy'),
(3, N'Galle Branch', N'Galle'),
(4, N'Jaffna Branch', N'Jaffna'),
(5, N'Matara Branch', N'Matara');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.DoctorBranch)
BEGIN
INSERT INTO dbo.DoctorBranch (
  DoctorID, BranchID)
VALUES
(6, 1),
(7, 2),
(8, 3),
(9, 4),
(10, 5),
(6, 2);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Appointment)
BEGIN
INSERT INTO dbo.Appointment (
  AppointmentID, PatientID, DoctorID,
  BranchID, AppointmentAt, Status)
VALUES
(1, 1, 6, 1, N'2026-09-20T09:00:00', N'COMPLETED'),
(2, 2, 7, 2, N'2026-09-21T09:00:00', N'COMPLETED'),
(3, 3, 8, 3, N'2026-09-22T09:00:00', N'COMPLETED'),
(4, 4, 9, 4, N'2026-09-23T09:00:00', N'COMPLETED'),
(5, 5, 10, 5, N'2026-09-24T09:00:00', N'COMPLETED'),
(6, 2, 6, 2, N'2026-09-28T10:00:00', N'SCHEDULED'),
(7, 3, 7, 2, N'2026-09-28T11:00:00', N'SCHEDULED'),
(8, 4, 8, 3, N'2026-09-29T09:00:00', N'CANCELLED'),
(9, 5, 9, 4, N'2026-09-29T10:00:00', N'SCHEDULED'),
(10, 1, 6, 1, N'2026-09-30T09:00:00', N'SCHEDULED');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Invoice)
BEGIN
INSERT INTO dbo.Invoice (
  InvoiceID, AppointmentID, ReceptionistID,
  Amount, Status)
VALUES
(1, 1, 11, 2500, N'PAID'),
(2, 2, 12, 2000, N'PAID'),
(3, 3, 13, 3000, N'PAID'),
(4, 4, 14, 1800, N'PAID'),
(5, 5, 15, 1500, N'PAID'),
(6, 6, 11, 2500, N'UNPAID'),
(7, 7, 12, 2000, N'UNPAID'),
(8, 8, 13, 3000, N'REFUNDED'),
(9, 9, 14, 1800, N'UNPAID'),
(10, 10, 15, 2500, N'UNPAID');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Feedback)
BEGIN
INSERT INTO dbo.Feedback (
  FeedbackID, AppointmentID, PatientID,
  Rating, Review)
VALUES
(1, 1, 1, 5, N'Clear explanation'),
(2, 2, 2, 4, N'Helpful consultation'),
(3, 3, 3, 5, N'Good service'),
(4, 4, 4, 3, N'Long waiting time'),
(5, 5, 5, 4, N'Friendly doctor');
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Notification)
BEGIN
INSERT INTO dbo.Notification (
  NotificationID, UserID, AppointmentID,
  Title, Message, NotificationType,
  Status, SentAt, ReadAt)
VALUES
(1, 1, 1,
 N'Visit confirmation', N'Appointment 1 confirmed.', N'CONFIRMATION',
 N'SENT', N'2026-09-19T08:00:00', NULL),
(2, 2, 2,
 N'Visit confirmation', N'Appointment 2 confirmed.', N'CONFIRMATION',
 N'SENT', N'2026-09-20T08:00:00', NULL),
(3, 3, 3,
 N'Visit confirmation', N'Appointment 3 confirmed.', N'CONFIRMATION',
 N'SENT', N'2026-09-21T08:00:00', NULL),
(4, 4, 4,
 N'Visit confirmation', N'Appointment 4 confirmed.', N'CONFIRMATION',
 N'SENT', N'2026-09-22T08:00:00', NULL),
(5, 5, 5,
 N'Visit confirmation', N'Appointment 5 confirmed.', N'CONFIRMATION',
 N'SENT', N'2026-09-23T08:00:00', NULL);
END;
GO

SELECT * FROM dbo.Appointment;
GO
SELECT * FROM dbo.Patient;
GO
SELECT * FROM dbo.DoctorBranch;
GO

-- PART 4: MY FIVE REQUIRED SQL QUERIES
-- Q1: SELECT | Scheduled appointments
SELECT AppointmentID,PatientID,DoctorID,AppointmentAt FROM dbo.Appointment WHERE Status='SCHEDULED' ORDER BY AppointmentAt,AppointmentID;
GO
-- Q2: JOIN | Readable appointment details
SELECT a.AppointmentID,CONCAT(p.FirstName,' ',p.LastName) AS PatientName,CONCAT(d.FirstName,' ',d.LastName) AS DoctorName,b.BranchName,a.AppointmentAt,a.Status FROM dbo.Appointment a JOIN dbo.Users p ON p.UserID=a.PatientID JOIN dbo.Users d ON d.UserID=a.DoctorID JOIN dbo.HospitalBranch b ON b.BranchID=a.BranchID ORDER BY a.AppointmentID;
GO
-- Q3: Aggregation | Appointment totals
SELECT COUNT(*) AS TotalAppointments,SUM(CASE WHEN Status='COMPLETED' THEN 1 ELSE 0 END) AS CompletedAppointments FROM dbo.Appointment;
GO
-- Q4: GROUP BY / HAVING | Doctors with multiple active visits
SELECT DoctorID,COUNT(*) AS VisitCount FROM dbo.Appointment WHERE Status<>'CANCELLED' GROUP BY DoctorID HAVING COUNT(*)>=2 ORDER BY VisitCount DESC,DoctorID;
GO
-- Q5: Subquery | Appointments with unpaid invoices
SELECT AppointmentID,PatientID,AppointmentAt FROM dbo.Appointment a WHERE EXISTS (SELECT 1 FROM dbo.Invoice i WHERE i.AppointmentID=a.AppointmentID AND i.Status='UNPAID') ORDER BY AppointmentID;
GO

-- PART 5: MY STORED PROCEDURE
CREATE OR ALTER PROCEDURE dbo.usp_Viva_DoctorAppointments
  @DoctorID int, @FromDate date, @ToDate date
AS
BEGIN
  SET NOCOUNT ON;
  IF @DoctorID IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.Doctor WHERE UserID=@DoctorID) THROW 51041,'Doctor does not exist.',1;
  IF @FromDate IS NULL OR @ToDate IS NULL OR @FromDate>@ToDate OR @ToDate='99991231' THROW 51042,'Invalid date range.',1;
  SELECT a.AppointmentID,CONCAT(u.FirstName,' ',u.LastName) AS PatientName,b.BranchName,a.AppointmentAt,a.Status FROM dbo.Appointment a JOIN dbo.Users u ON u.UserID=a.PatientID JOIN dbo.HospitalBranch b ON b.BranchID=a.BranchID WHERE a.DoctorID=@DoctorID AND a.AppointmentAt>=@FromDate AND a.AppointmentAt<DATEADD(day,1,@ToDate) ORDER BY a.AppointmentAt,a.AppointmentID;
END;
GO

-- Sample input and output
EXEC dbo.usp_Viva_DoctorAppointments @DoctorID=6,@FromDate='20260901',@ToDate='20260930';
GO

-- PART 6: MY TRIGGER
-- New viva business rule: COMPLETED is a terminal appointment state. This also protects existing completed-visit feedback.
CREATE OR ALTER TRIGGER dbo.trg_Viva_AppointmentCompleted
ON dbo.Appointment
AFTER UPDATE
AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS (SELECT 1 FROM inserted i JOIN deleted d ON d.AppointmentID=i.AppointmentID WHERE d.Status='COMPLETED' AND i.Status<>'COMPLETED')
   THROW 51043, 'A completed appointment cannot be reopened or cancelled.', 1;
END;
GO
-- Valid change: show the temporary result, then restore the original rows.
BEGIN TRY
 BEGIN TRANSACTION;
 UPDATE dbo.Appointment SET Status='CANCELLED' WHERE AppointmentID IN (6,7);
SELECT AppointmentID,Status FROM dbo.Appointment WHERE AppointmentID IN (6,7);
 ROLLBACK TRANSACTION;
 SELECT 'Valid multi-row change: PASS (rolled back)' AS TestResult;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
GO
-- Invalid change rejected; EXPECTED error 51043. Changes always roll back.
BEGIN TRY
 BEGIN TRANSACTION;
 UPDATE dbo.Appointment SET Status='CANCELLED' WHERE AppointmentID IN (1,6);
 ROLLBACK TRANSACTION;
 THROW 51999, 'TEST FAILED: invalid change was accepted.', 1;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 IF ERROR_NUMBER()<>51043 THROW;
 SELECT 'Invalid change rejected: PASS' AS TestResult,ERROR_NUMBER() AS ErrorNumber,ERROR_MESSAGE() AS ErrorMessage;
END CATCH;
GO
-- Original data after both demonstrations
SELECT * FROM dbo.Appointment;
GO
