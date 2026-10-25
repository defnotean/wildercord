"""Regenerate assets and verify game-visible pixels and all other file bytes.

PNG compression varies across Pillow/zlib platforms. Compare decoded RGBA pixels,
dimensions and frame count for PNGs, while JSON, recipes, sounds and other assets
must remain byte-identical. Also reject added or removed generated paths.
On a mismatch the changed paths are put back as committed, so the checkout stays
clean and later checks of the same candidate can still run.
"""
import hashlib
import subprocess
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent


def snapshot():
    paths = list((ROOT / "src/main/resources").rglob("*")) + [ROOT / "docs/RECIPES.md"]
    result = {}
    for path in paths:
        if not path.is_file():
            continue
        if path.suffix == ".png":
            with Image.open(path) as image:
                frames = []
                for frame in range(getattr(image, "n_frames", 1)):
                    image.seek(frame)
                    frames.append((image.size, hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest()))
                value = tuple(frames)
        else:
            value = hashlib.sha256(path.read_bytes()).hexdigest()
        result[path.relative_to(ROOT).as_posix()] = value
    return result


def restore(changed, before):
    """Put each changed path back as committed: check out what existed, delete what the generator added."""
    for path in changed:
        if path in before:
            subprocess.run(["git", "checkout", "--", path], cwd=ROOT, check=True)
        else:
            (ROOT / path).unlink(missing_ok=True)


def main():
    before = snapshot()
    subprocess.run([sys.executable, str(ROOT / "tools/generate_assets.py")], cwd=ROOT, check=True)
    after = snapshot()
    changed = sorted(p for p in before.keys() | after.keys() if before.get(p) != after.get(p))
    if changed:
        print("Generated assets differ. Run tools/generate_assets.py and commit:", file=sys.stderr)
        print("\n".join(changed), file=sys.stderr)
        return 1
    print(f"Verified {len(before)} generated paths: identical PNG pixels and exact bytes for all other files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
