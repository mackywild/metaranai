#!/usr/bin/env bash
set -euo pipefail
OUT="${1:-metaranai-v0.4-data.xml}"
adb exec-out run-as jp.metaranai.app cat shared_prefs/metaranai.xml > "$OUT"
test -s "$OUT"
echo "Backup OK: $OUT ($(wc -c < "$OUT") bytes)"
