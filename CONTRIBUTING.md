# Contributing to WiFi Data Meter

Thank you for your interest in contributing! Every bug report, feature idea,
and pull request is welcome.

## Ways to contribute

- **Report bugs** using the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Suggest features** using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md).
- **Submit code** via a pull request (see below).
- **Report security issues privately** — see [SECURITY.md](SECURITY.md), never
  open a public issue for a vulnerability.

## Development setup

1. Install the latest stable [Android Studio](https://developer.android.com/studio).
2. Clone this repository and open it in Android Studio.
3. Let Gradle sync, then run the app on a device or emulator.

Release builds are signed automatically in CI using secrets — you don't need
any keystore to build and run the debug version locally.

## Pull request process

1. Create a branch from `main` (e.g. `feature/my-change` or `fix/my-fix`).
2. Keep pull requests focused: one change per PR.
3. Make sure the app builds and run a quick smoke test on a device or emulator.
4. Open the PR against `main` and fill in the PR template.
5. All PRs are **squash-merged**, so keep your commit history tidy but don't
   worry about perfect commits.

## Code style

- Kotlin, following the existing code style in the project.
- UI follows Material 3 Expressive (dark theme) conventions used throughout
  the app — keep new screens visually consistent.

## Ground rules

- **Never commit secrets, keystores, passwords, or API keys.** CI will block
  pushes containing secrets.
- Be respectful in issues, PRs, and discussions.
