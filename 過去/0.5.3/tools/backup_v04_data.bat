@echo off
set OUT=metaranai-v0.4-data.xml
if not "%~1"=="" set OUT=%~1
adb exec-out run-as jp.metaranai.app cat shared_prefs/metaranai.xml > "%OUT%"
for %%A in ("%OUT%") do if %%~zA==0 (
  echo Backup failed: file is empty
  exit /b 1
)
echo Backup OK: %OUT%
