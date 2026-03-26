# Agent guidance — ADO Desktop

Use this file when planning or editing this repository in an AI-assisted environment.

## Product intent

Desktop client for **Azure DevOps**, focused on **creating pull requests** and the **code review** flow (diffs, threads, comments). Users sign in with a **Personal Access Token (PAT)** they provide; the app talks to Azure DevOps **REST APIs** (not scraping the web). The UI may resemble the browser experience where practical.

## Tech stack

- **Kotlin Multiplatform** with `common`, `feature-login`, `feature-pr` (`commonMain` + `jvmMain` / `androidMain` / `iosMain` as needed).
- **Compose Multiplatform** in `composeApp`: **desktop** (`jvm("desktop")`), **Android**, **iOS** (arm64 + simulator arm64). Full syntax-highlighted code review UI is on **desktop**; mobile uses a lightweight stub for the diff tab.
- **Ktor** client: **CIO** (desktop JVM), **OkHttp** (Android), **Darwin** (iOS).
- **Gradle** with **version catalog** in `gradle/libs.versions.toml`.

## Module layout

| Module          | Role |
|-----------------|------|
| `common`        | Shared utilities (e.g. Basic auth encoding), `expect`/`actual` platform name. |
| `feature-login` | Auth domain, PAT storage, Ktor session client + middleware. |
| `feature-pr`    | PR/release domain, FlowRedux machines, ADO REST repositories. |
| `composeApp`    | Compose UI, `App()`, platform bridges (`authBridge`, `pullRequestBridge`, `releaseBridge`). |

Main entry: **desktop** `dev.azure.desktop.MainKt` in `composeApp/src/desktopMain`. **Android** `MainActivity`. **iOS** `MainViewController()` for `ComposeUIViewController`.

## Commands

- Run desktop: `./gradlew :composeApp:run`
- Compile all main targets: `./gradlew compileAllTargets` (desktop, Android debug, iOS Simulator + iOS device arm64).
- Per target: `:composeApp:compileKotlinDesktop`, `:composeApp:compileDebugKotlinAndroid`, `:composeApp:compileKotlinIosSimulatorArm64`, `:composeApp:compileKotlinIosArm64`
- Android SDK: `settings.gradle.kts` fills `local.properties` `sdk.dir` from `ANDROID_SDK_ROOT` / `ANDROID_HOME` or `~/Library/Android/sdk` when missing; override manually if needed (see `local.properties.template`).

Repositories: **Maven Central** + **Google Maven** (order: Central first for Kotlin/Native prebuilts).

## Conventions for changes

- Keep diffs **focused** on the task; avoid drive-by refactors.
- Match existing **package** (`dev.azure.desktop`) and **Gradle** style.
- Do **not** commit PATs, `.env` files with secrets, or tokens; prefer secure storage when implementing sign-in.
- Prefer **domain + data split** in feature modules; use **expect/actual** or small platform bridges in `composeApp` for UI wiring.
- When working in the **data layer**, design for **provider abstraction**: define domain-facing repository/contracts that are VCS-provider agnostic, implement **Azure DevOps** as the first adapter now, and keep extension points ready for future **GitLab/GitHub** adapters.

## References

- Azure DevOps REST: Git (pull requests, iterations, threads), identities, projects — use a pinned `api-version` in requests.
- Design exploration may live under `stitch-gitpro/`; treat as reference, not as runtime code unless integrated.
