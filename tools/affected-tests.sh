#!/usr/bin/env bash
# affected-tests.sh - list (and optionally run) instrumented test classes affected by a change.
#
# Usage: tools/affected-tests.sh [--base <ref>] [--run <serial>] [--quick]
#   --base <ref>    git base to compare with (default origin/master); uses <ref>...HEAD
#                   plus uncommitted (git diff HEAD) and untracked files
#   --run <serial>  run the selected classes on that device via `am instrument`
#                   (batches of <= 25); if the result is ALL, runs the quick suite instead
#   --quick         additionally run the quick suite (-e annotation org.cog.hymnchtv.QuickTest)
#   --range A..B    (hidden, for testing) analyse a commit range as committed, nothing else
# Output (stdout): fully-qualified test classes, one per line; or the single word ALL; or nothing.
#
# Heuristics (a "test class" = file under hymnchtv/src/androidTest containing @Test):
#  - changed androidTest test file          -> that class
#  - changed androidTest helper/base file   -> every test referencing its name (transitively
#                                              through other helpers)
#  - changed main .kt/.java (any non-androidTest src set) -> tests referencing its simple class name
#  - changed res xml (layout/menu/drawable/...) -> tests referencing R.<type>.<file name>, the
#                                              ids it declares (@+id/x -> R.id.x) and, for
#                                              layouts, the ViewBinding class name
#  - changed res/values* files (strings, dimens, ...) -> tests referencing the names changed
#                                              in the diff (name="...")
#  - AndroidManifest.xml, *.gradle(.kts), gradle.properties, res/values*/{themes,styles,colors}*
#                                           -> ALL (shared behaviour; run the full suite)
#  - nothing matched -> prints nothing
# Matching is word-based text search, so it may over-select but should not miss direct references.

BASE=origin/master; RANGE=""; RUN=""; QUICK=0
AT_DIR=hymnchtv/src/androidTest
PKG=com.ziontkec.hymnal
QUICK_ANN=org.cog.hymnchtv.QuickTest
BATCH=25

while [ $# -gt 0 ]; do
  case "$1" in
    --base) BASE=${2:?--base needs a ref}; shift 2 ;;
    --range) RANGE=${2:?--range needs A..B}; shift 2 ;;
    --run) RUN=${2:?--run needs a device serial}; shift 2 ;;
    --quick) QUICK=1; shift ;;
    -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

