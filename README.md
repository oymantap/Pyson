<div align="center">

  <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Pyson Logo" width="128" height="128" />

  # Pyson

  **High-Performance Modern Python Code Editor for Android**

  [![Version](https://img.shields.io/badge/version-1.0.0-007ACC.svg?style=for-the-badge)](https://github.com/your-username/Pyson/releases)
  [![Language](https://img.shields.io/badge/Kotlin-100%25-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![C++ Engine](https://img.shields.io/badge/Engine-C%2B%2B20-00599C.svg?style=for-the-badge&logo=cplusplus&logoColor=white)](#c-native-core)
  [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)](LICENSE)
  [![Build](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg?style=for-the-badge&logo=githubactions&logoColor=white)](.github/workflows/build.yml)

</div>

---

## 📌 Overview

**Pyson** adalah aplikasi editor kode Python untuk platform Android yang dirancang dengan fokus utama pada **performa tinggi**, **keringanan**, dan **tampilan antarmuka modern**.

Berbeda dengan editor mobile konvensional yang kerap terasa lambat (*laggy*) saat menangani file berukuran besar, **Pyson** menggunakan arsitektur *hybrid*: pemrosesan teks dan analisis token dilakukan langsung di layer **C++ Native (JNI)**, sementara seluruh tampilan UI dibangun secara Deklaratif menggunakan **Jetpack Compose (100% Kotlin)** tanpa bergantung pada file XML UI bawaan Android Legacy.

---

## ✨ Fitur Utama

- ⚡ **C++ Native Core Engine**: Pemrosesan parsing, analisis token, dan penanganan buffer teks secara instan menggunakan C++ native library (`NDK`).
- 🎨 **100% Jetpack Compose UI**: Tampilan antarmuka *dark mode* modern tanpa sepotong pun file XML UI.
- 🔢 **Real-time Line Counter**: Penomoran baris yang presisi dan responsif.
- ⌨️ **Quick Symbol Toolbar**: Baris pintasan simbol-simbol khas Python (`:`, `Tab`, `()`, `[]`, `=`, dll.) untuk mempercepat penulisan kode di papan ketik layar sentuh.
- 🤖 **Automated CI/CD**: Pembangunan dan kompilasi APK secara otomatis menggunakan **GitHub Actions** tanpa ketergantungan pada Android Studio IDE.

---

## 🛠️ Spesifikasi & Teknologi

| Komponen | Spesifikasi |
| :--- | :--- |
| **Versi Aplikasi** | `1.0.0` |
| **Package Name** | `com.pyson` |
| **Bahasa Utama UI** | Kotlin 1.9 (Jetpack Compose / Material 3) |
| **Engine Native** | C++20 / CMake 3.22+ via JNI |
| **Minimum SDK** | Android 7.0 (API Level 24) |
| **Target SDK** | Android 14 (API Level 34) |
| **Build System** | Gradle (Kotlin DSL) & GitHub Actions Workflows |
| **Lisensi** | Apache License 2.0 |

---

## 📜 Lisensi

Proyek ini dilisensikan di bawah **[Apache License 2.0](LICENSE)**. Anda bebas menggunakan, memodifikasi, dan mendistribusikan ulang kode ini sesuai dengan ketentuan lisensi Apache 2.0.

<div align="center">
<sub>Built with ❤️ for ultra-fast mobile coding experience.</sub>
</div>

---

## 📂 Struktur Proyek

```text
Pyson/
├── .github/
│   └── workflows/
│       └── build.yml          # GitHub Actions CI/CD Pipeline
├── app/
│   ├── build.gradle.kts       # Konfigurasi Gradle Module
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── cpp/
│           │   ├── CMakeLists.txt
│           │   └── pyson_engine.cpp # C++ Native Processing Engine
│           ├── java/com/pyson/
│           │   ├── MainActivity.kt # Jetpack Compose UI Entrypoint
│            │  └── NativeEngine.kt # JNI Bridge
│            │  │___TerminalCL.kt
│           └── res/
│               └── mipmap-hdpi/
│                   └── ic_launcher.png
├── build.gradle.kts           # Root Gradle Build Configuration
├── settings.gradle.kts        # Repository & Module Settings
└── README.md
