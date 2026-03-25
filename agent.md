# Agent guidance — ADO Desktop

Use this file when planning or editing this repository in an AI-assisted environment.

## Product intent

Desktop client for **Azure DevOps**, focused on **creating pull requests** and the **code review** flow (diffs, threads, comments). Users sign in with a **Personal Access Token (PAT)** they provide; the app talks to Azure DevOps **REST APIs** (not scraping the web). The UI may resemble the browser experience where practical.

## Tech stack

- **Kotlin Multiplatform** with **JVM-only** `shared` module today (`commonMain`, `jvmMain`, `commonTest`). Mobile targets are intentionally out of scope until needed.
- **Compose Desktop** in `desktopApp` (Java **17**).
- **Ktor** client in `shared` (`ktor-client-core` + `ktor-client-cio` on JVM) for HTTP to `dev.azure.com` (or the org’s host).
- **Gradle** with **version catalog** in `gradle/libs.versions.toml`.

## Module layout

| Module      | Role |
|------------|------|
| `shared`   | API models, URL parsing, PAT-backed HTTP client, domain logic, tests. |
| `desktopApp` | Compose UI, window/host, wires `shared`. |

Main entry: `dev.azure.desktop.MainKt` → `Main.kt` in `desktopApp`.

## Commands

- Run app: `./gradlew :desktopApp:run`
- Compile desktop: `./gradlew :desktopApp:compileKotlin`
- Shared tests: `./gradlew :shared:allTests`

Repositories: **Google Maven** + **Maven Central** (required for Compose transitive deps).

## Conventions for changes

- Keep diffs **focused** on the task; avoid drive-by refactors.
- Match existing **package** (`dev.azure.desktop`) and **Gradle** style.
- Do **not** commit PATs, `.env` files with secrets, or tokens; prefer secure storage when implementing sign-in.
- Prefer **`shared`** for anything that should stay testable and host-agnostic; keep **`desktopApp`** for UI and platform-specific desktop concerns.

## References

- Azure DevOps REST: Git (pull requests, iterations, threads), identities, projects — use a pinned `api-version` in requests.
- Design exploration may live under `stitch-gitpro/`; treat as reference, not as runtime code unless integrated.
