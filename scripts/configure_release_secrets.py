"""Register private signing files with GitHub Actions. Never prints secret values.

Run only after GitHub API access is available:
python3 scripts/configure_release_secrets.py /path/to/private/signing.json
The JSON uses the four PAINLOG signing variable names documented in RELEASING.md.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('config', type=Path)
parser.add_argument('--repo', default='WassanBorn72/pain-log')
args = parser.parse_args()
config = json.loads(args.config.read_text())
root = Path(__file__).resolve().parents[1]
check_env = os.environ.copy()
check_env['PAINLOG_CERT_CHECK_PASSWORD'] = config['PAINLOG_STORE_PASSWORD']
certificate = subprocess.run([
    'keytool', '-exportcert', '-keystore', config['PAINLOG_KEYSTORE_PATH'],
    '-alias', config['PAINLOG_KEY_ALIAS'], '-storepass:env', 'PAINLOG_CERT_CHECK_PASSWORD',
], env=check_env, capture_output=True, check=True).stdout
expected = (root / 'docs/RELEASE_CERTIFICATE_SHA256.txt').read_text().strip()
if hashlib.sha256(certificate).hexdigest() != expected:
    raise SystemExit('Signing certificate does not match the current production key. No build or secret upload was performed.')

secrets = {name: config[name] for name in ['PAINLOG_STORE_PASSWORD','PAINLOG_KEY_ALIAS','PAINLOG_KEY_PASSWORD']}
secrets['PAINLOG_KEYSTORE_BASE64'] = base64.b64encode(Path(config['PAINLOG_KEYSTORE_PATH']).read_bytes()).decode('ascii')
for name, value in secrets.items():
    subprocess.run(['gh','secret','set',name,'--repo',args.repo],input=value,text=True,check=True)
    print(f'{name}: registered')
print('Keep a separate private backup of the original key and signing configuration.')
