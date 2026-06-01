# SmartVision AI — Setup Guide

## Quick Start

### Prerequisites
| Tool | Version |
|------|---------|
| Android Studio | Ladybug (2024.2) or later |
| Android SDK | API 35 |
| JDK | 17 |
| Kotlin | 2.0+ |

---

## Step 1 — Open Project

1. Extract `SmartVisionAI_Updated.zip`
2. Open Android Studio → **Open** → select the extracted folder
3. Wait for Gradle sync to complete (~2-3 min first time)

---

## Step 2 — Firebase Setup

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add Project** → name it "SmartVision AI"
3. Enable **Google Analytics** (optional)
4. Click **Android** icon → register app:
   - Package name: `com.smartvision.ai`
   - App nickname: SmartVision AI
5. Download `google-services.json`
6. Place it in `app/` folder (replace the placeholder if it exists)
7. In Firebase Console:
   - **Authentication** → Sign-in methods → Enable **Email/Password** and **Google**
   - **Firestore Database** → Create database → **Start in test mode**
   - **Storage** → Get started → **Start in test mode**

---

## Step 3 — Build & Run

```bash
# Option A: Android Studio
# Click the green Run ▶ button

# Option B: Command line
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Option C: Install directly
./gradlew installDebug
```

---

## Step 4 — Run Tests

```bash
# Unit tests
./gradlew test

# Android instrumentation tests (device/emulator required)
./gradlew connectedAndroidTest
```

---

## Module Architecture

```
Splash → Onboarding → Login
              ↓
           Home Dashboard
         ↙   ↓   ↓   ↘   ↘
    OCR  Translate ObjDetect Medicine Waste
         ↓
     AI Chat ← (auto-opens modules from chat)
         ↓
    History / Profile / Settings / About
```

---

## Viva Preparation

### Key Questions & Answers

**Q: What architecture pattern does the app use?**
A: MVVM + Clean Architecture. Presentation (Compose UI + ViewModel), Domain (UseCases, Models), Data (Repository + Room + Firestore).

**Q: How is OCR implemented?**
A: Google ML Kit Text Recognition v2. The camera frame is passed as `InputImage` to `TextRecognizer`. Results come back asynchronously via Task API, then mapped to `TextBlock` domain models.

**Q: Why Hilt for dependency injection?**
A: Compile-time DI with zero reflection overhead. Scoped to `SingletonComponent`, `ActivityComponent`, or `ViewModelComponent` as needed. Simplifies testing with `@HiltAndroidTest`.

**Q: How does offline support work?**
A: Room is the source of truth. All scan history writes go to Room first, then sync to Firestore asynchronously. If offline, data stays in Room and syncs when connection restores.

**Q: How does the translator show original text?**
A: The `TranslatorScreen` displays the translated text, then below it shows the original source text labeled "Original (Lang)" so the user can compare both in their own script.

**Q: What is StateFlow and why use it?**
A: `StateFlow` is a hot flow that always holds the latest value, survives configuration changes when scoped to `viewModelScope`, and integrates with Compose `collectAsState()` for reactive UI updates without memory leaks.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `google-services.json not found` | Download from Firebase Console and place in `app/` |
| `Hilt compile error` | Run `./gradlew clean` then rebuild |
| `Camera not opening` | Grant camera permission in device Settings |
| `Build fails — kapt` | Ensure JDK 17 in File → Project Structure → SDK Location |
| `Room migration error` | App uninstall/reinstall (fallbackToDestructiveMigration is set) |

---

## APK Build for Presentation

```bash
# Debug APK (for demo)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (needs signing config)
./gradlew assembleRelease
```
