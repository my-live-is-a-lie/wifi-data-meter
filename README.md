<div align="center">

# 📶 Wi‑Fi Data Meter

### Stay ahead of your data usage.

An Android app for tracking Wi‑Fi usage, managing data plans, and seeing where your data goes.

[![Version](https://img.shields.io/github/v/release/my-live-is-a-lie/mywifiaData?include_prereleases=true&style=for-the-badge&color=7c83fd)](https://github.com/my-live-is-a-lie/mywifiaData/releases)
[![Downloads](https://img.shields.io/github/downloads/my-live-is-a-lie/mywifiaData/total?style=for-the-badge&color=8b6de9)](https://github.com/my-live-is-a-lie/mywifiaData/releases)
![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Preview](https://img.shields.io/badge/status-preview%20%7C%20in%20development-e7b7ff?style=for-the-badge)

<a href="https://github.com/my-live-is-a-lie/mywifiaData/releases/download/v1.0.1-preview.1/app-universal-release.apk">
  <img src="https://img.shields.io/badge/Download-Universal%20APK-5264d8?style=for-the-badge&logo=android&logoColor=white" alt="Download the universal APK" />
</a>
&nbsp;
<a href="https://github.com/my-live-is-a-lie/mywifiaData/releases/tag/v1.0.1-preview.1">
  <img src="https://img.shields.io/badge/All%20architectures-Release%20files-454a5c?style=for-the-badge&logo=github&logoColor=white" alt="View all APK builds" />
</a>

**Early preview · Actively in development · Android 8.0+**

</div>

> ⚠️ This is an early preview and may contain bugs or unfinished features. The repository is private, so downloads require a GitHub account with repository access.

## ✨ Features

- **Usage dashboard:** plan usage, remaining data, cycle days, and upload/download totals.
- **Per-app analytics:** review app data usage with time-period filters, app filters, and sorting.
- **Usage history:** visual charts and summaries for recent days and billing periods.
- **Live floating window:** keep real-time usage visible while using other apps; monitor all apps or a selected app.
- **Internet speed test:** run a speed test and revisit previous results.
- **Background monitoring:** usage notifications, quota alerts, and a home-screen widget.
- **Flexible plans:** set your data allowance and billing-cycle start date.
- **Personalization:** Arabic and English interfaces, plus light and dark themes.

## 📱 Screenshots

<div align="center">
<table>
  <tr>
    <td align="center"><img src="docs/screenshots/home.jpg" width="210" alt="Home dashboard" /></td>
    <td align="center"><img src="docs/screenshots/apps.jpg" width="210" alt="App usage analytics" /></td>
    <td align="center"><img src="docs/screenshots/history.jpg" width="210" alt="Usage history" /></td>
    <td align="center"><img src="docs/screenshots/settings.jpg" width="210" alt="Settings" /></td>
  </tr>
  <tr>
    <td align="center">Home</td>
    <td align="center">Apps</td>
    <td align="center">History</td>
    <td align="center">Settings</td>
  </tr>
</table>
</div>

## ⬇️ Download & compatibility

The release includes a **universal APK** (recommended if you are unsure which file to choose) and ABI-specific builds for **ARM64**, **ARMv7**, **x86_64**, and **x86**.

- [Download the universal APK](https://github.com/my-live-is-a-lie/mywifiaData/releases/download/v1.0.1-preview.1/app-universal-release.apk)
- [View the release and all APKs](https://github.com/my-live-is-a-lie/mywifiaData/releases/tag/v1.0.1-preview.1)

Per-app usage requires Android's **Usage access** permission. The floating window also requires permission to display over other apps.

## 🛠️ Built with

**Kotlin · Jetpack Compose · Material 3 Expressive.** GitHub Actions builds and signs the release APKs, runs unit tests, and publishes the assets to GitHub Releases. Releases can be started manually, by pushing a `v*` tag, or from an `arena/*` branch with `[publish-release]` in the commit message.
