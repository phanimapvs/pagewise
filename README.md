<p align="center">
  <img src="assets/banner.png?v=3" alt="Pagewise — an Android reading tracker built with Kotlin and Jetpack Compose" width="100%">
</p>

<h1 align="center">Pagewise</h1>

<p align="center">
  An Android reading tracker built with <b>Kotlin</b> and <b>Jetpack Compose</b>.<br>
  Track your books, time your reading sessions, hit your goals, and keep notes — all in a clean Material 3 interface. ✨
</p>

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshots/01_home.png" width="30%" alt="Home — greeting, streak and current book">
  &nbsp;
  <img src="assets/screenshots/06_book.png" width="30%" alt="Book detail with per-book stats">
  &nbsp;
  <img src="assets/screenshots/05_more.png" width="30%" alt="More — reading report, achievements, readathons, shop, settings">
</p>
<p align="center">
  <sub>🏠 Home&nbsp;·&nbsp;📖 Book detail&nbsp;·&nbsp;⋯ More</sub>
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

> **Signing.** Release builds are signed with the app's release keystore when a `keystore.properties` file exists at the project root; without it (a fresh clone or CI) the build falls back to the debug key so it still compiles. The keystore and `keystore.properties` contain secrets and are **git-ignored** — never committed. To produce a signed release, create `keystore.properties` at the project root:
>
> ```properties
> storeFile=keystore/pagewise-release.jks
> storePassword=your-store-password
> keyAlias=pagewise
> keyPassword=your-key-password
> ```

## 🚀 Releases

Prebuilt APKs are available on the [Releases page](https://github.com/phanimapvs/pagewise/releases):

| Version | Download |
|---------|----------|
| 🆕 [v2.01](https://github.com/phanimapvs/pagewise/releases/tag/v2.01) *(latest)* | `Pagewise-2.01.apk` |
| 📦 [v2.0](https://github.com/phanimapvs/pagewise/releases/tag/v2.0) | `Pagewise-2.0.apk` |
| 📦 [v1.0](https://github.com/phanimapvs/pagewise/releases/tag/v1.0) | `Pagewise-1.0.apk` |

## 🔐 Permissions

| Permission | Why |
|------------|-----|
| 🌐 `INTERNET` | Catalogue search, cover art, definitions |
| 📷 `CAMERA` | ISBN barcode scanning and text capture |
| 🔔 `POST_NOTIFICATIONS` | Reading reminders and session notifications |
| ⏱️ `FOREGROUND_SERVICE` | Keeps the reading timer running reliably |

<p align="center"><sub>Made with ☕ and 📚 — happy reading!</sub></p>
