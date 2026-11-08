# Implementation status — 0.4.0

## Implemented in source
- Compose-based Android interface.
- ARM64-only runtime guard and app-private Kali rootfs storage.
- Official NetHunter Minimal rootfs download, SHA-256 validation and guarded TAR.XZ extraction.
- Android-patched PRoot launch adapter with an explicit missing-engine failure.
- Persistent interactive `/bin/sh` process with streamed output and command input.
- Clear separation of real commands, educational `demo-scan`, and unsupported wireless features.
- GitHub Actions build workflow that imports pinned ARM64 PRoot binaries and uploads the debug APK.

## Not yet verified
- GitHub Actions build has not been run from a published KaliDroid repository.
- No APK has been produced or installed on a physical Android device in this session.
- PRoot startup and Kali shell login have not been tested on OnePlus 12R.
- `cat /etc/os-release`, `id`, `uname -m`, `python3`, and `nmap` have not yet been validated on-device.

## Known limitations
- Shell transport currently uses process pipes, not a PTY. Full-screen TUI programs, terminal resize, robust Ctrl-C and job control are not supported yet.
- Minimal rootfs does not include every Kali tool. Tools must be installed inside Kali with apt when needed.
- PRoot provides a Linux userspace and fake-root view only; it does not boot a Kali kernel or grant Android root.
- Wi-Fi monitor mode, packet injection and kernel modules require kernel/driver/hardware support that stock unrooted Android does not provide.
