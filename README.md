# SPPU Result Watch

**Free and Open Source (FOSS) Android application for students of Savitribai Phule Pune University (SPPU)**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/thepinak503/sppuresult-android)

---

## ⚠️ NON-AFFILIATION NOTICE

**This application is NOT affiliated, associated, authorized, endorsed by, or in any way officially connected with Savitribai Phule Pune University (SPPU), or any of its subsidiaries, departments, or affiliates.**

This is an independent, community-developed application.

**Official SPPU website: [www.unipune.ac.in](https://www.unipune.ac.in)**

---

## Features

- Browse all published exam results from SPPU's official portal
- Filter results by department (FE, SE, TE, BE, MBA, MCA, B.Sc, B.Com, Law, and more)
- Search functionality with fuzzy matching
- View individual results using your seat number and mother's name (CAPTCHA verification required)
- Download result PDFs directly to your device
- Pull-to-refresh to check for newly published results
- Background notifications for new results
- High refresh rate support (120Hz/144Hz) on capable devices
- Scroll-to-top by re-selecting bottom navigation items

---

## Privacy & Data Handling

### What data enters this app:
- Result listings fetched from SPPU's official portal
- Seat number and mother's name (entered by you to view individual results)
- CAPTCHA images from SPPU's server

### What is stored locally on YOUR device:
- Basic result metadata (title, date, URL) in local SQLite database
- Downloaded result files (in your device's Downloads folder)

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

## Download

APKs are available in the [Releases](https://github.com/thepinak503/sppuresult-android/releases) section.

---

## Building from Source

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or newer
- Android SDK with compileSdk 36

### Build
```bash
git clone https://github.com/thepinak503/sppuresult-android.git
cd sppuresult-android
./gradlew assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Jetpack Navigation Compose
- **Dependency Injection:** Hilt
- **Local Database:** Room
- **Background Work:** WorkManager
- **Network/Scraping:** Jsoup
- **Image Loading:** Coil

---

**Disclaimer Last Updated: May 2026**
