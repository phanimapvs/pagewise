<p align="center">
  <img src="assets/banner.svg" alt="Pagewise — your reading, page by page" width="100%">
</p>

<h1 align="center">📖 Pagewise</h1>

<p align="center">
  An Android reading tracker built with <b>Kotlin</b> and <b>Jetpack Compose</b>.<br>
  Track your books, time your reading sessions, hit your goals, and keep notes — all in a clean Material 3 interface. ✨
</p>

<p align="center">
  <img alt="Latest release" src="https://img.shields.io/github/v/release/phanimapvs/pagewise?color=d99a4e&label=release&logo=github">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Material 3" src="https://img.shields.io/badge/Material%203-6750A4?logo=materialdesign&logoColor=white">
  <img alt="minSdk 26" src="https://img.shields.io/badge/minSdk-26-orange">
</p>

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshots/01_home.png" width="30%" alt="Home — greeting, streak and current book">
  &nbsp;
  <img src="assets/screenshots/02_library.png" width="30%" alt="Library with status filters">
  &nbsp;
  <img src="assets/screenshots/06_book.png" width="30%" alt="Book detail with per-book stats">
</p>
<p align="center">
  <img src="assets/screenshots/04_stats.png" width="30%" alt="Stats — level, totals and progress chart">
  &nbsp;
  <img src="assets/screenshots/03_goals.png" width="30%" alt="Goals — daily, monthly and yearly targets">
  &nbsp;
  <img src="assets/screenshots/05_more.png" width="30%" alt="More — achievements, readathons, shop, settings">
</p>
<p align="center">
  <sub>🏠 Home&nbsp;·&nbsp;📚 Library&nbsp;·&nbsp;📖 Book detail&nbsp;&nbsp;|&nbsp;&nbsp;📊 Stats&nbsp;·&nbsp;🎯 Goals&nbsp;·&nbsp;⋯ More</sub>
</p>

---

## ✨ Features

- 📚 **Library & shelves** — organize your books across custom shelves, with cover art.
- ➕ **Add books fast** — search an online catalogue, scan an ISBN barcode, or capture details with the camera (ML Kit text recognition).
- ⏱️ **Reading timer** — a foreground-service session timer with optional ambient background sounds. 🎧
- 📊 **Stats & insights** — reading statistics, pace, and infographics for your habits.
- 🎯 **Goals, achievements & readathons** — set reading goals, earn achievements 🏆, and run readathons.
- 📝 **Notes** — per-book notes and highlights.
- 🤖 **Reading assistant** — in-app helper including word definitions lookup.
- 🎨 **Personalization** — light/dark themes 🌙, appearance settings, and a selection of reading fonts (Atkinson Hyperlegible, Literata, Lora, Nunito, Playfair).
- 🔔 **Reminders** — reading notifications.

## 🛠️ Tech stack

- 🧩 **Language:** Kotlin (JVM 17)
- 🎨 **UI:** Jetpack Compose + Material 3, Navigation Compose, Material Icons Extended
- 🏗️ **Architecture:** ViewModel + repository, Compose lifecycle runtime
- 💾 **Persistence:** Room (with KSP), DataStore Preferences
- ⚙️ **Background work:** WorkManager, foreground `ReadingService`
- 🌐 **Networking:** OkHttp + kotlinx.serialization, Coil for image loading
- 📷 **Camera / scanning:** CameraX, ML Kit Barcode Scanning & Text Recognition

## 📋 Requirements

- 📱 **minSdk:** 26 (Android 8.0)
- 🎯 **targetSdk / compileSdk:** 36
- 🔖 **Current version:** 2.0.0

## 🔨 Building

```bash
./gradlew assembleRelease
```

> ⚠️ The release build is currently signed with the local debug key so it can be installed for testing. Replace with a real release keystore before publishing to the Play Store.

## 🚀 Releases

Prebuilt APKs are available on the [Releases page](https://github.com/phanimapvs/pagewise/releases):

| Version | Download |
|---------|----------|
| 🆕 [v2.0](https://github.com/phanimapvs/pagewise/releases/tag/v2.0) *(latest)* | `Pagewise-2.0.apk` |
| 📦 [v1.0](https://github.com/phanimapvs/pagewise/releases/tag/v1.0) | `Pagewise-1.0.apk` |

## 🔐 Permissions

| Permission | Why |
|------------|-----|
| 🌐 `INTERNET` | Catalogue search, cover art, definitions |
| 📷 `CAMERA` | ISBN barcode scanning and text capture |
| 🔔 `POST_NOTIFICATIONS` | Reading reminders and session notifications |
| ⏱️ `FOREGROUND_SERVICE` | Keeps the reading timer running reliably |

<p align="center"><sub>Made with ☕ and 📚 — happy reading!</sub></p>
