"""Exercise CI decisions and documentation failures against real temporary Git repos."""

import contextlib
import importlib.util
import io
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPTS = Path(__file__).resolve().parents[1]


def load_script(name):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / f'{name}.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


detector = load_script('detect_changes')
docs = load_script('check_docs')


class RepositoryTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.git('init', '--initial-branch=main')
        self.write('README.md', '# Example\n')
        self.write('docs/guide.md', '# Guide\n')
        self.write('app/src/Main.java', 'class Main {}\n')
        self.base = self.commit()

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root,
                                       stderr=subprocess.PIPE).decode().strip()

    def write(self, path, text):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding='utf-8')

    def commit(self):
        self.git('add', '--all')
        self.git('-c', 'user.name=CI Test', '-c', 'user.email=ci@example.invalid',
                 'commit', '-m', 'fixture')
        return self.git('rev-parse', 'HEAD')

    def detect(self, event_name, event):
        previous = Path.cwd()
        try:
            os.chdir(self.root)
            with contextlib.redirect_stdout(io.StringIO()):
                return detector.detect_changes(event_name, event)
        finally:
            os.chdir(previous)

    def assert_decision(self, base, head, expected):
        events = {
            'push': {'before': base, 'after': head},
            'pull_request': {'pull_request': {'base': {'sha': base}, 'head': {'sha': head}}},
        }
        for name, event in events.items():
            with self.subTest(event=name):
                self.assertEqual(self.detect(name, event), (expected, base, head))


class ChangeDetectionTests(RepositoryTest):
    def test_documentation_and_assets_use_same_rule_for_pr_and_push(self):
        self.write('README.md', '# Updated\n')
        self.write('docs/nested/流程 图.svg', '<svg/>\n')
        self.write('notes.markdown', '# Notes\n')
        self.assert_decision(self.base, self.commit(), True)

    def test_runtime_dependency_and_workflow_paths_require_full_build(self):
        for path in ('app/src/Main.java', 'app/src/main/res/values/strings.xml',
                     'app/src/main/res/readme.md', 'gradle/wrapper/gradle-wrapper.properties',
                     'gradle/README.md', 'build.gradle', 'settings.gradle',
                     'gradle.properties', 'gradlew', '.github/workflows/build.yaml',
                     '.github/README.md', 'scripts/README.md', '.gitignore'):
            with self.subTest(path=path):
                base = self.git('rev-parse', 'HEAD')
                self.write(path, 'changed\n')
                self.assert_decision(base, self.commit(), False)

    def test_full_range_includes_code_before_latest_documentation_commit(self):
        self.write('app/src/Main.java', 'class Main { int value; }\n')
        self.commit()
        self.write('README.md', '# Latest documentation\n')
        self.assert_decision(self.base, self.commit(), False)

    def test_pr_uses_merge_base_when_target_branch_advances(self):
        self.git('checkout', '-b', 'topic')
        self.write('docs/guide.md', '# Topic\n')
        head = self.commit()
        self.git('checkout', 'main')
        self.write('app/src/Main.java', 'class Main { int upstream; }\n')
        base_tip = self.commit()
        event = {'pull_request': {'base': {'sha': base_tip}, 'head': {'sha': head}}}
        self.assertEqual(self.detect('pull_request', event), (True, self.base, head))

    def test_documentation_deletion(self):
        (self.root / 'docs/guide.md').unlink()
        self.assert_decision(self.base, self.commit(), True)

    def test_code_deletion(self):
        (self.root / 'app/src/Main.java').unlink()
        self.assert_decision(self.base, self.commit(), False)

    def test_code_moved_into_docs_still_builds(self):
        self.git('mv', 'app/src/Main.java', 'docs/Main.md')
        self.assert_decision(self.base, self.commit(), False)

    def test_docs_moved_into_code_still_builds(self):
        self.git('mv', 'docs/guide.md', 'app/src/guide.md')
        self.assert_decision(self.base, self.commit(), False)

    def test_doc_rename_with_unusual_filename(self):
        self.git('mv', 'docs/guide.md', 'docs/新 指南\n第二行.md')
        self.assert_decision(self.base, self.commit(), True)

    def test_all_files_are_checked_beyond_path_filter_api_limit(self):
        for index in range(350):
            self.write(f'docs/{index}.md', '# Document\n')
        self.write('settings.gradle', 'changed\n')
        self.assert_decision(self.base, self.commit(), False)

    def test_empty_diff_and_unavailable_history_use_full_build(self):
        self.assert_decision(self.base, self.base, False)
        for before in ('0' * 40, 'f' * 40, '', '--invalid'):
            with self.subTest(before=before):
                self.assertEqual(self.detect('push', {'before': before, 'after': self.base}),
                                 (False, '', ''))

    def test_manual_trigger_always_builds(self):
        self.write('README.md', '# Docs only\n')
        head = self.commit()
        self.assertEqual(self.detect('workflow_dispatch', {'before': self.base, 'after': head}),
                         (False, '', ''))

    def test_cli_writes_github_outputs(self):
        self.write('README.md', '# Updated\n')
        head = self.commit()
        event_path = self.root / 'event.json'
        event_path.write_text(json.dumps({'before': self.base, 'after': head}))
        output_path = self.root / 'output'
        env = dict(os.environ, GITHUB_EVENT_NAME='push', GITHUB_EVENT_PATH=str(event_path),
                   GITHUB_OUTPUT=str(output_path))
        subprocess.run([os.sys.executable, str(SCRIPTS / 'detect_changes.py')],
                       cwd=self.root, env=env, capture_output=True, check=True)
        self.assertEqual(output_path.read_text(),
                         f'docs_only=true\nbase_sha={self.base}\nhead_sha={head}\n')


class DocumentationTests(RepositoryTest):
    def check(self):
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            return docs.check_docs(self.root)

    def test_valid_files_chinese_duplicate_and_explicit_anchors(self):
        self.write('docs/guide.md', '# 中文标题\n## Repeat\n## Repeat\n## Repeat-1\n'
                   '<a id="custom"></a>\n')
        self.write('docs/space name.markdown', '# File\n')
        self.write('README.md', '[Chinese](docs/guide.md#中文标题)\n'
                   '[Duplicate](docs/guide.md#repeat-1)\n'
                   '[Collision](docs/guide.md#repeat-1-1)\n'
                   '[Explicit](docs/guide.md#custom)\n'
                   '[Space](docs/space%20name.markdown#file)\n'
                   '[Source](app/src/Main.java)\n'
                   '[Reference][guide]\n[guide]: docs/guide.md#custom\n'
                   '[External](https://example.invalid/missing#anchor)\n'
                   '`[Example](missing.md)`\n```md\n[Example](missing.md)\n```\n')
        self.git('add', '--all')
        self.assertTrue(self.check())

    def test_missing_file_fails(self):
        self.write('README.md', '[Broken](docs/missing.md)\n')
        self.assertFalse(self.check())

    def test_missing_anchor_fails(self):
        self.write('README.md', '[Broken](docs/guide.md#missing)\n')
        self.assertFalse(self.check())

    def test_undefined_reference_fails(self):
        self.write('README.md', '[Broken][undefined]\n')
        self.assertFalse(self.check())

    def test_deleted_target_is_detected_in_other_documents(self):
        self.write('README.md', '[Guide](docs/guide.md)\n')
        self.git('rm', 'docs/guide.md')
        self.assertFalse(self.check())


if __name__ == '__main__':
    unittest.main()
