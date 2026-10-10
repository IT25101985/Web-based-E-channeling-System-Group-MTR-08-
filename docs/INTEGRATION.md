# Appointment and leader integration — 10 October 2026

## Baseline

Starting main: 6f4f317; def: 6b44bc7; appointment: 8801159. Earlier appointment work had reached main through PRs #8 and #9.

The baseline failed compilation because referenced doctor, hospital, notification, invoice and medical-record types were missing. The uploaded User model also lacked isActive(), which booking code called.

## Changes and provenance

- Appointment: preserve reschedule context, distinguish confirmations, handle failed saves safely, and add booking/controller regression tests.
- Merge existing doctor (1a85e95), notification (8d8f79e) and hospital (068e4fb) branch histories into def to retain their authors and earlier commits.
- Resolve hospital conflicts by restoring code under src/main/java/com/echanneling and its page under templates/admin.
- Import existing local implementations of shared clinic sessions, account status, notification delivery, billing/feedback, reception, bootstrap configuration, portal pages and assets. These commits integrate the group project; they do not claim new authorship of every module.
- Include existing local module and connected workflow regression suites. Exclude the unrelated DDD viva feature and its resources/tests.
- Correct the example database settings to the Spring property names the application reads; document setup and branch workflow.

## Verification

Results will be recorded after the integrated build finishes. The default tests use H2; live SQL Server and external email require separate checks.

