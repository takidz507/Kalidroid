# KaliDroid

**KaliDroid is an Android-native Kali Linux userspace workspace.** It is designed for ARM64 Android devices without Root and without requiring a separate Termux installation.

## Product principles
- Kali NetHunter Minimal ARM64 userspace, downloaded from the official Kali mirror and verified against its SHA-256 manifest.
- PRoot userspace execution — **not QEMU, not a Kali kernel, and not Android root**.
- A persistent interactive shell runs inside the Kali rootfs once the Android-patched PRoot runtime is bundled.
- Real execution, educational simulation, and unsupported hardware features are labelled separately.
- Wi-Fi monitor mode, packet injection, kernel modules and privileged mounts are not promised on stock unrooted Android.

## Current implementation
- Kotlin + Jetpack Compose Android application.
- Official rootfs installer with SHA-256 verification and guarded TAR.XZ extraction.
- ARM64 PRoot runtime imported at build time from a pinned upstream revision.
- Persistent interactive `/bin/sh` session, command input and streaming output.
- Explicit `demo-scan` simulation label; it does not scan networks or send packets.
- Unsupported wireless capture/injection features are identified rather than falsely advertised.
- GitHub Actions workflow builds an ARM64 debug APK and uploads it as an artifact.

## Terminal limitations
The current shell is a persistent interactive shell connected through process pipes. It supports normal command-line tools and streaming output, but it is not yet a full PTY terminal: full-screen terminal applications, reliable Ctrl-C/job control, terminal resize, and advanced terminal escape rendering remain future work. Do not treat this build as a completed terminal emulator.

## Build
See [docs/BUILD.md](docs/BUILD.md). The GitHub Actions workflow runs on every push and pull request, and can also be started manually from the Actions tab. It imports the Android-patched PRoot binaries from the pinned upstream revision, then builds `app-debug.apk`.

## First launch
- Device must support `arm64-v8a`.
- The app downloads the official Kali NetHunter Minimal ARM64 rootfs (about 120 MiB compressed at the time of writing) and verifies its SHA-256 before extraction.
- Allow at least 1.5 GiB of free internal storage for extraction and package growth.
- Rootfs is stored in app-private storage and is removed when the app is uninstalled.

## Security and licensing
Use network tools only on systems and networks you own or are explicitly authorized to test. PRoot does not bypass Android kernel security boundaries. The imported PRoot runtime contains GPL-licensed components; the upstream license notice is copied into the build artifact. Review [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md) before redistribution.
