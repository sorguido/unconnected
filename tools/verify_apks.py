#!/usr/bin/env python3
"""Verify APK signing/package identity without reading or printing private signing config."""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--build-tools', type=pathlib.Path, required=True)
parser.add_argument('--candidate', type=pathlib.Path, required=True)
parser.add_argument('--original', type=pathlib.Path, required=True)
parser.add_argument('--debug', type=pathlib.Path, required=True)
parser.add_argument('--output', type=pathlib.Path, required=True)
args = parser.parse_args()
# Digest retrieved from the published GitHub v1.1.0 asset metadata on 2026-10-30.
original_digest = '4e1583fc7421ba2ed508966ec8c567a336c0f0be5d6fa775a3a001885bf13e43'
assert hashlib.sha256(args.original.read_bytes()).hexdigest() == original_digest, 'Original APK differs from published asset'
records = []
for label, apk in [('unconnected candidate', args.candidate), ('Chromecast published v1.1.0', args.original), ('unconnected debug', args.debug)]:
    result = subprocess.run([str(args.build_tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], capture_output=True, text=True, check=True)
    cert = re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)', result.stdout).group(1)
    package = subprocess.run([str(args.build_tools / 'aapt2'), 'dump', 'badging', str(apk)], capture_output=True, text=True, check=True).stdout
    records.append(dict(artifact=label, sha256=hashlib.sha256(apk.read_bytes()).hexdigest(), size=apk.stat().st_size,
                        signer_sha256=cert, package=package.splitlines()[0],
                        permissions=[line for line in package.splitlines() if line.startswith('uses-permission:')],
                        sdk=[line for line in package.splitlines() if line.startswith(('minSdkVersion:', 'targetSdkVersion:'))], signature_verified=True))
assert records[0]['signer_sha256'] == records[1]['signer_sha256'], 'Signing identity differs'
assert "name='it.sorguido.unconnected'" in records[0]['package']
assert "name='it.sorguido.chromecastkillswitch'" in records[1]['package']
assert "name='it.sorguido.unconnected.debug'" in records[2]['package']
assert records[0]['signer_sha256'] != records[2]['signer_sha256'], 'Release must not be debug signed'
assert set(records[0]['permissions']) == {
    "uses-permission: name='android.permission.READ_CONTACTS'",
    "uses-permission: name='it.sorguido.unconnected.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'",
}, 'Unexpected APK permissions'
receipt = dict(date='2026-10-30', status='signed candidate; physical installation and call behavior unverified',
               original_release='https://github.com/sorguido/chromecast-killswitch-app/releases/tag/v1.1.0',
               original_asset_id=671937453, original_published_asset_sha256=original_digest,
               original_public_digest_matches_local_apk=True, artifacts=records)
args.output.write_text(json.dumps(receipt, indent=2) + '\n')
print('Verified: APK signatures valid; original signer matches; packages distinct; permissions expected.')
print('Candidate SHA-256:', records[0]['sha256'])
print('Certificate SHA-256:', records[0]['signer_sha256'])
