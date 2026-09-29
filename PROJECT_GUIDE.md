# CareLink demonstration guide

This guide connects the implementation to the project assessment. Marks also depend on the submitted report, individual code understanding and the live presentation; software changes cannot guarantee a grade.

## Demonstration sequence

1. Open the homepage. Explain that the doctor directory is read from the database.
2. Sign in as administrator. Show patient and doctor counts, recent appointments and revenue calculated from paid invoices.
3. Create a hospital branch and edit its details. Create a doctor account, set a specialization, clinic hours and consultation fee.
4. Register a patient. Sign in, update their profile, choose the doctor and book a future time. Explain the availability check and show the booking in history.
5. Try the same doctor and time with a second patient. Explain why the database lock prevents a duplicate booking.
6. Show rescheduling and cancellation. Only scheduled visits can change; cancelled appointments can be removed with confirmation.
7. Sign in as the booked doctor. Open the consultation, write a prescription and complete the visit. The patient's record becomes available in their portal.
8. Show a demonstration card checkout or an unpaid cash invoice. Download the invoice PDF. Explain that the amount comes from the doctor's saved fee, and real payments are not processed.
9. Submit a review after a completed visit. Approve it from the administrator workspace. Only approved reviews contribute to the doctor's rating.
10. Show the pending confirmation record created by booking. Edit or remove notification records from the administrator workspace. External email/SMS delivery needs an independently configured provider.

## Explain the code

| Area | Relevant implementation | What to explain |
| --- | --- | --- |
| Patient management | `PatientProfileController`, `Patient`, `UserService` | Inheritance, encapsulation, profile validation, safe field binding |
| Doctor management | `DoctorProfileController`, `DoctorService` | Doctor/account relationship, validated clinic hours and fees |
| Hospital management | `HospitalBranchController`, `HospitalBranchService` | Create, read, update and delete persisted branch records |
| Appointment management | `BookingService`, `AppointmentRepository` | Shared availability policy, future dates, ownership, transaction and database row lock |
| Notification management | `NotificationsController`, `NotificationService` | Notification record lifecycle and delivery status; pending outbox records are not sent messages |
| Feedback management | `PaymentController`, `FeedbackService` | Completed-visit requirement, 1–5 rating validation and moderation |
| Clinical records and billing | `DoctorScheduleController`, `CheckoutService`, `PdfService` | Atomic consultation completion, owned records, repeat-safe checkout and real PDF generation |

Design patterns: `UserFactory` selects the account type; the `Payment` interface is implemented by `CardPayment` and `CashPayment` and selected by `CheckoutService`. Notification strategy classes are extension points; external delivery is not connected to booking in this build.

## Data integrity and access

- Usernames are protected by a database unique index. Legacy duplicate accounts retain their records; later duplicates are renamed to `<name>_legacy_<id>` during startup.
- Passwords use BCrypt. Public registration cannot bind an ID or elevate an account role. Password recovery requires administrator identity verification.
- Forms use CSRF protection. Roles restrict the workspaces, with additional ownership checks for patient invoices and doctor medical records.
- Booking and checkout amounts cannot be changed by editing hidden client fields. A doctor row lock serializes competing reservations through commit.
- Multi-step prescription writes run in one transaction. Deletes clean dependent records in dependency order.
- SQL Server is the default. The explicitly selected `demo` profile uses a separate persistent H2 file and demonstration records. It does not show or import SQL Server data.

## Scope to describe accurately

The application has patient, doctor and administrator roles. Hospital branches have CRUD, while bookings currently use doctor clinic hours; per-branch slot assignment and a receptionist workspace are not wired into the booking interface. Password reset links by email, external email/SMS sending, and real card payments are not delivered by this build. The optional H2 demonstration database is separate from the installed SQL Server database. Current health measurements are self-reported profile values, not a fabricated medical trend.

The integration tests exercise page rendering, registration, role restrictions, CSRF, appointment persistence and conflict rejection, checkout ownership and repeat handling, valid PDF output, consultation record creation and branch CRUD. They use an isolated H2 database and roll back test changes. Live SQL Server and browser verification should also be performed before a presentation.
