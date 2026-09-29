-- ============================================================
-- Web-based E-channeling System
-- SQL Server (T-SQL) Schema
-- ============================================================

IF DB_ID('echanneling_system') IS NULL 
BEGIN
    CREATE DATABASE echanneling_system;
END
GO

USE echanneling_system;
GO

-- ============================================================
-- 1. USER
-- ============================================================
CREATE TABLE [user] (
    user_id         INT IDENTITY(1,1) PRIMARY KEY,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    user_type       VARCHAR(20) NOT NULL CHECK (user_type IN ('PATIENT','DOCTOR','RECEPTIONIST','ADMIN')),
    created_at      DATETIME DEFAULT GETDATE()
);

-- ============================================================
-- 2. HOSPITAL BRANCH
-- ============================================================
CREATE TABLE hospital_branch (
    branch_id   INT IDENTITY(1,1) PRIMARY KEY,
    branch_name VARCHAR(150) NOT NULL,
    location    VARCHAR(200) NOT NULL
);

-- ============================================================
-- 3. Subtype entities
-- ============================================================

-- PATIENT
CREATE TABLE patient (
    user_id             INT PRIMARY KEY,
    nic                 VARCHAR(20) NOT NULL UNIQUE,
    emergency_contact   VARCHAR(100),
    contact_numbers     VARCHAR(100),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES [user](user_id) ON DELETE CASCADE
);

-- DOCTOR
CREATE TABLE doctor (
    user_id         INT PRIMARY KEY,
    specialization  VARCHAR(100) NOT NULL,
    license_no      VARCHAR(50) NOT NULL UNIQUE,
    branch_id       INT NOT NULL,
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES [user](user_id) ON DELETE CASCADE,
    CONSTRAINT fk_doctor_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch(branch_id)
);

-- RECEPTIONIST
CREATE TABLE receptionist (
    user_id     INT PRIMARY KEY,
    work_shift  VARCHAR(50),
    counter_no  VARCHAR(20),
    CONSTRAINT fk_receptionist_user FOREIGN KEY (user_id) REFERENCES [user](user_id) ON DELETE CASCADE
);

-- SYSTEM ADMIN
CREATE TABLE system_admin (
    user_id     INT PRIMARY KEY,
    admin_level VARCHAR(50),
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES [user](user_id) ON DELETE CASCADE
);

-- ============================================================
-- 4. RECEPTIONIST_BRANCH
-- ============================================================
CREATE TABLE receptionist_branch (
    receptionist_id INT NOT NULL,
    branch_id       INT NOT NULL,
    PRIMARY KEY (receptionist_id, branch_id),
    CONSTRAINT fk_rb_receptionist FOREIGN KEY (receptionist_id) REFERENCES receptionist(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_rb_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch(branch_id) ON DELETE CASCADE
);

-- ============================================================
-- 5. APPOINTMENT
-- ============================================================
CREATE TABLE appointment (
    appointment_id  INT IDENTITY(1,1) PRIMARY KEY,
    patient_id      INT NOT NULL,
    doctor_id       INT NOT NULL,
    branch_id       INT NOT NULL,
    appt_date       DATE NOT NULL,
    appt_time       TIME NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED','RESCHEDULED')),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patient(user_id),
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(user_id),
    CONSTRAINT fk_appt_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch(branch_id),
    CONSTRAINT uq_doctor_slot UNIQUE (doctor_id, appt_date, appt_time)
);

-- ============================================================
-- 6. PAYMENT
-- ============================================================
CREATE TABLE payment (
    payment_id      INT IDENTITY(1,1) PRIMARY KEY,
    appointment_id  INT NOT NULL UNIQUE,
    processed_by    INT,
    amount          DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID','REFUNDED','FAILED')),
    created_at      DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_payment_appt FOREIGN KEY (appointment_id) REFERENCES appointment(appointment_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_admin FOREIGN KEY (processed_by) REFERENCES system_admin(user_id)
);

-- ============================================================
-- 7. FEEDBACK
-- ============================================================
CREATE TABLE feedback (
    feedback_id     INT IDENTITY(1,1) PRIMARY KEY,
    patient_id      INT NOT NULL,
    appointment_id  INT NOT NULL UNIQUE,
    rating          TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review          TEXT,
    created_at      DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_feedback_patient FOREIGN KEY (patient_id) REFERENCES patient(user_id),
    CONSTRAINT fk_feedback_appt FOREIGN KEY (appointment_id) REFERENCES appointment(appointment_id) ON DELETE CASCADE
);
