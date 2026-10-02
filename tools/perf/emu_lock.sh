#!/usr/bin/env bash
# Usage: tools/perf/emu_lock.sh <command...>
# Serialises emulator use (installs, instrumented tests, measurements) across all worktrees of this repo.
# The lock is per device (ANDROID_SERIAL), so different emulators never block each other.
set -euo pipefail
LOCK="$(git rev-parse --git-common-dir)/hymnchtv-emulator-${ANDROID_SERIAL:-default}.lock"
until mkdir "$LOCK" 2>/dev/null; do
  echo "waiting for emulator lock $LOCK (held by: $(cat "$LOCK/owner" 2>/dev/null || echo unknown))" >&2
  sleep 10
done
echo "$$ $(pwd) $(date '+%H:%M:%S')" > "$LOCK/owner"
trap 'rm -rf "$LOCK"' EXIT
"$@"
