"""Regenerate assets and verify game-visible pixels and all other file bytes.

PNG compression varies across Pillow/zlib platforms. Compare decoded RGBA pixels,
dimensions and frame count for PNGs, while JSON, recipes, sounds and other assets
must remain byte-identical. Also reject added or removed generated paths.
Afterwards every file the generator rewrote is put back byte for byte and every file it
added is deleted, pass or fail, so the checkout stays clean and later checks of the same
candidate can still run.
"""
import hashlib
import subprocess
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent


def generated_files():
    paths = list((ROOT / "src/main/resources").rglob("*")) + [ROOT / "docs/RECIPES.md", ROOT / "docs/features/fusion-altar.md"]
    return {path.relative_to(ROOT).as_posix(): path.read_bytes() for path in paths if path.is_file()}


def snapshot():
    result = {}
    for name in generated_files():
        path = ROOT / name
        if path.suffix == ".png":
            with Image.open(path) as image:
                frames = []
                for frame in range(getattr(image, "n_frames", 1)):
                    image.seek(frame)
                    frames.append((image.size, hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest()))
                value = tuple(frames)
        else:
            value = hashlib.sha256(path.read_bytes()).hexdigest()
        result[name] = value
    return result


def restore(original):
    """Write back every file the generator changed or removed and delete every file it added."""
    current = generated_files()
    for name in current.keys() - original.keys():
        (ROOT / name).unlink()
    for name, data in original.items():
        if current.get(name) != data:
            (ROOT / name).parent.mkdir(parents=True, exist_ok=True)
            (ROOT / name).write_bytes(data)


def main():
    original = generated_files()
    before = snapshot()
    try:
        subprocess.run([sys.executable, str(ROOT / "tools/generate_assets.py")], cwd=ROOT, check=True)
        after = snapshot()
    finally:
        restore(original)
    changed = sorted(p for p in before.keys() | after.keys() if before.get(p) != after.get(p))
    if changed:
        print("Generated assets differ. Run tools/generate_assets.py and commit:", file=sys.stderr)
        print("\n".join(changed), file=sys.stderr)
        return 1
    print(f"Verified {len(before)} generated paths: identical PNG pixels and exact bytes for all other files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