cd "$(git rev-parse --show-toplevel)" || exit 2
REF=""; [ -n "$RANGE" ] && REF=${RANGE##*..}   # in range mode, search the tree at the end commit

changed_files() {
  if [ -n "$RANGE" ]; then
    git diff --name-only "$RANGE"
  else
    git diff --name-only "$BASE...HEAD"
    git diff --name-only HEAD
    git ls-files --others --exclude-standard
  fi | sort -u
}

# Text of the change to one file (used to pull changed resource names out of values files).
diff_text() {
  if [ -n "$RANGE" ]; then git diff "$RANGE" -- "$1"; return; fi
  git diff "$BASE...HEAD" -- "$1"; git diff HEAD -- "$1"
  git ls-files --others --exclude-standard -- "$1" | while IFS= read -r u; do sed 's/^/+/' "$u"; done
}

# Current content of a file (deleted files fall back to the base version).
file_content() {
  if [ -n "$RANGE" ]; then git show "$REF:$1" 2>/dev/null || git show "${RANGE%%..*}:$1" 2>/dev/null
  else cat "$1" 2>/dev/null || git show "$BASE:$1" 2>/dev/null; fi
}

# androidTest files that contain any of the given fixed words, one path per line.
grep_files() {
  local args=() p
  for p in "$@"; do args+=(-e "$p"); done
  if [ -n "$REF" ]; then
    git grep -l -F -w "${args[@]}" "$REF" -- "$AT_DIR" | sed 's/^[^:]*://'
  else
    git grep -l --untracked -F -w "${args[@]}" -- "$AT_DIR"
  fi 2>/dev/null
}

TESTS=$(grep_files '@Test' | sort -u)
SEL=""; SEEN=""

is_test() { printf '%s\n' "$TESTS" | grep -qxF "$1"; }
base_name() { basename "$1" | sed 's/\.[^.]*$//'; }

# Add every test referencing any of the words; non-test hits (helpers) are expanded recursively.
refs_to() {
  [ $# -eq 0 ] && return
  local f hits
  hits=$(grep_files "$@")
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    if is_test "$f"; then SEL="$SEL$f"$'\n'; else expand_helper "$f"; fi
  done <<< "$hits"
}

expand_helper() {
  case "$SEEN" in *"|$1|"*) return ;; esac
  SEEN="$SEEN|$1|"
  refs_to "$(base_name "$1")"
}

# layout name fragment_home -> FragmentHomeBinding
binding_name() {
  echo "$1" | awk -F_ '{ for (i = 1; i <= NF; i++) printf "%s%s", toupper(substr($i,1,1)), substr($i,2); print "Binding" }'
}

handle_res() {   # $1 = path of an xml under .../res/<dir>/
  local f=$1 dir type name words
  dir=$(basename "$(dirname "$f")"); type=${dir%%-*}; name=$(base_name "$f")
  if [ "$type" = values ]; then
    words=$(diff_text "$f" | grep -E '^[+-]' | grep -o 'name="[^"]*"' | sed 's/^name="//; s/"$//' | sort -u)
    # shellcheck disable=SC2086
    [ -n "$words" ] && refs_to $words
    return
  fi
  words="R.$type.$name"
  [ "$type" = layout ] && words="$words $(binding_name "$name")"
  words="$words $(file_content "$f" | grep -o '@+id/[A-Za-z0-9_]*' | sed 's|@+id/|R.id.|' | sort -u | tr '\n' ' ')"
  # shellcheck disable=SC2086
  refs_to $words
}

ALL=0
FILES=$(changed_files)
while IFS= read -r f; do
  [ -z "$f" ] && continue
  case "$f" in
    "$AT_DIR"/*.kt|"$AT_DIR"/*.java)
      if is_test "$f"; then SEL="$SEL$f"$'\n'; else expand_helper "$f"; fi ;;
    "$AT_DIR"/*) ;;
    */AndroidManifest.xml|*.gradle|*.gradle.kts|gradle.properties) ALL=1; break ;;
    */res/values*/themes*|*/res/values*/styles*|*/res/values*/colors*) ALL=1; break ;;
    */res/*/*.xml) handle_res "$f" ;;
    hymnchtv/src/*.kt|hymnchtv/src/*.java) refs_to "$(base_name "$f")" ;;
  esac
done <<< "$FILES"

if [ "$ALL" = 1 ]; then
  RESULT=ALL
else
  # path -> fully-qualified class name
  RESULT=$(printf '%s' "$SEL" | sort -u | sed '/^$/d' \
    | sed "s|^$AT_DIR/[a-z]*/||; s|\.[a-z]*$||; s|/|.|g")
fi
[ -n "$RESULT" ] && echo "$RESULT"

# ---- optional execution -------------------------------------------------------------------
[ -z "$RUN" ] && exit 0

TMO=$(command -v timeout || command -v gtimeout || true)
instrument() {   # $1 = extra "-e key value" args
  # adb shell reads stdin: without </dev/null it swallows the rest of the batch loop's here-string.
  # am instrument exits 0 even when tests fail, so success is decided from its output.
  local out
  # shellcheck disable=SC2086
  out=$(${TMO:+$TMO 1800} adb -s "$RUN" shell am instrument -w $1 "$PKG.test/androidx.test.runner.AndroidJUnitRunner" < /dev/null)
  local rc=$?
  printf '%s\n' "$out"
  [ $rc -eq 0 ] && printf '%s' "$out" | grep -q '^OK (' && ! printf '%s' "$out" | grep -q 'FAILURES!!!\|INSTRUMENTATION_FAILED\|Process crashed'
}
run_quick() { echo ">> quick suite ($QUICK_ANN)" >&2; instrument "-e annotation $QUICK_ANN"; }

STATUS=0
if [ "$RESULT" = ALL ]; then
  echo ">> ALL selected; running the quick suite only. Run the full suite before merge." >&2
  run_quick || STATUS=1
else
  if [ -n "$RESULT" ]; then
    while IFS= read -r batch; do
      echo ">> batch: $(echo "$batch" | wc -w | tr -d ' ') classes" >&2
      instrument "-e class $(echo "$batch" | tr ' ' ',')" || STATUS=1
    done <<< "$(echo "$RESULT" | xargs -n "$BATCH")"
  else
    echo ">> no affected tests" >&2
  fi
  [ "$QUICK" = 1 ] && { run_quick || STATUS=1; }
fi
exit $STATUS
