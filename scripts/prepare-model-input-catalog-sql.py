#!/usr/bin/env python3
"""Prepare v6 SQL offline. Never connects to a database or commits."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[1]
COMMON = {'environment', 'expected_server_uuid', 'expected_schema', 'actor', 'mode',
          'previous_template_id', 'previous_version', 'previous_marker'}


def integer(value, maximum=9223372036854775807):
    if type(value) is not int or not 1 <= value <= maximum:
        raise ValueError('ID/version must be a positive integer within range')


def validate(config, stage):
    allowed = COMMON | {'expected_template_id', 'reader_ready'}
    if set(config) - allowed or COMMON - set(config):
        raise ValueError('Unknown or missing configuration keys')
    if config['environment'] not in ('DEVELOPMENT', 'PRODUCTION'):
        raise ValueError('environment must be DEVELOPMENT or PRODUCTION')
    uuid.UUID(config['expected_server_uuid'])
    if not re.fullmatch(r'[A-Za-z0-9_$-]{1,64}', config['expected_schema']):
        raise ValueError('Invalid schema')
    if not re.fullmatch(r'[A-Za-z0-9._@-]{1,50}', config['actor']):
        raise ValueError('Invalid actor')
    previous = [config[k] for k in ('previous_template_id', 'previous_version', 'previous_marker')]
    if config['mode'] == 'FIRST_INSTALL':
        if any(v is not None for v in previous):
            raise ValueError('FIRST_INSTALL requires null previous values')
    elif config['mode'] == 'UPGRADE':
        integer(previous[0])
        integer(previous[1], 5)
        if not isinstance(previous[2], str) or not previous[2].strip():
            raise ValueError('UPGRADE requires exact previous_marker')
    else:
        raise ValueError('mode must be FIRST_INSTALL or UPGRADE')
    if stage == 'publish':
        integer(config.get('expected_template_id'))
        if config.get('reader_ready') is not True:
            raise ValueError('publish requires reader_ready=true after deployed code verification')


def sql_value(value):
    if value is None:
        return 'NULL'
    if type(value) is bool:
        return str(int(value))
    if type(value) is int:
        return str(value)
    # Hex avoids quote/backslash/sql_mode-dependent escaping and SQL injection.
    return 'CONVERT(0x' + value.encode('utf-8').hex() + ' USING utf8mb4)'


def render(source, config, stage, manual=False):
    validate(config, stage)
    keys = COMMON | ({'expected_template_id', 'reader_ready'} if stage == 'publish' else set())
    for key in sorted(keys):
        pattern = r'(?m)^SET @ci_' + key + r'=(?:NULL|0);'
        source, count = re.subn(pattern, lambda _: f'SET @ci_{key}={sql_value(config[key])};', source)
        if count != 1:
            raise ValueError(f'Source input changed: {key}')
    if len(re.findall(r'(?m)^ROLLBACK;$', source)) != 1:
        raise ValueError('Expected exactly one final ROLLBACK')
    if re.search(r'(?im)^\s*COMMIT\s*;', source):
        raise ValueError('Source contains executable COMMIT')
    if manual:
        source = re.sub(r'(?m)^ROLLBACK;$', '-- MANUAL DECISION: results checked -> COMMIT; otherwise ROLLBACK; (same connection)', source)
    return ('-- Prepared offline. Use a dedicated interactive DB connection; stop on first error.\n'
            '-- ' + ('Transaction remains OPEN: promptly choose COMMIT or ROLLBACK in this same connection.\n'
                      if manual else 'Rehearsal: final ROLLBACK cancels changes.\n') + source)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--stage', choices=['register', 'publish'], required=True)
    parser.add_argument('--output-dir', type=Path, required=True, help='New directory; existing paths are never overwritten')
    args = parser.parse_args()
    try:
        config = json.loads(args.config.read_text(encoding='utf-8'))
        validate(config, args.stage)
        subprocess.run([sys.executable, str(ROOT / 'scripts/build-model-input-catalog-install.py'),
                        '--editor-v6', '--check'], check=True, capture_output=True)
        name = 'registration' if args.stage == 'register' else 'publication'
        source = (ROOT / f'docs/model-input-editor-remaining-v6-{name}-draft.sql').read_text(encoding='utf-8')
        outputs = {f'{args.stage}-rehearsal.sql': render(source, config, args.stage),
                   f'{args.stage}-manual.sql': render(source, config, args.stage, manual=True)}
        args.output_dir.mkdir(parents=True, exist_ok=False, mode=0o700)
        for name, content in outputs.items():
            path = args.output_dir / name
            with path.open('x', encoding='utf-8') as stream:
                stream.write(content)
            path.chmod(0o600)
        print(f'Prepared {args.output_dir.resolve()} (no DB access).')
        print('Run rehearsal first. Manual SQL leaves transaction OPEN; decide promptly in SAME interactive connection.')
    except (ValueError, TypeError, OSError, subprocess.CalledProcessError) as exc:
        parser.exit(1, f'Preparation failed: {exc}\n')


if __name__ == '__main__':
    main()
