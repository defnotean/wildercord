"""The moving pictures in the README (and on the mod's page), from the feature tour's frames.

Run the tour first (WILDERCORD_TOUR_ONLY=1 ./gradlew runClientGameTest), then from the project root:
    python tools/make_gif.py
It reads build/run/clientGameTest/screenshots/tour_*.png and writes, in docs/images:
    hero.gif            charging a spell, the release, and a Domain unfolding (tour_hero_*)
    shield-block.gif    a strong Shield's circles spawning in and stopping a bolt (tour_gif_shield_block_*)
    shield-shatter.gif  a weaker Shield's circles shattering, front to back (tour_gif_shield_break_*)
    glyph.gif           a Frost glyph going off under a husk (tour_gif_glyph_*)
    spells.gif          a slideshow of the spell shapes (tour_shape_*)
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
        print("no tour_hero_*.png frames: keeping the hero GIF there is")
        return
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


def save(frames, durations, out):
    """One shared palette keeps the file small and the colours steady from frame to frame."""
    palette = frames[len(frames) // 2].quantize(colors=192, method=Image.Quantize.MEDIANCUT)
    quantized = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in frames]
    quantized[0].save(out, save_all=True, append_images=quantized[1:], duration=durations, loop=0, optimize=True, disposal=1)
    print(f"{out.relative_to(ROOT)}: {len(frames)} frames, {out.stat().st_size // 1024} KB")


def crop(im, cx, cy, keep):
    """A 16:9 window {keep} of the screen wide, centred on ({cx}, {cy}) as fractions of it."""
    w, h = im.size
    cw, ch = int(w * keep), int(h * keep)
    left = min(max(0, int(w * cx - cw / 2)), w - cw)
    top = min(max(0, int(h * cy - ch / 2)), h - ch)
    return im.crop((left, top, left + cw, top + ch)).resize(SIZE, Image.LANCZOS)


def clip(name, out, cx, cy, keep, ms, hold=900):
    """A clip from a run of frames taken a tick apart."""
    files = sorted(FRAMES.glob(f"tour_{name}_*.png"))
    if not files:
        print(f"no tour_{name}_*.png frames: skipped {out}")
        return
    frames = [crop(Image.open(f).convert("RGB"), cx, cy, keep) for f in files]
    durations = [ms] * len(frames)
    durations[-1] = hold
    save(frames, durations, ROOT / "docs/images" / out)


# The shapes the slideshow shows, in order, with a caption-free crop of each.
SLIDES = ["beam", "crescent", "burst", "pillar", "rain", "nova", "prism", "comet", "lance", "domain"]


def slideshow(out, hold=1100, blend=3):
    """Each shape's still in turn, a short cross-fade between them."""
    stills = []
    for shape in SLIDES:
        f = FRAMES / f"tour_shape_{shape}.png"
        if f.exists():
            stills.append(Image.open(f).convert("RGB").resize(SIZE, Image.LANCZOS))
    if len(stills) < 2:
        print(f"not enough tour_shape_*.png stills: skipped {out}")
        return
    frames, durations = [], []
    for i, still in enumerate(stills):
        frames.append(still)
        durations.append(hold)
        nxt = stills[(i + 1) % len(stills)]
        for k in range(1, blend + 1):
            frames.append(Image.blend(still, nxt, k / (blend + 1)))
            durations.append(60)
    save(frames, durations, ROOT / "docs/images" / out)


def extras():
    clip("gif_shield_block", "shield-block.gif", 0.53, 0.44, 0.58, 70)
    clip("gif_shield_break", "shield-shatter.gif", 0.55, 0.46, 0.6, 60)
    clip("gif_glyph", "glyph.gif", 0.46, 0.5, 0.72, 80)
    slideshow("spells.gif")


if __name__ == "__main__":
    main()
    extras()
