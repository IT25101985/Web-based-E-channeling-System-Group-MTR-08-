# E-Channeling System

A professional, feature-rich web-based e-channeling management portal built with Spring Boot, designed to streamline doctor channeling operations for admins, doctors, receptionists, and patients.

## 🚀 Key Features
- **Modern UI**: Glassmorphic dark-mode interface using Tailwind CSS.
- **Role-Based Access**: Specialized dashboards for Admins, Doctors, and Patients.
- **E-Channeling Management**: Real-time appointment booking, rescheduling, and status tracking.
- **Clinical Records**: Digital prescriptions and medical history management with PDF generation.
- **Financial Module**: Automated invoice generation and payment tracking.
- **Feedback System**: Patient reviews and doctor rating analytics.

## 🛠️ Technology Stack
- **Backend**: Java 17, Spring Boot 3, Spring Security, Spring Data JPA.
- **Database**: SQL Server / MySQL (`echanneling_system`).
- **Frontend**: Thymeleaf, Tailwind CSS, FontAwesome.
- **Utilities**: PDFBox (for records), Twilio/JavaMail (for notifications).

## 👥 Module Distribution & Team

| Member (ID & Name) | Module Name | Backend Classes (Java) | Frontend Templates (HTML) |
| :--- | :--- | :--- | :--- |
| **IT25101975**<br>Pannila P.L.K.S | **User & Hospital Management** | `User`, `Admin`, `UserService`, `AdminController`, `AuthController`, `UserRepository`, `AdminRepository`, `SecurityConfig`, `DataInitializer` | `login.html`, `register.html`, `forgot-password.html`, `admin/users.html`, `admin/dashboard.html`, `index.html` |
| **IT25101987**<br>Salinda L.L.S.U | **Doctor Management** | `Doctor`, `Specialization`, `DoctorService`, `DoctorProfileController`, `DoctorRestController`, `DoctorRepository`, `Cardiology`, `Pediatrics` | `admin/doctor-management.html`, `doctor/dashboard.html` |
| **IT25101972**<br>Kulasuriya I.M.G.H.N | **Patient Management** | `Patient`, `PatientService`, `PatientProfileController`, `PatientRepository` | `patient/profile.html`, `doctor/patient-list.html` |
| **IT25101985**<br>Dhanukshan S. | **Appointment Management** | `Appointment`, `Schedule`, `AppointmentService`, `PatientBookingController`, `DoctorScheduleController`, `AppointmentRepository`, `ScheduleRepository` | `patient/book-appointment.html`, `patient/history.html`, `admin/appointments.html`, `doctor/view-appointments.html` |
| **IT25101980**<br>Ariyawansha N.S.K | **Clinical Records & Notifications** | `MedicalRecord`, `Prescription`, `MedicalRecordService`, `MedicalRecordRepository`, `PrescriptionRepository`, `PDFGenerator`, `PdfService` | `doctor/records.html`, `doctor/write-prescription.html`, `doctor/edit-record.html`, `patient/view-record.html` |
| **IT25101979**<br>Metaramba M.K.K.N | **Payments & Feedback Management** | `Invoice`, `Feedback`, `InvoiceService`, `FeedbackService`, `PaymentController`, `InvoiceRepository`, `FeedbackRepository`, `CardPayment`, `CashPayment`, `Payment`, `RatingService` | `admin/invoice-management.html`, `admin/review-management.html`, `patient/payment-success.html` |

## ⚙️ Setup Instructions
1. **Database**: Create a database named `echanneling_system` (or run `echanneling_schema.sql`).
2. **Configuration**: Update `src/main/resources/application.properties` with your database credentials.
3. **Build**: Run `./mvnw clean install`.
4. **Run**: Run `./mvnw spring-boot:run`.
5. **Access**: Open `http://localhost:8080` in your browser.

## 🔒 Default Credentials (After Init)
- **Admin**: `admin` / `admin123`
- **Doctor**: `doctor` / `doc123`
- **Patient**: `patient` / `pat123`