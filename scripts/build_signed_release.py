"""Build using a private signing JSON file, without printing or shell-expanding secrets."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('config', type=Path)
args, gradle_args = parser.parse_known_args()
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

required = ['PAINLOG_KEYSTORE_PATH','PAINLOG_STORE_PASSWORD','PAINLOG_KEY_ALIAS','PAINLOG_KEY_PASSWORD']
if any(not config.get(name) for name in required):
    parser.error('Private configuration is missing a required signing field')
values = {name: str(config[name]) for name in required}
environment = os.environ.copy()
environment.update(values)
root = Path(__file__).resolve().parents[1]
raise SystemExit(subprocess.run([str(root / 'gradlew'), *gradle_args, 'assembleRelease'], cwd=root, env=environment).returncode)
