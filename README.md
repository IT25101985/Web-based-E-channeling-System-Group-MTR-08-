# MediCare Pro / CareLink — E-Channeling System

SE2030 group project, **2026-Y2-S1-MTR-08**. Spring Boot 3.1.2, Java 17+, Thymeleaf, Spring Security and JPA.

Patients select a hospital, doctor and published clinic session, book appointments, view history, reschedule and cancel. Doctor, hospital, notification, feedback and billing modules share appointment records. A receptionist workspace supports desk bookings and cash collection.

## Run the demonstration

Install JDK 17 or later and set JAVA_HOME. The Maven wrapper downloads Maven and dependencies on first use.

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Open http://localhost:8080. The demo profile uses a separate H2 database in data/; it does not connect to SQL Server.

| Role | Username | Demo password |
| --- | --- | --- |
| Administrator | admin | admin123 |
| Doctor | drsmith | doctor123 |
| Patient | dhanu | patient123 |
| Receptionist | reception | Reception@123 |

These are demonstration accounts. Email sending is disabled by default in demo mode.
Alternatively, `.\run.ps1 -Profile demo -Build` tests, packages and starts the application.

## Use SQL Server

The default profile uses SQL Server. Copy application-local.properties.example to application-local.properties and supply your own connection settings. The local file is ignored by Git. Configure an existing SQL Server database, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

The default configuration uses Hibernate schema updates and legacy-account repair on startup. Back up an existing database first. database/finalization-sqlserver.sql is an optional additive migration for an existing CareLink schema, not a fresh database creation script; review it before applying it manually.

## Verification

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Integration tests use isolated H2 databases with mail delivery and scheduled notifications disabled. The SQL Server smoke test is opt-in using `-Dcarelink.sqlserver=true`; it requires your configured database. See [the integration record](docs/INTEGRATION.md) for this revision's actual results.

## Modules and workflow

- Appointment work: PatientBookingController, BookingService, AppointmentService, appointment/availability repositories and booking tests.
- Leader integration: connect team modules, resolve paths, maintain configuration, run regression tests and prepare release merges.
- Existing branch histories and authorship are retained. Integration commits do not imply sole authorship of imported modules.

Workflow: **Appoinment_Management → def → main**. Other module branches merge into def for integration. See [CONTRIBUTING.md](CONTRIBUTING.md) and [PROJECT_GUIDE.md](PROJECT_GUIDE.md).

## Current limits

Card checkout is simulated. SMS delivery and password-reset emails are not implemented. External SMTP requires configuration; sent means the SMTP server accepted the message. H2 tests do not verify live SQL Server, real payments or real mailbox delivery.


## Appointment Booking update

See [Dhanukshan's appointment update guide](docs/APPOINTMENT_UPDATES.md) for family bookings, conflict protection, archived history, checkout handoff and the lecture design-pattern examples. Existing SQL Server installations can use `database/appointment-updates-sqlserver.sql`.
