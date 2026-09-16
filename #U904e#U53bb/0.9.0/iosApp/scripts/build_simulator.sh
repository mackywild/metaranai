#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v xcodegen >/dev/null 2>&1; then
  echo "xcodegen が必要です: brew install xcodegen" >&2
  exit 2
fi
xcodegen generate
xcodebuild \
  -project MetaranaiIOS.xcodeproj \
  -scheme MetaranaiIOS \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
