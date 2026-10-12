# Technical hardening â€” 11 October 2026

The application updates are included in this repository. See docs/MAIN_RELEASE_2026_10_13.md for the current release verification.

## Changes

- Admin doctor/session changes use the shared session policy and database doctor lock. Published times come from clinic hours; overlapping sessions at another branch roll back the entire edit.
- Patient removal archives cancelled appointments. Account/doctor removal deactivates access; invoices, notifications and clinical history are retained. Invoice deletion is rejected. Refunds have an actor/time audit trail.
- Reception checks in appointments only on their visit date. Prescriptions complete checked-in visits; scheduled or future visits cannot be completed.
- Registration requires actual patient identity/contact/address fields. NIC parsing rejects pipe characters. Password creation/change/reset uses a shared minimum-eight-character, maximum-72-UTF-8-byte policy.
- Account email values are normalized and database uniqueness protects concurrent registrations. Existing imported accounts sharing emails are preserved using a database-managed uniqueness scope; recovery is blocked for ambiguous addresses. No account identities are fabricated or removed automatically.
- Booking locks the account and the attending identity before the doctor. Overlap checks apply across doctors and account owners, while allowing different relatives and adjacent times.
- CSV fields are quoted/escaped and spreadsheet formula prefixes are neutralized.
- Consultation fees, stored agreed fees, invoices, prescription prices and revenue use BigDecimal with DECIMAL(19,2) storage and explicit HALF_UP rounding.
- Patient/doctor/reception/admin appointment history is paginated; medical/invoice data is fetched in batches. Doctor record lists are queried by the authorized doctor. Dashboard counts/revenue use database aggregation.
- A production profile requires validated schema, verified encrypted SQL Server connections, a dedicated account and an HTTPS public origin. Versioned migration V003 adds supporting columns, tables and indexes.
- Card simulation is available only when explicitly enabled for demo/tests; production rejects it at startup and at the payment endpoint. Production supports counter cash.
- Password recovery issues a random single-use email link with a 15-minute expiry. Only a hash is stored in the reset table. Requests use generic responses and are throttled per account. Successful password changes invalidate reset tokens and expire registered sessions after commit.
- Email delivery claims a short database lease, sends outside the scheduler's transaction, and finalizes in another short transaction. SMTP remains at-least-once: a process crash after acceptance can cause a retry; SMTP offers no guaranteed exactly-once delivery.

## Dependency baseline

Spring Boot is upgraded from 3.1.2 to 4.1.1, with updated test modules, Jackson 3 integration and current managed dependencies. The unused Twilio SDK is removed; iText is upgraded to 5.5.13.4. Java 17 remains the minimum.

Official compatibility reference: [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)

## Existing installations

Apply database/finalization-sqlserver.sql, database/review-fixes-sqlserver.sql, then database/migrations/V003__technical_hardening.sql before production deployment. Development startup can update schema, but a production runtime should not execute schema migrations. See database/migrations/README.md.

Old fabricated NIC/contact values and previously deleted history cannot be safely repaired from code alone. Existing float amounts are converted to two decimal places; keep a database backup and verify financial totals before deployment.

Real card processing requires a payment-provider account and verified payment results. Actual email delivery requires hospital SMTP settings; local tests use mocks or a loopback SMTP sink.

## Validation

- Latest reports across all 13 suites: 133 tests, 132 passed, 1 skipped (opt-in live SQL Server smoke test), 0 failures, 0 errors.
- All 21 new regression tests passed, including cross-account concurrent family booking, safe archive retention, decimal persistence, reset expiry/single use, CSV injection protection, production configuration rejection and SMTP lease behavior outside a transaction.
- The full run initially found three old URL-pattern expectations that did not match the upgraded framework's relative /login redirect. Those assertions were corrected; all three affected suites passed on follow-up runs. No application check was removed to accommodate those assertions.
- Updated executable jar built successfully at target/echanneling-system-0.0.1-SNAPSHOT.jar.

## Pending live database step

The existing SQL Server connection succeeded with a longer timeout. Read-only preflight found two duplicate email groups:

- Account 15 (kavi) and account 18 (qwer).
- Account 3 (pavi) and account 8 (pavi_legacy_8).

The user has been asked which account keeps each email and what the other verified emails should be. V003 must not be applied until these conflicts are resolved. No live account data, receipts or clinical histories were modified. The new production deployment and live SQL Server smoke verification remain pending that step.

The packaged application's demo/test profile is verified against isolated H2 databases. Real SMTP credentials, payment-provider integration, a production TLS certificate and production load testing are not established by these tests.
