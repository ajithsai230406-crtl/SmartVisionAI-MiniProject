# Smart Vision AI

Smart Vision AI is a Kotlin + XML Android mini project with a premium dark-blue futuristic UI, MVVM architecture, CameraX, ML Kit OCR/Translation, SpeechRecognizer, TextToSpeech, Room, DataStore, Firebase Auth hooks, TensorFlow Lite task-vision integration and full Navigation Component flow.

## Build

Open this folder in Android Studio and let Gradle sync. The verified command is:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=2
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Firebase

The project includes Firebase Auth and Google sign-in wiring. For a real Google login, replace `app/google-services.json` with the file from your Firebase project using package `com.smartvision.ai`, then add the app SHA-1/SHA-256 keys in Firebase Console.

## Modules

- Splash and onboarding with Lottie pulse animation.
- Login/signup with Firebase-ready Google connection.
- Dashboard with animated particle background and AI feature cards.
- OCR scanner using CameraX and ML Kit Text Recognition.
- Translator using ML Kit Translation and Text-to-Speech.
- Voice assistant using Android SpeechRecognizer and TTS commands.
- Medicine and waste camera screens with TensorFlow Lite task-vision integration and safe demo fallback results.
- AI chat assistant with suggested prompts and typing delay.
- Room-backed scan/history storage and DataStore settings.
- Profile, Settings, About, Permission, Loading, Success and Error screens.

## Viva Notes

The app follows MVVM and repository pattern. UI state lives in ViewModels, data is stored through repositories, local history uses Room, settings use DataStore, and AI/camera modules are isolated for easy explanation during viva.
