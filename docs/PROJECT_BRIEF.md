# Project brief

**Minimalist Android launcher with on-device screen-time stats and a habit tracker.**
Name: **Tengwar Minimal** (see [Open decisions](#12-open-decisions)).

Brief v1.1 · 11 November 2026 · Status: M0 in progress.

This document is the starting context for the project: what we are building, what is in and out of v1, the technical decisions taken so far, and the order of work. `CLAUDE.md` holds the short rules that load in every Claude Code session; this file holds the reasoning. Update it when a decision changes.

## 1. Summary

A home-screen app for Android that replaces the icon grid with a clock and a short text list of apps, shows how much you have used your phone today, and tracks habits in a one-square-per-day grid. Everything is stored on the device.

The bet: usage numbers and habits work better on the screen you see every time you unlock than inside a separate app you have to remember to open.

**Project goals**

1. A launcher the maintainer uses every day. Dogfooding is the main product test.
2. A public codebase that demonstrates Android engineering: clear architecture, tested logic, CI, small reviewed changes.
3. A documented example of building software by directing an AI coding agent (Claude Code), including what went wrong.

**Decisions already taken**

| Topic | Decision |
|---|---|
| Codebase | Written from scratch. No code copied or adapted from other launchers or habit trackers. |
| Openness | Open source, public GitHub repository from the first commit. License: GPL-3.0. |
| Name | Tengwar Minimal. Application ID and Kotlin package: `io.github.ivandietta.tengwarminimal`. |
| Language | English for code, comments, commits, docs and UI strings. |
| v1 scope | Launcher + phone usage + habits + pause before opening (section 3). |
| Distribution | APK on GitHub Releases. |
| Tooling | Android Studio and Claude Code, both running locally. |

## 2. Principles

1. **Local only.** No accounts, no network. The app never declares `android.permission.INTERNET`, and CI enforces it.
2. **Least access.** v1 needs one special access (usage access), and every screen still works, in reduced form, without it.
3. **Calm.** Text instead of icons. No badges, and no notifications asking the user to come back.
4. **Honest numbers.** How screen time is calculated is documented and unit-tested. Where our number can differ from the system's, we say why.
5. **Original and readable.** Conventional solutions over clever ones. The maintainer can explain every change that is merged.

## 3. Scope of v1

| Module | What the user gets | Special access |
|---|---|---|
| Home and app list | Clock and date, up to five pinned apps as plain text, swipe up for every installed app with search. Works as the default home app. | None |
| Phone usage | Today's screen time and unlock count on the home screen. Tap for a per-app breakdown and the last seven days. | Usage access |
| Habits | Today's habits as a row of squares on the home screen, one tap to check in. A grid per habit with one square per day and a completion rate. A habit can be automatic: "under 2 h of screen time" checks itself. | None |
| Pause before opening | Apps the user flags open behind a short pause: a five-second countdown, how often and how long the app was used today, and one pending habit. | None |

**Out of scope for v1**

- **Strong blocking.** The pause only covers launches that start from this launcher. Catching launches from notifications, the recents screen or other apps needs an Accessibility service or an overlay. That is a research spike after v1 (see "Later" in section 7).
- Widgets, icon grids, icon packs, notification badges.
- Accounts, sync, server backups, anything that needs the network.
- Google Play and F-Droid listings.
- Tablet, foldable and landscape layouts beyond "does not break".

## 4. Screens

The one-page design (add it as `docs/one-page-design.svg`; the original is in Spanish) shows these screens and how they connect.

- **Home** is the hub: clock and date, today's screen time and unlocks, today's habits as squares, pinned apps.
  - Tap the screen time → **Usage**.
  - Swipe right → **Habits**.
  - Swipe up → **All apps**.
  - Tap a flagged app, here or in All apps → **Pause** → the app.
- **All apps**: alphabetical text list of every launchable app, with search. Flagged apps carry a pause mark.
- **Usage**: today's total, unlock count, time per app, and a bar per day for the last seven days.
- **Habits**: one block per habit with its name, completion rate and grid.
- **Pause**: app name, times opened and time spent today, a five-second countdown, one pending habit. "Go back" is the primary action. "Open anyway" becomes available when the countdown ends.
- **Settings** and first-run **onboarding**: set as default home, grant usage access, choose pinned apps, flag apps for the pause.

## 5. Technical decisions

These are the starting decisions. Each one becomes a short ADR in `docs/decisions/` when it is first implemented, and changing one means writing a new ADR.

| Area | Choice | Why |
|---|---|---|
| Language and UI | Kotlin, Jetpack Compose with Material 3, single activity | Current standard for new Android apps. |
| Minimum SDK | 29 (Android 10) | `RoleManager` (asking to become the default home app) was added in API 29, and the usage event types the app relies on are expected to be available by then. No legacy branches. |
| Target and compile SDK | Latest stable when the project is created (Android 17, API 37, at the time of writing) | Take it from the Android Studio template, not from memory. |
| Modules | One Gradle module, packages by feature (`home`, `apps`, `usage`, `habits`, `pause`, `settings`, `core`) | Extract modules only when a boundary has proven itself. |
| State | ViewModel + StateFlow, unidirectional data flow, coroutines and Flow | Conventional and easy to test. |
| Dependency injection | Hilt | Standard on Android; decided once so it is not reopened per feature. |
| Persistence | Room for habits, check-ins and daily usage snapshots; DataStore for settings | Local, typed, and migrations can be tested. |
| Background work | WorkManager for the daily usage snapshot, plus catch-up whenever the launcher comes to the foreground | Background jobs can be delayed; a launcher is in the foreground many times a day. |
| Platform access | Behind small interfaces: installed apps (`LauncherApps`), usage events (`UsageStatsManager`), clock and time zone | Keeps aggregation and date logic testable on the JVM. |
| App visibility | `<queries>` with a `MAIN`/`LAUNCHER` intent | Least package visibility that lists every launchable app. No `QUERY_ALL_PACKAGES`. |
| Dependencies | Open-source only, in the Gradle version catalog. No Google Play services, analytics, crash reporting or ads SDKs | Follows from principle 1 and keeps F-Droid possible later. |
| Build | Gradle Kotlin DSL and version catalog, as generated by the template | Versions come from current stable releases. |

## 6. Platform notes and pitfalls

**Launcher**

- The activity declares the `HOME` and `DEFAULT` categories. Asking to become the default goes through `RoleManager` with `ROLE_HOME`.
- Pressing Home while the launcher is already showing delivers a new intent: return to the main page. Back does nothing on the main page.
- The app list updates on install, uninstall and update without a restart (`LauncherApps.Callback`). Work-profile apps must at least not crash the list.
- The launcher process is restarted often. Keep startup light: nothing slow on the main thread, no loading state when returning home.
- Default-home behaviour cannot be unit-tested. Changes here need a manual check on a device or emulator, with the steps written in the pull request.

**Phone usage**

- Usage access (`PACKAGE_USAGE_STATS`) is a special access the user enables in system settings. There is no runtime dialog. Every usage feature needs a "not granted" state.
- The system keeps usage events for only a few days. Longer history has to come from our own daily snapshots in Room.
- `queryUsageStats()` returns whole interval buckets: a request in the middle of an interval includes that interval. Compute "today" from `queryEvents()` instead.
- Screen time comes from activity resumed/paused pairs (`MOVE_TO_FOREGROUND` and `MOVE_TO_BACKGROUND` are deprecated in favour of `ACTIVITY_RESUMED` and `ACTIVITY_PAUSED`). Unlocks come from keyguard-hidden events. Device shutdown and startup events close any session left open. Confirm each constant's "Added in API level" in the reference during M2.
- The snapshot must be idempotent and must backfill: on each run, write any missing day still covered by the system's retention.
- Decide and document what counts as screen time (open decision 3).

**Habits**

- A day is a local calendar date, not a timestamp range. Store ISO dates. Handle daylight-saving changes and time-zone changes in tests.
- Logic takes an injected clock. No direct calls to the system time in logic.

**Pause**

- It only covers launches initiated by this launcher. Say so in the UI and the README; do not imply blocking.

## 7. Roadmap

| Milestone | Release | Reference effort |
|---|---|---|
| M0 Foundation | none | a few days |
| M1 Launcher core | v0.1.0 | about 4 weeks |
| M2 Phone usage | v0.2.0 | about 3 weeks |
| M3 Habits | v0.3.0 | about 4 weeks |
| M4 Pause before opening | v0.4.0 | about 1 week |
| M5 Polish and launch | v1.0.0 | 1 to 2 weeks |

The M1 to M4 figures come from the one-page design and assume about 10 hours a week from someone who already programs. M0 and M5 are rough additions. Re-estimate after M1, when there is real data on pace. Current status: **M0 in progress.**

### M0 Foundation

- Project generated from Android Studio's Compose template; builds from a clean clone.
- Public GitHub repository with `LICENSE` (taken from GitHub's license template, not written from memory), README stub, `CLAUDE.md`, this brief, and a `.gitignore` that covers keystores and `local.properties`.
- CI on every pull request: build, unit tests, Android Lint, plus the static analysis chosen in ADR-0001.
- A check that fails when the merged manifest contains `android.permission.INTERNET`.
- ADR template and ADR-0001 (architecture) in `docs/decisions/`.
- Shared Claude Code permissions in `.claude/settings.json` for the Gradle and adb commands used daily.

**Done when:** CI is green on `main`, and the app installs and shows an empty home screen.

### M1 Launcher core → v0.1.0

- Registers as a home app. Onboarding asks to make it the default.
- Home: clock, date, up to five pinned apps as text. Pin, unpin and reorder.
- All apps: alphabetical list, search as you type, tap to launch, long-press for app info and uninstall.
- List reacts to installs and removals. Home press returns to the main page.
- Light and dark theme, system font scale respected, TalkBack labels on every control.
- Settings screen.

**Done when:** the maintainer has used it as the only launcher for a week without switching back, and the manual test checklist for home behaviour passes on one physical device and one emulator.

### M2 Phone usage → v0.2.0

- Onboarding step that explains and requests usage access; the app works without it.
- Home shows today's screen time and unlocks, with a seven-day mini chart.
- Usage screen: time per app today, a bar per day for the last seven days.
- Daily snapshot into Room, idempotent, with backfill and foreground catch-up.

**Done when:** the aggregation logic passes unit tests on synthetic event streams (unpaired events, sessions across midnight, reboot mid-session, time-zone change), and on the maintainer's phone today's total is within a few minutes of the system's Digital Wellbeing figure or the difference is explained in the docs.

### M3 Habits → v0.3.0

- Create, edit, archive habits: name, schedule (daily or N times a week), type (manual, or automatic "screen time under X").
- Home shows today's habits as squares; one tap checks in a manual habit.
- Habits screen: grid with one square per day and completion rate per habit.
- Automatic habits are derived from the M2 daily totals.

**Done when:** completion-rate and schedule logic passes unit tests, including week boundaries, daylight-saving and time-zone changes; Room migrations are tested; and the Habits screen scrolls smoothly with a full year of generated data.

### M4 Pause before opening → v0.4.0

- Flag apps in Settings or from the app list.
- Opening a flagged app from Home or All apps shows the Pause screen described in section 4. Countdown length is configurable.
- The limitation (launcher-initiated launches only) is stated in Settings and the README.

**Done when:** the flow passes a Compose UI test, and a flagged app cannot be opened from this launcher without passing the pause.

### M5 Polish and launch → v1.0.0

- Complete onboarding. Accessibility pass: TalkBack, large fonts, contrast, 48 dp touch targets.
- Export and import of habits and history to a file, since there is no cloud.
- Signed release built by CI from a tag; APK and SHA-256 checksum attached to the GitHub Release.
- Final README (section 11) and `docs/ai-workflow.md`.
- Developer verification handled (section 10).

**Done when:** a person who has never seen the project can install the APK from the release page and finish onboarding without help.

### Later

- Research spike on strong blocking: Accessibility service versus usage polling with an overlay, what each catches, and what Google Play requires for each.
- Google Play listing (new personal developer accounts need a closed test with 12 testers for 14 days) and F-Droid.
- Spanish translation, widgets, notification filtering.

## 8. Quality bar

- **Tests where the logic is.** Usage aggregation, habit math and date handling have unit tests in the same pull request as the code. Room has DAO and migration tests. Each feature has at least one Compose UI test for its main flow.
- **CI is the gate.** Nothing merges with a red build.
- **Small changes.** One slice of a milestone per pull request, reviewable in one sitting, with a description that states what changed, why, and how it was verified.
- **Manual checks are written down.** Anything that needs a device (default home, usage access, package events) has steps in `docs/manual-tests.md`.
- **Permissions are reviewed at each release.** Read the merged manifest's permission list before tagging; libraries can add entries.
- **Accessibility is not a final pass only.** New screens ship with content descriptions and survive the largest font scale.

## 9. Working with Claude Code

**Loop for each slice of work**

1. Start a fresh session. Name the milestone and paste or point to its acceptance criteria in this brief.
2. Plan mode first. Claude explores and proposes; the maintainer edits the plan until it is what they would do.
3. Implement on a branch, with tests in the same change.
4. Run the checks (build, unit tests, lint) and show the output.
5. Review twice: `/code-review` for a reviewer that did not see the conversation, then the maintainer reads the whole diff and tries the build on a device.
6. Merge through a pull request with CI green.

**Ownership**

- The maintainer decides scope, approves plans, and must be able to explain every merged change. If a diff cannot be explained, it is not merged yet.
- Claude ends each task with a short account of what changed and why, and points at the parts worth reading closely.
- Suggested practice: in each milestone, the maintainer writes one small pure piece by hand (a function and its tests) and asks Claude to review it, instead of the other way around.

**What lives in the repo**

- `CLAUDE.md`: short rules and commands, under 200 lines. Prune it as the code starts answering the questions itself.
- `docs/PROJECT_BRIEF.md`: this file. Referenced by path from `CLAUDE.md`, not imported, so it is read when needed and not loaded in every session.
- `docs/decisions/`: ADRs.
- `docs/ai-workflow.md`: a running log per milestone of what was delegated, what Claude got wrong and how it was caught, and what the maintainer decided. This is the raw material for section 11.
- `.claude/settings.json`: shared permissions. Add hooks later only for rules that can have no exceptions.
- `.claude/skills/`: procedures worth repeating, such as a release checklist, once they stabilise.
- `CLAUDE.local.md`: personal preferences. Git-ignored.

## 10. Releases and distribution

- **Channel:** GitHub Releases. Pre-releases for v0.x, a full release for v1.0.0.
- **Versioning:** semantic versions, a `versionCode` that always increases, and a `CHANGELOG.md` with user-visible changes.
- **Signing:** one release keystore, created once, kept out of the repository and backed up in two places. If it is lost, existing installs cannot be updated. CI signs from repository secrets.
- **Application ID:** permanent in practice. Changing it later makes Android treat the app as a different one. Settled in M0 as `io.github.ivandietta.tengwarminimal` (decision 1).
- **Developer verification:** Google is rolling out identity verification for developers of apps installed on certified Android devices, including apps installed outside Google Play. Per Google's published schedule it starts with installs from major app stores in a first group of countries on 30 September 2026, and is planned to cover all apps on certified devices worldwide in 2027. Once it applies, installing an APK from an unverified developer requires the user to complete a one-time "advanced flow" or to use ADB. As of November 2026 the Android Developer Console lists a free limited-distribution account (no government ID, up to 20 devices) and a full-distribution account (US$25, identity verification). Check the current rules before v1.0.0 (open decision 5).

## 11. Showcase plan

The project supports two claims: "I can build Android software" and "I can direct an AI coding agent". Everything below is evidence for one or the other.

**The repository is the portfolio piece.** The README has, in this order: what the app is in one line, a short screen recording, the feature list, the privacy statement (no `INTERNET` permission), how to install, how to build from source, the architecture in ten lines with links to the ADRs, and a "How this was built" section linking to `docs/ai-workflow.md`.

**LinkedIn**

- Add the project to the profile's Projects section at v0.1.0, and update it at v1.0.0.
- One post per release. Each has one screen recording, one engineering problem and how it was solved, and one thing learned about working with Claude Code.

| When | Anchor |
|---|---|
| Kickoff | The one-page design and this brief: planning before code. |
| v0.1.0 | What a launcher is on Android, and living with your own for a week. |
| v0.2.0 | Turning raw usage events into a trustworthy number; testing with synthetic streams. |
| v0.3.0 | The habit grid and why date math is harder than it looks. |
| v1.0.0 | Demo video, what worked and what did not with an AI agent, and the numbers. |

- Capture as you go: a recording per milestone, counts (pull requests, tests, sessions), and mistakes caught in review.
- State the division of labour plainly: what Claude wrote, what the maintainer decided and reviewed. Commits keep their `Co-Authored-By` trailer.

## 12. Open decisions

| # | Decision | Needed by | Proposal | Status |
|---|---|---|---|---|
| 1 | App name and application ID | Before v0.1.0 | `io.github.<github-user>.<name>`; check the name is not taken by another launcher. | **Decided** 11 Nov 2026: Tengwar Minimal, `io.github.ivandietta.tengwarminimal`. Closed in M0 because M0 names the public repository and renames the template's package. |
| 2 | License | Before the first public commit | See note below. | **Decided** 11 Nov 2026: GPL-3.0. |
| 3 | Does this launcher's own foreground time count as screen time? | M2 | No: report time spent in other apps. | Open |
| 4 | Public from the first commit, or from v0.1.0? | M0 | From the first commit; the history is part of the showcase. | **Decided** 11 Nov 2026: public from the first commit. |
| 5 | Developer verification account type | Before v1.0.0 | Limited distribution while testing; decide on full distribution for the public release. | Open |
| 6 | LinkedIn posts in Spanish, English or both | First post | Maintainer's call. | Open |
| 7 | Static analysis in CI besides Android Lint | M0 | ktlint. detekt's stable line (1.x) lags behind current Kotlin, and 2.0 is still in alpha. | **Decided** 11 Nov 2026: ktlint, recorded in ADR-0001. |
| 8 | Android system backup. The template sets `allowBackup="true"`, which lets Android copy app data to the user's cloud backup. | Before M2 stores data | Exclude app data from cloud backup and keep device-to-device transfer. Export and import (M5) stays the supported way to move data. | Open |

**License note.** Because the code is written from scratch, any open-source license is available, and while the maintainer is the only author it can be changed for future versions. The practical question is what others may do with it. GPL-3.0: anyone who distributes a modified version must publish its source, so derived apps stay open. Apache-2.0: anyone may reuse the code, including in closed apps, by keeping the license notice. The planned dependencies (AndroidX, Kotlin) are Apache-2.0, which can be combined into a GPL-3.0 app. Suggested: GPL-3.0 for an end-user app. This is not legal advice.

## 13. Risks

| Risk | Mitigation |
|---|---|
| Usage numbers disagree with Digital Wellbeing and users distrust them | One documented method, synthetic-stream tests, and an explanation of known differences. |
| Background snapshot is delayed or killed by the device maker's battery rules | Idempotent snapshot with backfill, and catch-up whenever the launcher is in the foreground. |
| A crash in the launcher leaves the user without a home screen | Keep startup minimal, guard platform calls, test on a low-end device. |
| Scope creep toward blocking | Strong blocking is explicitly after v1. |
| Sideloading gets harder as developer verification expands | Register before the public release (section 10). |
| The signing key is lost | Two backups, documented in a private note, never in the repo. |
| Merged code the maintainer cannot explain | The ownership rules in section 9. |

## 14. References

- Claude Code: [best practices](https://code.claude.com/docs/en/best-practices), [how Claude remembers your project](https://code.claude.com/docs/en/memory), [desktop quickstart](https://code.claude.com/docs/en/desktop-quickstart)
- Android: [package visibility](https://developer.android.com/training/package-visibility/declaring), [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager), [UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event), [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager)
- Distribution: [Android developer verification](https://support.google.com/android-developer-console/answer/16561738), [verification guides](https://developer.android.com/developer-verification/guides)
