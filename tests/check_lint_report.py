#!/usr/bin/env python3
"""Fail closed on missing/malformed lint XML; warnings are not silently ignored."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


def check_report(path: Path) -> None:
    root = ET.parse(path).getroot()
    if root.tag != 'issues':
        raise ValueError('Expected Android Lint <issues> report')
    failures = []
    for issue in root.findall('issue'):
        severity = issue.get('severity', '')
        if severity not in {'Fatal', 'Error', 'Warning', 'Information', 'Ignore'}:
            raise ValueError(f'Unknown lint severity: {severity!r}')
        # Warnings stay visible in the uploaded report. ChromeOsAbiSupport
        # specifically asks for x86, which this arm64-only app must not add.
        if severity in {'Fatal', 'Error'}:
            failures.append(f"{issue.get('id', '?')}: {severity}: {issue.get('message', '')}")
    if failures:
        raise ValueError('\n'.join(failures))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('report', type=Path)
    args = parser.parse_args()
    try:
        check_report(args.report)
    except (OSError, ET.ParseError, ValueError) as error:
        parser.exit(1, f'Lint gate failed: {error}\n')
    root = ET.parse(args.report).getroot()
    warnings = sum(issue.get('severity') == 'Warning' for issue in root.findall('issue'))
    print(f'Lint gate passed: 0 fatal/errors, {warnings} warnings (see report)')


if __name__ == '__main__':
    main()
