#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${TMPDIR:-/tmp}/kalidroid-proot-upstream"
REF="a5a2a72b7f84eba9d9a2a795f6f11e725b181ab9"
rm -rf "$WORK"
git clone --filter=blob:none --no-checkout https://github.com/oonid/pr.git "$WORK"
git -C "$WORK" fetch --depth 1 origin "$REF"
git -C "$WORK" checkout --detach FETCH_HEAD
"$ROOT/scripts/import-proot-engine.sh" "$WORK"
