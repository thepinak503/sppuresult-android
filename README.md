# SPPU Result Watch

**Free and Open Source (FOSS) Android application for students of Savitribai Phule Pune University (SPPU)**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/thepinak503/sppuresult-android)
![Version](https://img.shields.io/badge/version-1.5.0-blue)

---

## ⚠️ NON-AFFILIATION NOTICE

**This application is NOT affiliated, associated, authorized, endorsed by, or in any way officially connected with Savitribai Phule Pune University (SPPU), or any of its subsidiaries, departments, or affiliates.**

This is an independent, community-developed application.

**Official SPPU website: [www.unipune.ac.in](https://www.unipune.ac.in)**

---

## Features

### 📋 Smart Notifications (v1.4.0)
- **Subscribe to Departments** — Tap departments (FE, SE, TE, BE, MBA, B.Sc, B.Com, Law, Pharmacy, Architecture, and 20+ more) to only get notified about results for your subscribed branches
- **Priority Watchlist** — Set high-importance keywords for results that matter most (custom sound & vibration)
- **Smart Watchlist** — Free-form keyword matching with 200+ suggested keywords covering every SPPU program
- **Zero notification spam** — No notifications unless you explicitly subscribe or add keywords (no more 100+ unwanted notifications)
- **Background sync** — Periodic checks for new results, revaluation courses, exam dates, and circulars

### 📊 Result Browsing
- Browse all published exam results from SPPU's official portal
- Filter results by department (FE, SE, TE, BE, MBA, MCA, M.Sc, B.Sc, B.Com, BBA, B.A., B.Pharm, Law, Diploma, and more)
- Search with fuzzy matching (character-level subsequence matching)
- Sort by newest, oldest, or alphabetically
- Pull-to-refresh to check for newly published results
- High refresh rate support (120Hz/144Hz) on capable devices
- Scroll-to-top by re-selecting bottom navigation items
- Lazy scrollbar for fast navigation through long lists

### 📄 Result Viewing
- View individual results using your seat number and mother's name
- CAPTCHA verification with refresh support
- Download result PDFs directly to your device
- Auto-fill from saved profiles
- Saved profile management (multiple profiles supported)
- Bookmark results for quick access
- Vault: view and manage all downloaded results

### 🔄 Background Sync
- **Result Sync** — Periodic checking for new published results
- **Revaluation Sync** — Monitor new revaluation courses
- **Exam Dates Sync** — Get notified when exam form dates are updated
- **Circular Sync** — Receive new university circulars
- Configurable sync intervals (down to 1 minute)
- Boot receiver — reschedules sync after device reboot
- Foreground service for reliable background operation
- Server health monitoring with automatic retry on failure

### 🔍 Revaluation & Circulars
- Integrated revaluation result search and browsing
- Department filters for revaluation courses
- University circulars from 3 RSS feeds (Exam, Important, Academic Calendar)
- Exam form dates with start/end/late fee details

### 🎨 User Experience
- Material 3 design with dynamic color (Android 12+)
- Theme options: System, Light, Dark, Pitch Black (OLED)
- Language support: English, Marathi (मराठी)
- Animated transitions between screens
- Server status indicator (green/yellow/red dot)
- Pull-to-refresh with Material3 design
- App shortcuts for quick actions
- Calculator: SGPA/CGPA to percentage conversion
- 25+ curated SPPU links in the Links section
- Onboarding flow for first-time users

### 🔐 Privacy & Security
- **Zero third-party data transmission** — all communication goes directly to SPPU servers
- No analytics, no telemetry, no crash reporting
- No personal data stored outside your device
- Seat numbers and mother's names are sent ONLY to SPPU via HTTPS
- Support for biometric authentication
- Custom CA certificate pinning for SPPU HTTPS
- Data backup & restore (JSON export/import)
- Profiles stored encrypted on-device

---

## Tech Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Navigation** | Jetpack Navigation Compose |
| **Architecture** | MVVM + Repository Pattern |
| **DI** | Hilt |
| **Local DB** | Room |
| **Preferences** | DataStore |
| **Background Work** | WorkManager |
| **Scraping** | Jsoup + HttpURLConnection |
| **Image Loading** | Coil |
| **Serialization** | kotlinx-serialization |
| **Widget** | Glance App Widget |
| **Min / Target SDK** | 24 / 36 |
| **Build System** | Gradle 9.4.1 + AGP 9.2.1 |

---

## Download

APKs are available in the [Releases](https://github.com/thepinak503/sppuresult-android/releases) section.

[![Download Latest](https://img.shields.io/github/v/release/thepinak503/sppuresult-android?label=latest%20release)](https://github.com/thepinak503/sppuresult-android/releases/latest)

---

## Building from Source

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17 or newer
- Android SDK with compileSdk 36

### Debug Build
```bash
git clone https://github.com/thepinak503/sppuresult-android.git
cd sppuresult-android
./gradlew assembleDebug
```

APK at `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```
APK at `app/build/outputs/apk/release/app-release.apk`

---

## Privacy & Data Handling

### What data enters this app:
- Result listings fetched from SPPU's official portal
- Seat number and mother's name (entered by you to view individual results)
- CAPTCHA images from SPPU's server

### What is stored locally on YOUR device:
- Basic result metadata (title, date, URL) in local SQLite database
- Downloaded result files (in your device's Downloads folder)
- Your profile data (encrypted in DataStore)

### What is NEVER transmitted to third parties:
- Your seat number
- Your mother's name
- Your downloaded results
- Any personal information

**All communication happens ONLY between your device and SPPU's official servers. No data is sent to any developer, third-party, or analytics server. No tracking cookies, no analytics, no telemetry.**

---

## Legal Disclaimer

### 1. Ownership of Content

All examination results, circulars, notifications, and related content displayed through this app are the **exclusive intellectual property of Savitribai Phule Pune University (SPPU)**. This app merely acts as a browser/viewer for publicly accessible content on SPPU's official servers.

### 2. Fair Use Purpose

This application is provided for **educational, non-commercial purposes only**. It is designed to assist students in conveniently accessing their exam results without having to navigate the official portal manually.

### 3. Accuracy of Information

While we strive to display accurate and up-to-date information, this app **does NOT guarantee the accuracy, completeness, or timeliness** of any data displayed. **The official SPPU portal shall always be considered the sole authoritative source for result verification.**

### 4. Limitation of Liability

Under no circumstances shall the developer(s), contributor(s), or anyone associated with SPPU Result Watch be liable for:
- Any direct, indirect, incidental, or consequential damages
- Result discrepancies or errors in displayed information
- Server downtime or network connectivity issues
- Decisions made based on information from this app

### 5. No Warranty

THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.

### 6. User Responsibilities

By using this app, you acknowledge and agree that:
- You will use this app only for legitimate academic purposes
- You will not use this app for any illegal or unauthorized purpose
- You understand that result viewing requires entering your seat number and mother's name — this data is transmitted ONLY to SPPU's servers over HTTPS
- You will verify critical information (results, dates, deadlines) through official SPPU channels
- You will not misuse, redistribute, or sell any data obtained through this app

### 7. Governing Law

This disclaimer and any disputes arising shall be governed by the laws of India. Any legal proceedings shall be subject to the exclusive jurisdiction of courts in Pune, Maharashtra.

---

## Copyright

**Copyright © 2026 Savitribai Phule Pune University. All rights reserved.**

All result content, logos, and university-related trademarks displayed through or accessible via this app remain the sole property of Savitribai Phule Pune University.

---

**Disclaimer Last Updated: June 2026**
