# M0 Java core tasks

- [x] Confirm PR #1 merged, clean tree, and create a fresh branch from main.
- [x] Read accepted rules and research/pin compatible versions.
- [x] Add the Java 21 application, health module, wrapper, and build settings.
- [x] Add boundary verification and packaged-JAR HTTP checks.
- [x] Add a real core CI job and extend gate/manifest regression tests.
- [x] Add core-only container files and startup/build docs.
- [x] Run clean builds, compare JAR hashes, run repository/checker/DSL checks, and inspect the diff.
- [ ] Verify Compose startup on a Docker host and the new core job on GitHub (publication is not authorized).

Local checks on 2026-11-05: two Java 21.0.13+9 clean builds passed (1 Modulith test and 1 packaged-JAR HTTP test each). JAR SHA-256 matched: `c4350082c2d1c835a5c26cfd79949dc7402238d6708937ee46353c7946a5039d`. Repository checks, all 11 checker tests, real Structurizr validation, wrapper shell syntax, YAML parse, and whitespace checks passed.

M0 remains open: web, Python, database/migrations, auth, and full-stack smoke are separate slices.
