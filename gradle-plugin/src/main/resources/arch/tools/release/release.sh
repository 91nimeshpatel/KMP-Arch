#!/usr/bin/env bash
# The only way to build a store release (.claude/rules/contributing.md, docs/RELEASE_PLAN.md).
#
#   tools/release/release.sh v1.0.1 --e2e-log path/to/e2e.log [--no-upload]
#
# It refuses unless the rules hold: run from release/X.Y, clean tree, HEAD carries the tag (also on
# GitHub), and the E2E suite passed on this commit. Version numbers come from the tag, never by hand
# (versionCode = MAJOR*1000000 + MINOR*10000 + PATCH*100 + 99, as .github/workflows/release.yml).
# Every artifact is kept for good in $RELEASES_DIR/<tag>/ and attached to the GitHub Release <tag>:
# the signed AAB (with its R8 mapping and native symbols) and the iOS archive (with its dSYMs).
#
# A project with several apps (product flavors) names the app in the branch and tag
# (release/us-X.Y, us-vX.Y.Z) and builds bundle<Flavor>Release with that app's iOS configuration.
#
# Needs (never stored in this repository; keep them in a private vault):
#   KEYSTORE_PROPERTIES  a properties file with storeFile (relative to it), storePassword, keyAlias,
#                        keyPassword: the Play upload key
#   ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_FILE   App Store Connect API key, for signing (automatic,
#                        TEAM_ID in iosApp/Configuration/Config.xcconfig) and the upload;
#                        --no-upload builds and exports without uploading.
set -euo pipefail

die() { echo "release: $*" >&2; exit 1; }

TAG="${1:-}"; shift || true
E2E_LOG=""; UPLOAD=1
while [ $# -gt 0 ]; do
  case "$1" in
    --e2e-log) E2E_LOG="${2:-}"; shift 2 ;;
    --no-upload) UPLOAD=0; shift ;;
    *) die "unknown option $1" ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

