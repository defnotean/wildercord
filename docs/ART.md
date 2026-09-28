# The Wildercord look

A one-page style guide, so everything in the mod (icons, screens, magic circles, spell effects,
creatures, sounds) keeps looking and sounding like the same world as it grows. The numbers here
are the ones in the code; when you change one, change it here too.

## The idea

Magic in Wildercord is **light written in the air**. Every spell draws itself: a circle opens, its
runes are readable, and what it does is made of shaped light in its element's colour. Nothing is a
cloud of generic particles, and nothing hides the player who cast it.

Three rules hold everywhere:

1. **Light adds, darkness subtracts.** All magic is drawn with additive blending (`GlowLayers.GLOW`):
   a soft coloured halo under a white-hot core, so overlapping light burns brighter. Void magic is
   the one exception: it takes light away (`GlowLayers.DARK`, the `Light.DARK` colour flag), with a
   thin violet rim so it still reads.
2. **Readable before pretty.** Every spell's circle writes its runes out (see `SpellSigil`), so a
   player who has learned the emblems can read what's coming. Effects carry their element's
   signature so you can tell fire from frost at a glance, even at a distance.
3. **Never in the caster's face.** Nothing opens within about a block of a player's own eyes
   (`Fx.send`, `Sigils.send`); circles stay small (a charging circle is 0.42 blocks across the
   frame's radius, a telegraph 0.4); a longer spell adds star points, never size.

## Colour

Each element owns a palette: its rune colour, a highlight (the hot core) and an accent (contrast,
shadows, motifs). Shapes, modifiers and links have one colour each.

| Element | Primary | Highlight | Accent | Signature |
|---|---|---|---|---|
| Fire | `#F06E32` | `#FFD060` | `#FF3A1A` | embers rising, heat flares, flame slashes |
| Frost | `#8CDCFF` | `#E6FAFF` | `#3A8CFF` | crystal shards, a hard shatter ring, frost creeping |
| Storm | `#FFE650` | `#FFFBE0` | `#A8C8FF` | branching lightning of short jagged rays |
| Wind | `#C8F0DC` | `#FFFFFF` | `#7FE0C0` | crescents spiralling on their own tilts |
| Earth | `#B48C5A` | `#E8C890` | `#6E5436` | cracked seals, dust rings, stone spires |
| Life | `#6EDC64` | `#E8FFB0` | `#FFA8D8` | petals and leaves, a soft bloom |
| Void | `#B45AF0` | `#E0B0FF` | `#1A0830` | darkness imploding round a black core |
| Arcane | `#E678DC` | `#FFD8FA` | `#9A7CFF` | star seals, comets on tilted orbits |
| Time | `#F2D98A` | `#FFF8E0` | `#C8962E` | clock faces, sweeping hands, golden ticks |
| Blood | `#D2283C` | `#FF6474` | `#5A0A14` | crimson cuts, heartbeat rings, drops |

Family colours: Shape `#40C8BE` (teal), Modifier `#F0C440` (gold), Link `#A064F0` (violet).

Screens use the stone-and-gold palette in `tools/gui_art.py` and `tools/sigil_art.py`: near-black
violet stone (`#1B1726`), bevels of `#4A3F66`, gold trim (`#B08A3E`, highlight `#E8C46A`) and
amethyst gems (`#A064F0`).

## Magic circles

Built like a classic magic circle, from the outside in (`SpellSigil`, sizes as fractions of the
radius): a heavy frame (line 0.03) and a fine one, with rays on the star's points; a band of script
made of the spell's own rune emblems; a band in the first effect's ring pattern; a star polygon
{p/q} with a point for every rune and a roundel on each point (that rune's pattern round its
emblem); an inner ring; and the shape's emblem as the seal. Fine lines are 0.012 of the radius.
Bands turn in opposite directions, slowly. A circle **opens in stages**: frame, then script and
star drawing themselves, then roundels one by one in casting order. Secret spells replace the star
with a centrepiece of their own.

Every rune's emblem and ring pattern is unique (`tools/circle_art.py`, checked at generation):
the pattern's line says the family (Shape double, Effect solid, Modifier dashed, Link chain), its
motif says the element, the emblem's frame says the family again (square, circle, diamond, octagon).

## Motion

- **Open fast, leave slowly**: things appear in 2 to 6 ticks with an ease-out, and fade over the
  last 30% of their life.
- **Weight by size**: small hits are a flare, a ring and a few sparks; big ones add a ground
  shockwave and a camera shake (`ScreenFx`, scaled by the player's Screen Effect Scale).
- **Budget**: a good effect is a few strong shapes and a handful of particles. Shaped light is one
  packet per piece; never send hundreds a tick.
- **Bodies move too**: a caster raises both hands while charging and moves with the shape when it
  goes off; the beads on their Cord burn brighter as the charge builds.

## Shields and glass

A Shield is invisible until a spell comes. Then its magic circles (`ShieldCircles`: the spell that
raised it, drawn like any spell circle) spawn in between the spell and the creature, facing the
spell, stacked one behind another 0.24 blocks apart (one per 8 mana of strength, up to 7), the back
one first, each a little dimmer than the one in front. The one that holds a spell flares white at
its heart and sends ripples out across itself; the ones behind it ripple once, softly. A circle that
breaks goes like real glass (`ShieldBreak`): cracks shoot out from the heart in two ticks (long
radial cracks that wander and fork, joined by rings of short ones), then it bursts, its rim into
curved slivers of its frame line and its face into flat shards of glassy light, small near the heart
and larger further out, flung on the way the spell was going, tumbling, falling, skittering over the
ground and glinting when a face turns to the light. A stack breaks front to back, a circle every two
ticks, each with its own ring of breaking glass. The shard and crack textures are drawn by
`tools/shield_art.py`.

## Icons and textures

16×16 pixel art, drawn by code (`tools/item_art.py`): rune stones built from ASCII silhouettes,
one-pixel dark outline, lit from the top left like vanilla items, a pictogram painted with the
family's glow ramp; Tier III, Tier IV and innate runes animate. Particle textures are white on
transparency and tinted in game. World textures (`tools/world_art.py`, `tools/wear_art.py`) keep
vanilla's resolution and shading so modded things sit beside vanilla blocks and mobs.

## Sound

Every sound is synthesised by `tools/sound_art.py` in one key (D major pentatonic), so sounds that
play together harmonise. Play the mod's tonal sounds at **pitch 1.0** (0.5 and 2.0 stay in key),
UI sounds with `SimpleSoundInstance.forUI(event, 1.0F, 1.0F)` (they're levelled quiet already), and
the element's own cast and impact through its `Vfx.Theme`.

## Checking your work

Run the feature tour (`WILDERCORD_TOUR_ONLY=1 ./gradlew runClientGameTest`) and look at the
screenshots in `build/run/clientGameTest/screenshots/`: every shape, secret spell and creature is
filmed there, from the side where a spell flies away from the caster. `python tools/make_gif.py`
turns the `tour_hero_*` frames into the README's moving header.
