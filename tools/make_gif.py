"""The moving header at the top of the README, from the feature tour's frames.

Run the tour first (WILDERCORD_TOUR_ONLY=1 ./gradlew runClientGameTest), then from the project root:
    python tools/make_gif.py
It reads build/run/clientGameTest/screenshots/tour_hero_*.png and writes docs/images/hero.gif.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
FRAMES = ROOT / "build/run/clientGameTest/screenshots"
OUT = ROOT / "docs/images/hero.gif"
SIZE = (640, 360)
# The three parts of the clip, by frame number: how much of the screen to keep (centred), where the
# middle of the crop sits vertically, and how long each frame shows (ms).
PARTS = [
    (range(0, 18), 0.46, 0.6, 90),    # charging, close on the caster, the circle and their hands
    (range(18, 26), 0.62, 0.9, 70),    # the release: a nova bursting round them
    (range(26, 999), 0.72, 0.45, 110),  # a Domain unfolding
]


def part(i):
    for frames, keep, middle, ms in PARTS:
        if i in frames:
            return keep, middle, ms
    return PARTS[-1][1:]


def main():
    files = sorted(FRAMES.glob("tour_hero_*.png"))
    if not files:
        raise SystemExit("no tour_hero_*.png frames: run the tour first")
    frames, durations = [], []
    for i, f in enumerate(files):
        keep, middle, ms = part(i)
        im = Image.open(f).convert("RGB")
        w, h = im.size
        cw, ch = int(w * keep), int(h * keep)
        left, top = (w - cw) // 2, int((h - ch) * middle)
        frames.append(im.crop((left, top, left + cw, top + ch)).resize(SIZE, Image.LANCZOS))
        durations.append(ms)
    durations[17] = 500
    durations[-1] = 900
    # One shared palette keeps the file small and the colours steady from frame to frame.
    palette = frames[len(frames) // 2].quantize(colors=192, method=Image.Quantize.MEDIANCUT)
    quantized = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in frames]
    quantized[0].save(OUT, save_all=True, append_images=quantized[1:], duration=durations, loop=0, optimize=True, disposal=1)
    print(f"{OUT.relative_to(ROOT)}: {len(frames)} frames, {OUT.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
