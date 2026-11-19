"""Flag tick sentinels that can bite: a field set to a raw Long.MIN_VALUE and later subtracted.

`now - Long.MIN_VALUE` overflows negative, so an idle or cooldown check built on a fresh sentinel
reads as "not yet" forever (the mount crash and the contract-credit leak were both this). Guard the
use with an equality check (`last == Long.MIN_VALUE || ...`), halve the sentinel, or go through
Ticks.since / Ticks.NEVER. A line ending in `// ticks-ok` is an accepted exception.

Usage: python tools/lint_ticks.py [source-dir]   (default src/main/java)
Exit 0 when the tree is clean, 1 with one line per finding.
"""

import re
import sys
from pathlib import Path

# `name = Long.MIN_VALUE` but not `== Long.MIN_VALUE`, `>= Long.MIN_VALUE` or `Long.MIN_VALUE / 2`.
DECL = re.compile(r"(?<![=!<>])=\s*Long\.MIN_VALUE(?!\s*[/\d])")
NAME_BEFORE_EQ = re.compile(r"(\w+)\s*$")


def findings(path: Path):
    """Yield (line_number, name, path) for each unguarded raw sentinel that is subtracted."""
    source = path.read_text(encoding="utf-8", errors="replace")
    lines = source.splitlines()
    uses_ticks = "Ticks.since(" in source or "Ticks.NEVER" in source
    for number, line in enumerate(lines, 1):
        if "Long.MIN_VALUE" not in line or "ticks-ok" in line:
            continue
        if not DECL.search(line):
            continue
        # The name is the word just before the `=` (covers `long x = ...` and `a = -1, b = ...`).
        at_eq = line.index("= Long.MIN_VALUE") if "= Long.MIN_VALUE" in line else DECL.search(line).start()
        named = NAME_BEFORE_EQ.search(line[:at_eq].rstrip())
        if not named:
            continue
        name = named.group(1)
        if name in ("public", "static", "final", "private", "return"):
            continue
        # Subtracted: `now - tick` or `tick - now`, also through a qualifier (`now - voices.tick`).
        # The lookbehinds keep prose like "per-tick" from counting as a subtraction.
        subtracted = re.search(r"(?<![\w-])-\s*(?:\w+\.)?" + re.escape(name) + r"\b(?![\w-])", source) or \
            re.search(r"(?<![\w.])" + re.escape(name) + r"\s*-\s*(?=[\w(])", source)
        if not subtracted:
            continue
        guarded = re.search(r"(?<![\w.])(?:\w+\.)?" + re.escape(name) + r"\s*[!=]=|[!=]=\s*(?:\w+\.)?" + re.escape(name) + r"\b", source)
        if guarded or uses_ticks:
            continue
        yield number, name, path


def main(argv):
    root = Path(argv[1]) if len(argv) > 1 else Path("src/main/java")
    if not root.is_dir():
        print(f"not a directory: {root}", file=sys.stderr)
        return 2
    bad = 0
    for path in sorted(root.rglob("*.java")):
        for number, name, where in findings(path):
            print(f"{where}:{number}: {name} is set to a raw Long.MIN_VALUE and subtracted without a guard "
                  "(use Ticks.NEVER / Ticks.since, an equality check, or // ticks-ok)")
            bad += 1
    if bad:
        print(f"{bad} unguarded tick sentinel(s)", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
