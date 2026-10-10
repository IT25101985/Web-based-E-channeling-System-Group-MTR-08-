# Team Git workflow

The integration branch is named def; the appointment branch is Appoinment_Management.

1. Fetch and merge origin/def into your feature branch before new work.
2. Stage one coherent change. Describe actual behavior in the commit message, for example: fix(appointments): preserve reschedule context after validation errors.
3. Push your feature branch. Open a PR with base def and compare your feature branch. Include verification.
4. The leader integrates module branches into def, resolves conflicts and runs the full test suite. Preserve teammates' commits using merge commits. Do not force-push shared history.
5. After verification, open a release PR with base main and compare def. Use a merge commit to retain individual commits.
6. Fetch and sync feature branches after the release.

Keep passwords, local databases, generated build output, IDE files and backup copies out of commits. Do not split unchanged code into artificial commits. New commits use the current date; existing published dates and authors are retained.

