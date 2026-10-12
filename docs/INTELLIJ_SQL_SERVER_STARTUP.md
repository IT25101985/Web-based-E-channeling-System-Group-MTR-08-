# IntelliJ SQL Server startup

Choose **EChannelingApplication SQL Server** in IntelliJ and run it. The working
folder is the project root. Credentials remain in the existing local properties
file. Stop any previous app instance before using port 8080.

Imported accounts sharing an email keep their IDs, usernames, addresses,
passwords and related records. A database-managed uniqueness scope allows only
existing duplicate rows. One scope-zero reservation remains per email, so new
inserts cannot reuse it. New accounts also pass normalized-email validation.
The scope field cannot be supplied by registration forms or updated by the ORM.

Username/password login remains available. Email recovery returns the normal
generic response without issuing a token when multiple accounts match. Existing
reset tokens are rejected too. Manual email notifications to a shared address
are not attached to an arbitrary account history. Correct individual email
addresses can be recorded later after their owners confirm them.

Local schema-update startup adds the scope column and runs the repair in a
transaction. For schema-validation-only deployments, apply
`database/migrations/V004__legacy_email_compatibility.sql` before this version.
The older V003 migration still requires distinct emails; V004 only handles
email compatibility and does not replace V003's unrelated schema changes.
