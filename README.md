# BetterGlucoDash

**BetterGlucoDash** is a premium, privacy-first Android application designed for Continuous Glucose Monitor (CGM) users who demand superior analytics and high-fidelity data visualization. 

Unlike standard manufacturer apps, BetterGlucoDash focuses on "Insight through Motion," leveraging Google's **Material 3 Expressive** design system to turn raw data into actionable health metrics—all processed exclusively on your device.

---

## 🚀 Key Features

### 💎 Material 3 Expressive UI
- **OLED Optimized**: A deep navy/slate dark mode designed for high contrast and battery efficiency.
- **Fluid Motion**: Spring-physics animations for all transitions, card entries, and data updates.
- **Edge-to-Edge Layout**: Fully immersive experience that utilizes every pixel of your modern Android display.

### 📊 Advanced Analytics
- **Interactive Glucose Graph**: A custom-drawn Canvas renderer featuring smooth cubic bezier curves and an interactive scrubbing crosshair.
- **ADA-Compliant A1C**: Estimated HbA1c based on the **ADAG 90-day scientific standard** (requires 90 days of local data).
- **24h Glycaemic Control**: Real-time 24-hour average glucose tracking with data integrity checks.
- **Trend Intelligence**: Precise direction indicators (Rising Fast, Stable, Falling, etc.) synchronized with LibreLinkUp.

### 🔒 Privacy & Security
- **Local Source of Truth**: All glucose data is stored in a local **Room SQLite** database.
- **No Third-Party Servers**: Your health data is pulled directly from the source and never sent to our servers or any third party.
- **Encrypted Storage**: Authentication tokens and regional preferences are secured using Android's latest encryption standards.

---

## 🛠 Tech Stack

- **UI Layer**: Jetpack Compose, Material 3 Expressive, Inter Typeface.
- **Architecture**: Clean Architecture with MVVM + Unidirectional Data Flow (UDF).
- **Data Layer**: Room (SQLite), Retrofit, Kotlinx Serialization.
- **DI**: Hilt (Dependency Injection).
- **Concurrency**: Kotlin Coroutines & Flow.

---

## 📍 Regional Support

Currently supports **US** (`api-us.libreview.io`) and **Canada** (`api-ca.libreview.io`) regions for FreeStyle Libre 3 users. Units (mg/dL or mmol/L) are automatically locked based on your login region to ensure clinical accuracy.

---

## ⚠️ Medical Disclaimer

**BetterGlucoDash is NOT for medical decisions.** This application is intended for supplemental analytics only. Always refer to your primary CGM application and consult with your healthcare provider before making any changes to your medication or treatment plan. Use of this application for insulin dosing is strictly prohibited.

---

## 🏗 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 28+ (Minimum) / Android SDK 35 (Target)
- A valid LibreLinkUp account (US or Canada)

### Building
1. Clone the repository.
2. Open in Android Studio.
3. Sync Project with Gradle Files.
4. Run on a physical device or emulator (API 28+).

---

## 📄 License

This project is for educational and personal use. Reverse engineering of 1st-party APIs is performed for interoperability and supplemental analytics as per standard developer guidelines.
