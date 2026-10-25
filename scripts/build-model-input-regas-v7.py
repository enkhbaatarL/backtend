#!/usr/bin/env python3
"""Generate a new v7 catalog opening only BCRegasification row creation/deletion."""
import argparse
import copy
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('catalog', ROOT / 'scripts/build-model-input-catalog-install.py')
catalog = importlib.util.module_from_spec(spec)
spec.loader.exec_module(catalog)
POLICY = ROOT / 'docs/model-input-regas-rows-v7-policy.json'


def assemble():
    payload, sources = catalog.remaining_editor()
    policy = json.loads(POLICY.read_text())
    row = next(row for row in payload['tables'] if row[:3] == policy['key'])
    assert catalog.digest(catalog.compact(row)) == policy['baselineSha256']
    query = json.loads(row[14])
    row[11] = 'Y'
    query['capabilities'].update(ADD_ROW='READY', DELETE_ROW='READY')
    for field in query['fields']:
        field['policyStatus']['CREATE'] = 'READY'
        field['create'] = copy.deepcopy(policy['identityCreate' if field['columnNumber'] < 7 else 'capacityCreate'])
    row[14] = catalog.compact(query)
    sources[POLICY.name] = POLICY.read_text()
    return payload, sources


def versioned(text):
    # Payload is hex encoded; only the reused generator's v6 rollout metadata changes.
    return (text.replace('v6', 'v7').replace('@ci_version=6', '@ci_version=7')
        .replace('버전(6 미만)', '버전(7 미만)').replace('20261023', '20261025')
        .replace('2026-10-23', '2026-10-25')
        .replace('model-input-editor-remaining-v7-policy.json', POLICY.name)
        .replace('model-input-editor-remaining-v7-', 'model-input-regas-rows-v7-')
        .replace('model-input-editor-v7-deployment-guide.md', 'model-input-regas-rows-v7-deployment-guide.md')
        .replace('editor-remaining-v7-', 'regas-rows-v7-')
        .replace('python3 scripts/build-model-input-catalog-install.py --editor-v7', 'python3 scripts/build-model-input-regas-v7.py')
        .replace('work-notes/2026/2026-10-19-model-input-excel-editor.md', 'work-notes/2026/2026-10-25-regas-row-policy-enable.md'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    payload, sources = assemble()
    digest = catalog.digest(catalog.compact(payload))
    for kind, text in [('registration', catalog.registration(payload, sources, digest, True)),
                       ('publication', catalog.editor_publication(payload, digest))]:
        path = ROOT / f'docs/model-input-regas-rows-v7-{kind}-draft.sql'
        text = versioned(text)
        if args.check:
            assert path.read_text() == text, path
        else:
            path.write_text(text)
    print(f'PASS: v7 / {len(payload["tables"])} tables / {len(payload["scopes"])} scopes / {digest}')


if __name__ == '__main__':
    main()
