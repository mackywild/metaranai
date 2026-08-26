#!/usr/bin/env bash
set -euo pipefail
IN="${1:-metaranai-v0.4-data.xml}"
test -s "$IN"
adb push "$IN" /data/local/tmp/metaranai-v0.4-data.xml >/dev/null
adb shell am force-stop jp.metaranai.app
adb shell run-as jp.metaranai.app mkdir -p shared_prefs
adb shell run-as jp.metaranai.app sh -c 'cat /data/local/tmp/metaranai-v0.4-data.xml > shared_prefs/metaranai.xml'
adb shell am force-stop jp.metaranai.app
echo "Restore OK. Launch メタらない？ and verify history/DNA."
