# SmartVision AI — Step-by-Step Setup Guide

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35 (API 35)
- A Firebase account
- A Google AI Studio account for Gemini API key

---

## Step 1 — Clone / Open Project

```bash
# Unzip the downloaded archive
unzip SmartVisionAI_Final.zip -d SmartVisionAI
cd SmartVisionAI
```

Open in Android Studio:
`File → Open → select the SmartVisionAI folder`

---

## Step 2 — Firebase Setup

### 2a. Create Firebase Project
1. Go to https://console.firebase.google.com
2. Click **Add project** → name it "SmartVision AI"
3. Enable Google Analytics (optional)

### 2b. Register Android App
1. Click the **Android** icon
2. Package name: `com.smartvision.ai`
3. App nickname: `SmartVision AI`
4. Add your **SHA-1** fingerprint (see Step 2c)
5. Click **Register App**

### 2c. Get SHA-1 Fingerprint
```bash
# In the project root:
./gradlew signingReport

# Output will show:
# SHA1: XX:XX:XX:...:XX  ← copy this
```
Paste it in Firebase Console → Project Settings → Your Apps → Add fingerprint

### 2d. Download google-services.json
1. Firebase Console → Project Settings → **Download google-services.json**
2. Move it to: `app/google-services.json`

### 2e. Enable Firebase Services
In Firebase Console:

| Service | How to enable |
|---------|--------------|
| Authentication | Build → Authentication → Get Started → Enable **Google** and **Email/Password** |
| Firestore | Build → Firestore → Create database → Start in **production mode** → choose region |
| Storage | Build → Storage → Get started → choose region |

### 2f. Apply Firestore Security Rules
Firebase Console → Firestore → Rules tab → paste from `firebase.rules`:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /scan_history/{docId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == resource.data.userId;
    }
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## Step 3 — Gemini API Key

1. Go to https://aistudio.google.com/app/apikey
2. Click **Create API key**
3. Copy the key

### Add to local.properties
```properties
# app/local.properties  (THIS FILE IS GITIGNORED — never commit it)
GEMINI_API_KEY=AIzaSy...your_actual_key_here
```

### Read in build.gradle.kts
In `app/build.gradle.kts`, inside `android { defaultConfig { ... } }`:
```kotlin
val properties = java.util.Properties()
properties.load(rootProject.file("local.properties").inputStream())

buildConfigField(
    "String",
    "GEMINI_API_KEY",
    "\"${properties["GEMINI_API_KEY"]}\""
)
```

---

## Step 4 — Google Sign-In Setup

### 4a. Get Web Client ID
1. Firebase Console → Project Settings → General
2. Scroll to "Your apps" → Web API Key section
3. OR go to Google Cloud Console → APIs & Services → Credentials
4. Copy the **Web client OAuth 2.0 client ID**

### 4b. Add to strings.xml
```xml
<!-- app/src/main/res/values/strings.xml -->
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com</string>
```

### 4c. Wire Google Sign-In in LoginScreen
```kotlin
// In LoginScreen.kt — replace the signInWithGoogle() button's onClick:
val webClientId = stringResource(R.string.default_web_client_id)
val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(webClientId)
    .requestEmail()
    .build()
val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    try {
        val account = task.getResult(ApiException::class.java)
        account?.idToken?.let { viewModel.signInWithGoogleToken(it) }
    } catch (e: ApiException) { /* handle */ }
}

// In the button:
OutlinedButton(onClick = { launcher.launch(googleSignInClient.signInIntent) }) {
    Text("Continue with Google")
}
```

---

## Step 5 — Build & Run

```bash
# Sync Gradle
./gradlew dependencies

# Build debug APK
./gradlew assembleDebug

# Run on connected device
./gradlew installDebug
```

Or press **▶ Run** in Android Studio.

---

## Step 6 — Run Tests

```bash
./gradlew test                  # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests (requires device)
```

---

## Common Errors & Fixes

| Error | Fix |
|-------|-----|
| `google-services.json not found` | Make sure it's in `app/` not the root |
| `GEMINI_API_KEY not found` | Add `GEMINI_API_KEY=...` to `local.properties` |
| `SHA1 mismatch` for Google Sign-In | Re-run `./gradlew signingReport`, update in Firebase |
| `ML Kit model download failed` | Make sure device has internet on first run |
| `Hilt: @AndroidEntryPoint missing` | Ensure `SmartVisionApplication` has `@HiltAndroidApp` |
| `CameraX: Surface abandoned` | Call `provider.unbindAll()` before rebinding |
| Build error: `duplicate class` | Check for conflicting Firebase/GMS versions in dependencies |

---

## Project File Map

```
SmartVisionAI/
├── app/
│   ├── google-services.json          ← YOU MUST ADD THIS
│   ├── build.gradle.kts              ← all dependencies
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/smartvision/ai/
│       │   ├── MainActivity.kt
│       │   ├── MainActivityFinal.kt  ← Use this one (with DataStore)
│       │   ├── di/
│       │   │   ├── AppModule.kt
│       │   │   ├── UseCaseModule.kt  ← complete binding of all use cases
│       │   │   └── FirebaseModule.kt
│       │   ├── domain/
│       │   │   ├── models/Models.kt
│       │   │   └── usecase/UseCases.kt
│       │   ├── data/repository/
│       │   │   ├── ResultRepositoryImpl.kt
│       │   │   ├── MedicalScanUseCaseImpl.kt
│       │   │   └── UserPreferencesRepository.kt
│       │   ├── utils/
│       │   │   ├── ImageUtils.kt
│       │   │   └── Utils.kt
│       │   └── ui/
│       │       ├── theme/Theme.kt
│       │       ├── components/
│       │       │   ├── Components.kt
│       │       │   └── GalleryPicker.kt
│       │       ├── navigation/Navigation.kt
│       │       └── screens/
│       │           ├── auth/         LoginScreen + AuthViewModel
│       │           ├── home/         HomeScreen
│       │           ├── camera/       CameraScreen + CameraViewModel
│       │           ├── result/       ResultScreen + ResultViewModel
│       │           ├── ocr/          OcrScreen + OcrViewModel
│       │           ├── objectdetection/ ObjectDetectionScreen + VM
│       │           ├── qrscanner/    QrScannerScreen + QrScannerViewModel
│       │           ├── translator/   TranslatorScreen + TranslatorViewModel
│       │           ├── student/      StudentHelperScreen + VM
│       │           ├── settings/     SettingsScreen + SettingsViewModel
│       │           └── history/      HistoryScreen + HistoryViewModel
│       └── res/values/
│           ├── themes.xml
│           ├── colors.xml
│           └── strings.xml
├── firebase.rules                    ← paste into Firebase Console
├── gradle/libs.versions.toml
├── gradle.properties
└── README.md
```

---

## Performance Checklist

- [ ] Use `ImageUtils.scaleBitmap(bitmap, 1024)` before sending to ML Kit
- [ ] Enable `android.enableR8.fullMode=true` in `gradle.properties` for smaller APK
- [ ] Set `liveDetection = false` by default (power-intensive)
- [ ] Use `Dispatchers.Default` for bitmap processing in ViewModels
- [ ] Coil's `AsyncImage` handles disk caching automatically
- [ ] `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` prevents frame queue buildup

## APK Size Optimisation

```kotlin
// In app/build.gradle.kts:
android {
    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }
}
```

Use **AAB (Android App Bundle)** for Play Store → 40–60% smaller downloads.
