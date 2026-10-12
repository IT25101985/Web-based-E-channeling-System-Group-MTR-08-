# MediCare Pro - E-Channeling System

SE2030 group project **2026-Y2-S1-MTR-08**. Java 17+, Spring Boot 4.1.1, Thymeleaf, Spring Security and JPA.

Patients choose a hospital, doctor and published clinic session, book for themselves or family, reschedule, cancel and archive their history. Administrator, doctor, receptionist, notification, feedback and billing modules share these appointments.

## Start with SQL Server

1. Install JDK 17 or later and set JAVA_HOME. Open the repository folder containing pom.xml.
2. Start your SQL Server instance and create/use your local echanneling_system database.
3. Copy application-local.properties.example to application-local.properties. Set the actual SQL Server URL, username and password using the spring.datasource.* properties. This file is ignored by Git.
4. Run the commands below. The wrapper downloads Maven and dependencies on the first online build.

```powershell
Copy-Item application-local.properties.example application-local.properties
# Edit application-local.properties before starting.
.\mvnw.cmd clean package
java -jar target/echanneling-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=sqlserver
```

Open http://localhost:8080. SQL Server is the normal project database. New databases need initial administrator/doctor/hospital/session records before appointment booking; SQL Server mode does not seed demo accounts. Existing database IDs and records are retained. For the group's existing database, use its existing local configuration and accounts.

An alternative is `.\run.ps1 -Profile sqlserver -Build`.

### IntelliJ

Open pom.xml as a Maven project and let IntelliJ import dependencies. Select JDK 17 or later. Run `com.echanneling.EChannelingApplication` with program arguments `--spring.profiles.active=sqlserver` and the repository root as the working directory. Stop the previous application before reusing port 8080. If another application needs port 8080, use `--server.port=8081` and open http://localhost:8081.

If imported accounts share an email, the startup repair preserves their usernames, IDs and linked records. Email recovery refuses ambiguous addresses. See [SQL Server startup](docs/INTELLIJ_SQL_SERVER_STARTUP.md).

### Existing databases and production

Development uses Hibernate schema updates and transactional legacy-data repair. Review [the migration guide](database/migrations/README.md) before manually migrating an existing database. The SQL scripts target the application schema; the separate IT2140 assignment schema is optional. The prod profile requires a migrated database, verified TLS, a dedicated database account and configured environment variables.

## Optional demonstration

For a separate local H2 demonstration without installing SQL Server:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Demo accounts: admin / admin123, drsmith / doctor123, dhanu / patient123, reception / Reception@123. These are demo-only credentials. Demo mode uses data/carelink-demo and has email delivery disabled.

## Verify

```powershell
.\mvnw.cmd clean package
```

The default suite uses isolated H2 databases, with live SQL Server checks skipped. To verify your configured SQL Server database explicitly:

```powershell
.\mvnw.cmd test "-Dtest=SqlServerSmokeTest,SqlServerWorkflowTest" "-Dcarelink.sqlserver=true"
```

The smoke test renders existing records. The workflow test creates appointment fixtures in a transaction and rolls them back, with email and scheduled notifications disabled. See [the latest release verification](docs/MAIN_RELEASE_2026_10_13.md).

## Appointment workflow and map

Booking -> cash payment collection -> administrator check-in on the appointment date -> doctor prescription -> completed appointment -> patient feedback. Future-date check-in returns a clear message. The doctor can write a prescription after check-in.

See [Dhanukshan's appointment guide](docs/APPOINTMENT_UPDATES.md) and [lecture design patterns](DESIGN_PATTERNS.md). The home page's Hospitals section includes a branch selector, address map and directions links. Maps use saved hospital addresses and require internet; verify the addresses in hospital management. See [hospital map](docs/HOSPITAL_MAP.md).

The optional administrator DDD viva page reads its bundled catalogue using a separate configured assignment database. It does not replace the appointment database and is not required for normal booking.

## Team Git workflow

**Appoinment_Management -> def -> main**. Team integration keeps published branch history and authorship. See [CONTRIBUTING.md](CONTRIBUTING.md). Local passwords, databases, IDE files, generated output and diagnostic logs stay out of Git.

Card checkout is a demo simulation. Production uses counter cash. Email delivery and password-recovery links require SMTP configuration; SMS delivery is not implemented.
