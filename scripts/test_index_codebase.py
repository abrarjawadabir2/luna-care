"""Standard-library tests for the private code symbol indexer."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('index_codebase.py')


class IndexerTests(unittest.TestCase):
    def test_symbols_and_secret_exclusion(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            files = {
                '.gitignore': 'docs-private/\n.env\n',
                'app/src/Main.kt': '@Composable\nfun WelcomeScreen() = Unit\nclass ProfileViewModel {}\n',
                'backend/src/routes.ts': "fastify.get('/health', handler);\nexport function runServer() {}\n",
                'database/schema.sql': 'create table if not exists public.profiles (id text);\n',
                'backend/.env': 'GEMINI_API_KEY=sensitive-value-never-index\n',
                'docs-private/SECRET.md': 'PRIVATE_PLAN_SECRET=unpublished\n',
                'README.md': '# Public readme\n',
            }
            for name, content in files.items():
                dest = root / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_text(content, encoding='utf-8')
            subprocess.run(['git', '-C', str(root), 'add', '.'], check=True)
            proc = subprocess.run([sys.executable, str(SCRIPT), '--root', str(root)],
                                  capture_output=True, text=True)
            self.assertEqual(proc.returncode, 0, proc.stderr)
            idx = json.loads((root / 'docs-private/code-index.json').read_text())
            self.assertEqual(idx['indexed_files'], 3)
            result_text = json.dumps(idx)
            self.assertNotIn('sensitive-value-never-index', result_text)
            self.assertNotIn('unpublished', result_text)
            self.assertNotIn('GEMINI_API_KEY', result_text)
            self.assertIn('WelcomeScreen', result_text)
            self.assertIn('GET /health', result_text)
            self.assertIn('public.profiles', result_text)
            self.assertTrue((root / 'docs-private/CODE_INDEX.md').exists())
            query = subprocess.run([sys.executable, str(SCRIPT), '--root', str(root), '--query', 'welcome'],
                                   capture_output=True, text=True)
            self.assertEqual(query.returncode, 0, query.stderr)
            self.assertIn('WelcomeScreen', query.stdout)
            tracked = subprocess.run(['git', '-C', str(root), 'ls-files', 'docs-private'],
                                     capture_output=True, text=True, check=True)
            self.assertEqual(tracked.stdout, '')


if __name__ == '__main__':
    unittest.main()
