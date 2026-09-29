# Architecture

A tour of how Wildercord fits together, written for someone about to change it. It follows a
spell from the rune roster to the particles on screen, then covers player state, the client, the
asset pipeline and testing. For *what* each rune does and why, see [DESIGN.md](DESIGN.md).

- [The big picture](#the-big-picture)
- [1. The spell engine (`spell/`)](#1-the-spell-engine-spell)
- [2. Casting (`cast/`)](#2-casting-cast)
- [3. Player state (`player/`)](#3-player-state-player)
- [4. Heart Circles and passives](#4-heart-circles-and-passives)
- [5. Networking](#5-networking)
- [6. The client](#6-the-client)
- [7. Content: items, loot, effects](#7-content-items-loot-effects)
- [8. The asset pipeline (`tools/`)](#8-the-asset-pipeline-tools)
- [9. Testing](#9-testing)
- [10. Rules that keep it safe](#10-rules-that-keep-it-safe)
- [11. Minecraft 26.x notes](#11-minecraft-26x-notes)

## The big picture

```
                 Runes.java  ──(read by)──▶  tools/generate_assets.py ──▶ textures, models,
                     │                                                   lang, recipes, docs
                     ▼
 client:  CordScreen ──EditSpell──▶ server: SpellCaster.edit ──▶ Spellbook (attachment)
          key R ─────CastSpell───▶         SpellCaster.cast
                                            │  checks: Cord worn, spell unlocked, runes learned
                                            │          and held by the Cord, cooldown, mana
                                            ▼
                                   SpellCompiler.compile(runes)  →  Compiled(plan, cost, lines)
                                            ▼
                                   CastEngine.cast(plan)
                                     runSegment ─▶ deliver(group)   shapes find hits
                                                    └▶ onHit        effects apply, links fire
                                            ▼
                                   Vfx / TechniqueVfx / ElementFx ──▶ Fx.send ──▶ particles for every player
                                   Sigils / Light ──▶ sigil, spell circle and light particles
```

Three layers, each only knowing about the ones below it:

| Layer | Package | Knows Minecraft? | Tested by |
|---|---|---|---|
| Spell engine | `spell` | No | JUnit (`src/test`) |
| Runtime | `cast`, `player`, `content`, `world`, `net`, `mixin` | Yes (server side) | Client game tests |
| UI | `client` | Yes (client side) | Game-test screenshots |

## 1. The spell engine (`spell/`)

Everything here is plain Java with no Minecraft imports. That is deliberate: the Cord screen's
readout, the server's cast and the unit tests all run the exact same code.

### Runes are data

`RuneDef` is a record:

```java
record RuneDef(String id, String name, RuneFamily family, int tier, double cost,
               double multiplier, String element, EffectKind kind, Set<String> traits,
               String needs, String description, String category)
```

- **`family`**: `SHAPE`, `EFFECT`, `MODIFIER` or `LINK`.
- **`tier`** (1-4): which Cord can hold it (Twine holds I, Copper II, Amethyst III, Echo IV).
- **`cost`**: mana, for shapes, effects and links. For shapes, **`multiplier`** scales the cost of
  the effects they carry (an area costs more than a touch). For modifiers, `multiplier` scales the
  cost of whatever they attach to.
- **`traits`**: what modifiers may change on this rune (`Trait.POWER`, `DURATION`, `RADIUS`,
  `SPEED`, `PIERCE`, `SPLIT`...). Every effect also gets `FRUGAL`, every shape `COOLDOWN`.
- **`needs`**: modifiers only, the one trait a rune must have for the modifier to attach.
- **`kind`** (`EffectKind`): who an effect may touch: `HELPFUL` (you and allies), `HARMFUL`
  (never you or allies), `WORLD` (blocks and points), `MOVEMENT` (moves the caster).
- **`category`**: the sub-group the Codex shows it under (Damage, Control, Support... see
  `RuneCategories`).

`Runes` holds the whole roster as `public static final RuneDef` fields, built with four helpers
(`shape`, `effect`, `modifier`, `link`). One line per rune, and `tools/generate_assets.py` parses
these lines, so keep each call on a single line.

### Reading: `SpellCompiler`

`SpellCompiler.compile(List<RuneDef>)` turns a row of runes into a `SpellPlan` and returns a
`Compiled` record: the plan, its cost, its cooldown, the plain-English lines, warnings, where each
modifier attached, and a Blood Price health cost if any.

A `SpellPlan` is a tree:

```
Segment ── groups: [Group(shape, shapeMods, effects: [EffectNode(effect, mods)])...]
        └─ link: Link(link rune, mods, anchor group) ── next: Segment ...
```

The reading rules, in `Reader.segment`:

1. A **shape** starts a new group. An **effect** before any shape creates a group with the
   segment's *implicit shape*: `Self` at the start, `Trigger` (whatever set the link off) after
   On Hit, On Kill, On Hurt and so on, or the current shape for Echo, If Sneaking, If Airborne and
   Combo.
2. A **modifier** walks left over the runes seen so far in this segment (plus the link rune that
   opened it) and attaches to the first one whose traits include its `needs`. If nothing fits, the
   modifier is marked `UNATTACHED` and a warning is shown.
3. A **link** ends the segment. Everything after it is read recursively as the link's `next`
   segment. On Hit and On Kill remember the group before them as their *anchor*.

### Numbers: `SpellNumbers`

Every tunable number a modifier changes lives here: power, duration, radius, copies, bolt speed,
Zone and Domain timings, cooldowns. The compiler's readout and the runtime both call these same
functions, which is what keeps the explanation honest. Examples:

```java
SpellNumbers.power(effectNode)     // 1.5^Amplify × 0.6^Frugal × 2.5^Overcharge × 1.5^Focus
SpellNumbers.groupPower(group)     // Focus and Vow on the shape, × the shape's per-hit strength
SpellNumbers.domainRadius(group)   // 9 × Widen, capped at 24
SpellNumbers.cooldownTicks(cost, rapid, vows)
```

### Passives and Heart Circles (pure parts)

`Passives` holds the passive-spell rules: which runes may be sustained, slot unlocks, upkeep and
renewal intervals. `Circles` holds the Heart Circle table: condense thresholds, breakthrough
requirements, perks, and how circles and the Cord's spell enchantments scale power, cost,
cooldown and duration. Both are pure, so both are unit-tested.

### Discoveries, names and codes (pure parts)

- **`Secrets`**: the ten secret spells, each an exact rune sequence with a name, colour, cost
  multiplier and riddle. `Secrets.match` only matches the whole spell, nothing before or after.
- **`Feats`**: the Grimoire's keys (`reaction:shatter`, `secret:sunfall`, `feat:overcast`,
  `hint:` for a riddle read), every feat, and how much mana each first discovery condenses.
- **`PlayerAffinity`**: a player's own affinity with each element: the levels (100 to 10,000 points),
  what each gives (+3% power a level, a resistance from III, 10% off the element's share of a price at V),
  every way of earning points with its daily allowance (`Source`, `grant`), and which elements each
  reaction feeds. See [features/player-affinity.md](features/player-affinity.md).
- **`Leaning`**: which element a caster leans toward: their deepest affinity, at level I and 1.25x the
  runner-up. Colour only; the power is the affinity's.
- **`SpellNames`**: a readable name made from a spell's runes ("Splitting Frost Bolt"), and
  cleaning of custom names.
- **`SpellCodes`**: a spell as a `wc:bolt.frost.split` code and back (add-on runes as
  `namespace~path`), and the pattern that finds codes in chat.
- **`RuneSources`**: where each rune of the world is found (never crafted): a source id
  (`ancient_city`, `attunement:cherry_grove`, `ember_sanctum`, `starfall`...), how the tooltip names
  it, and its runes. `forSource(id)` is what dungeons, bosses and events use.
- **`Attunements`**: the Attunement rules (a biome, a condition over a `Place`, a riddle), matched
  by `cast.Attunement` every 5 ticks while a player meditates with a Blank Rune in hand.
- **`SpellSigil`**: the layout of a spell's magic circle, as fractions of its radius: the frame and
  its rays, the script band, the pattern band, the star ({p/q} with `points`/`step`, a point per
  rune), the roundels on its points (`pointOf`, `roundel`), the inner ring and the seal. The size
  never depends on the spell: more runes mean more points and smaller roundels.

## 2. Casting (`cast/`)

### The gate: `SpellCaster`

`SpellCaster.cast(player, spell)` is the only way a player's spell starts. It checks, in order: a
Cord is worn, the spell slot is unlocked, the spell compiles to something, the cooldown is over,
and the player can pay (mana, health for Blood Price, or a cracked Heart Circle when they
overcast). Only then does it charge, start the cooldown, condense mana toward the next Heart
Circle, and call `CastEngine.cast` (or `SecretSpells.cast` for a secret spell).

`activeSockets` decides which threaded runes actually fire: inside the Cord's sockets, learned,
still loaded (a rune from a removed add-on stays threaded but *silent*), and a tier the Cord can
hold. Runes outside those rules stay threaded but quiet, so swapping Cords never deletes a spell.

`SpellCaster.edit` / `editPassive` validate edits the same way before saving.

Around the gate sit the batch 5 systems it calls: `Charging` (hold to charge; `cast(player,
spell, charge)` adds up to 40% power), `Overcast` (a second press within 2 seconds cracks a Heart
Circle to pay), `Rhythm` (casts on the beat), affinities (`PlayerAffinities.onCast` counts the mana spent
toward each element in the spell, and the cast is marked `withAffinity`),
secret spells (`Secrets.match` routes the cast to `SecretSpells`, at `Secret.power` times the mana
and a 50% longer cooldown) and Twin Star (`Innates.consumeTwin`). Charge, rhythm and the Heart's
bonuses all end up in the cast's `power`.

Every 5 ticks `SpellCaster.tickPlayer` runs the per-player upkeep: `Meditation`, `HeartCircles`,
`PassiveCaster`, `Overcast` (mending), `Rhythm` (dropping a missed chain), `Charging` (the
full-charge chime and the 12-second fizzle), `LeyWalker`, then mana regeneration.

### Who casts: players and monsters

`Cast.caster` is a `LivingEntity`, so Runebound monsters and the Archivist run spells through the
same engine. Anything only a player has goes through `Casters`: `tell` (no-op for monsters),
`mayBuild` (monsters never change blocks), `creative`, and reach. `Targets` gives monsters their
own sides: their allies are other monsters, and they harm players, pets, golems, mannequins
(Phantom's afterimage) and their target. A monster's `Cast` gets its own `Heart.Bonuses` (its
power by difficulty), and anything that counts toward a player (spell kills, feats, Siphon,
Unison, Rune Seals) checks that the caster is a `ServerPlayer` first.

### A cast in flight: `Cast`

A `Cast` is the context every part of a spell carries: the caster, the level, the link depth,
the caster's bonuses (power and duration from Heart Circles and enchantments), whether it's a
passive renewing itself, and a **budget**:

- at most **64 creatures** and **32 blocks** touched, and **8 links** deep, shared by the whole
  cast through `child()`, so no chain of links can run away (only links go a level deeper: a bolt in
  flight, a shape's hits and a Linger's later landings run at their cast's own depth);
- shapes that strike repeatedly (Domain, Zone, Totem, Orbit, Wall, Trail, Rain, Barrage, Orb, Stream) take a fresh creature/block budget per strike with `pulse()`, while the Siphon cap and
  `once(...)` costs still count for the whole cast. The lasting ones book their next step as each
  runs (`ShapeRunners.steps`), never their whole lifetime up front, since `Scheduler` walks every
  waiting task each tick.

`Cast.alive()` is false once the caster leaves, dies or changes dimension (or, for a passive, once
it's switched off, or once `cancel()` is called, which is how a Domain that loses a clash
shatters), and every scheduled part checks it, so a spell never outlives its caster.

`Cast.Info` records what was cast: the plan (Mirrorfrost casts it back), its rune count (a kill
with six or more is a feat) and the caster's leaning element (their deepest affinity, for its colour).
Whether the caster's affinities add their power is the cast's own flag (`withAffinity`, kept by copies): set
for a spell from the Cord, an imbued release and Mirrorfrost, not for scrolls or passives.

`Cast.Trigger` is where a segment starts (the caster, or the thing that set a link off).
`Cast.Hit` is what a shape hit: creatures, the point, the direction, the block and face if any.

### Shapes: `CastEngine.deliver` and `ShapeRunners`

`CastEngine.runSegment` delivers each group, then handles the segment's link. `deliver` switches on
the shape: instant shapes (Self, Touch, Beam, Burst, Cone) compute their hits right away; moving or
lasting shapes (Bolt and Arc via the `RuneBolt` entity; Trail, Wall, Orbit, Ring, Pillar, Wave,
Mine, Totem, Domain, Crescent, Barrage, Orb, Blitz in `ShapeRunners`) schedule their work
with `Scheduler` and call `onHit` as they go. Split fans shapes out (`CastEngine.fan`,
`CastEngine.spread`); Volley repeats them (`CastEngine.volley`).

Two shapes meet other casters' magic. A `RuneBolt` checks its path each tick for an enemy
caster's bolt (`RuneBolt.collide`): both burst, harder for different elements, and a reacting pair
sets off a small reaction. A Domain registers with `DomainClash.open` as it expands; if it
overlaps another caster's, the weaker one (strength: power × shape power × circles × √(radius/9),
the incumbent holding a tie) is `cancel()`ed and shatters after 1.5 seconds.

### Effects: `CastEngine.onHit`, `Effects` and `Techniques`

`onHit` takes the creature budget, then applies each effect in the group (Stasis always first, so
it can hold everything after it), repeats lingering effects, pays out Siphon, and fires an anchored
On Hit / On Kill link.

`Effects.apply` is one `switch` on the rune's path. It splits the hit into `helped` (allies) and
`harmed` (fair game) lists with `Targets`, so friendly fire is impossible by construction. Simple
effects are a few lines inline; bigger ones live in helpers in `Effects` or, for the newer
techniques, in `Techniques`; the ten innate runes go to `Innates.apply`. Power already includes
the caster's affinity with the effect's element (`PlayerAffinities.power`: +3% a level) and, for innate runes,
+6% per circle.
All spell damage goes through `Effects.hurt`, which skips invulnerability frames (so stacked
effects all land), applies Execute, Fortune and Unison, the target's creature affinity (or a player
target's own resistance from affinity III) and the caster's elemental climate (`Affinities.multiplier`),
scales damage to players, and records the
hit for spell-kill counting and the innate runes that react to hits. After each effect,
`Effects.apply` tells `RuneSeals` its element and where it landed.

### Lasting magic: `Wards`, `Reactions`, `Spirits`, `Scheduler`

- **`Scheduler`**: delayed tasks, On Land watchers, and On Hurt / On Low Health watchers. Ticked
  once per server tick; cleared when the server stops.
- **`Reactions`**: short-lived marks (FROZEN, WINDSWEPT, PULLED, SOAKED, RESONANT, WET, CRACKED,
  SHADOWED, BLEEDING) and the bonuses they set off. Fire and storm reactions (Shatter, Wildfire,
  Conduct, Overload) are asked for by the effects that deal that damage (`Reactions.fire`/`storm`),
  blasts ask `blast` (Implode) and Repel asks `collapse`; the rest (Fracture, Blight, Unweave, Rupture,
  Elapse) go off for any spell damage of their element through `Reactions.hit`, one line in
  `Effects.hurt`, which also adds Cracked's extra. Their numbers, which runes leave shadowed and
  bleeding, and which runes' damage sets each off (for the Cord screen's tooltip line) are pure data in
  `spell.ReactionRules`; how every reaction looks is in `ReactionVfx`.
- **`WorldMagic`**: what an effect's element does to the world where it lands, called by
  `Effects.apply` after every effect (fire lights grass, candles and TNT and boils puddles into steam,
  frost freezes water, crusts lava over and puts fires out, storm conducts through water, scrapes copper,
  pulses rods and may charge creepers, wind turns projectiles, earth heaves block displays, life blooms
  and cures weakened zombie villagers, void draws items in and anchors endermen, time ages crops,
  copper, babies and furnaces, arcane shows the invisible, blood feeds nether wart), and being wet
  (`WorldMagic.wet`, read by Conduct and by `Effects.hurt` to dull fire). Which rune does what, and
  every cap, is pure data in `spell.WorldRules`. Block changes go through `Casters.mayEdit` and the
  cast's block budget, plus a per-cast allowance kept against `Cast.identity()`. Frost's crust on lava
  is written down in `TemporaryBlocks` and counts as one of `Effects.isTemporary`'s blocks. The
  creature side reaches into vanilla through small mixins: `EndermanMixin` (an anchored enderman's
  teleports fail), `CreeperAccessor`, `ZombieVillagerAccessor` and `AbstractFurnaceBlockEntityAccessor`.
- **`Wards`**: magic that answers what happens to a creature: Stasis (holds damage via
  `ALLOW_DAMAGE`), Reversal (`ALLOW_DEATH`), Reflect and Foresight, Infinity (holds projectiles),
  and the position history Rewind reads.
- **`Spirits`**: summoned wolves (Summon, Shades, capped at 6 per player) and the AI hold used by
  Freeze, Decree and Stasis. Both carry a saved end time, so a reload can never make a wolf
  permanent or leave a mob frozen.

None of the wards are saved: they last seconds, and a restart simply ends them.

### Discoveries, monsters and the world

- **`Grimoire`**: writes a key into the `grimoire` attachment. The first time for each, it
  condenses `Feats.reward` mana (riddles give none) and sends `Discovery` so the client shows a
  toast. `Grimoire.hint` picks a riddle for a Torn Page.
- **`SecretSpells`**: what each secret spell does, and `discover` (the Grimoire entry and a title
  on the first cast). Rebirth's death save is an `ALLOW_DEATH` hook.
- **`Innates`**: awakens a random innate rune three seconds after the 1st Circle forms (and for
  anyone with circles but no innate), and runs all ten. Their state (threads, stacks, debts,
  afterimages) is kept here per caster and dropped when the server stops.
- **`Unison`**: remembers the last player, element and time for each creature hit; a different
  player with a different element within 20 ticks gets 1.5x on that hit.
- **`Runebound`**: rolls each monster once as it loads (a tag stops it rolling again), stores its
  spell in the `runebound` attachment and its nameplate as a custom name, and every server tick
  runs each loaded Runebound: once its target is in range and in sight, it telegraphs (22 ticks,
  a circle held out in the right hand), then casts through `CastEngine`. It also handles their
  drops; guards placed by the Archive are marked at generation and join when their chunk loads.
  As each one loads (or is bound) it sets the synced `rune_marks` attachment (its spell's colour,
  Adept or not, and when a telegraphed cast lands), which every client draws as glowing marks on
  its body and a faint aura.
  **`Archivist`** is an illager with three phase
  spell lists, a boss bar that names the spell being cast, the rewrite at two thirds and one third
  health (pages tear from its tome in a burst of paper and pale light), and its own drops; the
  Archive Lectern's block entity wakes it. On the client it eases its casting and rewriting poses
  for its model.
- **`RuneSeals`**: a player's spell of the right element lights every seal of that element in the
  door it touched (found by flood fill); when no seal in the door is unlit, the door dissolves.
  Fire also lights unlit campfires.
- **`Affinities`** and **`Climate`**: creature affinities (weak +50%, resists half, immune) from the
  entity type tags `wildercord:affinity/weak_to_<element>`, `resists_<element>` and `immune_to_<element>`
  (a Runebound also resists its Cord's element; a reaction breaks through a resistance), with the
  "Weak!"/"Resisted" callouts and the Grimoire's Bestiary entries; and the elemental climate where a
  player casts (the Nether, a thunderstorm, snow, the deep...), cached per player per second and sent
  to the HUD when it changes. The pure rules are `spell.Affinity`, `spell.Bestiary` and
  `spell.ClimateRules`. See [features/affinities.md](features/affinities.md).
- **`PlayerAffinities`**: players' own affinities at runtime. `gain` keeps every source to its daily
  allowance (`affinity_tally`), scales by `affinity.gain_multiplier`, and announces each level (a message,
  the `Rise` toast, the first level's Grimoire entry, a leaning change). Points come from casting (told by
  `SpellCaster`), reactions and discoveries (told by `Grimoire`), block breaks, deaths and harm survived
  (Fabric events), a check every 100 ticks per player (where they stand, and vanilla's own statistics for
  fish, breeding, enchanting, pearls and elytra flight), and a few small hooks (`FurnaceResultSlotMixin`,
  `TameAnimalTriggerMixin`, `LivingEntityHealMixin`, `LightningRodBlockMixin`, `RuneItem`, `WorldMagic.age`).
  What they give is read in `Effects` (power), `Affinities` (resistance) and `Heart` (the price at V). See
  [features/player-affinity.md](features/player-affinity.md).
- **`LeyWalker`**: sends the ley seed at login and sets the `on_ley` attachment every 5 ticks;
  `Mana.of` and `HeartCircles.tick` read it. The Wellstone's block entity checks the line under it
  once a second and renews `well_until` on players within 12 blocks.
- **`TrainingDummy`**: a `LivingEntity` that heals back to full after every hit, floats each hit
  up as a text display, and keeps its last 5 seconds of hits for the DPS in its name.
- **`SpellChat`**: a chat message decorator that turns `wc:` codes into hoverable spell cards.
- **`advancement.Advancements`** and **`WildercordTriggers`**: the advancement tab's criteria
  (`wildercord:feat`, `grimoire`, `heart_circle`, `runes_known`, `cord`, `moment`). All but
  `moment` test the player's current state, so `Advancements.sync` can fire them at any time: on
  join, after a data pack reload, and from `Grimoire.unlock`, `HeartCircles.form` and
  `Spellbooks.set`/`setCord`. Moments (a cast, a long cast, a passive, a glyph going off, an Adept
  slain) are fired where they happen.

### Visuals: `Vfx`, `TechniqueVfx`, `ExpansionVfx`, `ElementFx`, `Fx`, `Sigils`, `Light`, `BlockFx`, `ScreenFx`

Visuals are sent from the server, so everyone sees the same show. `Vfx.Theme` gives each element
two colours, a mote, a spark and two sounds; shapes are built from shaped light and magic circles
(below), effects from their element's visual language in `ElementFx`, with a few vanilla particles
and primitives (`radial`, `helix`, `stream`, `shockwave`) on top.

`Fx.send` is the one place ordinary particles leave the server. It **skips any particle that would
appear right in front of a player's own eyes**, which keeps your own spells from blocking your
view. `Fx.sendAll` skips that check (for Heart Circle rings while you meditate or form a circle),
`Fx.sendOthers` sends to everyone but one player (the ring spin on every cast, which only onlookers
need), `Fx.sendFar` reaches 512 blocks (for a big Domain's dome), and `Fx.quietly(...)` mutes
everything inside it (passive renewals).

Magic circles are the mod's own particle, `wildercord:sigil` (`SigilOption`: a style, colour,
size, facing, lifetime and spin). The styles are `CIRCLE`, `RING`, `STAR`, `TARGET`, `CRACKED`,
`GLOW` and `BAND` (a thin plain ring, drawn in straight pieces so its line is the same width at any
radius). From 0.7 blocks up, `CIRCLE`, `RING`, `STAR`, `CRACKED` and `TARGET` are drawn the same way
(`SigilParticle.extractDrawn`): rings, strokes and little glyphs laid down as pieces of the thin-line
texture over a smooth glow (`sigil_soft`), so a seal twenty blocks across keeps fine, crisp lines
instead of stretching a 64-pixel texture into fat blocky strokes; they draw themselves in as they
open, and a dark one is a band of darkness with a thin rim of its tint. A spell's whole circle is one particle, `wildercord:spell_circle` (`SpellCircleOption`: the
spell's rune ids, a colour, a radius, a facing and a lifetime); each client builds it from the runes
(see `SpellSigil`), using every rune's own emblem and ring pattern
(`textures/particle/circle/<rune>_mark.png` and `_band.png`, drawn by `tools/circle_art.py`; add-on
runes without art fall back to their family's). `Sigils` builds full circles, ground reticles and
spell circles (`Sigils.spell`) and sends them to players within 128 blocks.

**Shields** (`Shields`) are one attachment and one particle. Raising one sets `spell_shield` on
the target (strength, end time, and the runes of the spell that raised it, for its circles). Nothing
is drawn until a spell comes. Flying shapes (`RuneBolt`, and the sparks, wisps, comets and others
that move through `ShapeRunners.contact`) ask `Shields.intercept` each step: a spell on course for a
shielded creature within 7 blocks sends `APPEAR`, and one reaching the front circle strikes it there.
Every harmful touch of a spell then meets the Shield in `Effects.apply` (and in `Effects.hurt` and
`Wards.stasis` for damage that didn't come through a shape): the first touch of a cast compares the
cast's `weight()` (its list price, set by `SpellCaster` so secrets count what they cost) with the
Shield's strength, then blocks the whole cast at that creature (`BLOCK`, with how many circles it
still broke) or shatters them all and lets it through (`BREAK`). Each client builds the stack of
circles and their shattering from those `ShieldOption` particles (`client.fx.ShieldCircles`,
`ShieldBreak`). `Cast.identity()` is shared by every part of one cast, so a blocked Zone stays
blocked on later pulses.

**Imbue** (`Imbuing`) is a link watched like On Hit: when its shape hits, the runes after it are
stored in the held item (Self), the looked-at block (Self with empty hands) or as a glyph on the
block it touched, instead of being cast. Items let go through ordinary events (a player's strike, an
arrow leaving the bow and `ProjectileMixin` when it lands, a block broken, being hurt, being used,
and `BlockItemMixin` when an imbued block is placed); glyphs are checked every other tick for a
creature on them or a rising redstone signal, and go off when their block is used, shot or broken
by someone else. Their circle and trigger zone follow the block's own outline. Either way `Imbuing.cast` compiles the stored runes with
`SpellCompiler.compileStored` (an effect with no shape lands on whatever set it off) and runs them
as their maker.

A third particle, `wildercord:light` (`LightOption`), is shaped light: a `RING` (a shockwave
racing out), a `RAY` (a beam, its pieces turned to face the viewer), a `SLASH` (a tapered crescent
sweeping across), an `ORB` (a glow wrapped in turning rings) and an `ARC` (a lightning arc that
each client redraws with a fresh jagged, forking path every tick; a flat one only jags sideways, so
it skitters over water or ground), each a soft halo (`sigil_beam`) under a hot core. `Light` sends them; every shape's visuals in `Vfx` and `TechniqueVfx` are built
from them, the circles and glows. `SigilOption.glow` is a camera-facing flash; it replaced vanilla's firework flash, which
showed up close as a pale square.

`wildercord:mote` (`MoteOption`, sent by `Motes`) is the small stuff: a glowing speck that drifts
and twinkles, one that spirals in to a point trailing light, a butterfly of light beating its wings,
and a soft billow of steam or smoke that swells, rises, drifts and thins (`mote_cloud_*`, lit like
the world). The mod's own smoke is these pale billows, never vanilla's black squares. Unlike the
other particles it respects the particles setting. `wildercord:ritual` (`RitualOption`) is an
attunement in progress, sent every check: each client (`client.fx.RitualCircles`) keeps one ritual
per player from them, the land's rune circle turning under them, motes rising into the blank and a
light gathering in the hand, building through the stages, bursting into the rune's emblem at the
finish and fading if they stop coming. The soft textures are drawn by `tools/mote_art.py`.

`ElementFx` holds the element visual languages that every effect (in `Vfx`, `TechniqueVfx`,
`Reactions`, `Innates` and `SecretSpells`) is drawn from: each element's `Palette` (its rune
colour, a highlight and a contrast), its motifs (embers and flame tongues, ice shards and frost
seals, lightning built from short rays, swirling crescents, ground cracks, leaf spirals, imploding
darkness and black cores, star seals and orbiting comets, clock faces, cuts and heartbeats) and its
signature impact (`ElementFx.impact`). Its light and circles go through `Fx.send`, so unlike
`Light` (128 blocks, for shapes) they keep out of a player's own eyes and stay silent inside
`Fx.quietly`; buffs a passive renews send everything at once for the same reason, never through
the `Scheduler`. `ElementFx.DARK` or'd into a colour draws a light as darkness (void's language);
`ringOption` and `slashOption` build the particle for another sender (Heart Circles send their
per-cast spin with `Fx.sendOthers`). The circle under every cast (`Vfx.castCircle`) is the spell's own
circle, flat on the ground, with enchanting glyphs rising from it and nothing flying outward; a Runebound's
telegraph is the same circle held out in its right hand.

`Sigils.send` (which carries circles and shaped light) follows the same two rules as `Fx.send`: it
sends nothing inside `Fx.quietly`, and it leaves a circle or light out for a player whose eyes it
would open right in front of (within 1.25 blocks), so a beam's hand circle never fills its own
caster's screen. Beams (`RAY`) are the exception: you should see your beam leave your hand. The
circle under your own feet is further than that and still shows.

The runes added in batch 6 (Spark, Ray, Nova, Wisp, Comet and the other energy balls and beams, and
the new protection, mining and element runes) have their visuals in `ExpansionVfx`, built the same
way. A bolt in flight is drawn by each client, not the server: `RuneBolt` syncs its two colours
(`DATA_COLOR`, `DATA_SECONDARY`) and the client's `BoltComets` draws it as a comet that follows the
entity's smoothed position every frame; the server only adds a few motes.

`ScreenFx` sends screen effects to players (the `ScreenFx` payload): a camera shake to everyone
near something huge (explosions, bursts, pillars, Domains opening, meteors, tremors, thunderclaps,
Sunfall), a field-of-view kick to a caster whose charged spell leaves their hands, a short punch
when one of their spells lands a hit of 8 or more, and a tint on the edges of the screen of
everyone inside a Domain. The client scales shake and kicks by vanilla's "Screen Effect Scale".

`BlockFx` builds visuals from block displays that grow, hold and shrink away: Tectonic Rise's
dripstone spires and the ice Glacial Lance closes around its targets. They never touch the world's
blocks, and any left behind by a restart are removed as their chunk loads.

## 3. Player state (`player/`)

All per-player state is a Fabric **data attachment** (`WildercordAttachments`): saved with the
player, synced to that player only, and copied through death where noted.

| Attachment | Type | Kept on death | Purpose |
|---|---|---|---|
| `cord` | ItemStack | yes | The worn Cord |
| `spellbook` | `Spellbook` | yes | Learned runes, the four spells and their custom names, passives and their switches |
| `mana` | float | no | Current mana |
| `cooldowns` | list of long | yes (kept on death) | When each spell is ready, as a game time; one more than 10 minutes off (from another world's clock) is ignored |
| `crystals` | int | yes | Mana Crystals used |
| `circles`, `condensed` | int | yes | Heart Circles and mana condensed |
| `spell_kills`, `boss_slain` | int, bool | yes | Heart Circle breakthroughs |
| `runebound_slain` | int | yes | Runebound slain, for the 6th Circle |
| `grimoire` | list of string | yes | Discoveries: reactions, secrets, feats and riddles read |
| `innate` | string | yes | The caster's innate rune |
| `element_casts` | map of string to int | yes | Casts per element as leaning counted them before affinities; read once to give a caster's affinities a start |
| `affinity` | map of string to int | yes | Affinity points per element (see [features/player-affinity.md](features/player-affinity.md)) |
| `affinity_tally` | day, map of string to double | yes (server only) | What each way of earning affinity has offered today: its daily allowance |
| `cracks` | count, mend time | yes | Circles cracked by overcasting, and when they mend |
| `meditating` | bool | no (not saved) | Worked out by the server each tick |
| `rhythm` | stacks, window | no (not saved) | The rhythm chain and the next beat |
| `charge` | spell, start, rune ids | no (not saved) | A spell being charged; synced to **everyone** nearby, who draw its circle (and hear its hum) |
| `spell_shield` | strength, until, colour | no (not saved) | A Shield on any creature; synced to **everyone** nearby, who draw its shell |
| `imbued_shot` | rune ids, colour | on the arrow (not saved) | An arrow fired from an imbued bow: the spell it releases where it lands (server only) |
| `cast_pose` | shape, time | no (not saved) | The spell just cast, for its casting pose; synced to **everyone** nearby |
| `cord_look` | tier, bead colours | no (not saved) | How the worn Cord looks on the wrist (kept up to date by `CordLook`); synced to **everyone** nearby |
| `on_ley`, `well_until` | bool, long | no (not saved) | On a ley line; near an awake Wellstone until |
| `spirit_until`, `frozen_until` | long | on the mob | End times for summons and frozen mobs |
| `runebound` | list of string | on the mob | A Runebound's spell |
| `travel` | `TravelData` | yes | Homes, waypoints, where `/back` goes, teleport requests on or off, the tracked waypoint (server only; see [features/travel.md](features/travel.md)) |
| `loadouts` | `LoadoutData` | yes | Saved Cord setups (up to 6) and the one last loaded, synced for the Cord screen's panel (see [features/loadouts.md](features/loadouts.md)) |
| `rune_marks` | colour, adept, cast time | on the mob (not saved) | How a Runebound's rune marks look; synced to **everyone** tracking it |

`Spellbook` is an immutable record with `withSpell`, `withPassive`, `learn`... Every edit returns a
new instance, which is what makes the attachment save and sync it. Rune ids are kept as strings
even when no such rune is loaded, so removing an add-on never deletes anything.

`Mana.of(player)` computes max mana and regeneration from every source in one place (ley lines and
Wellstones included), and runs on both sides (the server to regenerate, the client to draw the
HUD). `Heart` does the same for circles, breakthroughs and the spell bonuses (`manaCost`,
`cooldownTicks`, `upkeep`), so the HUD, the Cord screen and the server always agree on what a
spell costs. `Heart.active` is the circles minus any cracked ones; everything a circle grants
(mana, regeneration, power, perks, passive slots) reads it, while `Heart.circles` is what you've
formed.

## 4. Heart Circles and passives

- **Condensing**: `SpellCaster.cast` calls `HeartCircles.condense` with the mana spent; each first
  Grimoire discovery adds its reward straight to `condensed`.
- **Breakthroughs**: `Heart.progress` measures each `Circles.Requirement` (runes known, Cord tier,
  spell kills, a boss, reactions, Runebound slain, secrets, a named feat) against the attachments
  and the Grimoire; `Heart.ready` is condensed mana plus every requirement met.
- **Forming**: `HeartCircles.tick` (every 5 ticks, from `SpellCaster`) tells the player once when
  the heart is ready, then, while they meditate, counts up to `Circles.FORM_TICKS` (twice as fast on
  a ley line); taking damage resets it. `HeartCircles.form` adds the circle, refills mana and plays
  the breakthrough; at the 1st it also schedules the innate rune's awakening.
- **Perks**: Mana Skin is an `AFTER_DAMAGE` hook; Flow, Overflow and Archmage are folded into
  `Heart.bonuses`.
- **Spell kills**: `Effects.hurt` records the last spell hit on each creature (and the spell's
  rune count); an `AFTER_DEATH` hook counts a hostile mob that dies within 5 seconds of it, and a
  kill with six or more runes earns *Long Incantation*. A second hook gives every player within 96
  blocks of a dying boss the 7th Circle's boss breakthrough.
- **The rings**: `HeartCircles.rings` draws them. While you meditate or form a circle they go to
  everyone; the quick spin on every cast (`onCast`) goes to everyone but you.

`PassiveCaster.tick` runs each passive slot the working circles have opened: every second it
charges the upkeep (or marks the passive as faltering), and whenever the passive is due it casts it
with `passive = true`. Self passives renew every 2 seconds, quietly after the first time; Orbit
passives renew when their orbs run out, and their `Cast.alive()` turns false the moment
the passive changes or is switched off.

## 5. Networking

`WildercordNetworking` registers client-to-server payloads only. The client never decides
anything that matters; each handler calls into `SpellCaster`, which validates.

| Payload | Handler |
|---|---|
| `CastSpell(spell)` | `SpellCaster.cast` (-1 means the selected spell) |
| `SelectSpell(spell)` | `SpellCaster.select` |
| `EditSpell(spell, runes)` | `SpellCaster.edit` |
| `EditPassive(slot, runes)` | `SpellCaster.editPassive` |
| `TogglePassive(slot)` | `SpellCaster.togglePassive` |
| `ChargeSpell(spell, start)` | `Charging.request` (start a charge, or release it and cast) |
| `RenameSpell(spell, name)` | `SpellCaster.rename` |
| `InscribeScroll(spell)` | `SpellScrollItem.inscribe` |
| `LoadoutRequest(kind, index, name)` | `Loadouts.request`: save as new, save over, load, rename, delete, or load the next one (no runes travel: the server saves its own spellbook) |

Three notices go the other way: `Discovery(key)` (a new Grimoire entry; the client shows a toast),
`LeySeed(seed)` (sent at login: a one-way hash of the world seed that ley lines grow from) and
`ScreenFx(kind, strength, ticks)` (a camera shake, field-of-view kick, punch or Domain tint; see
`cast.ScreenFx`). The travel commands send `Waypoints.Track` (the tracked waypoint, for `WaypointHud`), and
`cast.Climate.Sync(conditions)` tells each player the elemental climate where they stand (once a second, only
when it changes) for the HUD's marks, and `cast.PlayerAffinities.Rise(element, level)` tells a player an
affinity reached a new level (the client shows its toast).
Everything else travels through synced attachments; `CHARGE` is synced to everyone nearby so they
can draw the circle.

## 6. The client

- **`CordScreen`**: the whole Cord UI, drawn in local coordinates inside a scaled pose so it stays
  crisp at every GUI scale. The header holds the Spells/Passives/Grimoire switch and the heart,
  mana and help badges; below are the rows, the family tabs and search box, category chips (only
  for categories you've learned something in), the Codex, and the plain-English readout, whose
  small tool buttons rename the spell (`RenameSpell`), copy or paste its spell code, and inscribe
  a scroll (`InscribeScroll`). It edits a local copy of the spells and sends `EditSpell` /
  `EditPassive` after each change. A list badge at the end of the tabs row (or `Ctrl`+`L`) opens
  `LoadoutPanel` over the window: saved loadouts to load, save over, rename and delete
  (`LoadoutRequest`); after a load the screen reads the spellbook again once it syncs. Beside the window, when there's room, `GuiSpellCircle` draws
  the edited spell's magic circle (laid out exactly as in the world), opening again whenever the
  spell changes; on the Grimoire page it shows the secret spells found so far, one after another,
  each named like a plate in a book. The Grimoire page replaces the rows, Codex and readout with
  everything discovered. It turns on SDL text input while open (see the 26.x notes).
- **`SpellHud`**: the panel beside the hotbar: selected spell, its runes and cost, the mana bar
  with a cost mark, cooldown, and passive drain (or the charge, while charging); above it the
  spell's name, rhythm notes and cracked circles (✦), the elemental climate after the name (a mark
  per element from `ElementGlyphs`, with a green ▲ or red ▼), and a beat ring that closes on the badge
  as the beat comes. Laid out by measuring, and shrinks to fit (short of room beside an offhand slot
  or attack indicator, it sits on top of them: `SpellHud.place`). A spell's reading is remembered
  by its runes (`SpellHud.read`) rather than compiled every frame.
- **`Tooltips`**: the game never wraps a tooltip line, so every tooltip a screen draws goes through
  `Tooltips.fit` (wrapped to at most 280 pixels, and cut short if taller than the screen), and the
  mod's items' tooltips are wrapped as they're built (a late `ItemTooltipCallback` phase).
- **`WildercordKeys`**: R (tap casts, hold charges), V (tap selects, hold opens the
  **`SpellWheelScreen`**), K, the unbound "cast spell N" keys and an unbound "Next loadout" key.
- **`fx/`**: everything magical is blended by `GlowLayers`: `GLOW` adds light to what's behind it
  (overlapping light burns brighter, and it never hides anything), `DARK` takes light away (void,
  for any colour carrying the `Light.DARK` flag). Both are vanilla's particle pipeline (reached
  through `RenderPipelinesAccessor`) with a different blend and no depth writes.
  `SigilParticle` (the magic circle: a flat, tinted, double-sided quad at full
  brightness) drawn in its own `SigilGroup`, which culls anything implementing `SigilGroup.Extent`
  by its whole extent;
  `SpellCircleParticle` (a spell's whole magic circle, built from its runes and opening in stages;
  a secret spell's circle gets its own centrepiece instead of the star: a sun, a snowflake, a
  horizon, a flower, lightning, a black star, a clock, wings, nested stones or a constellation);
  `BoltComets` (every bolt in flight as a comet with a tapering trail); `ScreenEffects` (the
  client side of `ScreenFx`); `ChargeHum` (the rising hum of anyone charging);
  `LightParticle` (rings, beams, slashes and orbs); `ChargeCircles` (the spell's circle in front of a
  charging caster's hands, opening as the charge builds, read from the synced charge);
  `AimPreview` (the reticle or dotted line while charging); `LeyMotes` (ley lines, worked out on
  the client from the ley seed: it traces a few blocks of a line's heart over the ground and lays a
  `LeyRibbon` along it, a streak of pale violet light that flows the length of its path; a few dozen
  at most). Client-only lights, made straight into the particle engine and never sent:
  `Glimmer` (a drifting mote, a faint shaft of lamplight, a flickering firelight glow, or a haze that
  follows an entity), `RingGlow` (rings and lines of soft light in a plane, flat or leaning and
  swinging round, with beads running along). `RuneAura` gives every Runebound a haze and drifting
  specks in its colour (thicker while it telegraphs); `ArchiveAmbience` finds an Archive by its
  lectern's block entity and, inside, reads the halls a slice a tick for lamps, braziers and shelves:
  shafts of light with dust in them, flickering braziers and embers, the arena's inlaid circle
  glowing (brighter while the Archivist is abroad), and quiet pages, whispers and chimes;
  `WellstoneHalo` hangs a turning ring over every awake Wellstone. Each spawner keeps a small
  budget of lights out at once (`Glimmer.Budget`).
- **`GrimoireToast`** (and `GrimoireToast.affinity`, a level reached, with the element's mark from
  `ElementGlyphs`), and the Grimoire page inside `CordScreen`, whose Affinities section draws each element's
  mark, level and a bar toward the next level (`GrimoireLine`'s `glyph` and `points`).
- **Casting poses**: a cast sets the synced `cast_pose`; `AvatarRendererMixin` reads it (and the
  charge, and the Cord's look) into the render state through the `CastingPose` interface that
  `AvatarRenderStateMixin` adds, and `PlayerModelMixin` moves the arms: both hands held out while
  charging, then a motion for the shape (a thrust, a Crescent's sweep, a Barrage's alternating blows,
  arms flung up for the great circles, a push, arms swept back for a Blitz). Arms that point where
  you look follow the head's turn, as vanilla's bow pose does.
- **`render/`**: `CordLayer` (added to every player renderer through
  `LivingEntityRenderLayerRegistrationCallback`) draws the worn Cord on the right wrist from the
  synced `cord_look`: a band in its tier's material (`CordModel.band`, textures from `wear_art.py`)
  and a bead (`CordModel.bead`, drawn cutout and, when it glows (see `CordGlow`), again emissive) for each rune of the ready
  spell, burning brighter while charging and flaring after a cast. `ArchivistRenderer` with its own `ArchivistModel` (a hooded, robed figure hovering
  over the floor, with long sleeves, a floating open tome with a turning page and three loose pages
  circling it; the pose comes from `ArchivistRenderState`'s casting and rewriting blends) and two
  emissive layers (`archivist_eyes`: its eyes and chest gem, always lit; `archivist_runes`: the
  writing on its pages and the light in its cuffs, blazing while it casts); `TrainingDummyRenderer`
  with its own `DummyModel`; and `RuneMarksLayer`, added through
  `LivingEntityRenderLayerRegistrationCallback` to every monster renderer whose model has a marks
  texture (humanoid, skeleton, parched, zombie villager, illager, witch). It draws the monster's own
  model again, emissive (`RenderTypes.eyes`), with white marks tinted by the spell's colour; the
  colour and brightness reach the render state as Fabric render-state data. Babies (their own
  models in 26.x) and monsters of other shapes get the aura only.
- **Mixins**: the Cord slot is added to the inventory menu on both sides (`InventoryMenuMixin`,
  menu index 46), synced from creative mode (`ServerGamePacketListenerImplMixin`), drawn in the
  survival inventory (`InventoryScreenMixin`, with `SlotWell` for the slot's frame) and placed in
  the creative inventory tab (`CreativeModeInventoryScreenMixin`). `LivingEntityRendererMixin`
  copies a Runebound's rune marks into its render state (a render layer only sees the state, and
  there is no event for this step). `GameRendererMixin` applies camera shake where the view bobs
  when you're hurt, and `CameraMixin` the field-of-view kicks. On the server side,
  `LightningRodBlockMixin` turns a Blank Rune by a struck rod into Lightning (and feeds nearby players' storm
  affinity), `FurnaceResultSlotMixin`, `TameAnimalTriggerMixin` and `LivingEntityHealMixin` tell
  `PlayerAffinities` of smelting, taming and a spell healing someone else, and
  `MannequinAccessor` sets up Phantom's afterimage (a mannequin wearing the caster's skin).

## 7. Content: items, loot, effects

- **Items** (`WildercordItems`): one `RuneItem` for every rune (the rune id is a data component,
  `wildercord:rune`, and the item model picks the texture from it; an innate rune's item can't be
  learned from), the Blank Rune, the four Cords, and the Mana Crystal.
- **Cord tiers** (`CordTier`): sockets, spell slots, highest rune tier, base mana and regeneration.
- **Loot** (`WildercordLoot`): extra pools on vanilla chest tables, weighted by tier (a Tier I rune
  is 8x as likely as a Tier IV one in the same pool), Torn Pages in old libraries and ruins, plus
  mob and boss drops. Fishing: a rune (an inline table of the sea list and the fishing runes of the
  world) and a Torn Page join vanilla's single-item treasure pool, and an extra pool on the main
  fishing table, gated by the loot condition `wildercord:magic_waters` (`MagicWatersCondition`, which
  asks `cast.Fishing` about a mana storm, a ley line or a thunderstorm at the bobber), adds a rune
  tangled in the line; `wildercord:fished_rune` (`FishedRuneFunction`) on both grants Reeled In and
  shows the cue. The odds are pure data in `content.FishingRules`. Only vanilla's own tables are
  touched, so a datapack that replaces a table keeps full control of it. Runebound and the Archivist
  drop their loot in code.
- **Entities** (`cast.WildercordEntities`): the bolt (`RuneBolt`, drawn by nothing but its
  particles), the Archivist and the Training Dummy.
- **Particles** (`WildercordParticles`): `wildercord:sigil` (a `SigilOption`),
  `wildercord:spell_circle` (a `SpellCircleOption`), `wildercord:light` (a `LightOption`),
  `wildercord:shield` (a `ShieldOption`: a Shield blocking a spell or shattering, which each client
  turns into the ripple, or the cracks and falling shards, in `client.fx.ShieldBreak`),
  `wildercord:mote` (a `MoteOption`: specks, butterflies of light, steam and smoke) and
  `wildercord:ritual` (a `RitualOption`: an attunement as it goes).
- **Imbued items** carry the `wildercord:imbued` component (`Imbued`: the stored runes, charges
  left, colour); `Imbued.release` says how each kind of item lets its spell go. Glyphs (spells
  imbued into blocks) are kept per dimension in `Imbuing.Glyphs`, a saved data file.
- **`mixin/AvatarRendererMixin`**: a player charging a spell raises both hands (the bow-drawing
  pose), pushing the circle open.
- **Effects and potions** (`WildercordEffects`): Clarity and Mana.
- **Enchantments**: data-driven JSON under `data/wildercord/enchantment/`, read in code through
  `Mana.enchantLevel` with the keys in `Mana` and `Heart`.
- **Blocks** (`WildercordBlocks`): the Wellstone (a block entity that checks the ley line under it
  and boosts players nearby), the Rune Seal (element and lit state; `cast.RuneSeals` opens doors)
  and the Archive Lectern (wakes the Archivist).
- **More items**: Spell Scroll (a `wildercord:scroll` component holds the spell, its name and
  author; casting it builds a `Cast` with base bonuses), Torn Page (a riddle from
  `Grimoire.hint`, and the distance and direction to the nearest Archive), Training Dummy.
- **Casting gear** (`gear/`): `GearDef` (pure: every staff, the tome and the foci, with their numbers),
  `GearBonuses` (pure: what the pieces in two hands do to a spell), `SpellSlots` (pure: which of the five
  spell slots are open), `Gear` (reads the hands; the charged-cast flourishes), `GearItems` and
  `GearLoot`. The server reads the hands when casting and stores the result on the `Cast` (`cast.gear`),
  so the whole spell uses the gear it was cast with. Server settings live in `config/` (`WildercordConfig`
  is pure and unit-tested; `Config` loads, reloads and syncs it), and the add-on API in `api/`, with its
  runtime side in `cast.AddonRunes`. See [features/gear-config-api.md](features/gear-config-api.md) and
  [API.md](API.md).
- **The Archive** (`world/`): `ArchiveStructure` finds a spot and sinks the piece so its stairway
  meets the ground; `ArchivePiece` builds the whole dungeon in its own coordinates (every
  terrain-dependent part is worked out per column, so chunks can generate in any order) and places
  Runebound guards once, in the chunk their feet are in. Placement and biomes are data
  (`worldgen/structure`, `worldgen/structure_set`, a biome tag). `LeyLines` is pure maths shared by
  both sides.

## 8. The asset pipeline (`tools/`)

Nothing under `src/main/resources/assets` or `data` is edited by hand except the mod metadata and
mixin configs. `python tools/generate_assets.py` rebuilds it all from the code:

1. **Reads the roster** by parsing the `shape(...)`, `effect(...)`, `modifier(...)`, `link(...)`
   lines in `Runes.java`.
2. **Textures**: rune icons from `item_art.py` (animated for Tier III-IV and innate runes),
   Cords, badges; GUI and HUD sprites from `gui_art.py`; magic circles and the wheel, beat ring and
   toast sprites from `sigil_art.py`; every rune's ring pattern and emblem from `circle_art.py`
   (handed out by a stable hash of the rune's id, and checked so no two runes share either); items,
   blocks, the two entity skins, the Archivist's two glow layers and the Runebound marks (one sheet
   per vanilla skin layout, under `textures/entity/runebound/`) from `world_art.py`; the worn
   Cord's band (one per tier) and bead from `wear_art.py`. `circle_art.py` hands designs out in the
   order runes are defined, so add new runes after the existing ones and no existing rune's emblem
   changes.
3. **Item models**: a `select` on the `wildercord:rune` component picks each rune's model.
4. **Language**: rune names and descriptions from `Runes.java`, UI strings from the `lang` dict,
   and each rune's "Craft:" and "Found:" tooltip lines.
5. **Recipes and recipe-book unlocks**: from `RUNE_RECIPES` plus the tier costs in
   `TIER_CATALYSTS`; it asserts that every Tier I-III rune has a recipe and no Tier IV or innate
   rune does (the generator keeps its own `INNATE` set, which must match `Runes.INNATE`).
6. **Mana data**: enchantments, brewing recipes, the Mana Crystal recipe.
7. **`docs/RECIPES.md`**, including where every rune drops, parsed from `WildercordLoot.java`.
8. **Batch 5 content** (`write_new_content`): item and block models, blockstates, the Archive's
   loot tables, recipes, the pickaxe tag and the worldgen JSON.
9. **Creature affinities** (`affinity_data.py`): the entity type tags under
   `data/wildercord/tags/entity_type/affinity/` and the affinity and climate text.
10. **The advancement tab** (`write_advancements`): every advancement under
   `data/wildercord/advancement/` (the recipe-book unlocks aside) from the `ADVANCEMENTS` list, their
   titles, the reward loot tables and the tab's background (`world_art.advancement_background`).
   Feat advancements take their title and text from `Feats.java` unless given their own. See
   `docs/features/advancements.md`.

Run `python tools/item_art.py` on its own to render review sheets of every icon into
`build/art-preview/` (`circle_art.py --preview` does the same for every rune's ring and emblem).

`python tools/make_gif.py` builds the README's moving header (`docs/images/hero.gif`) from the
feature tour's `tour_hero_*` frames: run the tour first.

### Sounds: `sound_art.py`

Every Wildercord sound is synthesised by `python tools/sound_art.py`, which writes mono Ogg Vorbis
files into `assets/wildercord/sounds/` and writes `sounds.json`. It needs numpy, scipy and ffmpeg
(with libvorbis), so it is kept apart from `generate_assets.py` and CI doesn't run it: run it by hand
after changing a sound, and commit the result. Seeds are fixed, so a rerun writes identical files.

The script is a handful of small building blocks (envelopes, FM bells, struck glass, band-passed
noise that moves, pitch sweeps, grains for crackle and grit, a chorus, a reverb from a synthetic
impulse) and then one function per sound. Everything tonal is in one key, D major pentatonic, so
sounds that play together (a cast, its circle, its impact) harmonise: play them at pitch 1. Each
sound is levelled for its role (the interface quiet, impacts punchy) and checked once encoded: peak,
loudness, DC offset, clicks at either end, energy above 8 kHz, energy where small speakers can play
it, and the seam of each loop.

The set is registered in `content/WildercordSounds`, whose `cast(element)` and `impact(element)`
fall back to Arcane: a cast (2 variants) and an impact (3) for each of the ten elements;
`charge_loop` (a seamless 2-second hum that `client/fx/ChargeHum` plays for anyone charging, its
pitch rising from 0.8 to 1.3), `charge_full` and `release` (played by `Charging`), `circle_open`,
`beam_fire`, `orb_hum` (made to be replayed every 10 ticks), `shield_up`, `shield_break`,
`domain_open` and `domain_close` (both heard from 32 blocks), `blink` and `magic_break`; the
interface's `rune_thread`, `rune_unthread`, `wheel_open`, `wheel_hover`, `wheel_select` and
`discovery`; and the heart's `circle_formed` and `overcast`. Subtitle text lives in
`generate_assets.py` (`NEW_LANG`) with the rest of `en_us.json`.

## 9. Testing

- **`src/test`**: JUnit 5 tests for everything in `spell/`: reading rules, attachment, costs,
  cooldowns, Blood Price, Vow, passive rules, circle and enchantment maths (`SpellCompilerTest`,
  `HeartAndPassivesTest`); and spell names, spell codes, secret spells, leaning, innate runes, the
  breakthrough table and `world.LeyLines` (`DiscoveryTest`); and the generated advancement tab
  (`AdvancementTreeTest`: every feat has an advancement, every parent, name, icon and reward
  exists). Fast and headless.
- **`src/gametest`**: `WildercordScreenshots` is a Fabric client game test. It builds a world,
  gives the player an Echo Cord and every rune, and:
  - screenshots the HUD and the Cord screen at GUI scales 1-4 and three window sizes;
  - casts every newer rune at husks (an exception anywhere fails the test);
  - asserts real mechanics (Stasis holds every damage type, Reflect, Foresight, Reversal,
    Blood Price, Combo, passives draining mana, forming a Heart Circle);
  - checks that the Cord screen enables text input.
- **`WildercordFeatureTour`** runs in a normal (not superflat) world and tours batch 5: charging,
  rhythm, the wheel, the Grimoire page and toasts, every secret spell, a Runebound's telegraph, the
  dummy, a domain clash, overcasting, an innate awakening, a ley line and Wellstone, and a placed
  Archive with its Archivist. It asserts what can be asserted and screenshots the rest as
  `tour_*.png`. Set `WILDERCORD_TOUR_ONLY=1` to skip the older screenshots and run just the tour.
  Spells that fly away from the caster are filmed from the side: `director` spawns an invisible
  text display as the camera and hides the HUD (the local player isn't drawn from another camera,
  so those shots show the spell and its targets only).
- **`WildercordAffinitiesTest`** checks creature affinities (frost on a blaze against a husk, fire on
  a hoglin and a blaze, a snow golem's immunity, a Shatter through a resistance, a Runebound's own
  element, the Bestiary), the climate in the Nether and the End, and the config switches.
- **`WildercordPlayerAffinityTest`** checks players' own affinities (casting grows its element, mining stone
  stops at the day's allowance, a level's toast and Grimoire entry, power on that element only, resistance
  from III against a Runebound, the config switch) and screenshots `affinity_toast` and `affinity_grimoire`.
  `PlayerAffinityTest` covers the pure rules.
- **`WildercordLoadoutsTest`** drives the loadouts panel (save, rename, load back), and checks quiet
  runes after a load, the cooldowns a load starts, the refusal while charging, the limit of six and the
  quick switch; screenshots `loadouts_panel` and `loadouts_panel_854x480`.
- **`WildercordAdvancementTest`** checks the server loaded the advancement tab, that a feat, a
  reaction, a secret, a Heart Circle, learning runes, a Cord and a cast each grant theirs, and
  that revoked ones come back from the player's state as they would on login.
- **`WildercordFishingTest`** rolls the fishing tables thousands of times through the server's loot
  API with a real bobber (treasure runes and Torn Pages at their rates; a tangled rune never in plain
  water or out of open water, and at its rate under a mana storm, on a ley line and in a
  thunderstorm; Reeled In), then casts Tidehook at a husk and Current in water, in the rain and on
  dry land.

## 10. Rules that keep it safe

- **The server decides everything.** Edits and casts are validated in `SpellCaster`; the client
  only draws.
- **Friendly fire is off.** `Targets.canHarm` / `canHelp` decide every effect's targets, and
  respect the `pvp` game rule (for other players and their pets alike). Damage to players is scaled
  down (`Effects.PVP_DAMAGE`).
- **Every cast has budgets** (creatures, blocks, link depth), and spirits are capped per player.
- **Monsters never change blocks.** Every block edit checks `Casters.mayBuild`, which is false for
  anything but a player allowed to build there.
- **Nothing temporary is permanent.** Summons and frozen mobs carry saved end times; Rampart
  blocks crumble when the server stops; Rampart, Span and Light blocks, and frost's crust on lava,
  are written down with their world (`cast.TemporaryBlocks`, as frozen water is in `Thaws`), so one
  left by a crash, or out of loaded ground when its time came, goes as its chunk loads, and one broken
  any way at all (a player, a piston, an explosion) drops nothing (`TemporaryBlockDropsMixin`; a crust
  broken early gives its lava back);
  block-display visuals are removed as their chunk loads; wards aren't saved at all.
- **Bosses are only ever slowed**, never frozen, swapped or held in place, so their fights can't
  break.

## 11. Minecraft 26.x notes

26.x is unobfuscated (no mappings), and several APIs differ from older versions. Things that are
easy to trip over:

- **GUI**: `GuiGraphicsExtractor` (`fill`, `text`, `blitSprite(RenderPipelines.GUI_TEXTURED, ...)`,
  `item`, `enableScissor`, `setComponentTooltipForNextFrame`); screens override
  `extractRenderState`; input events are `MouseButtonEvent`, `KeyEvent`, `CharacterEvent`.
- **Text input**: input goes through SDL, which only delivers typed characters while text input is
  on. A screen that reads characters must call
  `minecraft.textInputManager().startTextInput(this)` (the game turns it off when the screen
  closes). Vanilla `EditBox` does this for you.
- **Entities**: `Identifier` (not `ResourceLocation`), `EntityTypes`, `hurtServer`,
  `setInvulnerableTime`, `teleportTo(level, x, y, z, Set<Relative>, yRot, xRot, false)`.
- **Particles**: `ServerLevel.sendParticles(player, particle, overrideLimiter, alwaysShow, ...)`;
  without `overrideLimiter` a particle is only sent within 32 blocks.
- **Entity rendering**: renderers fill a render state (`extractRenderState`) and then `submit` to a
  `SubmitNodeCollector`; a `RenderLayer` sees only the state. Extra per-entity data travels as
  Fabric render-state data (`RenderStateDataKey`, `state.setData`). An emissive layer is
  `collector.order(1).submitModel(model, state, poseStack, RenderTypes.eyes(texture), light,
  OverlayTexture.NO_OVERLAY, argb, null, state.outlineColor)` (as vanilla `EyesLayer`); the tint's
  alpha is its strength. Many baby mobs have their own models and skin layouts.
- **Fabric events used**: `ServerLivingEntityEvents.ALLOW_DAMAGE` / `AFTER_DAMAGE` /
  `ALLOW_DEATH` / `AFTER_DEATH`, `ServerTickEvents`, `ServerEntityEvents.ENTITY_LOAD` /
  `ENTITY_UNLOAD` (Runebound, block-display cleanup), `ServerPlayConnectionEvents.JOIN` (the ley
  seed), `ServerMessageDecoratorEvent` (spell cards in chat), `ServerLifecycleEvents`,
  `LootTableEvents.MODIFY`, `PlayerBlockBreakEvents.BEFORE`, `HudElementRegistry`,
  `LivingEntityRenderLayerRegistrationCallback` (rune marks).
- **Dev client**: Loom's `runClient` loads classes from `build/`; rebuilding while it runs mixes old
  and new classes. The build supports `-PaltBuild` (outputs to `build-alt/`) for compile checks
  while a client is open.
