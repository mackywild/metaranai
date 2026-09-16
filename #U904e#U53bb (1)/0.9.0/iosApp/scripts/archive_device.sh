#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${APPLE_TEAM_ID:?APPLE_TEAM_ID を設定してください}"
if ! command -v xcodegen >/dev/null 2>&1; then
  echo "xcodegen が必要です: brew install xcodegen" >&2
  exit 2
fi
xcodegen generate
mkdir -p build
xcodebuild \
  -project MetaranaiIOS.xcodeproj \
  -scheme MetaranaiIOS \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$PWD/build/MetaranaiIOS.xcarchive" \
  DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
  -allowProvisioningUpdates \
  archive

echo "Archive created: $PWD/build/MetaranaiIOS.xcarchive"
echo "Xcode > Window > Organizer から TestFlight & App Store を選んでアップロードしてください。"
