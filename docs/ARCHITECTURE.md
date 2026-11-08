# KaliDroid architecture

## Runtime model

Android's existing Linux kernel remains in charge. KaliDroid downloads a Kali ARM64 root filesystem into `context.noBackupFilesDir/kali-rootfs` (or a dedicated app-private files directory), then launches a PRoot-compatible userspace process. This is a Linux userspace environment, not a VM and not a replacement kernel.

### First-run pipeline
1. Detect `arm64-v8a`, Android API level, free space, and app-private storage.
2. Fetch the official minimal ARM64 rootfs archive and its SHA256 manifest over HTTPS.
3. Resolve the exact archive checksum from the manifest; reject missing or ambiguous entries.
4. Download to a temporary file, verify SHA-256, then atomically promote it.
5. Extract safely: reject absolute paths, `..` traversal, links escaping the rootfs, and unexpected file types.
6. Install/prepare the Android ARM64 PRoot runner and test `uname -m`, `/etc/os-release`, and `id`.
7. Mark runtime `READY` only after all checks pass.

### Execution pipeline
Compose UI -> Kotlin RuntimeManager -> Rust policy/argument builder -> PRoot process -> Kali `/bin/sh -lc <command>`.

The initial scaffold only contains the Kotlin boundary and Rust policy core. A real PRoot Android binary and safe rootfs extractor are required before claiming end-to-end execution.

## Capability labels
- REAL: command actually executed inside the Kali userspace and returned a process exit code.
- SIMULATED: deterministic educational output generated locally; no network/system operation occurred.
- UNSUPPORTED: needs kernel privileges, unavailable kernel features, or unsupported Android hardware access.

Never silently fall back from REAL to SIMULATED. Show a visible label and reason.

## Android restrictions
PRoot uses ptrace-style syscall interception and is slower than native execution, especially for package management and compilation. It does not grant kernel root. Kernel modules, monitor mode, packet injection, raw hardware access, privileged mounts, and many low-level networking features are unavailable on an unrooted stock Android device.

## Security controls
- Rootfs and command working directories stay under app-private storage.
- No arbitrary privileged commands, no `su`, no system partition access.
- Process timeout, cancellation, output cap, and concurrency cap.
- Explicit user confirmation for network-active tools.
- Tool profiles are allowlisted; arbitrary shell is available only in the user's own local Kali session and is clearly marked.
- Simulation output is watermarked as simulated.
