"""Index textual candidates; this does not assign presentation ownership."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'docs/audit/full-spell-roster'
roster = json.loads((DEST / 'runtime-roster.json').read_text(encoding='utf-8'))
source = (ROOT / 'src/main/java/dev/wildercord/spell/Runes.java').read_text(encoding='utf-8')
constants = {path: name for name, path in re.findall(
    r'public static final RuneDef (\w+)\s*=\s*(?:shape|effect|modifier|link)\("([^"\n]+)"', source)}
files = []
for file in sorted((ROOT / 'src').rglob('*.java')):
    relative = file.relative_to(ROOT).as_posix()
    if not ('/main/' in relative or '/client/' in relative):
        continue
    if not any(word in file.stem for word in ['Vfx', 'Fx', 'Feels', 'Effects', 'Shapes', 'Formations', 'Flights', 'Comets', 'Circle', 'Forms']):
        continue
    files.append((relative, file.read_text(encoding='utf-8').splitlines()))
rows = []
for rune in roster['runes']:
    path = rune['id'].split(':', 1)[1]
    constant = constants.get(path)
    pattern = re.compile(r'\bRunes\.' + re.escape(constant) + r'\b') if constant else None
    candidates = []
    for relative, lines in files:
        matches = [number for number, line in enumerate(lines, 1)
                   if '"' + path + '"' in line or (pattern and pattern.search(line))]
        if matches:
            candidates.append({'file': relative, 'lines': matches})
    rows.append({'id': rune['id'], 'constant': constant, 'candidates': candidates,
                 'status': 'Textual references only; inspect dispatch and actual presentation before assigning an owner or verification.'})
(DEST / 'source-references.json').write_text(json.dumps(rows, indent=2) + '\n', encoding='utf-8', newline='\n')
print(f'Indexed {len(rows)} rune rows across {len(files)} candidate source files; native review remains pending.')
