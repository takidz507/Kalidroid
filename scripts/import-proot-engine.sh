#!/usr/bin/env bash
set -euo pipefail

# Import the ARM64 Android-patched PRoot binaries from a pinned upstream checkout.
# Usage: ./scripts/import-proot-engine.sh /path/to/oonid-pr-checkout
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
UPSTREAM="${1:?Pass the path to a checked-out oonid/pr repository}"
SRC="$UPSTREAM/android/proot-engine/src/main/jniLibs/arm64-v8a"
DST="$ROOT/app/src/main/jniLibs/arm64-v8a"

for name in libproot.so libproot-loader.so; do
  test -s "$SRC/$name" || { echo "Missing upstream binary: $SRC/$name" >&2; exit 1; }
done
mkdir -p "$DST"
install -m 755 "$SRC/libproot.so" "$DST/libproot.so"
install -m 755 "$SRC/libproot-loader.so" "$DST/libproot-loader.so"
sha256sum "$DST/libproot.so" "$DST/libproot-loader.so" > "$DST/SHA256SUMS.upstream"
cp "$UPSTREAM/LICENSE" "$ROOT/docs/THIRD_PARTY_PROOT_LICENSE" 2>/dev/null || true
printf 'Imported Android ARM64 PRoot binaries from %s\n' "$UPSTREAM"
sha256sum -c "$DST/SHA256SUMS.upstream"
