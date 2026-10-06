"""Regression coverage for the iOS shared-localization source audit."""
import runpy
import unittest
from pathlib import Path


CHECK = runpy.run_path(str(Path(__file__).resolve().parents[2] / 'scripts/ios-localization.py'))['check_source']


class LocalizationAuditTests(unittest.TestCase):
    def check(self, source):
        CHECK(source, {'歌曲': {}, '下载 {0}': {}}, 'Example.swift')

    def test_shared_keys_and_interpolation_arguments(self):
        self.check('Text(tr("歌曲")); Text(tr("下载 {0}", count))')

    def test_chinese_copy_requires_shared_key(self):
        with self.assertRaisesRegex(AssertionError, 'Unlocalized Swift text'):
            self.check('Text("歌曲")')

    def test_english_copy_requires_shared_key(self):
        for source in ['Text("Download")', 'Text("コピー")', 'Button("Retry") {}', '.navigationTitle("Settings")',
                       'SearchBar(prompt: "Search songs")', '.accessibilityHint("Copy title")',
                       r'Text("Downloaded \(count) songs")']:
            with self.subTest(source=source), self.assertRaisesRegex(AssertionError, 'Unlocalized Swift UI text'):
                self.check(source)

    def test_nested_interpolation_is_checked(self):
        with self.assertRaisesRegex(AssertionError, 'Unlocalized Swift text'):
            self.check(r'Text("\(ready ? "完成" : "等待")")')

    def test_fixed_game_terms_and_numeric_notation(self):
        self.check(r'Text("Rating"); Text("B30 · \(count) / 30"); Text("Lv. \(level)"); Text("↑0.15")')

    def test_identifiers_and_symbols_are_not_copy(self):
        self.check('Image(systemName: "magnifyingglass").accessibilityIdentifier("song-search")')

    def test_missing_or_interpolated_keys_are_rejected(self):
        for source in ['Text(tr("Missing"))', r'Text(tr("下载 \(count)"))']:
            with self.subTest(source=source), self.assertRaises(AssertionError):
                self.check(source)


if __name__ == '__main__':
    unittest.main()
