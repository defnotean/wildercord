"""Builds the feel kit: every sound of every element file in tools/feel, and the files that register them.

    python tools/feel/build.py                 build every part, then merge
    python tools/feel/build.py --only fire     build only tools/feel/fire.py (others' manifests are left as they are), then merge
    python tools/feel/build.py --merge         only regenerate sounds.json and kit_sounds.json from the manifests
    python tools/feel/build.py --check         verify (no synthesis): every manifest file exists, sounds.json is up to date

What is generated (commit it; never edit it by hand):
    src/main/resources/assets/wildercord/sounds/kit/<part>/<name>[_n].ogg
    tools/feel/manifest/<part>.json      one per part: its events, files, subtitle, attenuation
    src/main/resources/assets/wildercord/sounds.json     manifest/base.json (tools/sound_art.py's) + every part's events
    src/main/resources/assets/wildercord/kit_sounds.json the names the Java registry (WildercordSounds.kit) registers

Runs are deterministic (seeds come from crc32("name/variant")) and idempotent: a second run changes nothing. A part only ever
writes and prunes its own folder, so two people working on two parts never touch each other's files; only the two merged
files are shared, and they are regenerated, so a conflict there is resolved by running --merge.
"""
import argparse
import json
import sys
import tempfile
import zlib
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from feel import core  # noqa: E402

sa = core.sa
ASSETS = sa.ASSETS
SOUNDS = sa.SOUNDS
KIT = SOUNDS / "kit"
MANIFESTS = Path(__file__).resolve().parent / "manifest"


def load(part):
    module = __import__(f"feel.{part}", fromlist=["EVENTS"])
    return list(module.EVENTS)


def all_names(parts):
    """Every kit event name, checked: unique, prefixed by its element, and not a base event's."""
    base = json.loads((MANIFESTS / "base.json").read_text(encoding="utf-8"))
    seen = {}
    for part in parts:
        for ev in load(part):
            if part != "neutral" and not ev.name.startswith(part + "_"):
                raise SystemExit(f"{part}.py: event {ev.name!r} must start with '{part}_'")
            if ev.name in base or ev.name in seen:
                raise SystemExit(f"{part}.py: event name {ev.name!r} is already taken by {seen.get(ev.name, 'the base palette')}")
            seen[ev.name] = part
    return seen


def build_part(part):
    events = load(part)
    folder = KIT / part
    written = set()
    manifest = {}
    with tempfile.TemporaryDirectory() as tmp:
        scratch = Path(tmp) / "sound.wav"
        for ev in events:
            files = []
            for v in range(ev.variants):
                rng = np.random.default_rng(zlib.crc32(f"{ev.name}/{v}".encode()))
                x = ev.build(v, rng)
                stem = f"{ev.name}_{v + 1}" if ev.variants > 1 else ev.name
                path = folder / f"{stem}.ogg"
                sa.encode(x, path, scratch)
                written.add(path)
                sa.check(ev.name, x, path, loop=ev.loop)
                files.append(f"kit/{part}/{stem}")
            manifest[ev.name] = {"files": files, "subtitle": ev.subtitle, "attenuation": ev.attenuation}
    if folder.exists():
        for stale in folder.glob("*.ogg"):
            if stale not in written:
                print(f"removing {stale.relative_to(sa.ROOT).as_posix()}, no longer made")
                stale.unlink()
    MANIFESTS.mkdir(parents=True, exist_ok=True)
    text = json.dumps({"part": part, "events": manifest}, indent=2, sort_keys=True) + "\n"
    (MANIFESTS / f"{part}.json").write_text(text, encoding="utf-8", newline="\n")
    print(f"{part}: {len(manifest)} events, {len(written)} files")


def merged():
    """(sounds.json text, kit_sounds.json text) from the manifests, in a fixed order."""
    events = json.loads((MANIFESTS / "base.json").read_text(encoding="utf-8"))
    kit = {}
    for part in core.PARTS:
        path = MANIFESTS / f"{part}.json"
        if not path.exists():
            continue
        for name, ev in json.loads(path.read_text(encoding="utf-8"))["events"].items():
            if name in events:
                raise SystemExit(f"{name}: defined twice")
            entries = []
            for f in ev["files"]:
                entry = {"name": f"wildercord:{f}"}
                if ev["attenuation"]:
                    entry["attenuation_distance"] = ev["attenuation"]
                entries.append(entry if len(entry) > 1 else entry["name"])
            events[name] = {"subtitle": f"subtitles.wildercord.kit.{ev['subtitle']}", "sounds": entries}
            kit[name] = {"part": part}
    return json.dumps(events, indent=2) + "\n", json.dumps({"events": kit}, indent=2, sort_keys=True) + "\n"


def merge():
    sounds, kit = merged()
    (ASSETS / "sounds.json").write_text(sounds, encoding="utf-8", newline="\n")
    (ASSETS / "kit_sounds.json").write_text(kit, encoding="utf-8", newline="\n")
    print(f"merged: {len(json.loads(kit)['events'])} kit events into sounds.json and kit_sounds.json")


def check():
    problems = []
    sounds, kit = merged()
    for name, text in (("sounds.json", sounds), ("kit_sounds.json", kit)):
        if (ASSETS / name).read_text(encoding="utf-8") != text:
            problems.append(f"{name} is out of date: run python tools/feel/build.py --merge")
    for part in core.PARTS:
        path = MANIFESTS / f"{part}.json"
        if not path.exists():
            continue
        for name, ev in json.loads(path.read_text(encoding="utf-8"))["events"].items():
            for f in ev["files"]:
                if not (SOUNDS / f"{f}.ogg").exists():
                    problems.append(f"{name}: {f}.ogg is missing: run python tools/feel/build.py --only {part}")
    for p in problems:
        print("!!", p)
    return not problems


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="+", choices=core.PARTS)
    ap.add_argument("--merge", action="store_true")
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    if args.check:
        sys.exit(0 if check() else 1)
    if not args.merge:
        all_names(core.PARTS)
        for part in args.only or core.PARTS:
            build_part(part)
    merge()


if __name__ == "__main__":
    main()
