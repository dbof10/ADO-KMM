# ADO Desktop

Kotlin Multiplatform client for **Azure DevOps**, built with [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/). Shared UI and domain logic target **desktop (JVM)**, **Android**, and **iOS** (via the `iosApp` Xcode project).

## Modules

| Module | Role |
|--------|------|
| `composeApp` | Application entry, Compose UI, platform wiring |
| `common` | Shared utilities and cross-cutting code |
| `feature-login` | Sign-in flow |
| `feature-pr` | Pull request features |

Architecture follows clean layers (presentation → domain → data) with [FlowRedux](https://github.com/freeletics/FlowRedux) for feature state machines.

## Prerequisites

- **JDK 21** (desktop and Gradle use the JVM 21 toolchain)
- **Android SDK** — for Android builds, set `sdk.dir` in `local.properties` or use `ANDROID_SDK_ROOT` / `ANDROID_HOME` (the build can seed `local.properties` from env or the default macOS SDK path)
- **Xcode** (recent) — for the iOS host app in `iosApp/`

## Versioning

App version is defined in `version.properties` (`VERSION_NAME`, `VERSION_CODE`). Gradle syncs these values into `iosApp/Configuration/Version.xcconfig` on import — edit `version.properties`, not the xcconfig, as the header there notes.

## Desktop

Run the app:

```bash
./gradlew :composeApp:run
```

Package installers (output under `composeApp/build/compose/binaries/`):

```bash
./gradlew :composeApp:packageDmg    # macOS
./gradlew :composeApp:packageMsi    # Windows
./gradlew :composeApp:packageDeb    # Linux
```

Release variants: `packageReleaseDmg`, `packageReleaseMsi`, `packageReleaseDeb`. Other useful tasks: `runRelease`, `packageUberJarForCurrentOS`.

## Android

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease
```

Install a debug build on a connected device or emulator as usual with Gradle/Android Studio.

## iOS

1. Build the Kotlin framework (example for the simulator):

   ```bash
   ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
   ```

2. Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme.

Exact framework tasks may vary by architecture (`iosArm64` vs `iosSimulatorArm64`); use `./gradlew :composeApp:tasks --all` and search for `Ios` / `link` if needed.
