# H2S Dose Reader

Android app (Kotlin + Jetpack Compose) that reads chemically-treated H2S exposure strips via the
phone camera, applies adaptive color correction against on-strip reference patches, calculates a
PPM dose, logs it locally (Room) and syncs it to Firebase (Firestore + FCM).

## Prerequisites already in place

- OpenCV Android SDK vendored under `sdk/` (module `:sdk`), built via CMake/NDK.
- Firebase project wired up: `app/google-services.json` present, package
  `com.example.xposuredetectorsmart`.

## Building

```
./gradlew :app:assembleDebug
```

Install on a connected device/emulator:

```
./gradlew :app:installDebug
```

A real device (or an emulator with a working camera feed) is required to test the QR scan and
capture flow end-to-end - the emulator's simulated camera is enough to exercise the pipeline, but
strip color accuracy obviously depends on a real camera and real strips.

## Testing

- `./gradlew :app:testDebugUnitTest` - pure JVM tests (QR parsing, dose interpolation, color
  correction math, Firestore document mapping). No device needed.
- `./gradlew :app:connectedDebugAndroidTest` - instrumented tests (Room CRUD, the OpenCV-backed
  reference-patch detector, a Compose UI smoke test). Needs a connected device/emulator.
- `FirebaseIntegrationTest` additionally needs the Firestore emulator running first:
  `firebase emulators:start --only firestore` (it points at `10.0.2.2:8080`, never production).

## Trying it out

1. Generate a QR code encoding `h2s-dose:WRK_1001|2026-08-26|LocationA|morning` (any QR generator).
2. Launch the app, scan that code.
3. Point the camera at anything with a light patch, a mid-grey patch, and a saturated dark patch
   in frame (a printed color card works for a dry run) and tap **Capture Strip**.
4. The Results screen shows the interpolated PPM, confidence score, and detected patches.
5. Dashboard shows the running shift total against OSHA PEL/IDLH lines; Settings can export a PDF
   report and toggle dark mode / biometric lock.

## Notable decisions / deviations from the original spec

- **Dependency versions**: the spec pinned Kotlin 1.9.0 / AGP 8.1.0 / Hilt 2.46 / OpenCV 4.8.0.
  The project as handed off already used a much newer, pre-existing toolchain (AGP 9.3.2, Kotlin
  2.2.10, OpenCV 5.0.0 vendored under `sdk/`) - downgrading would have broken what was already
  building. All *other* libraries (Room, Hilt, CameraX, ML Kit, Firebase BOM, WorkManager,
  DataStore, Biometric, Navigation) were pinned to the latest stable release compatible with that
  toolchain, verified against Maven metadata before use.
- **AGP 9's "built-in Kotlin"**: this project doesn't apply the classic
  `org.jetbrains.kotlin.android` plugin - AGP compiles Kotlin itself. KSP (used for Room/Hilt
  codegen) registers generated sources through an API that conflicts with that mode; the
  documented workaround (`android.disallowKotlinSourceSets=false` in `gradle.properties`) is
  applied. This is an AGP/KSP interop wrinkle, not an app bug.
- **PDF export uses Android's built-in `PdfDocument`**, not iText. iText 7+ is AGPL-licensed,
  which would obligate open-sourcing this app (or buying a commercial license) - not something to
  decide silently on your behalf. The report includes a self-drawn cumulative-exposure chart with
  OSHA PEL/IDLH reference lines, a per-capture table, and a disclaimer footer.
- **WorkManager's minimum periodic interval is 15 minutes** (OS-enforced), not 30 seconds. Real
  near-real-time sync is covered two other ways: every capture attempts an immediate upload right
  away if online, and connectivity regaining triggers an immediate one-off sync; the 15-minute
  periodic job is just the offline-catch-up safety net underneath both.
- **No login screen**: workers are identified by QR scan, not credentials. The app signs in
  anonymously to Firebase Auth on first sync so Firestore's `request.auth != null` security rules
  (see `firestore.rules`) have something to check.
- **RGB->PPM lookup table** (`DoseCalculator`) uses placeholder reference colors describing a
  plausible white->dark-brown gradient. Replace with the actual manufacturer-calibrated swatch
  values for your strip lot before relying on this for real safety decisions.
- **Results screen** dropped the spec's separate "Adjust" button - re-running color correction on
  the same capture without new inputs is deterministic (same output) and re-saving would create a
  duplicate dose log, so "Retry" (recapture) and "Done" cover the real use cases.
- **No `RepositoryModule.kt`/`ProcessingModule.kt`**: every repository and image-processing class
  uses `@Inject constructor` directly (no interface indirection needed yet), so Hilt provides them
  without a hand-written `@Provides` module. Add one if/when you introduce interfaces with
  swappable implementations (e.g. for testing with fakes).
