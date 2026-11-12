# CLAUDE.md

Minimalist Android launcher with on-device screen-time stats and a habit tracker. Solo, open-source project.

Scope, technical decisions, roadmap and acceptance criteria are in `docs/PROJECT_BRIEF.md`. Read it before planning any milestone work. The current milestone is stated in its section 7.

## Commands

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- One test class: `./gradlew testDebugUnitTest --tests "*.ClassNameTest"`
- Lint: `./gradlew lintDebug`
- No-network check on every variant's merged manifest: `./gradlew checkNoInternetPermission`
- Install on the connected device or emulator: `./gradlew installDebug`
- Instrumented tests (need a device or emulator): `./gradlew connectedDebugAndroidTest`
- In Windows PowerShell or cmd, use `.\gradlew.bat` in place of `./gradlew`.

Before saying a task is done, run build, unit tests, lint and the no-network check, and show the output.

## Project rules

- **Original code only.** Do not copy or adapt code from other launchers or habit trackers (Olauncher and similar), whatever their license. Work from the official Android and Kotlin documentation.
- **No network.** Never add `android.permission.INTERNET` or any SDK that needs it: no analytics, crash reporting, ads or Google Play services.
- **One special access in v1: usage access.** Do not add `QUERY_ALL_PACKAGES`, an Accessibility service, overlay or notification access. If a task seems to need one, stop and raise it.
- **Dependencies.** Ask before adding one. Open-source licenses only. Look up the current stable version instead of recalling it, and declare it in `gradle/libs.versions.toml`.
- **Strings.** User-facing text goes in `strings.xml`, in English. No hardcoded strings in composables.
- **Secrets and releases.** Never commit keystores, `local.properties` or credentials. Do not tag or publish releases; the maintainer does that.
- **License.** See `LICENSE`. Do not add license headers to source files unless asked.

## Workflow

- One slice of a milestone per branch and pull request, small enough to review in one sitting.
- For anything beyond a small fix, propose a plan and wait for approval before editing.
- Usage aggregation, habit math and date handling ship with unit tests in the same pull request.
- A change to a decision in section 5 of the brief needs an ADR in `docs/decisions/`.
- User-visible changes get a line in `CHANGELOG.md`.
- Commits follow Conventional Commits.

## Communication

- The maintainer may write in Spanish. Reply in the language they use. Everything written to the repository is in English: code, comments, commits, docs and UI strings.
- The maintainer must be able to explain every merged change. End each task with a short account of what changed and why, and point out the parts worth reading closely.
- Prefer the plain, conventional solution over a clever one.

## Gotchas

- The system keeps usage events for only a few days. Anything older must come from our own daily snapshots.
- `queryUsageStats()` returns whole interval buckets that can extend outside the requested range. Compute "today" from `queryEvents()`.
- Usage access cannot be granted with a runtime dialog; the user enables it in system settings. Every usage feature needs a "not granted" state.
- Default-home behaviour, usage access and package events cannot be covered by unit tests. When a change needs a manual check on a device, say so and list the steps.
- Date logic uses local calendar dates and an injected clock. Do not read the system time directly inside logic.
