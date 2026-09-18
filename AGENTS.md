# gymbro agent guide

## Project

- Android app in Kotlin with Jetpack Compose and Room.
- Package and application ID: `com.brokenpip3.gymbro`.
- Data is local-only. Do not add accounts, network sync, analytics, telemetry, or runtime network permissions.
- Automatic Android backup is disabled. User-initiated backup is file-based JSON through the system picker.
- Keep the UI minimalist and use the existing Material 3 theme and semantic text roles.

## Commands

Use the Gradle wrapper, preferably inside `nix develop`:

```sh
just check-lint       # ktlint, Detekt, Android lint
just test             # JVM unit tests
just build-debug      # debug APKs
just install-debug    # build and install on a connected device
just run-emulator-start-headless
./gradlew :android:connectedDebugAndroidTest
```

Run Gradle commands one at a time. Use `--no-daemon -Djava.net.preferIPv4Stack=true` if the environment has Gradle or wildcard-IP issues.

The project uses Java 17, Android SDK/build tools 36, and the pinned Gradle wrapper. Do not use a system Gradle installation or commit generated `.gradle-home`, `build`, `dist`, or signing files.

## Data and migrations

- Room database: `android/src/main/java/com/brokenpip3/gymbro/data/GymbroDatabase.kt`.
- Current schema version: 2. Exported schemas are under `android/schemas`.
- For every schema change: update entities, increment the Room version, add an explicit migration, add migration tests, and regenerate the schema with Gradle.
- Never hand-edit exported schema JSON or use destructive migration fallback for release builds.
- Workout history keeps exercise and schedule snapshots so deleting current definitions does not erase history.

## Backup and restore

- `exercises` and `exercises_schedules` imports merge non-destructively.
- `full` import replaces all local data after confirmation.
- Keep backup validation and database writes transactional.
- A full import is blocked while a workout is active.
- When changing backup models, update codec/store tests and preserve `schemaVersion` compatibility.

## Debug data

Demo data is exposed only in debug builds through Settings > Developer tools. It creates exercises, schedules, completed history, and an unfinished workout. Keep it unavailable from release UI and do not use it as production data.

## Release

- Version is stored in `android/version.properties`; use `just version-bump <version>`.
- Release builds use R8, resource shrinking, and ABI splits for `arm64-v8a` and `x86_64`.
- Never publish the unsigned APKs produced by a plain `assembleRelease`.
- Release tags must be `v<VERSION_NAME>`. The workflow runs lint and JVM tests before building and verifies APK signatures.

## Testing and changes

- Add or update focused tests with behavior changes, especially for Room, repositories, backup, view models, and semantic UI contracts.
- Android Compose tests require a running device or emulator; JVM tests do not.
- Keep changes scoped to the request. Do not revert unrelated user changes, and do not commit unless asked.
