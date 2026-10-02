#!/usr/bin/env bash
# Build, sign and publish a Hymnal release to GitHub Releases (sub-project Z).
#
# Release convention (the in-app updater depends on it; keep in sync with update/ReleaseConvention.kt):
#   tag          vX.Y.Z, no suffix, equal to versionName in hymnchtv/build.gradle
#   versionCode  X*100000 + Y*1000 + Z*10 + n   (n = 0..9 rebuild digit)
#   assets       hymnal-X.Y.Z.apk          release-signed, applicationId com.ziontkec.hymnal   (required)
#                hymnal-X.Y.Z.apk.sha256   `shasum -a 256` output; the app refuses updates without it (required)
#   notes        the matching <release> entry of changelog_master.xml, plus the SHA-256
#
# Publishing never leaves an orphan tag: a DRAFT release is created with --target <commit> (drafts create no tag),
# all assets are uploaded and checked, then the draft is published, which is when GitHub creates the tag.
# If anything fails midway, re-run with --resume: it reuses dist/vX.Y.Z and finishes the draft.
#
# Signing: settings.signing (git-ignored, repo root) holds two paths OUTSIDE the repo:
#   keystore=/path/to/hymnal-release.jks
#   secure_properties=/path/to/hymnal-release.properties   (key.store.password, key.store.alias, key.alias.password)
# This script never reads or prints the passwords.
#
# Usage: tools/release.sh X.Y.Z [--dry-run | --resume]
set -euo pipefail

REPO="hitobias/hymnchtv"
APP_ID="com.ziontkec.hymnal"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

die() { echo "ERROR: $*" >&2; exit 1; }

version="${1:-}"
mode="${2:-}"
[[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] || die "usage: tools/release.sh X.Y.Z [--dry-run | --resume]"
[[ -z "$mode" || "$mode" == "--dry-run" || "$mode" == "--resume" ]] || die "unknown option: $mode"
tag="v$version"
dist="dist/$tag"
apk_name="hymnal-$version.apk"
sha_name="$apk_name.sha256"

# ---------- remote state ----------
remote_state() {
  local json
  if ! json=$(gh release view "$tag" --repo "$REPO" --json isDraft,assets 2>/dev/null); then
    echo "none"; return
  fi
  if [[ "$(jq -r '.isDraft' <<<"$json")" == "true" ]]; then echo "draft"; else echo "published"; fi
}
remote_tag_exists() { git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null 2>&1; }

if [[ "$mode" != "--dry-run" ]]; then
  command -v jq >/dev/null || die "jq is required (brew install jq)"
  gh auth status >/dev/null 2>&1 || die "gh is not logged in (run: gh auth login)"
  state=$(remote_state)
  if [[ "$state" == "published" ]]; then die "release $tag is already published"; fi
  if [[ "$state" == "none" ]] && remote_tag_exists; then
    die "tag $tag exists on origin without a release (not created by this script); inspect it before releasing"
  fi
  if [[ "$state" == "draft" && "$mode" != "--resume" ]]; then die "a draft $tag exists: re-run with --resume"; fi
else
  state="none"
fi

# ---------- build (skipped on --resume when artifacts are present) ----------
build() {
  [[ -f settings.signing ]] || die "settings.signing not found (see the header of tools/release.sh)"
  git diff --quiet && git diff --cached --quiet || die "working tree has uncommitted changes"
  ! git rev-parse -q --verify "refs/tags/$tag" >/dev/null || die "local tag $tag already exists"

  local gradle_file=hymnchtv/build.gradle version_name version_code
  version_name=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*/\1/p' "$gradle_file")
  version_code=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\1/p' "$gradle_file")
  [[ "$version_name" == "$version" ]] || die "versionName in $gradle_file is '$version_name', expected '$version'"
  python3 tools/release_notes.py check-version-code "$version" "$version_code" \
    || die "versionCode $version_code does not follow X*100000+Y*1000+Z*10+n for $version"

  rm -rf "$dist"
  mkdir -p "$dist"
  git rev-parse HEAD > "$dist/commit"
  python3 tools/release_notes.py notes "$version" hymnchtv/src/main/res/xml/changelog_master.xml > "$dist/notes.md" \
    || die "add a <release version=\"$version (MM/DD/YYYY)\"> entry to changelog_master.xml first"

  ./gradlew --console=plain :hymnchtv:testDebugUnitTest :hymnchtv:assembleRelease

  local apk_src=hymnchtv/build/outputs/apk/release/hymnchtv-release.apk sdk_dir build_tools badging
  [[ -f "$apk_src" ]] || die "$apk_src missing (an unsigned build is named *-release-unsigned.apk: check settings.signing)"
  cp "$apk_src" "$dist/$apk_name"

  sdk_dir=$(sed -nE 's/^sdk\.dir=(.*)$/\1/p' local.properties)
  build_tools="$sdk_dir/build-tools/37.0.0"
  "$build_tools/apksigner" verify --print-certs "$dist/$apk_name" | grep -E "^(Signer #1 certificate|V[0-9]+ Signer: certificate) (DN|SHA-256 digest)" \
      || die "apksigner did not verify a signing certificate in $dist/$apk_name"
  badging=$("$build_tools/aapt2" dump badging "$dist/$apk_name" \
    | sed -nE "s/^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'.*/\1 \2 \3/p")
  [[ "$badging" == "$APP_ID $version_code $version" ]] || die "APK badging mismatch: '$badging'"

  (cd "$dist" && shasum -a 256 "$apk_name" > "$sha_name")
  printf '\nSHA-256 (%s): `%s`\n' "$apk_name" "$(cut -d' ' -f1 "$dist/$sha_name")" >> "$dist/notes.md"
}

if [[ "$mode" == "--resume" ]]; then
  [[ -f "$dist/$apk_name" && -f "$dist/$sha_name" && -f "$dist/notes.md" && -f "$dist/commit" ]] \
    || die "--resume needs the artifacts in $dist from the earlier run"
  (cd "$dist" && shasum -a 256 -c "$sha_name") || die "$dist/$apk_name does not match its checksum"
else
  build
fi

ls -l "$dist"
if [[ "$mode" == "--dry-run" ]]; then
  echo "Dry run complete: $dist (nothing tagged or published)."
  exit 0
fi

commit=$(cat "$dist/commit")
git branch -r --contains "$commit" | grep -q "origin/" || die "commit $commit is not on origin yet: push it first"

# ---------- publish: draft -> upload -> verify -> publish ----------
if [[ "$state" == "none" ]]; then
  gh release create "$tag" --repo "$REPO" --draft --target "$commit" --title "詩歌 Hymnal $version" \
    --notes-file "$dist/notes.md" "$dist/$apk_name" "$dist/$sha_name"
else
  gh release upload "$tag" --repo "$REPO" --clobber "$dist/$apk_name" "$dist/$sha_name"
fi

assets=$(gh release view "$tag" --repo "$REPO" --json assets --jq '[.assets[].name] | sort | join(" ")')
[[ "$assets" == "$apk_name $sha_name" ]] || die "draft $tag has assets '$assets'; fix them and re-run with --resume"

gh release edit "$tag" --repo "$REPO" --draft=false
git fetch --tags origin
[[ "$(git rev-list -n1 "$tag")" == "$commit" ]] || die "published tag $tag does not point at $commit"
echo "Published $tag at $commit."
