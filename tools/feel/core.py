"""What an element's sound file needs: the DSP of tools/sound_art.py and a way to declare an event.

A file `tools/feel/<part>.py` (a part is an element, or `neutral`) defines builder functions and lists them:

    from feel.core import sa, event

    def fire_flick(v, rng):            # v: the variant number (0, 1, 2...), rng: numpy Generator seeded from the name
        ...
        return sa.finish(x, "impact")  # always end with finish(): it levels by role and removes clicks

    EVENTS = [event("fire_flick", fire_flick, variants=3, subtitle="hit")]

Nothing else to edit: `python tools/feel/build.py --only fire` writes the .ogg files, the part's manifest, and regenerates
sounds.json and kit_sounds.json (the Java registry reads that one).
"""
import re
import sys
from dataclasses import dataclass
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
if str(TOOLS) not in sys.path:
    sys.path.insert(0, str(TOOLS))

import sound_art as sa  # noqa: E402  (the DSP primitives, ROOT_HZ, note(), the scale degrees D E FS A B)

ELEMENTS = sa.ELEMENTS
PARTS = ELEMENTS + ("neutral",)
NAME = re.compile(r"^[a-z][a-z0-9_]*$")

# The four subtitle texts every kit event picks from (generate_assets.py NEW_LANG): a sound never needs its own line.
SUBTITLES = ("cast", "hit", "field", "tell")


@dataclass(frozen=True)
class Event:
    name: str                 # <element>_<verb>, e.g. fire_flick; neutral sounds have no prefix (tick, zap)
    build: object             # builder(v, rng) -> numpy array, ends with sa.finish(...)
    variants: int = 1
    role: str = "effect"      # informational: the role given to sa.finish inside the builder
    attenuation: int = 0      # blocks it can be heard from (0: Minecraft's 16)
    subtitle: str = "hit"     # one of SUBTITLES
    loop: bool = False        # seamless loop (checked as one)


def event(name, build, variants=1, role="effect", attenuation=0, subtitle="hit", loop=False):
    if not NAME.match(name):
        raise ValueError(f"event name {name!r}: lower-case letters, digits and underscores, starting with a letter")
    if subtitle not in SUBTITLES:
        raise ValueError(f"event {name}: subtitle must be one of {SUBTITLES}")
    if not 1 <= variants <= 6:
        raise ValueError(f"event {name}: 1 to 6 variants")
    return Event(name, build, variants, role, attenuation, subtitle, loop)
