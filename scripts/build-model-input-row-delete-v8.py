#!/usr/bin/env python3
"""Generate a new v8 catalog; keep published v5/v6/v7 definitions unchanged."""
import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('regas', ROOT / 'scripts/build-model-input-regas-v7.py')
regas = importlib.util.module_from_spec(spec)
spec.loader.exec_module(regas)


def assemble():
    payload, sources = regas.assemble()
    changed = []
    for row in payload['tables']:
        query = json.loads(row[14])
        caps = query['capabilities']
        if caps.get('ADD_ROW') != 'READY' or caps.get('DELETE_ROW') in ('READY', 'NOT_ALLOWED'):
            continue
        assert row[11] == 'Y' and caps.get('DELETE_ROW') == 'INCOMPLETE', row[:3]
        caps['DELETE_ROW'] = 'READY'
        row[14] = regas.catalog.compact(query)
        changed.append(row[:3])
    assert len(changed) == 51
    sources[Path(__file__).name] = Path(__file__).read_text()
    return payload, sources


def versioned(text):
    return (regas.versioned(text).replace('v7', 'v8').replace('@ci_version=7', '@ci_version=8')
        .replace('버전(7 미만)', '버전(8 미만)').replace('20261025', '20261026')
        .replace('2026-10-25', '2026-10-26')
        .replace('model-input-regas-rows-v8-', 'model-input-row-delete-v8-')
        .replace('model-input-row-delete-v8-policy.json', 'scripts/build-model-input-row-delete-v8.py')
        .replace('regas-rows-v8-', 'row-delete-v8-')
        .replace('python3 scripts/build-model-input-regas-v8.py', 'python3 scripts/build-model-input-row-delete-v8.py')
        .replace('work-notes/2026/2026-10-26-regas-row-policy-enable.md', 'work-notes/2026/2026-10-26-model-input-default-row-deletion.md'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    payload, sources = assemble()
    digest = regas.catalog.digest(regas.catalog.compact(payload))
    for kind, text in [('registration', regas.catalog.registration(payload, sources, digest, True)),
                       ('publication', regas.catalog.editor_publication(payload, digest))]:
        path = ROOT / f'docs/model-input-row-delete-v8-{kind}-draft.sql'
        text = versioned(text)
        if args.check:
            assert path.read_text() == text, path
        else:
            path.write_text(text)
    print(f'PASS: v8 / {len(payload["tables"])} tables / {len(payload["scopes"])} scopes / {digest}')


if __name__ == '__main__':
    main()
