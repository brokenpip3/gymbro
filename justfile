# build debug apk
build-debug:
    ./gradlew :android:assembleDebug

# run JVM unit tests
test:
    ./gradlew :android:testDebugUnitTest

# run Android lint
lint:
    ./gradlew :android:lintDebug

# install debug apk on a connected device/emulator (arch-aware)
install-debug:
    @command -v adb >/dev/null 2>&1 || { echo >&2 "adb is required but not installed."; exit 1; }
    ./gradlew :android:assembleDebug
    @for apk in android/build/outputs/apk/debug/android-*-debug.apk; do \
        if [ -f "$apk" ]; then \
            adb install -r "$apk"; \
            exit 0; \
        fi; \
    done; \
    echo "No arch-split debug apk found."; exit 1

# show connected Android devices
devices:
    adb devices

# build signed release APK (arch-split; set GYMBRO_KEYSTORE_* env vars)
build-release-apk:
    @if [ -n "${GYMBRO_KEYSTORE_BASE64:-}" ]; then \
        echo "Decoding release keystore..."; \
        echo "${GYMBRO_KEYSTORE_BASE64:-}" | base64 -d > android/release.jks; \
        export SOURCE_DATE_EPOCH=$(git log -1 --format=%ct); \
        export GYMBRO_KEYSTORE_PATH=release.jks; \
        trap 'rm -f android/release.jks' 0 1 2 3 15; \
        ./gradlew :android:assembleRelease; \
    elif [ -n "${GYMBRO_KEYSTORE_PATH:-}" ]; then \
        echo "Using release keystore from GYMBRO_KEYSTORE_PATH..."; \
        export SOURCE_DATE_EPOCH=$(git log -1 --format=%ct); \
        ./gradlew :android:assembleRelease; \
    else \
        echo "Warning: no release keystore configured. Building unsigned release."; \
        export SOURCE_DATE_EPOCH=$(git log -1 --format=%ct); \
        ./gradlew :android:assembleRelease; \
    fi
    @mkdir -p dist
    @VERSION=$(grep 'VERSION_NAME=' android/version.properties | cut -d'=' -f2); \
    for apk in android/build/outputs/apk/release/*.apk; do \
        [ -f "$apk" ] || continue; \
        base=$(basename "$apk"); \
        tmp="${base#android-}"; \
        tmp="${tmp/-release/}"; \
        new_name="gymbro-v$VERSION-$tmp"; \
        cp "$apk" "dist/$new_name"; \
        echo "Release APK created at: dist/$new_name"; \
    done

# full release build (lint → test → release apk)
build-release: check-lint test build-release-apk

# run kotlin linters
check-lint:
    ./gradlew :android:ktlintCheck :android:detekt :android:lintDebug

# auto-format code
check-fmt:
    ./gradlew :android:ktlintFormat

# increment version code and set version name
version-bump name="":
    @if [ -z "{{ name }}" ]; then \
        echo "Error: version name is required. Usage: just version-bump 1.0.1"; \
        exit 1; \
    fi; \
    CODE=$(grep 'VERSION_CODE=' android/version.properties | cut -d'=' -f2); \
    NEXT_CODE=$((CODE + 1)); \
    echo "Bumping version to {{ name }} ($NEXT_CODE)..."; \
    echo "VERSION_NAME={{ name }}" > android/version.properties; \
    echo "VERSION_CODE=$NEXT_CODE" >> android/version.properties;

# display the current version
version-current:
    @cat android/version.properties

# start android emulator if not running
run-emulator-start:
    @command -v emulator >/dev/null 2>&1 || { echo >&2 "emulator is required but not installed."; exit 1; }
    @if adb devices | grep -q emulator; then \
        echo "Emulator is already running."; \
    else \
        export ANDROID_AVD_HOME="${HOME}/.config/.android/avd" && \
        (emulator -avd dev_emulator -no-audio -no-boot-anim &) && \
        timeout 180s adb wait-for-device && \
        timeout 180s sh -c 'until adb shell getprop sys.boot_completed 2>/dev/null | grep -q 1; do sleep 2; done' || { \
            echo "Emulator did not finish booting within 180 seconds."; \
            exit 1; \
        }; \
    fi

# start android emulator in headless mode if not running
run-emulator-start-headless:
    @command -v emulator >/dev/null 2>&1 || { echo >&2 "emulator is required but not installed."; exit 1; }
    @if adb devices | grep -q emulator; then \
        echo "Emulator is already running."; \
    else \
        export ANDROID_AVD_HOME="$HOME/.config/.android/avd" && \
        (emulator -avd dev_emulator -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot-load &) && \
        adb wait-for-device; \
    fi

# stop android emulator
run-emulator-stop:
    @command -v adb >/dev/null 2>&1 || { echo >&2 "adb is required but not installed."; exit 1; }
    adb emu kill
