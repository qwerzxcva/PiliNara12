import tempfile
import unittest
from pathlib import Path
from check_lint_report import check_report


class LintGateTests(unittest.TestCase):
    def check(self, text):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'lint.xml'
            path.write_text(text, encoding='utf-8')
            check_report(path)

    def test_zero_issues_is_valid(self):
        self.check('<issues format="6"/>')

    def test_errors_fail(self):
        for severity in ('Fatal', 'Error'):
            with self.subTest(severity=severity), self.assertRaises(ValueError):
                self.check(f'<issues><issue id="Sample" severity="{severity}"/></issues>')

    def test_warning_is_reported_but_not_fatal(self):
        self.check('<issues><issue id="ChromeOsAbiSupport" severity="Warning"/></issues>')

    def test_information_is_not_failure(self):
        self.check('<issues><issue severity="Information"/></issues>')

    def test_missing_report_fails(self):
        with self.assertRaises(OSError):
            check_report(Path('/definitely-missing-lint-report.xml'))

    def test_wrong_root_fails(self):
        with self.assertRaises(ValueError):
            self.check('<html/>')

    def test_malformed_report_fails(self):
        with self.assertRaises(Exception):
            self.check('<issues>')

    def test_unknown_severity_fails(self):
        with self.assertRaises(ValueError):
            self.check('<issues><issue severity="Unknown"/></issues>')


if __name__ == '__main__':
    unittest.main()
