# Tengwar Minimal

A minimalist Android launcher with on-device screen-time stats and a habit tracker.

> **Status:** early development. The project foundation (milestone M0) is in progress, and there is no release to install yet.

## Privacy

Tengwar Minimal never declares the `android.permission.INTERNET` permission, so it cannot reach the network on its own. CI fails any change that adds it, including through a library. There are no accounts, analytics, ads or crash reporting.

## Build from source

You need Android Studio, or JDK 17 or later together with the Android SDK.

```bash
./gradlew assembleDebug
```

On Windows, run `.\gradlew.bat assembleDebug` instead.

## Documentation

- [Project brief](docs/PROJECT_BRIEF.md): scope, technical decisions and roadmap.
- [CLAUDE.md](CLAUDE.md): working rules for Claude Code, the AI coding agent used to build this project.

## License

Tengwar Minimal is free software, released under the [GNU General Public License v3.0](LICENSE).
