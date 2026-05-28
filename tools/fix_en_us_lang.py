#!/usr/bin/env python3
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TARGETS = [
    ROOT / "src/main/resources/assets/iska_utils/lang/en_us.json",
]


def fix_text(s: str) -> str:
    # Fix Minecraft formatting codes that got corrupted (e.g. "?7" instead of "§7").
    s = re.sub(r"\?([0-9a-fk-or])", r"§\1", s, flags=re.IGNORECASE)
    # Drop replacement characters if they appeared during a bad decode.
    # (Keep this conservative; we only remove the exact U+FFFD char.)
    s = s.replace("\uFFFD", "")
    return s


def main() -> int:
    changed = 0
    for path in TARGETS:
        if not path.exists():
            continue
        raw = path.read_bytes()
        text = raw.decode("utf-8", errors="replace")
        fixed = fix_text(text)
        if fixed != text:
            path.write_text(fixed, encoding="utf-8", newline="\n")
            changed += 1
    print(f"Fixed {changed} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
