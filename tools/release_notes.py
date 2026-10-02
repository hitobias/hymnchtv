#!/usr/bin/env python3
r"""Release helpers for tools/release.sh (sub-project Z).

Usage:
  release_notes.py notes X.Y.Z path/to/changelog_master.xml   Markdown release notes on stdout
  release_notes.py check-version-code X.Y.Z CODE              exit 0 if CODE follows the convention

versionCode convention: X*100000 + Y*1000 + Z*10 + n, n = 0..9 (rebuild digit).
The changelog is a trusted file from this repository.

>>> expected_code_base("1.0.0")
100000
>>> expected_code_base("2.9.2")
209020
>>> expected_code_base("2.10.0")
210000
>>> code_matches("2.10.0", 210000), code_matches("2.10.0", 210009), code_matches("2.10.0", 210010)
(True, True, False)
>>> code_matches("2.10.0", 209999)
False
>>> expected_code_base("2.100.0")
Traceback (most recent call last):
...
ValueError: minor and patch must be <= 99: 2.100.0
>>> change_to_markdown("修正 <b>錯誤</b>\n     第二行 <a href='x'>連結</a> ")
'- 修正 錯誤 第二行 連結'
"""
import re
import sys
import xml.etree.ElementTree as ET

TAG = re.compile(r"<[^>]+>")
SPACE = re.compile(r"\s+")


def expected_code_base(version):
    major, minor, patch = (int(part) for part in version.split("."))
    if minor > 99 or patch > 99:
        raise ValueError(f"minor and patch must be <= 99: {version}")
    return major * 100000 + minor * 1000 + patch * 10


def code_matches(version, code):
    return 0 <= int(code) - expected_code_base(version) <= 9


def change_to_markdown(text):
    return "- " + SPACE.sub(" ", TAG.sub("", text)).strip()


def release_notes(version, changelog_path):
    root = ET.parse(changelog_path).getroot()
    for release in root.iter("release"):
        if release.get("version", "").split(" ")[0] == version:
            items = [change_to_markdown("".join(change.itertext())) for change in release.iter("change")]
            if not items:
                raise LookupError(f"<release> {version} has no <change> entries")
            return "\n".join(items) + "\n"
    raise LookupError(f"no <release> entry for {version} in {changelog_path}")


def main(argv):
    try:
        if len(argv) == 4 and argv[1] == "notes":
            sys.stdout.write(release_notes(argv[2], argv[3]))
            return 0
        if len(argv) == 4 and argv[1] == "check-version-code":
            return 0 if code_matches(argv[2], argv[3]) else 1
    except (LookupError, ValueError, ET.ParseError) as error:
        sys.stderr.write(f"release_notes.py: {error}\n")
        return 1
    sys.stderr.write(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
