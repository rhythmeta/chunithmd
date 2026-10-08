# chunithmd

[简体中文](README.md) | English

An Android and iOS companion for CHUNITHM players, with song lookup, score tracking, Rating analysis, and camera recognition. Part of the Rhythmeta ecosystem, it shares business logic through Kotlin Multiplatform and uses native interfaces built with Miuix on Android and SwiftUI on iOS.

## Features

- **Song catalog**: Search by title, alias, or ID, with sorting, filters, chart details, and data for the Japanese, international, and Chinese servers.
- **Scores and profiles**: Manage multiple player profiles, record scores manually, browse play history, and calculate chart Rating and Best 50.
- **Song selection tools**: Chart constant tables, score queries, random picks, Rating improvement recommendations, and plate progress.
- **Collections and sharing**: Favorite songs, organize chart collections, share them through `CHMD1` links, and export Best 50 and constant tables as images.
- **Camera recognition**: Identify songs on the song selection screen while holding the device vertically, or read scores while holding it horizontally. Import photos from the gallery and review recognized scores before saving.
- **Imports and cloud services**: Import scores from Diving Fish, LXNS, and Otogame; use Rhythmeta accounts, community aliases, and manual cloud backups.
- **Languages**: Simplified Chinese, Traditional Chinese, English, and Japanese.

## Get the app

| Platform | Minimum OS | Build artifacts |
| --- | --- | --- |
| Android | Android 10 (API 29) | [APK build workflow](https://github.com/rhythmeta/chunithmd/actions/workflows/build-apk.yml), with separate APKs for each CPU architecture |
| iOS | iOS 26 | [IPA build workflow](https://github.com/rhythmeta/chunithmd/actions/workflows/build-ipa.yml), providing an unsigned IPA |

Download artifacts from a successful workflow run. The iOS IPA requires signing before installation; you can also build and run the app from source using Xcode.

On first launch, follow the setup flow to download the catalog and cover images, then select the server for your profile in Settings. The cached catalog is available offline. Scanner models download separately when you first use the scanner and are not included in the app package. Once downloaded and verified, recognition runs on the device; scanned images are not uploaded.

Review recognition results and explicitly save them to add a play record. Check statuses such as FC / AJ / AJC against the original image. Cloud backups are created manually, and restoring a backup replaces this game's local personal data.

## Build from source

```sh
git clone https://github.com/rhythmeta/chunithmd.git
cd chunithmd
```

Both platforms use the Git commit count as the default build number, so a full Git history is recommended. To override it, set `CHUNITHMD_BUILD_NUMBER` to a positive integer.

### Android

Install JDK 21, the Android SDK, and an Android Studio version compatible with the repository's Android Gradle Plugin. The project currently uses Kotlin 2.4.20, AGP 9.5.0-alpha05, and Gradle 9.8.0. Build with the included Gradle Wrapper.

Install the SDK components and configure the SDK path through `ANDROID_HOME` or `sdk.dir` in `android/local.properties`:

```sh
sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"
cd android
./gradlew :app:assembleDebug
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`. Alternatively, open `android/` in Android Studio and run the `app` configuration.

For a release build, use `:app:assembleRelease`. Signing reads `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`, using a PKCS12 keystore. Add `-PCHUNITHMD_SPLIT_RELEASE_APKS=true` to generate separate APKs for each CPU architecture.

### iOS

You need macOS, an Xcode version that supports this project and your iOS 26 or later target device, plus JDK 21 and the Android SDK described above. The iOS build also configures the shared module's Android target, so the Android build environment is required.

1. Open `ios/chunithmd.xcodeproj`.
2. Select the `chunithmd` scheme and a simulator or physical device. The shared module supports Apple Silicon simulators and ARM64 devices.
3. For a physical device, configure your signing team, then build and run.

An Xcode build phase automatically generates and embeds the KMP `Shared` framework. See the [iOS README](ios/README.md) for additional platform notes in Chinese.

## Repository layout

```text
android/
  app/                 Android app, Miuix interface, and platform adapters
  shared/              KMP domain models, calculations, data access, and shared state
ios/                   SwiftUI app, system integrations, and UI tests
shared/                Protobuf contracts for cross-platform backups and collection sharing
localization/          Shared translation source for all four languages
src/static-bundle/     Catalog merging, normalization, and static asset generation
static-worker/         Cloudflare publishing configuration for catalogs and covers
model-assets/          Scanner model sources and platform exports
models-worker/         Separate publishing configuration for scanner models
scripts/               Data, model, localization, and packaging tools
test/                  Static asset and iOS helper script tests
```

Platform-independent calculations, validation, network requests, and business state belong in `android/shared/src/commonMain`. Native layers handle UI, navigation, cameras, system permissions, and storage adapters. Android uses Room / DataStore; iOS uses protected snapshot files. Credential storage uses Keystore and Keychain adapters respectively.

## Development and validation

Run the following commands from the repository root. Static asset tooling uses Node.js 22 and pnpm 10.28.2; localization tooling uses Python 3.

```sh
pnpm install --frozen-lockfile
pnpm typecheck
pnpm test
python3 scripts/generate-localization.py --check
```

Run shared logic tests and compile the Android app:

```sh
cd android
./gradlew :shared:testAndroidHostTest :app:compileDebugKotlin
```

Run iOS UI tests with Product → Test in Xcode. To update translations, edit `localization/strings.json`, run `python3 scripts/generate-localization.py`, and commit the generated `Translations.kt` alongside the source changes.

### Catalog and scanner models

The clients use published assets by default, so normal app builds do not require regenerating catalogs or models. The catalog build fetches upstream data and cover images, merges LXNS catalog and alias data, and validates the output:

```sh
pnpm build:static-bundle
```

Generated assets go to `static-worker/public/`, with a report at `artifacts/static-bundle-report.json`. `pnpm verify:static-bundle` checks published remote assets; set `CHUNITHMD_STATIC_BASE_URL` to configure the publication URL. See the [build entry point](scripts/build-static-bundle.ts) and [default sources](src/static-bundle/build.ts) for data sources and other environment variables.

Android recognition uses ONNX detection models and PaddleOCR; iOS uses Core ML detection models and system Vision. Models are published separately, with file size and SHA-256 verification during download. See the [model documentation](model-assets/README.md) for export, verification, and asset build instructions.

## License

This project is licensed under [GNU GPL v3](LICENSE). Third-party models and related assets include their own license notices, such as the [PaddleOCR license](android/app/src/main/assets/scanner/PaddleOCR-LICENSE.txt) and [source notice](android/app/src/main/assets/scanner/PaddleOCR-NOTICE.txt).
