"""Compare CI APK signer to checked-in Android OAuth clients; never reads private keys."""
import json
import re
import subprocess
import sys
from pathlib import Path

apksigner, apk = sys.argv[1:3]
output = subprocess.check_output([apksigner, 'verify', '--print-certs', apk], text=True)
actual = re.search(r'Signer #1 certificate SHA-1 digest: ([0-9a-fA-F]+)', output)
if not actual:
    raise SystemExit('Could not determine APK signer SHA-1')
sha1 = actual.group(1).lower()
print('APK_SIGNER_SHA1=' + ':'.join(sha1[i:i+2].upper() for i in range(0, len(sha1), 2)))
sha256 = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)', output)
if sha256:
    print('APK_SIGNER_SHA256=' + sha256.group(1))
config = json.loads(Path('app/google-services.json').read_text())
registered = {
    c.get('android_info', {}).get('certificate_hash', '').replace(':', '').lower()
    for client in config.get('client', [])
    for c in client.get('oauth_client', [])
    if c.get('client_type') == 1 and c.get('android_info', {}).get('package_name') == 'jp.metaranai.app'
}
if sha1 not in registered:
    print('::warning::APK signer SHA-1 is absent from checked-in Google Android OAuth clients. Register this certificate in Firebase/Google Cloud and refresh google-services.json. Play installs require the Play App Signing certificate separately.')
    print('GOOGLE_APK_CERTIFICATE_MISMATCH')
else:
    print('GOOGLE_APK_CERTIFICATE_MATCH')
