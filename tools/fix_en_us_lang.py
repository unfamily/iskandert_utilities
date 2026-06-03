#!/usr/bin/env python3
"""Fix UTF-8 mojibake and corrupted Minecraft formatting codes in lang JSON files."""
from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LANG_DIR = ROOT / "src/main/resources/assets/iska_utils/lang"

# UTF-8 bytes mis-decoded as Latin-1 (e.g. section sign, middle dot, plus-minus).
_MOJIBAKE_RE = re.compile(r"(?:Â.|Ã.|â€.)")


def fix_mojibake_substrings(text: str) -> str:
    def repl(match: re.Match[str]) -> str:
        chunk = match.group(0)
        try:
            return chunk.encode("latin-1").decode("utf-8")
        except (UnicodeDecodeError, UnicodeEncodeError):
            return chunk

    return _MOJIBAKE_RE.sub(repl, text)


def fix_text(text: str) -> str:
    text = text.replace("\uFFFD", "")
    text = re.sub(r"\?([0-9a-fk-or])", r"§\1", text, flags=re.IGNORECASE)
    text = fix_mojibake_substrings(text)
    return text


def collect_targets() -> list[Path]:
    if not LANG_DIR.is_dir():
        return []
    return sorted(LANG_DIR.glob("*.json"))


def main(argv: list[str] | None = None) -> int:
    argv = argv if argv is not None else sys.argv[1:]
    check_only = "--check" in argv
    targets = collect_targets()
    if not targets:
        print(f"No lang JSON files under {LANG_DIR}", file=sys.stderr)
        return 1

    needs_fix: list[Path] = []
    for path in targets:
        raw = path.read_bytes()
        text = raw.decode("utf-8", errors="replace")
        if fix_text(text) != text:
            needs_fix.append(path)

    if check_only:
        if needs_fix:
            print("Lang files need fixing:")
            for path in needs_fix:
                print(f"  {path.relative_to(ROOT)}")
            return 1
        print(f"All {len(targets)} lang file(s) OK.")
        return 0

    changed = 0
    for path in targets:
        raw = path.read_bytes()
        text = raw.decode("utf-8", errors="replace")
        fixed = fix_text(text)
        if fixed == text:
            continue
        path.write_text(fixed, encoding="utf-8", newline="\n")
        changed += 1
        print(f"Fixed {path.relative_to(ROOT)}")

    if changed == 0:
        print(f"No changes ({len(targets)} file(s) checked).")
    else:
        print(f"Fixed {changed} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
