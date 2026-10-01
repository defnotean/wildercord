"""Regenerate assets and verify game-visible pixels and all other file bytes.

PNG compression varies across Pillow/zlib platforms. Compare decoded RGBA pixels,
dimensions and frame count for PNGs, while JSON, recipes, sounds and other assets
must remain byte-identical. Also reject added or removed generated paths.
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