# 1. The tag: vX.Y.Z.
[[ "$TAG" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || die "tag must look like v1.0.1, got '$TAG'"
MAJOR="${BASH_REMATCH[1]}"; MINOR="${BASH_REMATCH[2]}"; PATCH="${BASH_REMATCH[3]}"
NAME="$MAJOR.$MINOR.$PATCH"
CODE=$(( MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 + 99 ))

# 2. Version control: release branch, clean tree, tag on HEAD here and on GitHub.
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
[ "$BRANCH" = "release/$MAJOR.$MINOR" ] || die "run from release/$MAJOR.$MINOR (cut it from main), not '$BRANCH'"
[ -z "$(git status --porcelain)" ] || die "working tree is not clean"
git fetch -q --tags origin
[ "$(git rev-parse "$TAG^{commit}" 2>/dev/null)" = "$(git rev-parse HEAD)" ] || die "tag $TAG must point at HEAD (git tag -a $TAG -m ...)"
git ls-remote --exit-code --tags origin "refs/tags/$TAG" >/dev/null || die "push the tag first: git push origin $TAG"
[ "$(git rev-parse "origin/$BRANCH")" = "$(git rev-parse HEAD)" ] || die "push $BRANCH first"

# 3. The E2E suite passed on this commit (tools/e2e/run.sh output, which names the commit it ran on).
[ -n "$E2E_LOG" ] && [ -f "$E2E_LOG" ] || die "--e2e-log <file>: the tools/e2e/run.sh output for this commit"
grep -Fxq "==> commit $(git rev-parse HEAD)" "$E2E_LOG" || die "$E2E_LOG is not a clean run of this commit ($(git rev-parse --short HEAD))"
grep -Eq "[0-9]+ passed" "$E2E_LOG" || die "$E2E_LOG shows no passing E2E run"
! grep -Eq "[0-9]+ (failed|error)" "$E2E_LOG" || die "$E2E_LOG shows failures"

: "${KEYSTORE_PROPERTIES:?set KEYSTORE_PROPERTIES to the properties file of the upload key (vault)}"
[ -f "$KEYSTORE_PROPERTIES" ] || die "no file $KEYSTORE_PROPERTIES"
: "${ASC_KEY_ID:?set ASC_KEY_ID}" "${ASC_ISSUER_ID:?set ASC_ISSUER_ID}" "${ASC_KEY_FILE:?set ASC_KEY_FILE}"
TEAM="$(sed -n 's/^TEAM_ID *= *//p' iosApp/Configuration/Config.xcconfig)"
[ -n "$TEAM" ] || die "set TEAM_ID in iosApp/Configuration/Config.xcconfig"
APP="$(basename "$ROOT" | tr ' ' '-' | tr 'A-Z' 'a-z')"

OUT="${RELEASES_DIR:-$(dirname "$ROOT")/releases}/$TAG"
[ ! -e "$OUT" ] || die "$OUT already exists: a tag is built once"
mkdir -p "$OUT"
echo "release: $TAG -> versionName $NAME, versionCode/build $CODE, artifacts in $OUT"

# 4. The checks every change passes.
./gradlew qualityCheck allTests

# 5. Android: the signed bundle, with its mapping and native symbols kept beside it.
KEYS="$(cd "$(dirname "$KEYSTORE_PROPERTIES")" && pwd)"
set -a; . "$KEYSTORE_PROPERTIES"; set +a
KEYSTORE_FILE="$KEYS/$storeFile" KEYSTORE_PASSWORD="$storePassword" KEY_ALIAS="$keyAlias" KEY_PASSWORD="$keyPassword" \
  ./gradlew :androidApp:bundleRelease -PversionName="$NAME" -PversionCode="$CODE"
unset storePassword keyPassword
AAB_NAME="$APP-$NAME-vc$CODE.aab"
cp androidApp/build/outputs/bundle/release/androidApp-release.aab "$OUT/$AAB_NAME"
( cd "$OUT" && unzip -q "$AAB_NAME" 'BUNDLE-METADATA/*' -d meta \
  && zip -q -9 -j "mapping-$NAME.txt.zip" meta/BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map \
  && if [ -d meta/BUNDLE-METADATA/com.android.tools.build.debugsymbols ]; then
       (cd meta/BUNDLE-METADATA/com.android.tools.build.debugsymbols && zip -q -r "$OUT/native-debug-symbols-$NAME.zip" .)
     fi \
  && rm -rf meta )

# 6. iOS: archive (kept, with its dSYMs), then export and upload to App Store Connect. Signing is
#    automatic; the API key lets Xcode create or refresh the distribution certificate and profile.
ASC_AUTH=(-allowProvisioningUpdates -authenticationKeyPath "$ASC_KEY_FILE"
  -authenticationKeyID "$ASC_KEY_ID" -authenticationKeyIssuerID "$ASC_ISSUER_ID")
ARCHIVE="$OUT/$APP-$NAME.xcarchive"
xcodebuild archive -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release \
  -destination 'generic/platform=iOS' -archivePath "$ARCHIVE" \
  MARKETING_VERSION="$NAME" CURRENT_PROJECT_VERSION="$CODE" "${ASC_AUTH[@]}" -quiet
cat > "$OUT/ExportOptions.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>method</key><string>app-store-connect</string>
  <key>destination</key><string>$([ $UPLOAD = 1 ] && echo upload || echo export)</string>
  <key>teamID</key><string>$TEAM</string>
  <key>signingStyle</key><string>automatic</string>
  <key>uploadSymbols</key><true/>
</dict></plist>
PLIST
xcodebuild -exportArchive -archivePath "$ARCHIVE" -exportOptionsPlist "$OUT/ExportOptions.plist" \
  -exportPath "$OUT/ios-export" "${ASC_AUTH[@]}"
( cd "$OUT" && zip -q -r -y "dSYMs-$NAME.zip" "$(basename "$ARCHIVE")/dSYMs" \
  && zip -q -r -y "$(basename "$ARCHIVE").zip" "$(basename "$ARCHIVE")" )

# 7. Checksums and the GitHub Release with every artifact.
( cd "$OUT" && shasum -a 256 ./*.aab ./*.zip > SHA256SUMS )
cat > "$OUT/NOTES.md" <<NOTES
$TAG: versionName $NAME, versionCode / iOS build $CODE, commit $(git rev-parse --short HEAD) on $BRANCH.
Built by tools/release/release.sh; E2E: $(grep -Eo "[0-9]+ passed" "$E2E_LOG" | tail -1).
iOS $([ $UPLOAD = 1 ] && echo "uploaded to App Store Connect" || echo "exported, not uploaded"); Android bundle to upload to Play.
NOTES
# The release workflow may already have opened a draft for this tag (unsigned APK); it becomes the
# published release with the store artifacts rather than the run failing at the end.
if gh release view "$TAG" >/dev/null 2>&1; then
  gh release edit "$TAG" --title "$TAG" --notes-file "$OUT/NOTES.md" --draft=false
  gh release upload "$TAG" --clobber "$OUT"/*.aab "$OUT"/*.zip "$OUT/SHA256SUMS"
else
  gh release create "$TAG" --verify-tag --title "$TAG" --notes-file "$OUT/NOTES.md" \
    "$OUT"/*.aab "$OUT"/*.zip "$OUT/SHA256SUMS"
fi
echo "release: done. Upload $OUT/$AAB_NAME to Play, then merge $BRANCH back into main."
