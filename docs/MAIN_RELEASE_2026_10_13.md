# Main release - 13 October 2026

The release integrates the appointment updates and missing local project fixes into the GitHub repository, preserving the published patient-module changes and team branch history.

## Included

- Appointment validation, retained family identity and agreed fees, safe booking errors, cancellation/archive history, and lecture design-pattern implementations.
- Clear administrator check-in messages, date validation and the checked-in doctor prescription workflow.
- SQL Server shared-email compatibility without deleting imported accounts, account recovery, session/account controls, decimal billing and invoice audit handling.
- Existing module UI corrections, Spring Boot 4.1.1 configuration and matching regression tests.
- Home-page hospital selector, saved-address map and directions.
- The existing optional administrator IT2140 viva catalogue and its resources, using a separate assignment database.
- Updated SQL Server, IntelliJ and demo setup instructions. Credentials, local databases, IDE metadata and generated files are excluded.

## Verification of this GitHub checkout

Java 24.0.1 and the Maven wrapper were used on 13 October 2026.

The complete default test run discovered 154 tests. It caught two booking-controller regressions in safe error reporting; the controller was corrected, and all seven tests in PatientBookingControllerTest were rerun successfully. The other default suites passed. The corrected source was packaged successfully; BUILD SUCCESS.

The two normally skipped live checks, SqlServerSmokeTest and SqlServerWorkflowTest, were then enabled explicitly against the existing SQL Server database. Both passed with schema validation, startup repair, email sending and scheduled notifications disabled. The workflow fixtures were rolled back.

The latest reports across all 18 suites contain **154 tests, 154 passed, 0 failures, 0 errors, 0 skipped**. These reports combine the complete default run, the corrected controller rerun and the separate live SQL Server run; they are not a claim that every check ran in one Maven invocation.

The smoke check renders public, administrator, patient and doctor pages. The live workflow covers booking, rejecting future-date check-in, cash collection, visit-day check-in, prescription completion, approved feedback and receipt access. It does not verify external SMTP or a real card gateway.

Git whitespace checks and a scan against the local database password passed. No local credentials are published. The production profile was not deployed.

The packaged executable JAR was started separately on port 8081 using the existing SQL Server configuration with schema validation and side effects disabled. Tomcat started successfully; the homepage and hospital-map script both returned HTTP 200, and the homepage contained the map panel.
