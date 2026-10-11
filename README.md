# Tap on Phone Demo

Android portfolio app by Bruno Breda with Compose, a Kotlin checkout state machine, payment SDK integration, local HTTP reconciliation and optional Firebase Authentication / Firestore history.

**All payments and taps are simulated.** This app does not read NFC cards, accept bank cards, implement EMV or move money. No workplace SDK or terminal dependency is used.

## Build

JDK 17 with compiler, Android SDK 35, Android Studio. Minimum Android API 26.

```sh
./gradlew :core:test
./gradlew -PincludeAndroid=true :app:lintDebug :app:assembleDebug
```

Set `includeAndroid=true` in your user Gradle properties when opening Android Studio. CI includes JVM tests and Android lint/APK jobs. Gradle wrapper is included.

`payment-sdk` vendors the public Kotlin source from `brunobredadeveloper/kotlin-payment-sdk` commit `0071ea02a47d79b29d03ff76ae5c9376d8454913`, so no unpublished Maven dependency or sibling checkout is required. A future release can replace this pinned snapshot with a published artifact.

## Demonstration

1. Enter `25,90`, choose approve or decline, start checkout and press **Simular aproximação**.
2. To use the companion backend, run `merchant-payments-console/server.py` and enable Merchant Console.
3. Emulator URL: `http://10.0.2.2:8080`. USB phone: `adb reverse tcp:8080 tcp:8080`, URL `http://127.0.0.1:8080`.
4. Choose Timeout: the server records approval but delays its response. The app shows uncertainty.
5. Press **Consultar resultado** to retrieve the original payment, without creating another charge.
6. Open the console dashboard and view/refund that operation.

Debug builds permit HTTP for this local lab. Release uses Android default TLS policy. The backend is unauthenticated and must remain local. Checkout persists the pending key and amount before calling the gateway; a process restart recovers an uncertain checkout and blocks new charges until resolved. This is a single-device demonstration, not a durable payment processing system.

## Firebase setup

The app runs without a Firebase account. To activate cloud history:

1. Create your own development Firebase project and register Android package `dev.breda.tap`.
2. Enable Authentication's Anonymous provider.
3. Create Cloud Firestore and deploy the supplied `firestore.rules` to that project.
4. Copy `app/src/main/assets/firebase-config.example.json` to `firebase-config.json`, inserting project ID, Firebase application ID (`mobilesdk_app_id`) and API key from the Android app configuration.
5. Rebuild. The screen shows Firebase configured; **Carregar histórico Firebase** fetches this anonymous user's records.

The app explicitly initializes a named FirebaseApp with FirebaseOptions. It therefore does not require the Google Services Gradle plugin. Never add an Admin SDK service-account key to the app. The personal config file is ignored by Git.

Each history document is stored at `users/{uid}/demoPayments/{requestKey}`. Rules require the authenticated UID to match the path, validate amount/status and require `simulation=true`; unmatched collections are denied. These client-written documents are demo history, not authoritative payment records. Losing the anonymous account (for example clearing app data) means losing access to that UID's history. Firebase failures are shown separately and do not change the checkout outcome. Cloud history writes are best effort; no durable retry outbox is implemented yet.

## Architecture and validation

- `core`: pure checkout transitions and exact decimal-to-cents conversion.
- `payment-sdk`: typed outcomes, coroutine handling, session idempotency.
- `app`: Compose UI, HttpGateway, pending checkout persistence and Firebase adapter.

5 JVM tests passed using Kotlin 2.1.20/JUnit: money validation, legal tap transition, duplicate tap, stale response and uncertain request retention. Android compilation/device testing and Firebase integration/rules-emulator testing are still pending. No production readiness or certification is claimed.
