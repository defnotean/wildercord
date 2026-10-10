---
title: Performance and Visual Settings
nav_order: 12
---

# Performance and visual settings

Wildercord keeps decoration separate from gameplay. Lowering effects never changes damage, timing or where a spell lands. Warning circles and combat cues stay readable.

## Open the settings

Bind a key to **Magic visual settings** in **Options > Controls > Key Binds > Wildercord** (it has no default key). You can also open it from the Life journal in the [Grimoire]({{ '/progression/grimoire/' | relative_url }}).

Your choices are saved on your own computer and only change what you see.

## Presets

| Preset | What it sets |
|---|---|
| **Performance** | Your spells balanced, other spells minimal, reduced flash on, camera motion off |
| **Balanced** | The default |
| **Cinematic** | Fuller effects |

Presets don't change your combat animation or camera choice (see below).

## Every setting

| Setting | Choices | What it changes |
|---|---|---|
| Your spells | full, balanced, minimal | Detail and particles for spells you cast |
| Other spells | full, balanced, minimal | Detail and particles for spells from other players and creatures |
| Reduced flash | on, off | Softens bright flashes and screen-filling light |
| Camera motion | on, off | Camera shake and view kicks on heavy impacts |
| Colour-blind-safe telegraphs | on, off | Draws magic circles, rings and warning reticles in the Okabe-Ito palette, which stays distinct under common colour blindness |
| Spell titles | shown, hidden | The name and rank badge shown when a mastered spell is cast |
| Blade trails | full, subtle, off | The ribbon of light an aura blade leaves. Techniques always show |
| Body aura | full, calm, off | The aura around a swordsman's body |
| Impact | full, soft, off | Hit-stop, view nudge and flash when aura blows land. Off keeps a small flash |
| Technique banners | everyone's, yours only, off | The name of an art or Dominion as it goes off |
| Sword strings | by the crosshair, by the hotbar, hidden | Where your [sword string]({{ '/progression/aura/' | relative_url }}#sword-strings) marks show |
| Sigil tracing | on, off | Whether you trace a glyph while you charge. See [Sigil tracing]({{ '/spellcraft/casting/' | relative_url }}#sigil-tracing) |
| Trace assist | none, light, strong | How much help you get while tracing |
| Incantations | shown, hide mine, hide others', hidden | The [incantation]({{ '/spellcraft/casting/' | relative_url }}#incantations) words over casters |

The screen also has a **Benchmark this scene for 30 seconds** button that measures your frame rate where you stand.

## Combat presentation

The **Combat presentation...** button sets how sword fights look and how the camera behaves:

- **Combat animations:** Classic, or Articulated (experimental).
- **Camera:** Stable or Default. Stable stops view bob, shake and field-of-view kicks.

**Reset** returns to Classic and Stable. Neither choice changes damage, movement or timing. Full details on [Combat Presentation]({{ '/masters/combat-presentation/' | relative_url }}).

## Tips for busy fights

- Set **Other spells** to minimal first. That usually helps most when many players cast.
- For less motion, use Stable camera, Impact off, Body aura calm or off, and Blade trails subtle or off.
- Turn on **Reduced flash** if bright releases bother you. Some small flashes remain.
- Turn on **Colour-blind-safe telegraphs** if warning rings and ward circles look alike to you. Each colour is swapped for the nearest safe one: reds become vermillion, greens bluish green, blues sky blue or blue. Whites and greys stay as they are.
- The Cinematic launcher profile includes Iris, but you must install and pick a shader pack yourself.

## Measure a problem

Operators can test in the practice arena:

- `/wildercord practice benchmark`: cast for 10 seconds, then read how many effects the server sent and limited.
- `/wildercord practice moving` and `stress <1-24>`: add moving or extra targets so a test is easy to repeat.
- `/wildercord visualstats` and `/wildercord visualstats reset`: show or clear the effect counters.

Server counters aren't frame times. When you report slowdown, include your hardware, render distance, shader pack, preset, the spell, how many targets, and whether your frame rate was capped.
