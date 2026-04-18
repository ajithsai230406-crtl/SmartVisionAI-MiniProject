# 🤖 Smart Vision AI — Android App

> A next-generation multi-modal AI assistant combining Computer Vision, NLP, and Speech processing.
> Built with Kotlin + Jetpack Compose | CameraX | ML Kit | TensorFlow Lite | Gemini API

---

## 📱 Screenshots

| Home / Modules | Scan Mode | History | Settings |
|---|---|---|---|
| Module grid with animated radar hero | CameraX preview + mode chips | Recent scan list | Toggles + performance mode |

---

## ✨ Features

| Module | Technology | Online/Offline |
|---|---|---|
| 🔍 Object Detection | ML Kit + TFLite EfficientDet | Offline ✅ |
| 📄 Text Scanner (OCR) | ML Kit Text Recognition | Offline ✅ |
| 🌐 Translator | ML Kit Translate | Offline (packs) |
| 🎓 Student Helper | Gemini API | Online 🌐 |
| 💊 Medical Scanner | Gemini API + OCR | Online 🌐 |
| ♻ Waste Classifier | TFLite custom model | Offline ✅ |
| 🎤 Voice I/O | Android SpeechRecognizer + TTS | Offline ✅ |

---

## 🚀 Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android device/emulator with API 26+

### Step 1 — Clone / Import Project
```bash
git clone https://github.com/yourname/SmartVisionAI
# OR open the folder directly in Android Studio
```

### Step 2 — Add your Gemini API Key

1. Go to [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
2. Create a free API key
3. Open `app/build.gradle` and replace:
```gradle
buildConfigField "String", "GEMINI_API_KEY", "\"YOUR_GEMINI_API_KEY_HERE\""
```
with your actual key:
```gradle
buildConfigField "String", "GEMINI_API_KEY", "\"AIza...your_key...\""
```

### Step 3 — Add TFLite Models (Optional — for offline object detection & waste classification)

Download and place these files in `app/src/main/assets/`:

| File | Source |
|---|---|
| `efficientdet_lite0.tflite` | [TF Hub EfficientDet-Lite0](https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1) |
| `waste_classifier.tflite` | Train your own or use a Kaggle waste dataset |

> **Without these files**, the app will automatically fall back to ML Kit (for object detection) and Gemini API (for waste classification). Everything still works!

### Step 4 — Sync and Build
```
File → Sync Project with Gradle Files
Build → Make Project
Run → Select your device
```

### Step 5 — Grant Permissions
On first launch, grant:
- 📷 Camera
- 🎤 Microphone (for voice input)

---

## 🏗 Architecture

```
com.smartvision.ai/
├── di/                     ← Hilt dependency injection
│   └── AppModule.kt
├── data/
│   ├── local/              ← Room DB + DataStore
│   │   ├── AppDatabase.kt
│   │   ├── ScanHistoryDao.kt
│   │   └── SettingsPreferences.kt
│   └── model/
│       └── ScanHistoryEntity.kt
├── repository/
│   └── ScanHistoryRepository.kt
├── ml/                     ← AI/ML processing
│   ├── MLKitRepository.kt  ← OCR, Object Detection, Translation
│   ├── GeminiRepository.kt ← Student, Medical, Waste (AI reasoning)
│   └── TFLiteRepository.kt ← On-device TFLite models
├── viewmodel/              ← MVVM ViewModels
│   ├── ScanViewModel.kt
│   ├── HistoryViewModel.kt
│   └── SettingsViewModel.kt
├── ui/
│   ├── theme/              ← Dark cyberpunk theme (#00E5CC cyan)
│   │   ├── Theme.kt
│   │   └── Typography.kt
│   ├── screens/            ← Full-screen Composables
│   │   ├── SplashActivity.kt
│   │   ├── HomeScreen.kt
│   │   ├── ScanScreen.kt
│   │   ├── HistoryScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── ModuleDetailScreen.kt
│   └── components/         ← Reusable Composables
│       ├── ObjectDetectionOverlay.kt
│       ├── VoiceOverlayScreen.kt
│       ├── LanguagePickerDialog.kt
│       ├── LoadingAnimation.kt
│       └── SmartCard.kt
├── utils/
│   ├── VoiceManager.kt     ← SpeechRecognizer + TTS
│   ├── ImageUtils.kt       ← Bitmap helpers
│   └── NetworkUtils.kt     ← Connectivity monitoring
├── SmartVisionNavHost.kt   ← Navigation graph + Bottom Nav
├── MainActivity.kt
└── SmartVisionApp.kt       ← Hilt application class
```

---

## 🎨 UI Design System

- **Primary Color:** `#00E5CC` (Cyan)
- **Background:** `#080C0D` (Near-black)
- **Cards:** `#111820` with `#1C2A32` borders
- **Typography:** Inter / System font, 400/500/700 weights
- **Animations:** Infinite pulsing radar rings, scan line, bounding boxes, ripple mic

---

## 📦 Key Dependencies

```gradle
// Jetpack Compose BOM 2023.10.01
// CameraX 1.3.1
// ML Kit: text-recognition, object-detection, image-labeling, translate
// TensorFlow Lite 2.13.0 + support 0.4.4
// Gemini AI SDK 0.2.2
// Hilt 2.48
// Room 2.6.1
// DataStore 1.0.0
// Accompanist Permissions 0.33.2
```

---

## ⚡ Performance Tips

- ML Kit object detection is throttled to **1 analysis per 2 seconds** to save battery
- Gemini API calls are only made on **manual capture**, not per frame
- TFLite models run **entirely on-device** — no network needed
- Images are **resized to 1024px max** before ML processing

---

## 🔒 Privacy

- No images are stored permanently
- Camera frames are processed in-memory only
- Gemini API calls send text only (no raw images unless you extend it)
- Scan history is stored locally in Room DB — never uploaded

---

## 🎓 Academic Use

This project demonstrates:
- **MVVM + Clean Architecture** with Repository pattern
- **Hilt dependency injection** across all layers
- **Jetpack Compose** declarative UI with custom animations
- **CameraX** real-time frame analysis pipeline
- **Multi-modal AI** (Vision + NLP + Speech)
- **On-device ML** for offline capability
- **Room + DataStore** for local persistence

---

## 📄 License

MIT License — Free to use for academic and personal projects.

---

*Built with ❤️ using Kotlin + Jetpack Compose*
