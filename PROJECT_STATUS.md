# KaliDroid — project status 0.4.0

The source project now includes a persistent interactive shell adapter and a GitHub Actions ARM64 debug-build workflow.

## Available in source
- Kotlin/Jetpack Compose Android app.
- Official Kali NetHunter Minimal ARM64 rootfs installer with SHA-256 verification.
- Safe archive extraction checks.
- Android-patched PRoot import script pinned to upstream revision `a5a2a72b7f84eba9d9a2a795f6f11e725b181ab9`.
- Persistent interactive `/bin/sh` process with streamed output, command entry, connect and disconnect.
- Real / simulated / unsupported command labels and unit tests for the command policy.
- GitHub Actions workflow: push, pull request and manual workflow dispatch; builds and uploads debug APK.

## Not completed / not verified
- The project has not yet been pushed to a dedicated GitHub repository because the connected GitHub actions available in this session cannot create a new repository.
- GitHub Actions has not run; no APK artifact exists yet.
- No on-device test has been run. The PRoot engine import, shell launch and Kali package manager still need CI/device validation.
- The shell uses pipes, not a PTY. Full-screen terminal programs, reliable Ctrl-C/job control, terminal resizing and advanced ANSI rendering are pending.

## Device constraints
- ARM64 Android, no root required.
- PRoot is userspace only; it does not boot a Kali kernel or provide real Android root.
- Monitor mode, injection, kernel modules and privileged mounts are not promised on stock Android.
