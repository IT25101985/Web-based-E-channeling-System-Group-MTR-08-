# Demonstration and module guide

1. Start with the demo profile using the README.
2. As administrator, inspect hospitals, doctors and published clinic sessions.
3. As a patient, select a hospital, doctor and available future time. Book with valid contact details and check history.
4. Try the same doctor/time with a second patient and show that the slot is unavailable.
5. Reschedule a booking. Invalid details should preserve the appointment ID for retry; successful reschedules have a distinct confirmation.
6. Cancel a scheduled appointment and verify that its slot becomes available.
7. As the doctor, create the consultation record and complete the visit.
8. Demonstrate receptionist bookings, cash collection and PDF invoices. Card checkout is simulated.
9. Submit feedback for a completed visit and demonstrate moderation.
10. Inspect patient/doctor notifications and administrator delivery status. Email requires configured SMTP.

## Appointment implementation

BookingService centralizes availability and booking rules. Doctor row locks serialize reservations; appointment locks protect updates. It checks active patient status, published hospital sessions, future times, ownership and contact fields.

PatientBookingController restricts bound form fields, preserves reschedule context on validation failures, distinguishes booking/reschedule confirmations and hides unexpected internal error details.

TC_BOOK_AppointmentBookingTest covers the booking lifecycle. Its duplicate-slot test uses sequential attempts; it is not a concurrent load test. PatientBookingControllerTest covers redirects and confirmation/error messages.

## Leader integration

Integration retains doctor, hospital and notification branch histories, restores hospital source paths, connects shared services, restores templates/assets and adds the existing local regression suites. See docs/INTEGRATION.md for the baseline and verification.

Each member should explain their module and record their actual contribution. Integration commits do not establish sole authorship of imported modules.

