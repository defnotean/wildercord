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

Every effect automatically gets `FRUGAL`, so Frugal always works. Pick the **kind** carefully:
`HARMFUL` effects never touch you or your allies, `HELPFUL` only touch you and your allies,
`WORLD` acts on blocks and points, and `MOVEMENT` moves the caster whatever was hit.

### 2. Categorise it

In `RuneCategories.categoryFor`, add `"gust"` to the `control` list (or it falls back to
`damage`). Categories are what the Codex chips and rows use.

### 3. Make it do something

In `Effects.applyEffect`, add a case. You get ready-made lists: `harmed` (fair game), `helped`
(allies), `moved` (the caster on Self, otherwise `harmed`), plus `power`, `duration` and `amplify`
already worked out from its modifiers, the shape, the caster's Heart Circles and Cord
enchantments, the charge, the rhythm chain and the caster's leaning:

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
  invulnerability frames, scales PvP, applies Execute, Fortune and Unison, and counts spell kills.
- **The caster may be a monster.** `cast.caster` is a `LivingEntity` (a Runebound or the
  Archivist casts your rune too). Send messages with `Casters.tell`, check `Casters.creative`, and
  never assume a `ServerPlayer`.
- **Changing blocks?** Check `Casters.mayBuild(cast.caster)`, `level.mayInteract` and
  `cast.takeBlock()` (as `Effects.mayEdit` does), so monsters never grief and the block budget
  holds.
- **Schedule later work with `Scheduler.later(ticks, ...)`** and check `cast.alive()` inside.
- **Anything bigger than a few lines** belongs in a helper (see `Techniques`).
- **Don't stop a boss's AI or move it.** Use `Spirits.isBoss` (see `Spirits.hold`).

An element comes with things for free: the effect counts toward elemental leaning, joins Unison,
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
   Every shape gets `COOLDOWN` (for Rapid, Vow and Blood Price).
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

Every Tier I-III rune must have a recipe (the generator asserts this); Tier IV and innate runes
must not. Add
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

### Regenerate

```bash
python tools/generate_assets.py
```

This writes the texture, item model, language entries, recipe, recipe-book unlock, and updates
`docs/RECIPES.md`. Commit the generated files along with your code.

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
