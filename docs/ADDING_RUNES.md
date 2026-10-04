# Adding runes

Runes are data plus a little behaviour, so most new runes touch the same handful of places. This
guide walks through each family with a real example, then lists the checklist to finish with.

This guide is for runes built into Wildercord. **Adding runes from another mod?** Use the add-on API
instead: no changes to Wildercord at all. See [API.md](API.md) and [From another mod](#from-another-mod).

Before starting, read [ARCHITECTURE.md](ARCHITECTURE.md) sections 1 and 2; they explain the reading
rules and how a cast runs.

- [Where things go](#where-things-go)
- [An effect: "Gust"](#an-effect-gust)
- [A shape](#a-shape)
- [A modifier](#a-modifier)
- [A link](#a-link)
- [An innate rune](#an-innate-rune)
- [A signature fusion](#a-signature-fusion)
- [Art, recipes and loot](#art-recipes-and-loot)
- [Checklist](#checklist)
- [From another mod](#from-another-mod)

## Where things go

| Step | File |
|---|---|
| Define the rune | `src/main/java/dev/wildercord/spell/Runes.java` |
| Put it in a Codex category | `spell/RuneCategories.java` (`categoryFor`) |
| What it does | `cast/Effects.java` (effects), `cast/CastEngine.java` + `cast/ShapeRunners.java` (shapes), `spell/SpellNumbers.java` (modifier numbers), `spell/SpellCompiler.java` + `cast/CastEngine.java` (links) |
| How it looks | `cast/Vfx.java` or `cast/TechniqueVfx.java` (magic circles through `cast/Sigils.java`) |
| Its own feel (sounds, cue, impact, aftermath) | `cast/feel/<Element>Feels.java` and `tools/feel/<element>.py` (see [Give it its own feel](#give-it-its-own-feel)) |
| Its icon | `tools/item_art.py` (`GLYPHS`) |
| Its magic ring (pattern and emblem) | automatic: `tools/circle_art.py` gives every rune its own, run by `generate_assets.py` |
| Its recipe | `tools/generate_assets.py` (`RUNE_RECIPES`) |
| Where it drops | `content/WildercordLoot.java` |
| Tests | `src/test/java/dev/wildercord/spell/`, and the smoke list in `src/gametest/.../WildercordScreenshots.java` |

Names, descriptions and tooltips are generated from `Runes.java`: you never edit the language file
by hand.

## An effect: "Gust"

Say we want **Gust**: a Tier I wind effect that knocks targets back a little and cushions your
next fall.

### 1. Define it

In `Runes.java`, with the other effects (keep the call on **one line**; the asset generator parses
it):

```java
public static final RuneDef GUST = effect("gust", "Gust", 1, 5, "wind", EffectKind.HARMFUL, "A gust knocks targets back and slows their fall.", POWER);
```

The arguments are: path, name, tier, mana cost, element, kind, description, then traits. The
traits decide which modifiers can change it:

| Trait | Changed by |
|---|---|
| `POWER` | Amplify, Overcharge, Focus, Execute |
| `DURATION` | Extend |
| `RADIUS` | Widen, Focus |
| `LINGER` | Linger |
| `SHARE` | Kindred (given for you: see below) |

Every effect automatically gets `FRUGAL`, so Frugal always works, and every `HELPFUL` one `SHARE`, so Kindred can
share it, unless it acts on a place or only ever on its caster: then add it to `Runes.Unshared`. Pick the **kind** carefully:
`HARMFUL` effects never touch you or your allies, `HELPFUL` only touch you and your allies,
`WORLD` acts on blocks and points, and `MOVEMENT` moves the caster whatever was hit.

### 2. Categorise it

In `RuneCategories.categoryFor`, add `"gust"` to the `control` list (or it falls back to
`damage`). Categories are what the Codex chips and rows use.

### 3. Make it do something

In `Effects.applyEffect`, add a case. You get ready-made lists: `harmed` (fair game), `helped`
(allies), `moved` (the caster on Self, otherwise `harmed`), plus `power`, `duration` and `amplify`
already worked out from its modifiers, the shape, the caster's Heart Circles and Cord
enchantments, the charge, the rhythm chain and the caster's affinity with its element:

```java
case "gust" -> harmed.forEach(t -> {
	Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
	push(t, away.scale(1.2 * power).add(0, 0.3, 0));
	t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks(3, duration), 0, false, true));
	Reactions.mark(t, Reactions.Mark.WINDSWEPT);   // sets up Wildfire
	Vfx.push(level, t, away);
});
```

Rules of thumb:

- **Damage always goes through `Effects.hurt(cast, target, source, amount)`.** It skips
  invulnerability frames, scales PvP, applies Execute, Fortune and Unison, counts spell kills, and
  sets off the reactions any damage of the rune's element sets off (earth Fracture, life Blight,
  arcane Unweave, wind Rupture, time Elapse). Fire and storm damage ask `Reactions.fire` / `storm`
  for their multiplier themselves.
- **Leaving a mark?** `Reactions.mark(t, Reactions.Mark.SHADOWED)` (a void curse) or `BLEEDING` (a
  cut) sets up Blight or Rupture; add the rune to `ReactionRules.SHADOWS` / `BLEEDS` too, so its
  tooltip says so. A new harmful rune of a triggering element that deals no damage belongs in
  `ReactionRules.QUIET`.
- **The caster may be a monster.** `cast.caster` is a `LivingEntity` (a Runebound or the
  Archivist casts your rune too). Send messages with `Casters.tell`, check `Casters.creative`, and
  never assume a `ServerPlayer`.
- **Changing blocks?** Check `Casters.mayBuild(cast.caster)`, `level.mayInteract` and
  `cast.takeBlock()` (as `Effects.mayEdit` does), so monsters never grief and the block budget
  holds.
- **Schedule later work with `Scheduler.later(ticks, ...)`** and check `cast.alive()` inside.
- **Anything bigger than a few lines** belongs in a helper (see `Techniques`).
- **Don't stop a boss's AI or move it.** Use `Spirits.isBoss` (see `Spirits.hold`).

An element comes with things for free: the effect counts toward its casters' affinities, joins Unison,
lights Rune Seals of its element, and (for fire) lights campfires.

### 4. Give it a look

Add a method to `Vfx`, `TechniqueVfx` or `ExpansionVfx` (where the batch 6 runes live) and call it
from your case. Use `Vfx.theme("wind")` for the element's colours and sounds, `ElementFx` for its
visual language (its palette, motifs and signature impact), shaped light from `Light` (rings, beams,
slashes, orbs) and the primitives (`Vfx.radial`, `Vfx.ring`, `Vfx.helix`, `Vfx.stream`,
`Vfx.shockwave`). Something big can shake the camera of everyone nearby with `ScreenFx.shake`.
[ART.md](ART.md) has the colours, sizes and timings to match. Always send particles through these helpers or `Fx`, never
`level.sendParticles` directly: `Fx.send` keeps particles out of the caster's face. For a flash,
emit a `SigilOption.glow` with `Vfx.emit` (so it goes through `Fx.send` too), not vanilla's
firework flash; magic circles go through `Sigils`.

## A shape

Shapes decide *where* and *who*. A new shape needs:

1. **A definition** with `shape(path, name, tier, cost, effectMultiplier, description, traits...)`.
   The multiplier scales the cost of every effect it carries (Touch 1.0, Burst 1.5, Domain 3.0).
   Every shape gets `COOLDOWN` (for Rapid, Vow and Blood Price). A modifier that needs `COOLDOWN`
   changes the whole spell, so its multiplier prices the whole spell, not the shape's group
   (`SpellCompiler.wholeSpell`).
2. **Numbers** in `SpellNumbers` (radius, duration, speed...), reading the group's modifiers with
   `g.count(Runes.WIDEN)` and so on. Put them here, not in the runner, so the readout can use them.
3. **A readout phrase** in `SpellCompiler.shapePhrase`, e.g. `"A spiral (5 blocks)"`.
4. **Delivery**: a branch in `CastEngine.deliver` that finds the hits and calls
   `CastEngine.onHit(cast, g, new Cast.Hit(...), anchored)`. For anything that moves or lasts,
   write a runner in `ShapeRunners` that schedules its steps.
   - Hitting repeatedly? Give each strike its own budget with `cast.pulse()`.
   - Hitting each creature once? Use `cast.child()` (shared budget).
5. **A category** in `RuneCategories` (personal, direct, projectile, area, lingering).

## A modifier

Modifiers are pure numbers:

```java
public static final RuneDef ECHOING = modifier("echoing", "Echoing", 2, 1.3, Trait.DURATION, "...");
```

The fourth argument is the cost multiplier on what it attaches to; the fifth is the **trait it
needs**. The compiler attaches it to the closest rune on its left with that trait. Then read it
where the number is computed, in `SpellNumbers` (e.g. multiply `duration(...)` by
`1.3^count(ECHOING)`), mention it in `SpellCompiler.effectsPhrase` or `shapePhrase`, and add a
unit test. If it needs a trait nothing has yet, add the trait to `Trait` and to the runes it should
work on.

## A link

Links change *when* the rest fires, so they touch both the compiler and the runtime:

1. **Define it** with `link(path, name, tier, cost, description, traits...)`.
2. **Reading** (`SpellCompiler.Reader.segment`): decide the implicit shape of what follows. Most
   links use `Runes.TRIGGER` (the thing that set it off); conditions like If Sneaking keep the
   current shape; Delay and Pulse use `Runes.SELF`.
3. **Readout**: a header in `SpellCompiler.describe`, e.g. `"When you jump:"`.
4. **Runtime** (`CastEngine.runSegment`): when the condition happens, call
   `runSegment(cast.child(), link.next, new Cast.Trigger(...))`. Use `Scheduler` for anything that
   waits, and always create the child cast *before* waiting.
5. **Cost**: links have a small flat cost; if yours repeats the rest (like Pulse), multiply the
   rest's cost in `SpellCompiler.cost`.

## An innate rune

Innate runes are Tier I effects: each caster wakes with one of them, at random, at the 1st
Circle, and they're never crafted or dropped. On top of the effect steps above:

1. Add it to `Runes.INNATE`, and to the `innate` list in `RuneCategories.categoryFor`. Update the
   count in `DiscoveryTest.innateRunesAreSeparate`.
2. Add its path to the `INNATE` set at the top of `tools/generate_assets.py` (it gets the animated
   Tier IV icon treatment, no recipe, and stays out of loot).
3. Route its case in `Effects.applyEffect` to `Innates.apply`, and keep any state it needs in
   `Innates`, cleared when the server stops. Its power already grows +6% per circle.

## A signature fusion

A signature fusion is a fused rune made from two *particular* effects at the Fusion Altar, in place of
their elements' fusion (see [features/fusion-altar.md](features/fusion-altar.md#signature-fusions)). On
top of the effect steps above (it's never crafted or found, so no recipe and no loot):

1. Define it in the "Signature fusions" section of `Runes.java` (Tier III, or IV for a grand one) and
   add it to `Runes.SIGNATURE`.
2. Add one line to `Fusions.SIGNATURES`: `new Signature(Runes.FIRST, Runes.SECOND, Runes.RESULT)`. The
   tools read these lines, so keep each on one line. Its two runes must be effects a caster can come by
   (never innate or fused), and that exact pair of runes must be one no other signature uses. Several
   signatures may share a pair of elements; each still needs its own ring and emblem in `circle_art.py`,
   which asserts that no two runes share either.
3. Its behaviour goes in `cast/SignatureFusions.java` (and `SignatureWards` for anything that answers
   what happens to a creature), its numbers in `SignatureRules`, its look in `SignatureVfx`.
4. Its icon goes in `tools/signature_art.py`, which can draw with its partner element's colours.
5. Add it to `WildercordSignatureFusionTest` with a check of its core effect.

The Grimoire, the altar's screen, the circle (a braid of its two runes' elements with a star) and the
docs' tables all follow from the `Signature` line.

## Art, recipes and loot

### Icon

Add a pictogram to `GLYPHS` in `tools/item_art.py`, keyed by the rune path. It's a small ASCII grid
painted onto the rune's stone or gem:

```
'*' core (brightest)  '+' light  '#' main  '-' shade  'o' dark carve  'k' near-black  'w' white
```

Keep it about 7-9 wide and centred. Run `python tools/item_art.py` and look at
`build/art-preview/item_art_1x.png` to check it at true size on both backgrounds.

### Recipe

Every Tier I-III rune must have a recipe (the generator asserts this); Tier IV runes, innate runes
and the runes of the world (below) must not. Add
the themed items to `RUNE_RECIPES` in `tools/generate_assets.py`:

```python
"gust": ["minecraft:feather", "minecraft:wind_charge"],
```

The Blank Rune and the tier's extra cost (`TIER_CATALYSTS`) are added for you. Keep it under 9
ingredients in total.

### Loot

Add the rune to a pool in `WildercordLoot.java` (`common`, `uncommon`, or a structure's list), or as
a mob drop in `MOB_DROPS`. Weights within a pool scale with tier automatically. The tooltip's
"Found:" line and `docs/RECIPES.md` are generated from this file.

### A rune of the world (found only)

A rune that should never be crafted, whatever its tier, belongs to a place instead:

1. Define it at the end of `Runes.java`, in the "Runes of the world" section (after everything
   else, so no older rune's magic circle changes).
2. Put it in a `source(...)` line in `spell/RuneSources.java` (one line each: the asset generator
   reads them for the tooltip's "Found:" line and `docs/RECIPES.md`). Don't give it a recipe: the
   generator asserts it has none.
3. Its behaviour goes in `cast/ExplorerEffects.java` (effects), `cast/ExplorerShapes.java` (shapes
   and condition links) and `cast/ExplorerVfx.java` (visuals); `Effects` and `CastEngine` hand
   anything they don't know to these.
4. Hand it out: a vanilla structure's chest through `sourcePool(...)` in `WildercordLoot`, a biome
   through a rule in `spell/Attunements.java` (plus its `attunement:<id>` source), a fishing line
   (add it to the `fishing` source: the fishing pools draw from it), or one of Wildercord's own
   places, whose loot tables the generator writes from the source
   (`WildercordLoot.foundRune(sourceId, random)` picks one in code).

### Regenerate

```bash
python tools/generate_assets.py
```

This writes the texture, item model, language entries, recipe, recipe-book unlock, and updates
`docs/RECIPES.md`. Commit the generated files along with your code.

## Give it its own feel

Every spell already gets a **feel** from how it is built (see `dev.wildercord.cast.feel`): the shape's *motion* (Flick, Hurl, Beam,
Slash, Blast, Seal, Call, Aura), the mana-dominant *element* (and an accent element), the first effect's *role* (Strike, Bind, Mend,
Move, Time, World, Call up), a *band* from cost, charge and tier (S, M, L, XL: band M is the mod's usual sizes), and the modifiers
on it. It travels inside `Vfx.Theme` (`theme.feel()`), so any shape or effect code that has a theme has the feel.
`RunicAnimations` gives every shape and effect an illustrated three-beat release and first-landing
animation from its explicit entry in `assets/wildercord/animations/rune_choreography.txt`. Add a distinct
opening, middle and finish when you add a castable rune; `RuneChoreographyTest` checks roster coverage and
rejects a reused complete sequence. All effects in a group get one, including secondary effects and the two
inside a woven rune. A rune with a distinctive wind-up, movement or lasting field should also have an authored
signature and VFX of its own. To make a rune **signature**, do two things, both in files that are yours alone.

### 1. Its signature (Java)

Each element has one class, `src/main/java/dev/wildercord/cast/feel/<Element>Feels.java` (`FireFeels`, `FrostFeels`... `ShapeFeels`),
with an empty `register()`. Add a `Signature` per rune there. Everything is optional:

```java
static void register() {
    Signature.of("meteor")                                   // a rune path, or "addon:rune"
        .motion(Motion.CALL)                                 // treat its spells as another motion (pose, cue)
        .scale(1.4)                                          // read bigger than its cost says (0.6 to 2.2 overall)
        .accent(0xFFB040)                                    // the theme's second colour for spells led by this rune
        .sound(Phase.CUE, "fire_flick")                      // a kit sound at the hand when it is cast
        .sound(Phase.IMPACT, "fire_whump", 1.0F, 1.0F)       // ...at each impact (name, volume, pitch)
        .replace(Phase.IMPACT)                               // ...instead of the element's impact sound
        .hook(Phase.AFTERMATH, ctx -> MyFx.crater(ctx))      // your own particles: ctx has level, feel, theme, at, dir, target, cast
        .register();
}
```

- **Phases:** `CUE` (the first 200 ms, once per cast, for the spell's first group), `TRAVEL` (each tick a projectile flies: Bolt and
  Arc), `IMPACT` (where a Bolt, Beam or Touch lands, every Chain jump and Bounce), `AFTERMATH` (after an impact of band M or bigger),
  `HIT` (each creature the effect touches; `replace(Phase.HIT)` drops the generic glow).
- A sound or hook **adds** to the default of its phase; `replace(phase)` makes yours stand alone (for `IMPACT` the shape's flare and
  rings stay, the element's impact *sound* goes).
- A signature on an **effect** applies to every group whose *first* effect it is (and to `HIT` for that effect anywhere); on a **shape** id
  to groups with that shape; the effect's wins.
- Play kit sounds with `Feels.sound(level, at, "name", volume, pitch)` (or `Feels.sound(level, at, feel, "name", ...)`, which scales
  the volume by the band). It adds a random pitch spread of about 3%, and an unknown name is skipped and logged once, so you can
  name a sound before it exists. **Every** `Fx.sound` (yours, vanilla's, the kit's) is capped at 3 of one sound event per level per
  tick, so a Burst on thirty mobs plays three, not thirty.
- Timing (wind-ups, delays) stays in your effect's own code: use `Scheduler.later` and `cast.feel(group).band()` for scale.
- Read the feel in your own effect code with `Vfx.Theme theme` (`theme.feel()`, which is null outside a shape's delivery) or, for a group,
  `cast.feel(group)`; `feel.mod("widen")` counts a modifier, `feel.element()`, `feel.role()`, `feel.band()`, `feel.scale()`.

### Shared helpers

- **Marks show themselves.** `Reactions.mark(...)` already makes a mark visible (a halo in its colour every half
  second and a tick when it's first set: `cast/feel/MarkHalos`). For a mark a rune keeps in its own state, show the
  same halo with `MarkHalos.show(level, entity, Reactions.Mark.FROZEN)`, or your own colour with
  `MarkHalos.halo(level, entity, 0xRRGGBB, MarkHalos.Style.CROWN)` (styles `CROWN`, `ORBIT`, `DRIP`, `MOTES`,
  `CRACKS`); call it every `MarkHalos.PERIOD` (10) ticks while the mark lasts.
- **Scales:** `Feels.step(i)` is the i-th note of the pentatonic scale as a pitch (0 = 1.0, 5 = an octave up): play a
  tonal kit sound at `step(k)` for the k-th of anything (a Totem's beats, a Chain's jumps) and it climbs in key.
- **Per-hit power:** `new Cast.Hit(...).times(1.3)` makes a hit's effects stronger or weaker (Touch uses 1.3, the near
  and far halves of a Cone 1.35 and 0.85). `CastEngine.onHit` multiplies it into the shape's power.
- **Tells:** `Tells.handoff(level, at, k)` (a violet ring and ting where a link hands on), `Tells.gate(cast, passed)`,
  `Tells.armed(cast)`: for a link from an add-on, the same language as the built-in ones.
- **Neutral kit sounds anyone may play** (`tools/feel/neutral.py`): `gesture_<motion>`, `note_shape`, `note_effect`,
  `note_mod`, `note_link`, `link_ting`, `gate_pass`, `gate_fail`, `tell_tick`, `tell_toll`, `tell_rumble`, `tell_zap`,
  `tell_drip`, `tell_crack`, `field_pulse`, `tap_release`, `ready_ping`, `fizzle`. `FeelTest` fails if code names a kit
  sound that no file defines.

### 2. Its sounds (Python)

Sounds are synthesised, never recorded. Each element has one file, **`tools/feel/<element>.py`** (`neutral.py` for the shared ones), which
lists events. A builder gets the variant number and a seeded random generator, uses the DSP of `tools/sound_art.py` (`sa.`), and ends
with `sa.finish(x, role)`:

```python
from feel.core import sa, event

def fire_flick(v, rng):                         # v = 0, 1, 2...
    ...
    return sa.finish(x, "impact")               # roles: cast, impact, effect, grand, loop, ui, tick, tell, pulse

EVENTS = [event("fire_flick", fire_flick, variants=3, subtitle="hit")]
```

- **Names:** lower-case, `<element>_<verb>` (`fire_flick`, `blood_slice_heavy`); a name must start with its file's element (only
  `neutral.py` has bare names: `tick`, `link_ting`) and be unique across every file and the base palette. The name is the event id
  (`wildercord:fire_flick`) *and* what you pass to `Feels.sound`.
- **Subtitle:** one of `cast`, `hit`, `field`, `tell` (four texts already exist; you never touch the language file).
- **Variants:** 1 to 6; anything that can repeat quickly (a field's pulse, a tick) wants 3 or more; `Feels.sound` picks at random.
- **Key:** tonal parts in D major pentatonic (`sa.note(sa.D, octave)`, degrees `sa.D sa.E sa.FS sa.A sa.B`). Play tonal sounds at pitch 1.0,
  0.5 or 2.0; for a scale from one sample use the ratios 1.0, 1.122, 1.26, 1.498, 1.682, 2.0. Noise-like sounds may sit anywhere from 0.7 to 1.4.
- **Volume:** the role levels it (`finish`); `Feels.sound(..., feel, ...)` scales by band (S 0.7, M 1.0, L 1.15, XL 1.3).
- **Worked examples to copy:** `fire.py` (`fire_flick`: a short one with a pitch step per variant; `fire_whump`: noise, a body and grains),
  `blood.py` (`blood_slice`, `blood_slice_heavy`: one recipe from a factory used for two events), `neutral.py` (`tap_release`, `link_ting`, `ready_ping`).

Build with **`python tools/feel/build.py --only fire`** (needs numpy, scipy and ffmpeg, like `sound_art.py`). It writes
`sounds/kit/<element>/*.ogg`, `tools/feel/manifest/<element>.json`, and **regenerates `sounds.json` and `kit_sounds.json`** (the Java
registry, `WildercordSounds.kit(name)`, reads the latter): never edit those three by hand. A run is deterministic (seeds come from the
event name and variant) and idempotent. `--merge` only regenerates the two merged files (what to run after a merge conflict in them),
`--check` verifies without synthesising. Build only your own part: it never touches another element's files, so two people never
conflict except in the two merged files, which are regenerated. `python tools/sound_art.py` (the base palette) still works and merges too.
`SoundKitTest` fails if a manifest, a file or a subtitle is missing.

## Checklist

- [ ] `RuneDef` on one line in `Runes.java`, with the right tier, kind, element and traits
- [ ] Category in `RuneCategories.categoryFor`
- [ ] Behaviour (and `SpellNumbers` / readout text for shapes, modifiers and links)
- [ ] A visual through `Vfx` / `TechniqueVfx` / `Fx`
- [ ] A glyph in `item_art.py`
- [ ] A recipe (Tier I-III) or a drop (Tier IV) or both
- [ ] `python tools/generate_assets.py`
- [ ] A unit test for any new reading or number rule
- [ ] Add it to the smoke list in `castEverything` if it has runtime behaviour
- [ ] `./gradlew build` passes; `./gradlew runClientGameTest` passes
- [ ] A line in `docs/DESIGN.md` and in `CHANGELOG.md`
- [ ] If it could be sustained safely, consider `Passives` (and say why in the PR)

## From another mod

An add-on registers its runes through `dev.wildercord.api` ([API.md](API.md)) from a `WildercordAddon`
listed under the `wildercord` entrypoint. The same rules apply, only the places differ:

| Built-in rune | Add-on rune |
|---|---|
| A `RuneDef` line in `Runes.java` | `api.effect(...)` / `shape` / `modifier` / `link`, then `.register()` |
| A case in `Effects.applyEffect` | `.onApply(ctx -> ...)`, with `ctx.hurt` for damage and `ctx.mayEdit` for blocks |
| A branch in `CastEngine.deliver` | `.onDeliver(ctx -> ...)`, calling `ctx.hit(...)` (and `ctx.pulse()` per repeated strike) |
| Numbers in `SpellNumbers` | `.numbers(power, duration, radius)` on a modifier |
| A branch in `CastEngine.runSegment` | `.onLink(ctx -> ...)`, calling `ctx.fire()` or `ctx.fireAt(...)` |
| `RuneCategories.categoryFor` | `.category(...)` (a new one is added to the family) |
| A reaction in `Reactions` | `api.registerReaction(id, element, ...)`, with `api.mark` / `hasMark` |

Unknown runes reach the engine's default branches (`Effects.applyEffect`, `CastEngine.deliver`,
`CastEngine.runSegment`), which hand them to `cast.AddonRunes`. Built-in runes never go that way, so a new
built-in rune still needs its own case.
