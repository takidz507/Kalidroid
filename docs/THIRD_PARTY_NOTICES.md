# Third-party notices

## Android-patched PRoot engine

KaliDroid imports the ARM64 Android-patched PRoot executable and loader from:

- Repository: https://github.com/oonid/pr
- Pinned revision: `a5a2a72b7f84eba9d9a2a795f6f11e725b181ab9`
- Files: `libproot.so`, `libproot-loader.so`

The build workflow copies the upstream `LICENSE` file into the build artifact and writes SHA-256 hashes for the imported binaries. Review the upstream license and all upstream notices before redistributing builds. This notice is not a substitute for the license text.


## Android-patched PRoot engine (upstream `oonid/pr`)

The CI workflow imports the upstream ARM64 `libproot.so` and `libproot-loader.so` from a pinned revision. The upstream repository is multi-licensed: its Android integration and scripts are MIT; its PRoot-derived runtime includes GPL-2.0-or-later and GPL-3.0-or-later components. The upstream LICENSE file must accompany redistribution, and the exact obligations must be reviewed before publishing an APK.
