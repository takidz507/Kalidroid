# Build KaliDroid

## GitHub Actions

The workflow at `.github/workflows/android-build.yml` is configured for `push`, `pull_request`, and manual `workflow_dispatch` events. Once this project is pushed to a GitHub repository:

1. Open **Actions**.
2. Select **KaliDroid Android build**.
3. Run the workflow manually or push a commit to trigger it.
4. Download the `kalidroid-arm64-debug` artifact after a successful run.

The workflow checks out a pinned public Android-patched PRoot revision, imports the ARM64 runtime binaries, verifies their hashes, builds the debug APK, and uploads the APK plus upstream license and hash records.

## Local build

Requirements: JDK 17, Android SDK Platform 35, Build Tools 35.0.0, Gradle 8.11.1, Git, and network access.

```bash
./scripts/build-proot-engine.sh
gradle --no-daemon assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## First launch

- Device must support `arm64-v8a`.
- The app downloads the official Kali NetHunter Minimal ARM64 rootfs and verifies it against the official SHA-256 manifest.
- The compressed archive is about 120 MiB; extraction and future apt packages need more free space. The installer requires at least 1.5 GiB free.
- Rootfs is stored in app-private storage.

## Important

This is a debug APK, not a Play Store release. PRoot is a userspace compatibility layer, not a second kernel and not root. The current shell is interactive but pipe-backed, not a full PTY terminal.
