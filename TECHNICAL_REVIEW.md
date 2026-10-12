> Historical review. Implementation updates and current validation are recorded in FIXES_AND_VALIDATION.md.

# Technical review â€” 11 October 2026

Scope: primary application in D:/se demo/src, Maven configuration, application configuration, and database migration scripts. This is a source review plus an existing-suite run, not a penetration test or a visual browser audit. This document records findings before the subsequent fixes; consult FIXES_AND_VALIDATION.md and the current release record.

## High priority

1. Admin doctor edits can publish overlapping sessions at different hospitals. AdminController.java:307 checks whether a session already exists at the selected branch, then inserts 09:00â€“17:00 for the next 30 days. It bypasses ClinicSessionService.save and its doctor lock/overlap policy. Adding hospital B to a doctor who has hospital A sessions produces contradictory availability. Use the shared session service, require explicit session times, and test cross-hospital overlap.

2. Cancelling and deleting a paid appointment destroys its invoice. PatientBookingController.java exposes deletion of cancelled appointments; AppointmentService.java:64 deletes invoice rows along with notifications and medical records. A patient can pay, cancel, then delete the booking, removing revenue/refund history. Preserve financial records and hide/archive the booking. UserService.deleteUserById also deletes associated clinical and financial history; deleting a doctor affects many patients.

3. A future scheduled appointment can be completed without attendance. DoctorScheduleController.java:175 accepts isAwaitingConsultation(), which includes SCHEDULED and CHECKED_IN. No appointment-time or attendance check guards prescription creation. Posting a prescription against a next-month scheduled booking immediately marks it COMPLETED. Define the allowed clinical transition, usually CHECKED_IN to COMPLETED, with an explicit override policy if needed.

## Medium priority

4. Registration stores fabricated identity/contact data. AuthController.java:125 and :135 replace missing NIC and phone with the same fixed values. Missing address also becomes a fabricated address. Require needed data or preserve missing values explicitly. The NIC character class [x|X|v|V] additionally admits the literal pipe character.

5. Registration password rules differ from changes/resets. AuthController.java:114 accepts six characters; UserService.saveWithNewPassword requires eight and at most 72 UTF-8 bytes. saveUser does not apply that shared strength/length policy. Centralize the policy and apply it before hashing every raw password.

6. Patient appointment conflicts are not checked. BookingService.availableSlots queries reservations only for the selected doctor, and book locks only that doctor. One patient can book overlapping self appointments with different doctors/hospitals. Check attending-patient identity and overlap, while allowing legitimate family bookings; serialize concurrent patient writes too.

7. CSV export writes raw user-controlled fields. AdminController.java:87 does not quote commas, quotes or newlines and does not neutralize spreadsheet formula prefixes. A name with a comma corrupts column alignment; formula-like text can be interpreted by a spreadsheet. Use a CSV writer and safe spreadsheet export rules.

8. Money is represented as Double. Invoice.amount and booked consultation fees use floating point, and revenue is summed with mapToDouble. Decimal amounts can accumulate representation errors. Use BigDecimal and fixed-scale DECIMAL columns with explicit rounding.

9. Growing datasets will slow dashboards. DoctorScheduleController loads all system appointments before filtering. PatientProfileController.java:244â€“245 performs two additional lookups per appointment; lists are unpaginated. Query by authorized doctor/patient in the database, bulk fetch related records and paginate history.

10. Database deployment relies on development settings. application.properties uses ddl-auto=update, SQL Server sa, and trustServerCertificate=true. Use a separate production profile, restricted database permissions, verified TLS certificates and versioned migrations. This is a deployment-readiness weakness, not evidence that the local demo has been compromised.

## Feature and reliability limits

- CardPayment is an explicitly labelled demonstration and checkout sets its invoice to PAID without a payment gateway. Suitable only for demonstrations; real payments require verified provider results and reconciliation.
- Password recovery intentionally directs users to an administrator; automated identity-verified recovery is absent.
- NotificationDeliveryService sends SMTP while holding database locks. Slow delivery delays appointment operations, and a crash after SMTP accepts a message but before transaction commit can cause a duplicate on retry. Design for at-least-once delivery and minimize lock duration.
- Only username uniqueness is installed by LegacyDataRepair. Email uniqueness is checked with a full application-side scan during registration, with no matching entity/database constraint shown in the reviewed app migrations. Concurrent registrations can share an email. Choose and enforce an identity policy consistently.
- Source includes an old dependency baseline (Spring Boot 3.1.2). No current vulnerability scan was run, so no specific CVE or support-status claim is made.

## Existing strengths

CSRF protection, role-restricted routes, booking form field allowlists, owner checks on prescriptions/invoices, BCrypt hashing, doctor-row booking locks, appointment versioning, locked feedback submissions and a persistent notification outbox are present.

## Validation boundary

The existing automated suite was invoked offline using the installed Maven cache. Live SQL Server smoke coverage is opt-in and was not enabled; browser behavior, accessibility, production load and external email/payment integrations remain unverified. No application source was changed by this review.

Current run result: BUILD SUCCESS; 112 tests discovered, 111 passed, 1 skipped (live SQL Server smoke test), 0 failures, 0 errors. These passing tests do not establish coverage of the source-review findings above.
