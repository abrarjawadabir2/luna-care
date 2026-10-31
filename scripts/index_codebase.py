#!/usr/bin/env python3
"""Build a searchable, local-only LunaCare code index. No external dependencies.

python3 scripts/index_codebase.py                 # build
python3 scripts/index_codebase.py --query username # search
"""
from __future__ import annotations
import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

EXT = {'.kt', '.kts', '.java', '.ts', '.tsx', '.js', '.jsx', '.rs', '.py',
       '.sql', '.html', '.css', '.scss', '.json', '.yml', '.yaml', '.toml', '.sh', '.ps1'}
SKIP_DIR = {'.git', '.agent', '.agents', '.gsd', '.gemini', '.antigravity', '.impeccable',
            'docs-private', 'node_modules', '.gradle', '.idea', 'build', 'dist', 'target',
            'coverage', '.venv', '__pycache__', 'test-results', 'playwright-report'}
SKIP_FILE = {'package-lock.json', 'Cargo.lock', 'gradle-wrapper.properties'}
REGEX = {
    'kotlin_function': re.compile(r'\bfun\s+(?:[\w<>?.]+\.)?(\w+)\s*\('),
    'type': re.compile(r'\b(?:(?:data|sealed|enum)\s+)?(?:class|interface|object)\s+(\w+)'),
    'typescript_function': re.compile(r'\b(?:async\s+)?function\s+(\w+)\s*\('),
    'typescript_binding': re.compile(r'\b(?:export\s+)?(?:const|let)\s+(\w+)\s*(?:=|:)'),
    'typescript_type': re.compile(r'\b(?:interface|type|class|enum)\s+(\w+)'),
    'rust_function': re.compile(r'\b(?:async\s+)?fn\s+(\w+)\s*(?:<[^>]+>)?\s*\('),
    'python_definition': re.compile(r'^\s*(?:async\s+def|def|class)\s+(\w+)\s*[(:]'),
    'sql_table': re.compile(r'\bcreate\s+table\s+(?:if\s+not\s+exists\s+)?([\w."]+)', re.I),
    'sql_policy': re.compile(r'\bcreate\s+policy\s+"?(\w+)', re.I),
    'api_route': re.compile(r'\b(?:fastify|app|router|server)\.(get|post|put|patch|delete|options)\s*\(\s*[\'"]([^\'"]+)', re.I),
    'html_landmark': re.compile(r'<\s*(main|nav|section|form)\b[^>]*\bid\s*=\s*[\'"]([\w-]+)', re.I),
}

def run_git(root: Path, *args: str) -> str:
    result = subprocess.run(['git', '-C', str(root), *args], capture_output=True)
    if result.returncode:
        raise RuntimeError('This command requires a local Git checkout.')
    return result.stdout.decode('utf-8', errors='surrogateescape')

def symbols(name: str, source: str) -> list[dict]:
    ext = Path(name).suffix.lower()
    result = []
    next_composable = False
    for n, line in enumerate(source.splitlines(), 1):
        if ext in {'.kt', '.kts', '.java'}:
            if '@Composable' in line:
                next_composable = True
            checks = ['type', 'kotlin_function']
        elif ext in {'.ts', '.tsx', '.js', '.jsx'}:
            checks = ['typescript_type', 'typescript_function', 'typescript_binding', 'api_route']
        elif ext == '.rs':
            checks = ['type', 'rust_function']
        elif ext == '.py':
            checks = ['python_definition']
        elif ext == '.sql':
            checks = ['sql_table', 'sql_policy']
        elif ext == '.html':
            checks = ['html_landmark']
        else:
            checks = []
        for kind in checks:
            match = REGEX[kind].search(line)
            if match:
                value = (' '.join(match.groups()) if kind == 'api_route' else
                         '#'.join(match.groups()) if kind == 'html_landmark' else match.group(1))
                if kind == 'api_route':
                    value = match.group(1).upper() + ' ' + match.group(2)
                if len(value) < 140:
                    result.append({'kind': 'composable' if kind == 'kotlin_function' and next_composable else kind,
                                   'name': value, 'line': n})
                if kind == 'kotlin_function':
                    next_composable = False
    return result

def build(root: Path) -> dict:
    tracked = sorted(p for p in run_git(root, 'ls-files').splitlines() if p)
    files = []
    for relative in tracked:
        parts = Path(relative).parts
        if not parts or any(p in SKIP_DIR for p in parts):
            continue
        if parts[-1] in SKIP_FILE or parts[-1].startswith('.env') or Path(relative).suffix.lower() not in EXT:
            continue
        path = root / relative
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 1_000_000:
            continue
        if not path.resolve().is_relative_to(root):
            continue
        raw = path.read_bytes()
        try:
            source = raw.decode('utf-8')
        except UnicodeDecodeError:
            continue
        files.append({'path': relative, 'module': parts[0], 'language': path.suffix.lower().lstrip('.'),
                      'lines': len(source.splitlines()), 'sha256': hashlib.sha256(raw).hexdigest(),
                      'symbols': symbols(relative, source)})
    try:
        revision = run_git(root, 'rev-parse', 'HEAD').strip()
    except RuntimeError:
        revision = 'uncommitted'
    return {'schema': 1, 'revision': revision,
            'generated_utc': datetime.now(timezone.utc).isoformat(),
            'tracked_files': len(tracked), 'indexed_files': len(files),
            'symbol_count': sum(len(x['symbols']) for x in files), 'files': files}

def main() -> int:
    parser = argparse.ArgumentParser(description='Private LunaCare code-symbol index')
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--query', help='Search existing index by path, function or route')
    parser.add_argument('--limit', type=int, default=30)
    args = parser.parse_args()
    root = args.root.resolve()
    folder = root / 'docs-private'
    path = folder / 'code-index.json'
    if args.query:
        if not path.exists():
            print('Generate the index first: python3 scripts/index_codebase.py', file=sys.stderr)
            return 1
        index = json.loads(path.read_text(encoding='utf-8'))
        count = 0
        for file in index['files']:
            matches = [s for s in file['symbols'] if args.query.lower() in s['name'].lower()]
            if args.query.lower() not in file['path'].lower() and not matches:
                continue
            print(file['path'])
            for s in matches[:10]:
                print('  line {}: {} {}'.format(s['line'], s['kind'], s['name']))
            count += 1
            if count >= max(1, args.limit):
                break
        print('Matched files displayed:', count)
        return 0
    index = build(root)
    folder.mkdir(exist_ok=True)
    if any(p.startswith('docs-private/') for p in run_git(root, 'ls-files').splitlines()):
        print('ERROR: docs-private contains Git-tracked files.', file=sys.stderr)
        return 1
    path.write_text(json.dumps(index, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    markdown = ['# LunaCare private code index', '', 'Local only; do not commit this file.', '',
                'Revision: ' + index['revision'], '',
                'Indexed {} files and {} symbols.'.format(index['indexed_files'], index['symbol_count']), '']
    for file in index['files']:
        markdown.append('## ' + file['path'])
        for sym in file['symbols']:
            markdown.append('- {}: {} (line {})'.format(sym['kind'], sym['name'], sym['line']))
        markdown.append('')
    (folder / 'CODE_INDEX.md').write_text('\n'.join(markdown) + '\n', encoding='utf-8')
    print('Indexed {} files, {} symbols -> docs-private/code-index.json and CODE_INDEX.md'.format(
        index['indexed_files'], index['symbol_count']))
    return 0

if __name__ == '__main__':
    sys.exit(main())
