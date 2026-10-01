"""Snapshot all uncommitted paths; preserve changes and avoid attributing earlier work."""
from collections import Counter
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
output = ROOT / "docs/audit/2026-09-30-uncommitted-file-inventory.md"
output.touch(exist_ok=True)
records = subprocess.check_output(["git", "status", "--porcelain=v1", "-z", "--untracked-files=all"], cwd=ROOT).decode("utf-8").split("\0")
rows = []
index = 0
while index < len(records):
    record = records[index]
    index += 1
    if not record:
        continue
    status, path = record[:2], record[3:]
    if "R" in status or "C" in status:
        original = records[index]
        index += 1
        path = original + " → " + path
    rows.append((status, path))
groups = Counter(path.split("/")[0] for _, path in rows)
text = ["# Uncommitted working-tree file inventory", "",
        "Snapshot of the WilderCord checkout on September 30, 2026, after implementation. This includes changes already present before this implementation; it does not attribute every path to one work session. No commit or push was performed.", "",
        f"**{len(rows)} paths:** {sum(status == '??' for status, _ in rows)} untracked, {sum(status != '??' for status, _ in rows)} tracked changes. Generated captures, logs, profiles and build outputs are ignored and listed separately in the implementation report.", "",
        "## Directory totals", "", "| Directory | Paths |", "|---|---:|"]
text.extend(f"| {name} | {count} |" for name, count in sorted(groups.items()))
text += ["", "## Full path list", "", "Git status uses `??` for an untracked file; the two status columns otherwise describe index and working-tree changes.", "", "| Status | Path |", "|---|---|"]
text.extend(f"| `{status}` | `{path}` |" for status, path in sorted(rows, key=lambda row: row[1]))
output.write_text("\n".join(text) + "\n", encoding="utf-8")
print(f"{output}: {len(rows)} uncommitted paths")
