# Architecture

A tour of how Wildercord fits together, written for someone about to change it. It follows a
spell from the rune roster to the particles on screen, then covers player state, the client, the
asset pipeline and testing. For *what* each rune does and why, see [DESIGN.md](DESIGN.md).

- [The big picture](#the-big-picture)
- [1. The spell engine (`spell/`)](#1-the-spell-engine-spell)
- [2. Casting (`cast/`)](#2-casting-cast)
  - [Monsters of the wilds](#monsters-of-the-wilds-monster)
  - [Spell mastery](#spell-mastery-mastery-masterychoices-inscriptions) and [its hooks](#mastery-hooks-for-other-systems-apispellmasteryapi)
- [3. Player state (`player/`)](#3-player-state-player)
- [4. Heart Circles and passives](#4-heart-circles-and-passives)
- [Aura: the swordsman's path](#aura-the-swordsmans-path-aura) and [its hooks](#hooks-for-the-next-wave-apiauraapi)
- [Aura: the swordsman's path](#aura-the-swordsmans-path-aura) and [its hooks for the next wave](#hooks-for-the-next-wave-apiauraapi)
  - [Crescents in flight](#crescents-in-flight-auracrescents) and [the world of aura](#the-world-of-aura-auraworld)
- [5. Networking](#5-networking)
- [6. The client](#6-the-client)
- [7. Content: items, loot, effects](#7-content-items-loot-effects)
- [8. The asset pipeline (`tools/`)](#8-the-asset-pipeline-tools)
- [9. Testing](#9-testing)
- [Hooks for other systems](#hooks-for-other-systems)
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
- **`MasteryRules`**, **`MasteryTraits`** and **`MasterySigil`**: spell mastery's pure parts. `MasteryRules` holds a
  spell's identity (`key`: its rune ids in order, Knots untied, hashed past 512 characters), the five ranks and their
  thresholds (0, 100, 350, 1,000, 3,000), what each kind of outcome is worth (`strike`, `heal`, `HELP`, `UTILITY`), the
  moment (`situation`), repetition (`repetition`, `forget`), practice (`practice`, capped at 60) and the trait caps
  (`traitPower`, `COST_FLOOR`...). `MasteryTraits` is the catalogue (one `trait(...)` line each, read by the asset
  generator for the language file), a spell's makeup (`Profile.of`), and the weighted, seeded offer (`offer`).
  `MasterySigil` draws the personal sigil from a seed (`seed(owner, key)`, `glyph(seed)`). See
  [Spell mastery](#spell-mastery-mastery-masterychoices-inscriptions).
- **`Overchannel`**: casting as a performance, pure: the stages a heart holds (`stagesFor`), when a stage is due
  and when an overheld channel tears (`due`, `tears`), the beat and the HUD's next beat (`beatTime`, `onBeat`,
  `nextBeat`), what a release adds (`power`, `traceBonus`) and its surge chance (`surgeChance`, `combined`), the
  drain (`drainPerTick`, `fed`), how much of a reported trace counts (`validTrace`) and what tearing loose does
  (`backfire`, never damage). `Tuning` is the server's `channeling` settings.
- **`TraceGlyph`**: a spell's tracing glyph (3-5 strokes between nine points, from an FNV hash of its rune ids),
  path geometry (`resample`, `nearest`, `distanceTo`), the score of a traced path (`score`: coverage times
  `0.4 + 0.6 × precision`, held back for a path over `LENGTH_ALLOWANCE` times the glyph's length) and the assist
  levels (`Assist`: tolerance and pull).
- **`Incantation`**: every rune's syllable (hand-tuned for the best known, otherwise an onset, vowel and ending
  made from the id, never an English word or a rune's name, unique within the roster, given in roster order so a
  new rune never changes an old one's), and a spell's incantation (`of`, `line`).
- **Each world's own magic** (see [DESIGN.md](DESIGN.md#each-worlds-own-magic)): `Resonance` (one resonance: id,
  name, riddle, runes, twist, colour), `ResonanceTwists` (the 27 twists: name stems, omen, description, and the `Fit` of
  spells each can ride), `ResonanceForge` (the draw: SHA-256 of the world seed and reroll salt into a `java.util.Random`,
  the pool of crafted Tier I-III runes, templates, `problem` for what can't be a resonance, names and riddles),
  `RuneQuirks` (the quirks, their conditions over an `Attunements.Place`, and their draw on a stream of its own) and
  `ResonanceLore` (the one place that decides what a client may know: found, read, announced, or nothing).
- **`RuneReading`** and **`RuneHints`**: a rune's reading stage from its progress (absent means understood), and what an
  unread rune shows (a hint from its element, family, kind and description words, or a written one) and a glimpsed one
  (its text cut into pieces with the numbers veiled).
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

**Performed casting** (`Charging`, rules in `spell.Overchannel`). A charge fixes, as it begins, the ticks it takes
to fill and the stages the caster's working circles hold, and prices the spell (a `Channel`, server only, with the
ticks spent steadying and the reported trace). Every tick, for a player with a charge (`Charging.everyTick`, from
`SpellCaster`'s tick handler, before its every-5-ticks gate), sneak held counts as steadying, and past full the
overchannel drains `drainPerTick` from stage I on while `fed` (never the spell's own price), climbs when `due`
(updating the synced `charge` with the stage and when it landed, so every client's beat and cracks agree), and
tears when held `GRACE` past the last stage: `backfire` stops the charge (the release that follows does nothing),
burns mana, dazes (`CastLock.daze`, quiet and without the recovery window a foe's seal gives, plus Slowness) and
surges harmlessly (`WildSurge.fizzle`). The client reports a trace with `TraceSpell` just before the release;
`Charging.trace` keeps it for that charge only. At release, `Charging.performance` reads the stage, the beat and
`validTrace` into a `Performance` (power, surge chance), and `SpellCaster.cast(player, spell, charge, performance)`
multiplies it into the cast's power, notes it on the `Cast` (`Cast.performance`, so `Effects.hurt` counts it inside
the players' bonus cap through `SpellDefenceRules.capBonus(bonus, performance, cap)`), and rolls the surge with any
overcast's as one chance (`WildSurge.roll`). The line above the hotbar says how it went; `Charging.last` keeps it for
tests.

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
`CastEngine.spread`); Volley repeats them (`CastEngine.volley`). The second batch of new runes' shapes (Glaive,
out and back with a fresh budget for the way back; Imprint, which erupts where you stood; Latch, which holds one
creature and strikes it again and again) are in `CraftedShapes`, with On Reaction and On Weakness: `onHit`
counts each creature's reactions (`Reactions.count`) and weak strikes (`Affinities.weakStrikes`) before and after
their group lands, and fires the rest at those that went up.

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
+6% per circle. The second batch of new runes' effects are in `CraftedRunes`, which also shares a Kindred effect
(applying it again, at half power, to the caster and the nearest ally it missed) and draws Belated's clock
(`onHit` holds a Belated effect back); Thirst is read in `Effects.hurt`, which heals the caster a share of what the
hit really took, and Gash's "no healing" is in `mixin/LivingEntityHealMixin` (which also counts healing others
toward life affinity).
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

**`Soar`** (flight) is saved, unlike the wards, because it changes a player's own abilities (`mayfly`
and the flying speed). A flight writes the `soaring` attachment (its end, the speed from before, and
whether it's over and the player is coming down), and that note is what lets Soar take back only a flight
it gave. `Soar.tick` runs every flier each server tick: the warning 3 seconds before the end, the take-off
and gusts, a ward check every 10 ticks (`DungeonWards.warded`), and at the end the flight taken back and a
gentle descent (slow falling, Feather Fall's drift through `Effects.glide`, and an `ALLOW_DAMAGE` hook that
cancels fall damage) until the player lands. Leaving (and `SERVER_STOPPING`) takes the flight away before
the player is saved but keeps the note, and `Soar.login` gives back what's left or lets them down; death
(`AFTER_DEATH`) ends it, a new body (`AFTER_RESPAWN`) starts without it, and a dimension change sets them down.
Creative and spectator players are never touched. A grounding hit (`Soar.ground`: any `Reactions.mark` of
PULLED, Weigh, Downdraft) ends it and starts a 30-second rest. The pure numbers are `SoarRules`; the
server's one-shot visuals and the wing shape are `SoarVfx`, and every client draws the wings and wake from
the synced note (`client.fx.SoarWings`).

### Discoveries, monsters and the world

- **`Grimoire`**: writes a key into the `grimoire` attachment. The first time for each, it
  condenses `Feats.reward` mana (riddles give none) and sends `Discovery` so the client shows a
  toast. `Grimoire.hint` picks a riddle for a Torn Page.
- **`SecretSpells`**: what each secret spell does, and `discover` (the Grimoire entry and a title
  on the first cast). Rebirth's death save is an `ALLOW_DEATH` hook.
- **`WorldBonds`**: ties the world's own magic, mastery and residues together through their hooks: the Lingering Mark
  trait's residue sink (`Residues.leave`, one per caster every 8 s), the `world_quirk` mastery circumstance and the
  World-Tuned trait (offered to spells with a quirked rune), and a wake condition binding about one in four harmonies
  (by name hash) to a ley crossing for their first waking.
- **`WorldResonances`**: this world's resonances and quirks at runtime. (Players and the server config call resonances
  **harmonies**, since an arcane rune is already named Resonance; the code keeps the older word.) `Ledger` (saved data with the overworld) holds
  the draw, the seed and settings it was drawn under, and who found each first; `ledger(server)` draws it again only
  when the count, the reroll salt or the seed changes, or a rune it used is gone. `SpellCaster.cast` calls `wake` for a
  spell that isn't a secret: a match (and every wake condition passing) is found (`discover`: Grimoire, title, the
  `Revealed` toast payload, the announcement) and `TwistMagic.ride` marks the cast. `refresh(player)` works out the
  player's `world_lore` view at login, respawn and after any change; `hint` serves Torn Pages.
- **`TwistMagic`** and **`TwistVfx`**: what each twist does and how it looks. A ride is keyed by `Cast.identity()` in a
  weak map: its set piece goes off as the spell leaves the hands (3 ticks after the press, as the spell does), and
  `CastEngine.onHit` hands each hit to `TwistMagic.onHit`. A twist's lasting parts book one step at a time on the
  `Scheduler`; Second Voice runs the root again on `cast.again(0.5)` (a new identity, so it never echoes again); Pale
  Steed's horse carries `spirit_until`, as a summoned wolf does.
- **`WorldQuirks`**: `Effects.applyEffect` multiplies an effect's power and duration by `WorldQuirks.power` and
  `duration`, and `Effects.apply` calls `after` for a quirk's faint second strike. The place is read where the effect
  lands; the first time a quirk holds for a player's spell it's written into their Grimoire.
- **`RuneReadings`**: reading runes at runtime. `RuneItem.use` and `Innates.awaken` call `learned` for a rune newly
  learned (progress 0 in `rune_reading`); the add-on events `AFTER_CAST` (each rune of the cast, Knots and weaves
  untied, +1) and `SPELL_HIT` (the last cast's runes, +1 once, when it lands on something) move it on; understood, the
  entry is dropped. `stage(player, rune)` answers on both sides.
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

### Monsters of the wilds: `monster/`

Six creatures that spawn on their own (the design and every number are in DESIGN.md's
[Monsters of the wilds](DESIGN.md#monsters-of-the-wilds)). The package keeps out of `cast/` except through three small
seams: `Effects.apply` calls `Monsters.onSpell(cast, hit, effect)` after every effect (light shows a Gloomstalker, earth
grounds a harpy, even for spells that do no harm), `Effects.currentElementNow()` is public so a creature's `hurtServer`
can ask which element is landing (with `Dungeons.spellLanding()`, read through `MonsterMagic`), and `Runebound.pool`
asks any `RuneboundKin` for its spells.

- **`MonsterRules`** (pure, `MonsterRulesTest`): the six `Kind`s (id, weight, pack size), the spawn rate's weights,
  root time by difficulty, when a Gloomstalker hides (`veiled`) and how opaque it draws, what a curled Geode Crawler
  takes, how a Mana Ooze fills, grows and splits (`feed`), and the lob that lands a bubble exactly (`lob`, flown as the
  bubble flies: move, then fall).
- **`WildMonster`**: the shared base (a `Monster`): a synced byte of pose flags (`WINDUP`, `ACTING`, `GUARD`, `STUNNED`,
  `VEILED`, `ALT`) eased on the client into `pose(flag, partial)` for the models, as `DungeonBoss` does; `holdPose`
  holds a creature without AI in a pose for the tests' pictures.
- **The creatures**: `Bramblewalker`, `Gloomstalker`, `ThunderwingHarpy`, `GeodeCrawler`, `BogWitchFrog` (each a
  `WildMonster`, its signature a small state machine in `customServerAiStep` or one goal, so nothing else moves it
  mid-attack) and `ManaOoze` (an `AbstractCubeMob`, hopping like a slime, its death split turned off). The harpy flies by
  its own steering (no paths): it eases its velocity toward a goal each tick and `travelFlying`s while airborne.
  `BogBubble` is the frog's projectile (pickable, so anything that strikes it pops it); `ThrownBramble` the thrown drop.
- **`MonsterMagic`**: monster spell damage (`hurt`: `indirectMagic` from the monster through `SpellDefence`), the vine
  root (the Root rune's Slowness VII and its visuals), shoves and knockback that sync to a player's client, kit sounds.
- **`MonsterContent`**: the entity types (`notInPeaceful`, so vanilla discards them on Peaceful and refuses their spawns),
  attributes, the drops (`MonsterDropItem` and its three with uses), the spawn eggs and the creative tabs.
- **`MonsterSpawns`**: each type's spawn rule (`SpawnPlacements`: the vanilla monster check, its own ground, and the
  server's switch for natural spawns) and one Fabric biome modification over every overworld biome (by where they
  generate and by the vanilla `is_overworld` tag) that adds each to its biomes' monsters, its weight scaled by
  `monsters.spawn_rate` as the world loads. `monstersIn(biome)` reads a biome's spawns back (the tests).

### Spell mastery: `Mastery`, `MasteryChoices`, `Inscriptions`

Spells grow with their caster (see [DESIGN.md](DESIGN.md#spell-mastery) for the rules and numbers). The state is two
attachments in `player.MasteryAttachments`: the private `MasteryBook` (a record per spell key: experience, practice,
four trait slots with borrowed and changed bits, the waiting offer, circumstance counts, the sigil's seed, when it was
last used and who taught it) and the public `Look` of the spell a player has ready (a hash of its key, its rank, its
sigil's seed, and flags for the looks its traits give it), which is all anyone else's client learns.

The flow of one cast:

1. **At the gate.** `SpellCaster.cast` multiplies the price by `Mastery.costFactor` and the cooldown by
   `Mastery.cooldownFactor` (passed as the factor `Heart.manaCost` and `Heart.cooldownTicks` already take, so the Cord
   screen, HUD and wheel, which call the same functions, agree), and a helpful-only spell's power by
   `Mastery.powerFactor`. Then `Mastery.onCast` gives the `Cast` its **`Tally`**: the spell's key and runes, the traits
   that work on it, the moment (`MasteryRules.situation`), its circumstances, and what it has earned so far. The tally
   rides on `Cast`'s shared state, so links, pulses, a storm's echo and Twin Star share it (and its per-cast cap); a
   parried spell drops it (`Cast.reflected`). `onCast` also sets the public look, rings Bellsong, sends motes for the
   cosmetic traits and, for a custom-named spell at Adept or higher, the `MasteryChoices.Title` to every player within 32
   blocks.
2. **As it lands.** `Effects.hurt` multiplies the hit's bonuses by `Mastery.damageBonus` (before `SpellDefenceRules.capBonus`,
   so the defence cap holds), skips the wet penalty for Undying Flame (`Mastery.wetFire`), and after the hit calls
   `Mastery.afterDamage`: experience by the foe's worth and what the hit took (or practice, for a dummy or the practice
   arena), and the traits that answer a strike (Soul Sip, Mana Harvest, a mark, a leap through `Effects.hurt` again with
   a guard so it never leaps twice). `CastEngine.onHit` calls `Mastery.afterHit` after a group's effects (allies helped,
   a caster moved, a block worked), and `mixin.LivingEntityHealMixin` reports health a spell restores through
   `Mastery.healed` (the cast being applied is `Effects.applyingCast()`). `RuneBolt`, `CastEngine.beam` and `aimPoint`
   stretch their reach by `Mastery.range`; `Charging.fullTicks` by `Mastery.chargeSpeed`.
3. **Once a second.** What casts earned waits in `Mastery`'s pending map (with each spell's memory of recent places and
   kinds of foe, for repetition and variety) and is written into the book by `Mastery.flush`, also on leaving, on death
   and as the server stops. A rank reached gets an offer (`MasteryChoices.offerIfDue`: three from
   `MasteryTraits.offer`, plus a borrowed trait to keep) and a `MasteryChoices.Rise` toast. The same tick refreshes each
   player's look from their selected spell (`Mastery.refreshLook`), leaving a look set by a cast alone for two seconds
   so its circle can find it.

**Choices** come from the Cord screen as `MasteryChoices.Request(kind, spell, slot, trait)`: the server reads the spell
from its own spellbook, finds the record, and chooses (the trait must be on offer), re-rolls or unbinds (once per
slot, for `MasteryRules.CHANGE_LEVELS` levels).

**Inscription** (`Inscriptions`): `SpellScrollItem.inscribe` asks `Inscriptions.inscribe` to add an `Inscription` component
(key, the inscriber's own traits, sigil seed, rank, author) when the spell is Adept or higher. Using the scroll casts it
with those traits through `Mastery.onScrollCast` (a tally that learns nothing); using it while sneaking runs
`Inscriptions.study`, which checks the runes match the inscription, the reader knows them and their Cord holds them,
and that they have no record of the spell, then threads it into an empty row and writes a fresh record with the traits
borrowed and the teacher's seed.

### Mastery hooks for other systems: `api.SpellMasteryApi`

Three small hooks let other parts of the mod (or add-ons) feed spell mastery without touching it:

- **`addCircumstance(id, (caster, spell) -> boolean)`**: a new circumstance a spell's casts are counted in, checked as
  each spell is cast (beside the built-in ones in `MasteryTraits.CIRCUMSTANCES`). A trait can ask for it with
  `.when(id)`, exactly as Undying Flame asks for `rain`. A world's rune quirk or standing in a place of power is one
  line each.
- **`registerTrait(MasteryTraits.custom(...))`** and **`addOfferSource((caster, key, spell, rank) -> List<Weighted>)`**:
  traits from outside the catalogue (namespaced ids) and when to offer them. Offered traits join the catalogue's in
  the same seeded draw, each with its weight, and must fit the spell. Their hooks are the catalogue's (`Hook.DAMAGE`,
  `ON_STRIKE`, `COSMETIC`...), so they work through the same code and caps.
- **`setResidueSink((level, at, caster, spell, element, trait) -> ...)`**: what a trait with `Hook.RESIDUE` (Lingering
  Mark) leaves where its spell first lands each cast. Until a residue system sets one, the default leaves a faint
  glimmer in the element's colour.

Everything registered is called on the server thread; a hook that throws is logged and skipped.
### A world that remembers magic: `Residues`, `PowerPlaces`

The design and its numbers are in DESIGN.md's [A world that remembers magic](DESIGN.md#a-world-that-remembers-magic).

- **The pure rules.** `world.ResidueRules` holds the ten `Kind`s (element, block path, whether it takes the ground's
  place (`COVER`) or lies on it (`REST`), lifetime, reagent), when a cast leaves one (`strength`: price ÷ the
  threshold, an overcast, a boss, a reaction), how many blocks and how far (`cells`, `spread`), how long (`lifetime`),
  the caps, and where one may go (`judge` over a `Site`: never anything built, never a block with contents, a spell's
  passing block, a ward or a place the caster may not edit). `world.ResidueLedger<P>` is one dimension's record,
  indexed by position, by chunk (the caps, and what waits for a chunk) and by the time each fades (a `TreeMap`), so the
  server only ever reads the residues whose time has come. Positions pack exactly as `BlockPos.asLong` and chunks as
  `ChunkPos.pack`. Both are unit-tested (`ResidueRulesTest`, `ResidueLedgerTest`).
- **When.** `Effects.apply` calls `Residues.onSpell` after `WorldMagic.onSpell`: a player's (or a boss's) cast strong
  enough, rested (5 s a caster) and the first of its cast (`cast.once("residue")`) leaves its effect's element where it
  landed. The cast's price is `Cast.weight()`; an overcast is marked by `SpellCaster` (`Cast.markOvercast`, kept by
  copies). Every reaction in `Reactions` (and Blackspark in `Techniques`) calls `Residues.reaction`, which rolls 35%.
- **Placing.** `Residues.leave` picks columns round the impact (its own first), finds each column's surface
  (`groundFor`), and asks `judge` (the pure rule, then `DungeonWards.warded`, then permission last, so a claim mod only
  hears about blocks a residue could really take: `Casters.mayEdit` for a player, `spells_edit_blocks` and mob griefing
  for a boss) and the ledger's caps. Natural ground is the `wildercord:residue_ground` block tag. It reuses the terrain
  spells' rules: never over a `TemporaryBlocks` record, `Effects.isTemporary` now counts residues (so pistons and other
  spells leave them alone), loaded ground only.
- **Fading.** `Residues.Record` is a `SavedData` per dimension (codec of `Row`s). Every 20 ticks the sweep takes up to 64
  due residues: in loaded ground each fades (the replaced state back, only if the residue still stands), otherwise it's
  `park`ed by chunk; `ServerChunkEvents.CHUNK_LOAD` notes chunks with parked residues and the next sweep fades them.
  Harvested by a player (`PlayerBlockBreakEvents.AFTER`, or a bottle on an eddy) the record goes and a `COVER` residue's
  ground comes back; gone any other way (`ResidueBlock.affectNeighborsAfterRemoval`) the record goes at the end of the tick.
  `Residues.fastForward` moves every residue's clock (tests).
- **Blocks.** `content.ResidueBlocks` registers the ten (no items; `COVER` ones `IMMOVEABLE`, `REST` ones `POPPED`; void
  scar and bloodmoss get path types through Fabric's `LandPathTypeRegistry`). `ResidueBlock` is one class switching on its
  kind: shapes, `entityInside` (ash thaws, fulgurite's Speed, the eddy's updraft on both sides, the glyph's Glowing,
  stilled sand's Slowness and unlimited item lifetime), random ticks (a wildbloom's seeding through `Residues.seed`,
  bloodmoss feeding wart), a void scar's scheduled tick every 4 ticks (pull, stage from its record, animals nudged
  away). `ResidueAmbience` is the client-side display tick: the mod's `MaterialOption` and `MoteOption` particles and a
  kit sound (`<element>_smoulder`, `_glint`, `_fizz`...).
- **Reagents.** `content.Reagents` registers the ten `ReagentItem`s (Cinder Ash carries `COOKING_FUEL`, the Bottled Gale a
  `CONSUMABLE` with Slow Falling) and the `wildercord:reagents` item tag; `ReagentItem` holds the small uses (Geode Grit
  calls `CraftedRunes.prospect(player, ...)`, Star Dust writes its light to `TemporaryBlocks`). At the altar,
  `spell.AltarReagents` (pure, `AltarReagentsTest`) turns the plan `Fusions.plan` would make into the reagent's
  (`Result`: the plan, copies, whether the catalyst or a rune socket is kept, health paid); `FusionAltarMenu` reads a
  reagent from any rune socket (it reads as empty to `Fusions.plan`), shows `plan()` with the change, and on Fuse pays,
  keeps and multiplies as the result says. `FusionAltarScreen` writes the reagent's line in the panel.
- **Places and times of power.** `spell.ClimateRules` gained `LEY_CROSSING`, `FULL_MOON`, `NEW_MOON`, `NOON`, `DAWN`
  and `DUSK`, celestial `Shift`s, and a `Tuning` (a crossing's bonus, the celestial scale). `Surroundings` carries the
  crossing, the moon's phase (`moonPhase(dayClock)`) and the celestial switch; its old 12-argument constructor knows
  none of them. `cast.Climate` fills them in (`surroundings(level, feet, head, ley, crossing)`), applies `tuning()` from
  the `places_of_power` settings, sends the tuning with the conditions, and answers `costFactor(player)` on both sides,
  which `Heart.rawCost` multiplies in. `world.LeyLines` runs two weaves (`first`, `second`; `strength` is the larger) and
  finds crossings (`crossing`, `atCrossing`, `crossingIn(chunk)`, `nearestCrossing`); `LeyWalker` tells a Cord wearer on
  stepping onto one (and grants Crossroads).
- **The hooks** other features call: `Residues.leave(level, element, at, strength, by)` (a mastery trait leaving
  residues), `Residues.kindAt`, `PowerPlaces.isPlaceOfPower(level, pos)`, `PowerPlaces.at(level, pos)` and
  `PowerPlaces.of(player)` (what's favoured there now: conditions and factors; a resonance waking only at a place of
  power), and `#wildercord:reagents` / `Reagents.kindOf(stack)`.

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
| `charge` | spell, start, rune ids, ticks to fill, stages held, stage reached and when, traceable | no (not saved) | A spell being charged; synced to **everyone** nearby, who draw its circle (cracking as it overchannels), hear its hum and read its incantation |
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
| `mastery` | `MasteryBook` | yes | Every spell's mastery record (see [Spell mastery](#spell-mastery-mastery-masterychoices-inscriptions)) |
| `mastery_look` | spell hash, rank, sigil seed, flags | no (not saved) | The public look of the spell a player has ready; synced to **everyone** nearby, who draw its rank and sigil on its circle |
| `rune_reading` | map of string to int | yes | Runes still being read, and how far (see `spell.RuneReading`); a rune absent is understood |
| `world_lore` | `WorldLore` | no (not saved) | What the player may know of their world's resonances and quirks (`spell.ResonanceLore`'s view), worked out by the server |
| `soaring` | end time, speed before, falling | no (ended at death) | A Soar flight or its gentle descent: the note that Soar gave the flight, saved so a logout, restart or crash is tidied at login; synced to **everyone** nearby, who draw the wings |
| `aura` | `AuraAttachments.Data` (method, stage, experience, aura held, practice) | yes (the aura held empties on a new body) | A player's aura: the swordsman's path (see [Aura](#aura-the-swordsmans-path-aura)) |
| `aura_state` | `AuraAttachments.State` (stance settled at, guard raised and until, slash ready at, backlash until, stillness held, trial until) | no (not saved) | What a player's aura is doing now, for their HUD's beat ring and marks |
| `aura_look` | `AuraAttachments.Look` (colour, stage, lit, guarding, breathing) | no (not saved) | How a player's aura looks; synced to **everyone** nearby, who draw the blade's glow |
| `aura_presence` | `AuraPresence.Look` (shell up, shell struck at, the colour of a spell on the blade and until when) | no (not saved) | The top stages' look: aura armour's shell and the spellblade; synced to **everyone** nearby |
| `aura_timers` | `AuraPresence.Timers` (step ready at, Dominion ready at, Dominion until) | yes, and kept through death (game time, so a relog never resets a rest) | The top stages' cooldowns, for the owner's HUD |
| `aura_arts` | `SwordStrings.Cooldowns` (when each art is ready again, by id) | yes, and kept through death (game time) | Sword strings' arts' rests, for the owner's reader and Aura page |

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

## Aura: the swordsman's path (`aura/`)

The melee counterpart to the Cord (see [DESIGN.md](DESIGN.md#aura-the-swordsmans-path) for the rules and numbers). The
package follows the mod's layers: the rules are pure, the runtime is server side, the client only draws.

- **Pure parts** (no Minecraft types, unit-tested by `AuraRulesTest`): `AuraRules` (stages' default capacity and threshold,
  how experience fills and waits at a threshold, what fills aura and what spends it, the Edge's armour-piercing lift
  `pierced`, the slash's damage, backlash as `Spend`, experience by worth, the trials' numbers, every method's passive by
  stage, the colour by stage), `BreathingMethod` and `BreathingMethods` (the ten, and the registry add-ons add to),
  `MethodSources` (where manuals are found: loot tables with a chance and weights, or sources in code such as the trades)
  and `AuraStages` (the stage registry: all five registered). `AuraRules` also holds the top stages' numbers: the step's
  distance, cost, cooldown and untouchable ticks, `armourAbsorb`, `intimidated` and `senseRange`, Dominion's radius, length,
  rest and `dominionWeakened`, `spellbladePower`, `markChance` and `markFor`, and `stillnessTicks` for the tempest.
- **State** (`AuraAttachments`, see the table in [Player state](#3-player-state-player)): the private `Data` (method, stage,
  experience, aura held, practice), the owner-only `State` (the stance's beat, the guard, cooldowns, backlash, a trial's
  progress) and the public `Look` (colour, stage, lit, guarding, breathing), all anyone else's client learns.
- **`Aura`**: reading a player's aura on either side (`data`, `stage`, `method`, `capacity`, `aura`, `color`, `coated`,
  `holdsWeapon` against the `#wildercord:aura_weapons` item tag), `gain` and `spend` (the server's rate, Starlit's passive, the
  hooks, the capacity, `backlash` short of the price), and every tick for every player with a method: the breathing stance
  (a still sneak with an aura weapon settles after `SETTLE_TICKS`; letting sneak up for no more than `BOB_TICKS` and pressing
  it again is a breath, on the beat `AuraRules.onBeat` worth `BEAT_GAIN`; moving, standing up or a blow taken breaks it),
  `AuraGuard.tick`, the attribute modifiers (Edge reach on `entity_interaction_range`, Flow's `sweeping_damage_ratio`, Stone,
  Hourglass and Gale), and the public look (only set when it changes). It owns the Aura key's payload, `Aura.Key(trigger)`,
  and dispatches it to the technique registry (`press`).
- **`AuraCombat`**: blows. `mixin.PlayerAuraMixin` wraps the call that hurts the foe in `Player.attack`, `stabAttack` (a
  spear's thrust) and each of a sweep's blows (`blow`): a coated blow lands at `coat` (the coat bonus, the method's element
  through `cast.AuraElements`, held to the cap and the PvP scale against a player by `againstPlayer`, and the Edge's
  `pierced` share), and what it really took (killing blows too, which Fabric's after-damage event misses) is answered by
  `landed`: aura (a full swing only, faded by repetition per 16-block cube and kind of foe, a quarter from dummies),
  experience (`AuraExperience`, with spell mastery's moment), the method's passive, and the trials. The swing's strength is
  noted at the head of `attack`, since vanilla resets it before the blow lands. Flow's sweep: `isSweepAttack` answers yes
  for any coated aura weapon, and `doSweepAttack`'s box and range widen. `projected` is aura off the blade (the slash, a
  spark): the damage type `wildercord:aura`, the element, and against a player `cast.SpellDefence.hurt` (armour, Warding,
  the cap, the PvP scale, the spellguard).
- **`cast.AuraElements`**: the one bridge into the spell engine's package-private parts: an aura blow's element meets
  `Reactions.hit`, `Reactions.fire`/`storm` and `Affinities.multiplier` (creature affinity and the climate) through a `Cast`
  of the striker's own.
- **`AuraGuard`**: the technique (sneak and the key), the guard dropping when sneak goes, and what reaches a guarding player
  (`incoming`, from `mixin.LivingEntityAuraMixin`, a `@WrapMethod` round `LivingEntity.hurtServer`): halved as far as aura
  pays, or turned whole in the perfect moment (`AuraRules.perfect`, which is `spell.Parry.timed`): the attacker staggered,
  a projectile sent back a tick later (after vanilla bounces it) at its shooter, now the guard's. `parries(target)` is asked
  by `cast.Shields.stops`, so a spell meets a perfect guard as it would a Shield raised at the last moment: negated, the
  parry feat written, and answered with the Shield's counter-lance in the aura's colour.
- **`AuraSlash`**: the technique (a tap): price, cooldown, `AuraRules.slashDamage` scaled by the server, and the crescent's
  flight, a step a tick on the `Scheduler` (the Crescent shape's, without a `Cast`).
- **`AuraBreakthroughs`**: whether a breakthrough waits (`ready`), the stillness trial (counted while the stance holds at
  `PowerPlaces.isPlaceOfPower`), the stronger-foe trial (opened by a blow on a stronger foe, spoiled by any spell of the
  player's on it, met when the player's blow fells it in time), `complete(player, trial)` for any trial a stage allows,
  and `breakThrough` (the next stage, a full pool, a title, the sound, `AuraVfx.breakthrough`, the Grimoire's `aura:<stage>`).
- **`AuraMethods`** and **`BreathingManualItem`**: learning (the first method brings Glow; a switch asks twice and goes
  back to the start of the stage), the one manual item with its `wildercord:breathing_method` component, and the manuals in
  the creative tab. **`AuraLoot`**: a pool per `MethodSources` loot table as tables load (scaled by the rune loot
  multiplier), and the loot function `wildercord:random_breathing_method` the trades use
  (`data/wildercord/villager_trade/aura/`, added to the weaponsmith's and cleric's level 5 tags).
- **`AuraSense`**: the breath's sense, sent to the breather alone as `AuraSense.Sensed(entity ids, colour, ticks)`; from Form
  further (`senseRange`) and pulsed every 3 seconds in a fight (`combat`).
- **`AuraVfx`**: the server's shaped light for the stance, the beat, the guard, the perfect guard, the slash,
  a breakthrough, and the top stages: the step's start, streak and landing, the shell struck, Intent's ring and a creature
  faltering, Dominion's rising (a great `Sigils.ground` circle for its whole length and a band round it), its pulse, its end and
  its chain, a spell drawn into the blade, riding it and slipping off, the slash carrying a spell, and an aura mark.
- **`AuraPresence`**: the top stages' own attachments, beside `AuraAttachments` (left as it was, for the duelists and knights
  that read it): the public `Look` (aura armour's shell and when it was struck, a spell on the blade) and the owner's saved
  `Timers` (the step's and Dominion's rests, Dominion's end).
- **`AuraStep`** (Form, `DOUBLE_TAP`): `direction` (`getLastClientMoveIntent`, or ahead and level), `path` (the body's box swept
  in `STEP_PROBE` pieces, climbing `STEP_CLIMB`, stopped by collision, a change in `DungeonWards.warded`, the world border,
  lava or fire), the move (`STEP_TICKS` small teleports on the `Scheduler`, rotation kept), the untouchable moment
  (`untouchable`, asked by `mixin.LivingEntityAuraMixin`), and `Stepped(entity, from, to, yaw, colour)` to everyone tracking the
  player and the player.
- **`AuraArmour`** (Form): `up` (aura at or above `ARMOUR_MIN`), `share` (what it would take, `armourAbsorb`, nothing from a
  fall, drowning, starving, suffocation or the void) and `paid` (spends, flares the shell, `aura_armour`), called by the mixin
  only once the hit has landed.
- **`AuraIntent`** (Form): `active` (a coated blade at Form), `presses` (`intimidated` against `stageOf`: a player's stage or an
  aura look's), `pulse` (once a second from `Aura.tick`: Slowness I and a chance to falter for creatures; a vignette through
  `ScreenFx.tint` and a transient movement-speed modifier for players, let go by `release` once it stops reaching them).
- **`AuraDominion`** (Sovereign, `HOLD`): `Field`s by owner (centre, radius, start, until, colour), `raise` (cost, timers,
  `aura_dominion`, the circle, a camera shake, the Grimoire's `aura:dominion`), `tick` (Slowness on the foes inside every half
  second, the trickle, the pulse, the end), `weakened` (from the mixin: a striker inside a foe's field hits weaker,
  `dominionWeakened`, times the PvP scale for a player), `chain` (from `AuraCombat.landed`: once a tick to the nearest other foe
  inside, as `projected` aura) and a gain hook that doubles aura gained inside one's own field.
- **`Spellblade`** (Edge and up) and **`cast.BladeCasting`**: `SpellCaster.cast` asks `Spellblade.wants` (Edge, a blade, sneaking,
  not a secret, not overchannelled, and `BladeCasting.rides`: a group of the first segment that reaches out) as the spell is
  paid for, and its release then calls `draw` in place of `CastEngine.cast`: the paid `Cast`, the segment and the release are
  held (`Held`) until the next `AuraSlash.loose` takes them, or slip off unused (`tick`) and leave through the release.
  `AuraSlash.fly` then calls `BladeCasting.begin` (the segment's allowance, and its Self groups delivered on the caster),
  `cut` for each of the first `SPELLBLADE_TARGETS` foes cut (`CastEngine.onHit` per reaching group, at `spellbladePower`),
  `broke` where it ends having cut nothing, and `follow` (`CastEngine.runLink`, split out of `runSegment` for this).
- **`AuraMarks`**: `strike` (from `AuraCombat.landed` for a coated blow or the slash: `markFor` the method's element, at
  `markChance` times `mark_chance_multiplier`, a rest per striker and foe) and `leave` (through `Reactions.mark`, or fire, or
  poison, for 3 seconds).
- **`AuraBreakthroughs`** also runs the top stages' trials: `stillTrial` picks stillness or the tempest (`tempest`: thundering
  and the sky above), counted to `stillnessTicks(next)`; `foeTrial` picks the guardian's (a boss, `GUARDIAN_WINDOW`) or the
  stronger foe's; and `DUEL` is a trial id left for the duelists.

### Sword strings: `aura.SwordString`, `StringReader`, `SwordStrings`

Arts set off by a short run of ordinary swings (see [DESIGN.md](DESIGN.md#sword-strings) for the rules and numbers). The
client reads, the server judges:

- **Pure parts** (no Minecraft types, unit-tested by `SwordStringsTest`): `SwordString` (the grammar: `Token`s `swing`,
  `full`, `low`, `leap`, `run`, `counter`, `step`, each a bit in a swing's marks and a weight; `parse`/`text`, `endsIn` and
  `fits`, and `cutBy`, whether a shorter string is played on the way to a longer one), `StringReader<T extends Spelled>` (the
  detector: `stroke(now, marks, recover, candidates, usable)` answers `Landed`, `Completed`, `Refused` or `Fumbled`, `tick`
  answers `Lapsed`, `cue(GUARD|STEP, now)` marks the next swing a counter or a step cut; `best` and `compare` rank strings that
  fit the same swings) and `StringRules` (windows by the blade's `recover`, the cue windows, the late grace, the server's
  slack, the request allowance, and the placeholder arts' prices).
- **Client** (`client/`): `mixin.MinecraftStringsMixin` on `Minecraft.startAttack` (its head notes what the player was doing:
  attack strength, sneak, airborne, sprint, what's under the crosshair; the punch packet's send, or `piercingAttack`, says the
  swing really went) hands each swing to `SwordStringsClient`, which reads it (aura weapon, method, strings on, no screen, not
  charging; a creature under the crosshair, or nothing within `ENGAGED_TICKS` of a blow, a perfect guard or a step; never a
  block), checks an art's rest (`SwordStrings.COOLDOWNS`, plus its own prediction), price and condition, and sends
  `SwordStrings.Perform(art, marks)`. It plays `aura_string_tick` (up the scale), `aura_string_complete` and
  `aura_string_fumble` for the player alone, and drives `StringHud`: the row of marks after vanilla's crosshair element (or,
  with `MagicQuality.stringIndicator` HOTBAR, above the aura strip through `AuraHud.draw`), its phases (live with the window's
  line, done, fumble, refused, lapse) and a cue's waiting mark. `StringHud.glyph` and `string` draw the marks anywhere (the
  Aura page's Sword strings tab uses them).
- **Server** (`SwordStrings`): the payloads (`Perform` in; `Cue` and `Refused` out), the saved and synced `aura_arts`
  attachment (`Cooldowns`: rests by art id, game time, kept through death), what it saw (`swung`, from
  `mixin.SwordStringsSeenMixin` on `handlePunch` and `mixin.PiercingWeaponStringsMixin` on `PiercingWeapon.attack`; `cue`, from
  `AuraGuard.feedback` and `AuraStep.go`), `check` (closed, no weapon, silenced, not ready, condition, no aura, unseen), and `perform`
  (the performer, then the price through `Aura.spend` never past empty, the rest, the hooks, the Grimoire's
  `aura:sword_string`, and a method's own art's `aura:art_<id>`). `PlaceholderArts` registers the five common arts a method
  without its own plays (every built-in method has its own now: an add-on's method plays them), on `AuraVfx.artArc`, `artCounter`, `artLine` and `artRing`, each with its own `AuraFx` trail and
  impacts (below); a method's own arts (`aura.arts`, below that) step them aside.

### Feel and spectacle: `aura.AuraFx`, `AuraFxRules`, `client.AuraFxClient`

How aura looks and sounds, one toolkit for every art (the notes for later steps are in
[AURA_OVERHAUL.md](AURA_OVERHAUL.md#from-step-2-feel-and-spectacle)). The server says what happened, each client draws it as
its own camera sees it:

- **Pure numbers** (`AuraFxRules`, unit-tested by `AuraFxRulesTest`): the `Stroke`s (an arc's tilt, start, end, height, reach,
  sweep and life; `THRUST` a straight lance), which stroke a swing's string marks make (`stroke`), whether a swing is a Flow
  sweep (`sweeps`, the same test as `mixin.PlayerAuraMixin`'s), mirroring in a run, the trail's growth by stage and its
  first-person shape (`OWN_*`), the blow's `Weight` (hit-stop, nudge, flash) and `blow(swing, critical)`, the body aura's
  `intensity` and fight window, the `BannerKind`s and their slide.
- **Server** (`AuraFx`): five clientbound payloads (`Trail`, `Impact`, `Banner`, `BurstCue`, `Flare`) and the calls that send
  them (`trail`, `impact`, `banner`, `burst`, `bodyAuraFlare`, `sound`; `art(player)` chains them in the player's colour), the
  method sound families (`SoundFamily`, `family`, `registerFamily`), the ordinary swing's trail for onlookers (`swingBegins`
  from `AuraCombat.swing`, `swung` from the two sword string mixins, `swept` from `AuraCombat.sweep`), the fight window
  (`fighting`, into `AuraPresence.Look.fightUntil`) and `performed`, an `AuraApi.onString` hook giving every art its banner,
  flare and `ART` sound. `AuraCombat.blow` (coated blows), `AuraVfx.perfect`, `AuraSlash`, `AuraStep`, `AuraDominion` and
  `PlaceholderArts` call it. `mixin.PlayerAuraMixin` drops vanilla's sweep particle under a Flow sweep.
- **Client** (`client/`): `AuraFxClient` receives them and decides per viewer (own first person thin and low, a struck
  player sees no flash of their own; `MagicQuality`'s `trails`, `bodyAura`, `impact`, `banners`, `others`, `cameraShake`),
  draws the player's own ordinary swing trail at once (`attackBegins`/`swung` from `mixin.MinecraftStringsMixin`, predicting
  the sweep), the body's motes and the first-person `whisper` band; its `counts()` are for the game test. `fx.AuraTrail` (a
  ribbon walked in half-piece steps, carried with the entity), `fx.AuraBurst` (flash, star, ring, echo, sparks; a whisper near
  the owner's eyes), `fx.AuraWisp` and `fx.LightStrokes` (the painting they share) are particles; `fx.HitStop` with
  `mixin.EntityRenderDispatcherHitStopMixin` holds a struck or striking entity's pose (`Pose`) for a few milliseconds;
  `ScreenEffects.nudge` turns the camera a touch; `AuraBanners` draws the technique name (own screen's left edge, over others'
  heads); `render.AuraBodyLayer` (on every `AvatarRenderer`, data from `AvatarRendererGearMixin`) draws the stage's body aura
  from `textures/entity/aura/body.png` (`tools/aura_art.py`), in two inks without shaders and `GLOW_PIPELINE` under Iris.
- **Sounds**: `tools/feel/aura_methods.py` (part of the feel kit's `aura` part) makes `aura_<method>_swing`, `_impact` and
  `_art` for each built-in method and `aura_steel_*`.

### The methods' arts: `aura.ArtRules`, `aura.arts`

Each of the ten breathing methods' own five arts on the sword strings (the rules and numbers are DESIGN.md's
[The methods' arts](DESIGN.md#the-methods-arts); notes for the later steps in
[AURA_OVERHAUL.md](AURA_OVERHAUL.md#from-step-4-arts-ii)):

- **Pure parts** (`aura.ArtRules`, unit-tested by `ArtRulesTest`): the slots' prices and rests, every art's numbers, its
  record in `ARTS` (`Art`: id, method, slot, cost, cooldown, the balance model's primary, area, control, reach, mend, aura and
  toll, and its `Kind`s) and `power`, `SLOT_POWER` and `POWER_SPREAD` (the test holds each art to its slot's worth, each
  method's five within 10% of the others', each method leading in its own thing and keeping its own kinds); the PvP rules
  (`pvpLeft`, `hold`, `thrown`, `ignite`, `dragged`, `silence`), the mending bucket (`MEND_CAP`, `MEND_WINDOW`, `mendLevel`,
  `mendRoom`, `drink`), shapes (`inCone`, `alongAcross`), `falloff`, `chain`, `crusts`, `backdraft`, `spearTilt`, `rootedMend`,
  `novaAura`, `stored`, `sanguineWound`, `frenzy`, `moonToll`, and the Grimoire key `grimoireKey(id)` (`aura:art_<id>`).
- **The framework**: `api.AuraApi.ArtSlot` (the five strings and stages; `slot.art(id, cost, cooldown, performer)`, the Final
  Art gated by `FINAL_GATE`), `registerArts(method, arts)` (each art `forMethod(method)` and registered as a string; the common
  arts' `available` skips a method with arts of its own, so `stringsOf` never offers both), `hasArts`, `arts(method)` (its own
  or the common five, by stage, both sides: the Aura page and the Grimoire read it), `artMethod(artId)`, `gateFinalArts(gate)`,
  and `conflicts(string, except, method)` (arts of two different methods never conflict). `SwordStrings.perform` writes the
  Grimoire entry; `Feats.reward` gives it 60; `client.GrimoireToast` names it under its method's manual.
- **`aura.arts.MethodArts`**: `init` registers the ten methods' lists (`EmberArts`, `RimeArts`, `ThunderArts`, `GaleArts`,
  `StoneArts`, `VerdantArts`, `HollowArts`, `StarlitArts`, `HourglassArts`, `CrimsonArts`, each `arts()` built with
  `MethodArts.art(slot, id, performer)`, which takes the price and rest from
  `ArtRules.art(id)` and checks the slot), `blocked` (an art with nowhere to go), `whenLanded` (where a leap comes down, its
  fall forgotten), `SOUNDS` (each art's voice, checked by the tests), and the lifecycle (`forget`, `clear`).
- **The shared kit** (all server side):
  - `ArtKit`: who (`harmable` with `canHarmPlayer`, `helpable`, `boss`), where (`flat`, `right`, `bladeSide`, `hand`, `floor`),
    whom (`around`, `arc`, `line`, `beam`, `primary`, `attacker`, `nearest`), damage (`hits(player, fx)` returns `Hits`:
    `strike(foe, factor[, weight])` and `raw`, each through `AuraCombat.projected` with the `PVP_ART_CAP` ledger per player and
    the impact drawn by weight; `weapon`, `scale`), what it does to them (`lift`, `knock`, `shove`, `pull`, `draw`, `ignite`,
    `chill`, `slow`, `freeze`, `hold`, `holdLater`, `shock`, `root`/`rooted` (a hold that leaves the foe turning), `drag` (a
    steady pull, `PVP_DRAG` on a player), `steady` (the strike's knock taken back off a creature, so a field or an echo keeps
    it), `wound` (bleeds through `Hits.raw`, marked `BLEEDING`, harder on the move, each drop drunk by a `Drink`), all capped
    by `ArtRules` for players and bosses; holds rest 80 ticks a player), what it gives (`mend` through the one bucket a body,
    `drink`, `mendRoom`; `giveBack` aura a tick after the art, once its own price is paid, `givenBack` for
    the tests), and moving the swordsman (`path`, `dash(player, path, ticks, stretch)`, `beside`, `behind`, `fits`, `blink`,
    `launch`); `arcFrom` is `arc` from another spot (an echo).
  - `ArtLight`: shaped light for an art (`world(player)` for everyone, `spectacle(player)` for others and the owner's third
    person only, through `AuraFx.spectacle`/`Shown`), with a thin dark rim under it by day (`bare()` without): `ring`,
    `groundRing`, `ray`, `slash`, `tongues` (flames), `whirl`, `swirl`, `shards`, `orb`, `arc` (lightning), `sigil`, `ground`,
    `flash`, and `shade` (a soft disc of darkness: under an orb, a black sphere's edge).
  - `ArtFields`: what an art leaves on the ground for a while (`open(owner, kind, shape, ticks, period, pulse)` with `strip`,
    `disc`, `ring` shapes; `foes`/`allies` inside; `count`, `inside`; ticked on `END_SERVER_TICK`).
  - `ArtBlocks`: stone, ice, roots and trees that rise and sink as block displays (`spire`, with or without its dust; `slab`,
    `sheet`, `sprout` for a bush or a crown of leaves grown from nothing), tagged through `BlockFx.fresh` so a restart's
    leftovers are removed as their chunk loads. The arts draw with the mod's own motes, dust and light, never block or
    item particles (in the game test client those draw as tan cubes: see AURA_OVERHAUL.md's notes from step 4).
  - `ArtWards`: what an art leaves on its swordsman or a foe: `mirror`/`mirrored` and `eye`/`inEye`, whose `deflection`
    `AuraGuard.deflection` asks when no perfect guard turns a projectile (from `mixin.EntityAuraDeflectMixin`); `juggled` and
    `juggle`, the bonus `AuraCombat.blow` multiplies in; `harden`/`hardened` (Resistance I and full knockback resistance, a
    transient modifier taken off when it ends); `crust`/`crusts`; `silence`/`silenced` (Null Parry: `Statuses.silence` and an
    interrupt, a creeper's fuse put out; a player held to `SILENCE_PLAYER_TICKS`, refused arts by `SwordStrings.check`'s
    `SILENCED` and the Aura key but the guard by `Aura.press`); `star`/`starred`/`burstStar` (Starlit's stars, drawn every half
    second); `frenzy`/`frenzyStacks` (a transient attack speed modifier, fed by the swordsman's own melee blows through
    `AFTER_DAMAGE`); `stop`/`stopped`/`release` (Thousand Moments' stored blows); `leapedFrom` (where an Hourglass swordsman
    last left the ground, for Rewind Leap).
- **Elsewhere**: `AuraGuard.caught(player)` (the blow a perfect guard just caught, for Backdraft, Rooted Parry and Sanguine
  Parry), `Aura.giveBack` (aura back with no multipliers or hooks), `AuraStep.path` and
  `afterimages` (the rushes), `cast.WorldMagic.frostWater` (Skate over water, the terrain spells' checks), `cast.Sigils.send`
  (an `except` for spectacle), `config` `aura.art_damage` and `art_terrain`, `client.AuraFxClient` (draws `Shown` only out of
  first person), `client.AuraScreen.browse` and the methods' swatches (the Sword strings tab), `client.CordScreen.addArts`
  (the Grimoire's sword arts).
- **Sounds**: `tools/feel/aura_arts.py` (part of the feel kit's `aura` part) makes `aura_art_<id>` for each art, and
  `aura_art_sunfall_impact`, `aura_art_winters_hush_shatter`, `aura_art_event_horizon_crush`, `aura_art_comet_dash_burst` and
  `aura_art_thousand_moments_release`. Lang: `tools/aura_art.py` `LANG` (`aura.wildercord.art.<id>`
  and `.desc`).
- **Tests**: `ArtRulesTest` (prices, slots, the balance model, identities and kinds, PvP caps, silence and drag, the mending
  bucket, drinks, Crimson Moon's floor, shapes, the lang and sounds for every art), `SwordStringsTest` (the common arts and a
  method's own side by side), game test `WildercordArtsTest` (all fifty played with the real keys, checked on the server and
  filmed in first and third person, the Aura page's tab browsed; `WILDERCORD_ARTS=a,b` plays only those). The common arts are
  played in the game tests by a method the tests register (`gametest.TestMethods.plain()`, "Plain Breath", no arts of its own).

### Momentum and openings: `aura.Momentum`, `aura.Stance`, `aura.arts.Finishers`

Momentum, stance, openings and finishers (the rules and numbers are DESIGN.md's
[Momentum and openings](DESIGN.md#momentum-and-openings); notes for the later steps in
[AURA_OVERHAUL.md](AURA_OVERHAUL.md#from-step-5-momentum-and-openings)):

- **Pure parts** (unit-tested by `MomentumRulesTest` and `StanceRulesTest`): `aura.MomentumRules` (the meter, `tier`, `peak`,
  `priceFactor`/`strength`/`stanceFactor` by tier, what builds it and `artFoe`, `reach`, `loss`, the ebb's `current`, `ceiling`
  and `budget`, the methods' `Temper`s and the foe-state bits they feed on) and `aura.StanceRules` (`Kind`, `pool`, `Source` and
  `wear`, `artWeight`, `guardBreak`, the PvP caps, `worn` and `left`, `openTicks`/`steadyTicks`, `finisher`, `finisherAura`).
- **`aura.Momentum`**: the `MOMENTUM` attachment (`State`: value, held until, ebb a tick, an awakening's floor and its end; synced
  to its owner only, not saved, gone with a death or a change of world), read on both sides by `value`/`tier`/`peak`/`price`
  (`SwordStrings.price` and the client's `SwordStringsClient.why` ask it), changed on the server by `add` (through
  `AuraApi.onMomentum` hooks and `momentum_gain`, held to a ceiling), `lose`, `engaged` (a fight holds it; written only when the
  hold moves on half a second), `hold` (`AuraApi.holdMomentum`) and `reset`. Fed by `hit` (`AuraCombat.blow`: `worthy`,
  `helpless`, `states`, the per-foe budget), `artLanded` (`ArtKit.Hits`), `guarded` (`AuraGuard`), `stepThrough`
  (`AuraStep.untouchable`), `broke`/`finished` (`Stance`), `struck` (`AFTER_DAMAGE`) and `performed` (the Final Art's release, an
  `onString` hook). `FINAL_GATE` is given to `AuraApi.gateFinalArts`. `refreshLook` keeps `AuraPresence.Look.momentum` (every
  player's body aura burns brighter with it: `AuraFxRules.momentumGlow`) and `tick` (from `Aura.tick`) lets old budgets go.
- **`aura.Stance`**: the `STANCE` attachment on any living entity (`State`: worn, when, pool, kind, opened until, steady until,
  breaks; synced to everyone near, not saved), `kind`, `eligible` (a swordsman, `ArtKit.harmable`, a real foe or a practice
  target, `pvp_stance` for players), `wear` (through `AuraApi.onStance` hooks; opens at the pool), `blow` (`AuraCombat.blow`),
  `art` (`ArtKit.Hits`, with its PvP ledger), `slash` (`AuraCombat.projected` outside an art), `guardBreak` (a perfect guard),
  `guarded` (a held guard's catch or a shield's block, players only), `open`/`stagger`, `finisher` and `finished` (asked by
  `AuraCombat.blow` before and after the blow lands: the extra rides the blow itself), and the upkeep (opened marks; a stance
  whole again let go). `AuraApi.Finisher`/`registerFinisher`/`finisher(method)` hold each method's look; `onFinisher` and
  `onStanceBroken` hear of them.
- **`aura.arts.Finishers`**: the ten methods' looks and the common one (`init`, from `MethodArts.init`), each `frame` (trail, grand
  impact, flare, the shared stinger and its voice, a seal on the ground) and its own light; `IDS`, `SOUNDS`.
- **Where it touches the rest**: `AuraCombat.blow` (the finisher's extra, the blow's wear, the clean hit) and `artStrike` (an art's
  projected strike, so `projected` wears stance only for the slash and sparks); `ArtKit.Hits` (momentum's strength, the art's
  wear and momentum, the art taken from `SwordStrings.performing`); `SwordStrings.price`, `check`, `perform` and `refusal(player,
  ...)`; `PlaceholderArts` (now through `Hits`); `AuraGuard` (perfect guards, the held guard's wear); `AuraStep.untouchable`;
  `Aura.press` (no guard while opened); `AuraApi.ArtCondition.hintKey(player)`; `config.WildercordConfig.AuraMomentum` and
  `Config.Sync.combat` (`Config.momentum`, `Config.stance` on the client).
- **Client**: `client.StanceHud` (the bars over foes, the opened seal, the crosshair cue, your own stance off the strip;
  `drawn()`/`ownShown()` for the tests), `AuraHud.momentum` (the line under the aura bar) and `StanceHud.ownOnStrip` (your own
  stance along its top edge), `AuraScreen` (the two technique rows, the finisher row and the momentum line on the Sword strings
  tab, prices at your momentum), `AuraFxClient.bodyIntensity` (the momentum glow).
- **Assets**: `tools/aura_art.py` `LANG` (the finishers' names and descriptions, the lines), `tools/feel/aura.py`
  (`aura_momentum_rise`, `aura_momentum_peak`, `aura_stance_break`, `aura_finisher`) and `tools/feel/aura_finishers.py`
  (`aura_finisher_<method>`).
- **Tests**: `MomentumRulesTest`, `StanceRulesTest` (and the lang and voices), `WildercordConfigTest` (the keys, old files, the
  sync bits), game test `WildercordMomentumTest` (momentum built, lost, ebbing, its tiers, the gate, the farming guards; stance worn,
  opened, finished; all eleven finishers filmed; a boss; a duel with a second player; the HUD and the page).

### Awakening: `aura.Awakening`, `AwakeningRules`, `AwakeningFx`, `aura.arts.Awakenings`

Awakening, the spent state and the Sovereign's awakened Dominion (the rules and numbers are DESIGN.md's
[Awakening](DESIGN.md#awakening); notes for the later steps in [AURA_OVERHAUL.md](AURA_OVERHAUL.md#from-step-6-awakening)):

- **Pure part** (unit-tested by `AwakeningRulesTest`): `aura.AwakeningRules`: `full`, the input's `HOLD_TICKS`/`CHARGE_SHOWS`/`charge`,
  `ticks(stage, scale)`, `extend`, `SPENT_TICKS`/`COOLDOWN_TICKS`, `price`, `damage`, `Phase`, `awakened`/`spent`/`ended` (`Ending`),
  `Refusal` and `refusal` (in order; `key()` its line), the look's `BURST_AT`/`RISE`/`GUTTER`/`GLOW`/`SPENT_GLOW` and `form`, and the
  awakened Dominion's `Sovereign` numbers and the ten `Flavour`s (`of(methodId)`, `nameKey()`).
- **`aura.Awakening`**: the `AWAKENING` attachment (`State`: phase, since, until, spent until, ready at, the art price share, ticks fed;
  saved, synced to everyone near, kept through death), read on both sides by `awakened`/`spent`/`left`/`age`/`refusal`/`ready`/
  `priceShare`/`damage`; on the server `awaken` (the `awaken` technique on `TAP_HOLD`, from Edge), `begin`, `fed` (an `onFinisher`
  hook), `endNow`, `tick` (from `Aura.tick`: the end, the spent state's slow put back, the recovery) and `end` (the pool burned,
  momentum emptied, the slow, the hooks); the speed modifiers (`aura_awakened_speed`, `aura_awakened_attack`); `init` registers the
  Final Art opener, the finisher hook, the death rule (`AFTER_RESPAWN`) and momentum held again after a change of world or a return.
- **`aura.AwakeningFx`**: the moment on the server (`transform`: stinger, voice, banner, flare, the gathering rings, then `burst`),
  `burning`, `fed`, `spent`, `recovered`; `SOUNDS`, `voice(methodId)`.
- **`aura.arts.Awakenings`**: `flourish` (each method's burst, in the arts' own looks) and `Ground` (the awakened Dominion: `ground`
  draws the method's own ground, `raised` what it does at once, `tick` its beat, `struck` Crimson's drink; one `ArtKit.Hits` for the
  whole of it).
- **Where it touches the rest**: `Aura` (the technique, `Awakening.init` after momentum, the tick, `gain` and `giveBack` refused while
  spent), `SwordStrings.price` (the share), `AuraCombat.coat` (the damage), `AuraDominion` (`Field.ground`, `sovereign`, `radius`, the
  wider and longer field, the weaker foes, the two-foe chain, the faster flow, the ground in place of the circle via
  `AuraVfx.dominionRise(..., circle)`), `AuraApi` (`Trigger.TAP_HOLD`, `AwakeningHook`/`onAwakening`, `awakened`, `spent`,
  `awakeningLeft`, `awakeningRefusal`, `awaken`, `endAwakening`), `config.WildercordConfig.AuraAwakening` and `Config.Sync.combat`
  (`Config.awakening`, `Config.awakeningMomentum` on the client).
- **Client**: `WildercordKeys` (the tap-and-hold: `auraSecond`, `awakeningCharge`, `tapHoldSentAt`; a lone tap waits while an
  awakening is ready), `client.AwakeningHud` (the charge ring by the crosshair, the first-person edge glow, the strip's `mark`;
  `counts()` for the tests), `AuraHud` (the mark, the rim and racing light while awakened, the lines above, the spent ash),
  `AuraFxClient` (`awakenedForm`, `bodyIntensity`, `awakeningMotes`: gathering, embers, ash), `render.AuraBodyLayer` (`Body.awaken`
  and `spent`: eyes from Edge, the borrowed mantle and corona, `updraft`, the taller corona, the ash), `AuraScreen` (the trigger's
  line, the awakening's state at the end of the aura line).
- **Assets**: `tools/aura_art.py` `LANG` (the technique, the lines, the refusals, the ten Dominions' names and descriptions),
  `tools/feel/aura_awakening.py` (`aura_awaken`, `aura_awaken_<method>` and `_steel`, `aura_awaken_fed`, `aura_spent`, `aura_recovered`).
- **Tests**: `AwakeningRulesTest` (and the lang and voices), `WildercordConfigTest` (the keys, ranges, old files, the sync bits), game
  test `WildercordAwakeningTest` (the key and its refusals, a double tap, a press let go early, a lone hold; what it gives; the moment
  filmed; each stage's form and each method's flourish; finishers feeding it; the Final Art; the end, spent, recovered, resting; a
  death; a duel; all eleven awakened Dominions; the HUD and the page).

### Ways: `aura.Ways`, `WayRules`, `Crossroads`, `WayEffects`, `WayBanner`, `CrossroadsIncense`

The four Ways, chosen at the crossroads at the Edge breakthrough (the rules and numbers are DESIGN.md's [Ways](DESIGN.md#ways); notes for
the later steps in [AURA_OVERHAUL.md](AURA_OVERHAUL.md#from-step-7-ways)):

- **Pure part** (unit-tested by `WayRulesTest`): `aura.WayRules`: the Ways' ids, colours and `node(way, stage)` ids, `CHANGES` (the
  technique each node changes, for the page), `NodeState` and `state(...)`, the settling (`SETTLE_XP`, `FORM_WAKES`, `awake`, `toWake`,
  `wakeProgress`, `settle`, `settles`), the crossroads (`OPEN_DELAY`, `CROSSROADS_TICKS`, `CALL_TICKS`, `CALL_REST`, `LEAVE`,
  `LEAN_TICKS`, `RADII`, `SPREAD`, `angle`, `STANDARD_HEIGHT`, `REACH`, `STRIKE_RADIUS`, `strike` (the look ray against a standard's
  axis)), every node's numbers and their formulas (`finisher`, `cascades`, `bulwarkStance`, `armourShare`, `behind`, `fromBehind`,
  `harmLeft`, `steadied`, `lossScale`, `share`), the voices (`voice`, `SOUNDS`) and the balance model (`Worth`, `WORTH`, `worth`).
- **`aura.Ways`**: the `WAY` attachment (`State`: the Way, when chosen, experience owed and what was owed, the Way left before, how many
  changes; saved, synced to everyone near, kept through death), read on both sides by `on`/`way`/`wayless`/`has(player, nodeId)`/
  `nodeState`; on the server `choose` (a first choice owes nothing), `unbind`, `set` (operators, tests), `earned` (settling, from
  `AuraExperience.earn`, with a line as each node wakes); `builtIn()` and `init` (registers the four through `AuraApi.registerWay`, the
  rest of the Ways' parts, and the line on joining for a swordsman past Edge with none).
- **`aura.Crossroads`**: the `CROSSROADS` attachment (`State`: where it rose, the `Standard`s (a Way and a foot each), when it rose and
  fades, the lean; told to its swordsman alone, not saved), `open(player, Reason)` (placing the standards: `place`, `ground`, `seen`),
  `breathing` (the stance calling it, from `Aura.stance`), `brokeThrough` (from `AuraBreakthroughs.breakThrough` at Edge), `swung`
  (from `mixin.SwordStringsSeenMixin`: a strike, through `aimed`, then `lean` or `choose`), `close`, the upkeep (`tick`: fading, drawing
  every 4 ticks through `look`, each Way's shape in `ArtLight`), and the death, world-change and leaving rules.
- **`aura.WayEffects`**: the Blade's, the Bulwark's and the Shadowstep's nodes: hooks registered in `init` (`onMomentum` for Keen Edge,
  `onStance` for Wide Guard, Unbroken and Slip, `onFinisher` for Cascade's extra and its cascade, and Thousand Shadows' ready step), and
  what the techniques ask: `pierces`, `slashPrice`/`slashRest` (`AuraSlash`), `feed` (`Awakening.fed`), `coversAll`/`perfectWindow`/
  `held`/`turnsShot`/`slipSpot`/`slip` (`AuraGuard`), `armourShare`/`armourCost` (`AuraArmour`), `lossScale` (`Momentum.struck`),
  `spentSlows` (`Awakening`), `challenge` (`AuraIntent.pulse`), `bastion` (`AuraDominion.tick`), `fromBehind`/`unseen`, `linger`/
  `stepPrice`/`stepRest`/`stepped` (`AuraStep`); `counted(what)` for the tests.
- **`aura.WayBanner`**: the Banner's: `ally` (the rule chorus casting keeps), `swordsmen`/`company`, `momentumBuilt` (from `Momentum.add`),
  `auraGained` (from `Aura.gain`), the steadiness store (`steady`, `steadiness`), `cry` (an `onFinisher` hook), `presence` (`AuraIntent`),
  `bannerNear`, `shelter`/`sheltering` (`AuraDominion`), the awakening's rally (an `onAwakening` hook), the allies' faster flow in a
  Banner's Dominion (an `onGain` hook), and `harm` (in `mixin.LivingEntityAuraMixin`, after Dominion's weakening: all steadying together,
  an unbroken Bulwark's too, capped and PvP-scaled).
- **`aura.CrossroadsIncense`**: the item (`INCENSE`, a two-second `Consumable`), `refusal` (Ways off, below Edge, no Way, not at a place of
  power), the unbinding and the crossroads after it, the smoke as it burns.
- **Where it touches the rest**: `Aura` (`Ways.init` after awakening, the stance's call, `gain`'s share), `AuraBreakthroughs` (Edge raises
  the crossroads), `AuraExperience.earn` (settling), `AuraSlash` and `Crescents` (`Flight.pierce`, `scale`, a pierced guard, a clash won),
  `AuraGuard` (`faces`, the perfect window, `held`, shots turned, the slip and a stagger that doesn't throw back), `AuraRules`
  (`perfect(raised, now, window)`, `armourAbsorb(..., costScale)`), `AuraArmour`, `AuraIntent`, `AuraDominion` (`fields()`, the bastion's
  slow, the Ways' ticks), `AuraStep` (`Stepped.linger`), `Awakening` (`fed`, the spent slow), `Momentum` (`add`, `struck`), `StanceRules`
  (`playerFinisherCap`), `AuraCommand` (`way <id|none>`, `way crossroads`), `AuraApi` (`Way`, `WayNode`, `WayHook`, `registerWay`, `ways`,
  `way`, `wayOf`, `hasWayNode`, `wayNodeState`, `chooseWay`, `unbindWay`, `openCrossroads`, `onWay`), `config.WildercordConfig.AuraWays`
  and `Config.Sync.combat` bit 8 (`Config.ways`).
- **Client**: `client.WayHud` (a standard's name, what a strike does and its nodes above the hotbar, from the `CROSSROADS` attachment),
  `AuraScreen` (the Way tab: `way`, `node`, `details`; `showWay`, `showingWay`, `wayTabPoint` for the tests), `AuraClient` (`Afterimage.life`,
  the lingering afterimage), `render.AuraShellLayer` (a lingering afterimage holds its light).
- **Assets**: `tools/way_art.py` (the emblems as GUI sprites `aura/way_<id>` and `way_unknown`, the incense's icon, model and recipe, and
  `LANG`), `tools/feel/aura_ways.py` (`aura_crossroads`, `aura_way_*`, added to the aura part).
- **Tests**: `WayRulesTest` (the Ways, nodes, states, settling, the crossroads' geometry and strike, every node's numbers and caps, the
  balance, the lang, art and voices), `WildercordConfigTest` (the keys, ranges, old files, the sync bit), game test `WildercordWaysTest`
  (the crossroads at the breakthrough, its strikes and the moment; the stance calling it; each Way's moment; every node on husks, a rival
  and an ally; the incense; a death; the Aura page).

### Hooks for the next wave: `api.AuraApi`

The top stages, the spellblade and aura marks use these too; duelists, aura-forged gear, aura knights and PvP tuning slot in
through them, all on the server thread unless noted, registered at start-up:

| Call | What it's for |
|---|---|
| `registerStage(new AuraStages.Stage(4, "form", 110, 1800))` | Opens or changes a stage (all five are registered from `AuraRules`). An unregistered stage's threshold still caps experience, so nothing earned is lost; `AuraStages.highest()` and `canBreakThrough` follow the registry. Its name and description are `aura.wildercord.stage.<id>` and `.desc`, its Grimoire entry `aura:<id>` (toast `toast.wildercord.aura.<id>`). |
| `allowTrial(stage, trial)` / `trials(stage)` | Which trials make the breakthrough into a stage. Built in: `AuraBreakthroughs.STILLNESS` and `STRONGER_FOE` (allowed for Flow and Edge), `TEMPEST` and `GUARDIAN` (allowed for Form and Sovereign). `DUEL` is an id left for the duelists, allowed nowhere until they allow it; any other id is a trial of your own. The Aura page lists them (`screen.wildercord.aura.trial.<id>`; `duel`'s line is already there). |
| `completeTrial(player, trial)` | Your trial was met: the waiting breakthrough is made, if the trial is allowed for it. |
| `registerTechnique(new Technique(id, stage, trigger, cost, performer))` | A technique of the Aura key. `Trigger.TAP` (Aura Slash), `SNEAK_TAP` (Aura Guard), `DOUBLE_TAP` (Aura Step), `HOLD` (Dominion) and `TAP_HOLD` (Awakening: a tap, then a held press): the client sends every trigger (a lone tap waits out the double tap's moment when the player has a double-tap technique, or an awakening is ready), and the server runs the highest-stage technique for it that the player has reached (`techniqueFor`). The performer pays through `spend`. Its name and description are `aura.wildercord.technique.<id>` and `.desc`; the Aura page lists it with its key and cost. Safe to read on both sides. |
| `gain(player, amount, source)` / `onGain(hook)` | Fill aura with every rule applying (the server's rate, Starlit, the capacity), or hear of (and change) each gain: `hook.modify(player, amount, source)` returns the amount. Sources: `hit`, `stance`, `beat`, or yours. |
| `spend(player, cost, reason)` / `onSpend(hook)` / `backlash(player)` | Spend aura (short of the price: everything there, and backlash, never damage), hear of each spend (`paid`, `reason`, `backlash`), or bring backlash outright. |
| `registerMethod(method)` | Another breathing method (a namespaced id; its manual, loot and trades work at once; give it lang keys `aura.wildercord.method.<namespace>.<path>`, `.lore`, `.flavour`). Passives of your own: `Flavour.NONE` and your own code in `onGain` or the blow. |
| `addMethodSource(new MethodSources.Source(id, lootTable, chance, weights))` | Another place manuals turn up: a loot table (read as tables load), or a source in code with `lootTable` "" that draws with `Source.draw(random)`. |
| `grantMethod(player, methodId, sourceId)` | Teach a method outright (a duelist's lesson): as reading its manual, switching cost included, without asking twice. `manual(methodId)` makes the item. |
| `stage`, `method`, `aura`, `capacity`, `color` | Reading a player's aura (both sides; a client knows only its own player's, and everyone's `Aura.look`). |
| `registerArts(methodId, List.of(ArtSlot.FIRST.art(id, cost, cooldown, performer), ...))` / `hasArts`, `arts(methodId)`, `artMethod(artId)`, `gateFinalArts(condition)` | A breathing method's own five arts, one an `ArtSlot` (the five strings and stages), replacing any it had; the common arts step aside for its swordsmen. `arts(methodId)` is its own or the common five, by stage, both sides. A method's own art goes into the Grimoire the first time it's played (`ArtRules.grimoireKey`). `gateFinalArts` changes what every Final Art waits on (`FINAL_GATE`; a full pool until then). |
| `registerString(new StringArt(id, string, stage, cost, cooldownTicks, available, condition, performer))` / `StringArt.of(id, "swing swing low", stage, cost, cooldown, performer)` | An art set off by a sword string, on both sides (the client reads strings against the registry). `available` (both sides) says whether the player has it at all besides the stage (`forMethod(id)` for a method's own); `condition` (an `ArtCondition`, both sides, with a `hintKey` line) what else it waits on (`PlaceholderArts.FULL_POOL` for the Final Art); `performer(player, StringContext)` acts on the server and returns whether it went off: the mod then spends `cost` (never past empty), rests it `cooldownTicks`, and tells the hooks. Its name and description are `aura.wildercord.art.<id>` and `.desc`. Same id replaces; `unregisterString(id)` takes one out (the placeholders, `PlaceholderArts.IDS`, once a method's own arrive). Registering logs any `conflicts`. |
| `strings()`, `string(id)`, `stringsOf(player)`, `allStringsOf(player)`, `artOf(player, id)`, `artReadyAt(player, id)` | The registry by stage; what a player can play now (stage reached, available: the reader's candidates); everything open to them whatever their stage (the Aura page); one by id including their own; when an art is ready again. Both sides. |
| `conflicts(string, exceptId)` | The registered arts that would get in a string's way: the same swings, or a shorter string played on the way to it, or a longer one it cuts short (`SwordString.cutBy`). For a writing screen to warn before a player settles on a string. |
| `addStringSource(player -> arts)` | Arts a player has of their own (techniques they wrote): asked on both sides each time a string is read or checked, so it reads only synced data and is quick. |
| `registerSounds(methodId, new AuraFx.SoundFamily(swing, impact, art))` | A method's swing, impact and art sounds, by feel kit name (`AuraFx.SoundFamily.named("ember")` is `aura_ember_swing`, `_impact` and `_art`, so another method's family can be borrowed); a method without one plays the neutral `aura_steel_*`. |
| `onAwakening(new AwakeningHook() { awakened(player, ticks); ended(player); })`, `awakened`, `spent`, `awakeningLeft`, `awakeningRefusal`, `awaken`, `endAwakening` | Awakening: hear of one begun and one ended (spent after), read it on both sides, awaken a swordsman through every check, or end one now. |
| `onString((player, art, context) -> ...)` | Hears of every art performed, after it's paid for (momentum, a bonded blade's resonance, a trial). `StringContext` carries the swings' marks (`released(token)`), the creature the last swing struck (the server's own `lastHurtMob`, or null) and the time. |
| `registerWay(new Way(id, color, List.of(new WayNode(nodeId, stage), ...)))`, `ways()`, `way(id)`, `wayOf(player)`, `hasWayNode(player, nodeId)`, `wayNodeState`, `chooseWay`, `unbindWay`, `openCrossroads`, `onWay(new WayHook() { chosen(player, way, first); unbound(player, way); })` | Ways: add one (the crossroads raises a standard for it, up to six; the Aura page draws a column; names `aura.wildercord.way.<id>`, `.creed`, `.short`, nodes `aura.wildercord.way_node.<id>`, `.passive`, `.change`), read a player's on both sides, ask whether a node is in force (walked, reached, settled), set or take one outright for a rite of your own, raise the crossroads, hear of choices and unbindings. A node's effect is its Way's own code, through the hooks here, asking `hasWayNode`. |

Other seams: `AuraCombat.blow` and `landed` (where aura marks and Dominion's chain join a blow), `AuraCombat.projected`
(aura damage at anything, with the spell defences), `AuraRules.capBonus`, `AuraCombat.againstPlayer` and
`AuraRules.dominionWeakened` (PvP tuning), the order of `mixin.LivingEntityAuraMixin` (the step's moment, Dominion's
weakening, the guard, then aura armour), `AuraIntent.stageOf` (anything carrying an `AuraAttachments.Look` counts as an aura
user of that stage, for Intent: an aura knight is pressed on only by a higher stage), `AuraDominion.inside`/`end`, the
`Aura.Key` payload's triggers, and `AuraAttachments.Look` (aura knights and duelists can carry one: `AuraBlade` draws any
player's blade from it; a mob's would need its render state to carry an `AuraBlade.Glow`, whose five-part constructor is
kept beside the one with a spell's colour).
The world of aura (below) registers the trial `duel` for Form and Sovereign through `allowTrial`, completes it with
`completeTrial`, teaches through `grantMethod` and `manual`, and adds the forged gear's and the sash's effects with `onGain`.

Other seams wave 2 will want: `AuraCombat.blow` and `landed` (where a spellblade's channelled spell or an aura mark would
join a blow), `AuraCombat.projected` (aura damage at anything, with the spell defences), `AuraRules.capBonus` and
`AuraCombat.againstPlayer` (PvP tuning), `AuraGuard.incoming` (aura armour would add its share there), the `Aura.Key`
payload's triggers, and `AuraAttachments.Look` (aura knights and duelists can carry one: `AuraBlade` draws any player's
blade from it; a mob's would need its render state to carry an `AuraBlade.Glow`).

### Crescents in flight: `aura.Crescents`

Every Aura Slash flies through `Crescents`, whoever loosed it: `AuraSlash.fly` (a player's), `AuraFighter.release` (a
duelist's or a knight's) and a perfect guard sending one back (`Crescents.reflect`). `launch(caster, origin, aim, colour,
damage, bonus, speed, range, width, targets, weak, mayCut, cut)` adds a `Flight`; `END_SERVER_TICK` steps them all (the
first step a tick after the launch, as the old `Scheduler` steps were): **every crescent moves** (stopped by a solid block;
`AuraVfx.slashStep` draws it), then **any two of different casters that met** on the way (`AuraWorldRules.meets`: the
nearest their fronts came over the tick) **clash** (`AuraVfx.clash`, a shove, the Grimoire's `aura:clash`), then the rest
**cut** what they reached (the old geometry exactly), each creature once. `cutting()` is the flight cutting a creature
right now, so a perfect guard can send it back; a held guard facing it (`AuraGuard.guarding` and `facing` for a player, the
`Crescents.Guarding` interface for a mob) takes its cut and stops it. The bonus is a multiplier counted with the element
(`AuraCombat.projected(player, target, damage, bonus, answer)`, held to the cap against a player), which is where Skyrend
Glaive's slash goes.

## The world of aura (`aura/world/`)

Wandering duelists, fallen knights, aura-forged gear and the Breath Sash (the rules and numbers are DESIGN.md's [The
world of aura](DESIGN.md#the-world-of-aura)):

- **`AuraWorldRules`** (pure, `AuraWorldRulesTest`): the duel's stage, a duelist's numbers by stage, yielding, mob guards'
  timing, knights' numbers by rank, spawn chances, the forgings and the sash, `meets` (the clash) and `slashAgainstPlayer`.
- **`AuraFighter`**: what a duelist and a knight share, a `PathfinderMob` with a method and a stage (synced bytes) and a byte
  of pose flags (`WINDUP`, `GUARD`, `STAGGER`, `BOW`, `YIELD`, `DRAWN`, `SIT`, `DASH`) eased on the client; coated blows (a
  transient attack modifier), a guard (`raiseGuard`, `guarded`: halved, perfect, broken by an axe, and `catches` for
  crescents), the slash (`windUp`: the tell, the aim fixed `SLASH_LOCK` ticks before with a line of light on the ground,
  then `release` into `Crescents`) and `projected` (aura through `SpellDefence`, as monster magic is).
- **`Duelist`** (`MobCategory.CREATURE`): wanders, sits by its fire, and `mobInteract` hands a use to `DuelistDuels`. Its
  `hurtServer` takes only its challenger's harm in a fight (`DuelistDuels.counts`), else turns blows aside and ignores the
  rest; a blow that would leave it at `YIELD_SHARE` yields instead. In a duel its AI is a `MeleeAttackGoal` gated by
  `fighting()` and a small state machine in `customServerAiStep` (the guard after a blow, the slash at range, the dash
  once a `step` technique is registered). It saves its stay, rest and camp; a duel never survives a reload.
- **`DuelistDuels`**: offers and duels, on `duel.DuelRules.Duel` (countdown, fight, knockout, forfeit, draw); the challenger
  knocked out in `ALLOW_DEATH`, the health the duelist took counted in `ALLOW_DAMAGE`/`AFTER_DAMAGE` and given back with
  `DuelRules.restored`, the stagger's effects taken off; the lesson (`grantMethod` or the manual, `AuraExperience.grant`,
  `Grimoire.unlock("aura:duelist")`, `completeTrial(TRIAL)` unless magic touched it). `Duels` refuses a player duel to anyone
  in one of these.
- **`DuelistSpawner`**: the wandering spawner (a village bell through the POI manager, a dirt path, a camp with a
  `TemporaryBlocks` campfire), its caps and `loaded`.
- **`FallenKnight`** (`MobCategory.MONSTER`, `notInPeaceful`, `Enemy`): rank, the slash and guard by `AuraFighter`, open after
  its slash, a quarter more harm while its guard is broken. **`KnightSpawner`**: `haunt` (the structure tag
  `#wildercord:knight_haunts`, or a spawner on cobblestone near) and `spawnIn`/`raise` (rank, a method by the place's
  `MethodSources` weights). **`KnightLoot`**: the loot function `wildercord:knight_method`.
- **`ManualPageItem`** and **`ManualPagesRecipe`** (`wildercord:manual_pages`): pages carry the manual's own
  `wildercord:breathing_method` component; the recipe is a shapeless one that matches only pages of one method.
- **`ForgedGear`**: the `wildercord:aura_forged` component and what each forging does (`slashBonus`, `slashReach`,
  `slashTargets`, `guardCost`, and an `onGain` hook for Lumenedge), and the Breath Sash (`sash`, `capacity`, `settleTicks`):
  `Aura.capacity`, `Aura`'s stance, `AuraSlash.loose` and `AuraGuard.raise`/`incoming` call these. The sash is casting gear
  (`GearDef.BREATH_SASH`, a new `GearKind.SASH` the tome slot takes). `Config.Sync` carries `forged_gear` and
  `sash_capacity` so a client's aura bar agrees.
- **`AuraWorld`**: the registrations (entity types, the page and the shard, the spawn eggs, the recipe serializer, the
  loot function, the field guide's entries, the creative tabs) and `init`.
- **Client** (`client/auraworld/`): `DuelistModel` and `FallenKnightModel` (humanoid models with a hood, cloak, mantle and
  scabbard, or a helm, crest, pauldrons and rags; their layouts in their javadoc), `AuraFighterRenderer` (a
  `HumanoidMobRenderer`: its skin, a glow layer through `RenderTypes.eyes` tinted with the aura's colour, lowered while it
  sits or kneels, and an `AuraBlade.Glow` in the render state so the existing item-layer mixins draw its blade's aura) and
  `AuraWorldClient` (layers, renderers, and the forged weapons' tooltip lines).

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
| `TraceSpell(chargeStart, accuracy)` | `Charging.trace` (how well the charge's glyph was traced, sent just before the release; the server decides how much counts) |
| `RenameSpell(spell, name)` | `SpellCaster.rename` |
| `InscribeScroll(spell)` | `SpellScrollItem.inscribe` |
| `LoadoutRequest(kind, index, name)` | `Loadouts.request`: save as new, save over, load, rename, delete, or load the next one (no runes travel: the server saves its own spellbook) |
| `MasteryChoices.Request(kind, spell, slot, trait)` | `MasteryChoices.request`: choose a trait, re-roll the offer or unbind a trait (registered by `MasteryChoices`, with its own throttle) |
| `Aura.Key(trigger)` | `Aura.press`: the Aura key pressed one of its ways (`AuraApi.Trigger`: tap, sneak and press, double tap, hold), dispatched to the technique registry (registered by `Aura`, with the casting throttle) |

Aura sends two notices of its own: `AuraSense.Sensed(ids, colour, ticks)`, the creatures a breath of the stance sensed, to the
breather alone, and `AuraStep.Stepped(entity, from, to, yaw, colour)`, a step taken, to everyone tracking the stepper and the
stepper (for the afterimages). Spell mastery sends two notices of its own: `MasteryChoices.Rise(name, rank, colour, choice, seed)` (one of your spells
reached a rank: a toast with its sigil, and the Cord screen opens the choice) and `MasteryChoices.Title(caster, name,
rank, colour)` (a named Adept spell cast nearby: its title by the caster).
Three notices go the other way: `Discovery(key)` (a new Grimoire entry; the client shows a toast),
`LeySeed(seed)` (sent at login: a one-way hash of the world seed that ley lines grow from) and
`ScreenFx(kind, strength, ticks)` (a camera shake, field-of-view kick, punch or Domain tint; see
`cast.ScreenFx`). The travel commands send `Waypoints.Track` (the tracked waypoint, for `WaypointHud`), and
`cast.Climate.Sync(conditions)` tells each player the elemental climate where they stand (once a second, only
when it changes) for the HUD's marks, and `cast.PlayerAffinities.Rise(element, level)` tells a player an
affinity reached a new level (the client shows its toast). `cast.WorldResonances.Revealed(kind, name, colour)` carries
the name of a resonance just found or a quirk just met, for its toast. `Config.Sync` also says whether runes start
unread.
`cast.Climate.Sync(conditions, crossing, celestial)` tells each player the elemental climate where they stand and the
server's tuning of places and times of power (once on joining, then once a second only when it changes) for the HUD's
marks and lines and a spell's price at a ley crossing, and `cast.PlayerAffinities.Rise(element, level)` tells a player an
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
  per element from `ElementGlyphs`, with a green ▲ or red ▼; for 8 seconds after `Climate.changedAt()` the lines saying
  why, `SpellHud.whyLines`, which the Grimoire page reuses), and a beat ring that closes on the badge
  as the beat comes. Laid out by measuring, and shrinks to fit (short of room beside an offhand slot
  or attack indicator, it sits on top of them: `SpellHud.place`). A spell's reading is remembered
  by its runes (`SpellHud.read`) rather than compiled every frame.
- **`Tooltips`**: the game never wraps a tooltip line, so every tooltip a screen draws goes through
  `Tooltips.fit` (wrapped to at most 280 pixels, and cut short if taller than the screen), and the
  mod's items' tooltips are wrapped as they're built (a late `ItemTooltipCallback` phase).
- **Aura on the client.** `AuraHud` draws the aura bar on top of the spell panel (`SpellHud` lifts everything above it) or,
  without a Cord, alone in its place: stage diamonds (the next pulses gold while a breakthrough waits), the aura held in
  the method's colour with Aura Slash's price marked, the breath's beat ring (`AuraRules.nextBeat`), a trial's progress,
  backlash dimming it and a raised guard edging it in gold. `AuraScreen` is the Aura page, opened from the Cord screen's
  Aura badge (drawn with or without a Cord). `AuraClient` keeps what aura sense outlined (`mixin.EntityRendererAuraSenseMixin`
  makes them glow, in the aura's colour, for this player only). `fx.AuraBlade` draws the blade's aura: `AvatarRendererGearMixin`
  carries each player's `Glow` (from the synced look) into their render state; `ItemInHandLayerAuraMixin` (third person) and
  `FirstPersonAuraMixin` (first person) mark the main hand's item while it's drawn; `ItemLayerAuraMixin`, at the end of each
  item layer's submit, draws round the model in its own space: the outline traced from a flat item's side faces (cached
  per model) with its principal axis as the blade's line, or the extent of a special model (the trident in hand). All of
  it is `RenderTypes.eyes`, so shader packs take it as glowing eyes. A spell riding the blade (`AuraPresence.Look.bladeSpell`,
  carried in `Glow.spell`) adds a soft glow of its colour and two coils winding up the blade. `render.AuraShellLayer` (on every
  `AvatarRenderer`) draws the top stages on the body: aura armour's shell (a `PlayerModel` baked a little larger,
  `SHELL`/`SLIM_SHELL`, its skin's second layer hidden, in `RenderTypes.eyes` with `shell.png`, light gathered at each face's
  edges, flaring after `shellStruckAt`) and a step's afterimages (`AuraClient.afterimages`: the parent model again at each
  point the step passed, backed out of the body's turn and set down at the point turned the dash's way, in
  `entityTranslucentEmissive` over the skin and `eyes` over the shell's texture, fading over 12 ticks). `AuraClient` takes
  `AuraStep.Stepped` notices (four afterimages along the way, each as the body passes it). The aura bar dims Form's diamond
  while the step recharges, burns Sovereign's while a Dominion stands (a thread under it while it rests), and writes a spell's
  time on the blade and a Dominion's time left above itself; the Aura page lists aura marks, the spellblade, aura armour and
  Intent beside the registry's techniques. `WildercordKeys` holds a lone tap of the Aura key back for the double tap's 8 ticks
  when the player has a double-tap technique (`AuraApi.techniqueFor(stage, DOUBLE_TAP)`), so Aura Step never looses a slash.
- **`WildercordKeys`**: R (tap casts, hold charges), V (tap selects, hold opens the
  **`SpellWheelScreen`**), K, the unbound "cast spell N" keys and an unbound "Next loadout" key.
- **Performed casting, the client's half.** **`SigilTrace`** follows the local charge (when the server marks it
  traceable and the player hasn't turned tracing off): sneak held steadies, and `mixin/MouseHandlerMixin` hands the
  frame's mouse movement to `SigilTrace.mouse` instead of turning the view (sensitivity read as vanilla does, held
  between 0.04 and 0.3 degrees a count; the assist pulls the point toward the line); it draws the glyph round the
  crosshair as a HUD element (16% of the screen's short side at any GUI scale) and, on release, scores the path
  (`TraceGlyph.score`) and sends `TraceSpell` just before `ChargeSpell`. **`fx/Incantations`** speaks each charging
  player's syllables as their roundels open (with a kit whisper) and draws them in `LevelRenderEvents.COLLECT_SUBMITS`
  as vanilla text submits (billboarded, glowing-sign outline, full bright, fading by distance), so they render under
  shader packs the way name tags do. **`CastingOptions`** keeps this player's choices in
  `config/wildercord-casting.json` (tracing on or off, its assist, whose incantations show), set from
  `MagicSettingsScreen`. The overchannel is drawn from the synced charge too: `ChargeCircles.Circle` adds four
  cracks per stage in its `extras` pass, swells, trembles and throws `Glimmer` sparks and `LightParticle` arcs as stages
  land and reddens near the tear; `ChargeHum` strains higher with a waver; `ScreenEffects.drawStrain` closes the
  caster's screen edges in (a HUD element under the rest); `SpellHud` shows the stage (✦I to ✦III) and the charge's
  beat ring.
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
  at most; and at each ley crossing within 56 blocks, found chunk by chunk with `LeyLines.crossingIn` and remembered, a
  flat `RingGlow` of rings and crossed lines, a `Glimmer` shaft and rising motes). Client-only lights, made straight into the particle engine and never sent:
  `Glimmer` (a drifting mote, a faint shaft of lamplight, a flickering firelight glow, or a haze that
  follows an entity), `RingGlow` (rings and lines of soft light in a plane, flat or leaning and
  swinging round, with beads running along). `RuneAura` gives every Runebound a haze and drifting
  specks in its colour (thicker while it telegraphs); `ArchiveAmbience` finds an Archive by its
  lectern's block entity and, inside, reads the halls a slice a tick for lamps, braziers and shelves:
  shafts of light with dust in them, flickering braziers and embers, the arena's inlaid circle
  glowing (brighter while the Archivist is abroad), and quiet pages, whispers and chimes;
  `WellstoneHalo` hangs a turning ring over every awake Wellstone; `SoarWings` draws a Soar flier's wings
  of wind (strokes of `LightParticle` carried along with the body) and the wake off their wingtips, from the
  synced `soaring` note. Each spawner keeps a small
  budget of lights out at once (`Glimmer.Budget`).
- **Spell mastery on the client.** `MasteryPanel` is the Cord screen's mastery panel (drawn in its local space like
  `LoadoutPanel`): the spell's circle through `GuiSpellCircle.draw(..., look)`, its rank and bar, the four trait slots,
  the waiting offer as cards, re-roll and unbind, and what it's been used in most; it sends `MasteryChoices.Request`.
  `CordScreen` draws a rank badge and bar on each row (`drawRankBadge`), mastery lines in the readout, the side circle
  with the spell's look, opens the panel from a badge or `Ctrl`+`M`, and by itself when a rank was just reached
  (`MasteryClient.takePrompt`). `MasteryClient` shows the rank toast (`RankToast`, the sigil as its icon), the spoken
  names (`Titles`: a HUD element that projects the caster's head with `GameRenderer.projectPointToScreen`, or falls back
  to a line low on the screen; hidden by `MagicQuality.spellTitles`), and an inscribed scroll's sigil under its tooltip
  (`SigilTooltip`, through Fabric's `ClientTooltipComponentCallback`). The circles themselves: `SpellCircleParticle`
  takes a mastery look (from `SpellCircleOption`'s `rank`, `sigil` and `flags`, or, for the circles that follow a caster
  in `SpellFormations` and `ChargeCircles`, from the caster's synced `mastery_look` through an overridden `look()`),
  draws the sigil in place of the seal, deepens its colour from Adept, and adds the outer rings and Mythic's shimmer;
  `GuiSpellCircle` draws the same in screens, and its `sigil` draws a sigil anywhere.
- **`RuneReadingText`**: a rune's text as the local player has read it (a hint, the glimpse with its numbers drawn
  obfuscated, or in full). The Codex's tooltips and search, its corner marks (a ? unread, a dot glimpsed) and, through
  `RuneItem.describe`, every rune item's tooltip go through it. The Grimoire page adds *This world's resonances*, *This
  world's quirks* and *Runes you're still reading* from `world_lore` and `rune_reading`; the readout names a found
  resonance (`Heart.foundResonance`) and says what its twist does.
- **`GrimoireToast`** (and `GrimoireToast.affinity`, a level reached, with the element's mark from
  `ElementGlyphs`), and the Grimoire page inside `CordScreen`, whose Affinities section draws each element's
  mark, level and a bar toward the next level (`GrimoireLine`'s `glyph` and `points`).
- **Casting poses**: a cast sets the synced `cast_pose`; `AvatarRendererMixin` reads it (and the
  charge, and the Cord's look) into the render state through the `CastingPose` interface that
  `AvatarRenderStateMixin` adds, and `PlayerModelMixin` moves the arms: both hands held out while
  charging, then a motion for the shape (a thrust, a Crescent's sweep, a Barrage's alternating blows,
  arms flung up for the great circles, a push, arms swept back for a Blitz). Arms that point where
  you look follow the head's turn, as vanilla's bow pose does.
- **`monster/`**: the monsters' models (hand-built `LayerDefinition`s with `setupAnim` reading `MonsterRenderState`'s eased
  poses, each layout in its javadoc) and renderers. `WildMonsterRenderer` adds glow layers through `RenderTypes.eyes`
  (each a texture of only what glows, its strength per frame). The Gloomstalker draws through vanilla's translucent
  entity pass with its alpha as the model tint (`MonsterRules.opacity`), the way vanilla draws a creature invisible to all
  but its team, so it's shader-safe; its eyes are a separate glow layer, always lit. The Mana Ooze draws its inside and
  its clear outside translucent (as a slime's outer jelly) and its heart through `eyes`; the frog's bubble likewise.
  `MonsterClient.init` registers them all.
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
- **`wildlife/`** (the client's half of magical wildlife): a model per creature (`LumenStagModel`, `GlimmerwingModel`...,
  hand-written boxes posed in `setupAnim` from one shared `WildlifeRenderState`) and `WildlifeRenderer`, which draws a
  baby small and adds a glow layer (`RenderTypes.eyes` with a texture of only what glows, at the strength the renderer
  works out: the moon's phase for a stag, the night for the rest, so it's safe under shader packs). Flat parts (wings,
  moss) are zero-thickness boxes grown by 0.001 so their two faces never fight. `MossbackTortoiseRenderer` draws the
  garden: three real plant block models (through the context's `BlockModelResolver`, as the mooshroom's mushrooms are)
  set on the shell's crown and following it as it rocks and settles. `FrostPrint` is a rimehare's print, a flat quad in
  vanilla's translucent particle layer, made straight into the particle engine by its renderer when the hare lands.
  Dust, sparks and motes are the mod's `MoteOption`, spawned by each creature's own client tick.
- **Mixins**: the Cord slot is added to the inventory menu on both sides (`InventoryMenuMixin`,
  menu index 46, and the gear slots and the Backpack slot after it), synced from creative mode (`ServerGamePacketListenerImplMixin`,
  for any `PlacedSlot`), drawn in the survival inventory (`InventoryScreenMixin`, with `SlotWell` for the
  slot's frame and `GearTray` for the gear slots) and placed in the creative inventory tab
  (`CreativeModeInventoryScreenMixin`). `AbstractRecipeBookScreenMixin` keeps a click on the gear tray from
  counting as outside the window, `AbstractContainerScreenMixin` names an empty gear slot on hover,
  `PlayerGearMixin` drops the gear slots and worn backpack with the inventory on death, and `AvatarRendererGearMixin` carries
  worn gear into the render state. `LivingEntityRendererMixin`
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
- **Monsters** (`monster.MonsterContent`): the six monsters of the wilds, the frog's bubble and the thrown bramble,
  their drops and spawn eggs; their loot tables, brews and icons come from `tools/monster_art.py`.
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
- **`wildercord:inscription`** (`content.Inscription`): on a Spell Scroll inscribed from an Adept spell, the spell's key, its
  inscriber's own traits, its sigil's seed, its rank and its author; it's also the scroll's tooltip image.
  and the Archive Lectern (wakes the Archivist). The ten residues are `ResidueBlocks` and their reagents `Reagents`
  (see [A world that remembers magic](#a-world-that-remembers-magic-residues-powerplaces)).
- **More items**: Spell Scroll (a `wildercord:scroll` component holds the spell, its name and
  author; casting it builds a `Cast` with base bonuses), Torn Page (a riddle from
  `Grimoire.hint`, and the distance and direction to the nearest Archive), Training Dummy.
- **Casting gear** (`gear/`): `GearDef` (pure: every staff, the tome and the foci, with their numbers),
  `GearSlot` (pure: the list of gear slots, each with the kinds it takes, its icon and how it shows on the
  wearer), `GearLayout` (pure: where they sit on the inventory screens), `GearBonuses` (pure: what a
  player's slotted pieces and the held pieces of any kind whose slot is empty do to a spell),
  `SpellSlots` (pure: which of the five spell slots are open), `Gear` (combines slots and hands; the
  charged-cast flourishes), `GearSlots` (reads and writes the `wildercord:gear` attachment; drops it on
  death), `GearItems` and `GearLoot`. The server reads the slots and hands when casting and stores the
  result on the `Cast` (`cast.gear`), so the whole spell uses the gear it was cast with. The slots are
  menu slots after the Cord's (`menu.GearInventorySlot`), drawn as a tray above the survival panel
  (`client.GearTray`) and worn by `client.render.GearLayer`, fed by `GearLook` from the render state. Server settings live in `config/` (`WildercordConfig`
  is pure and unit-tested; `Config` loads, reloads and syncs it), and the add-on API in `api/`, with its
  runtime side in `cast.AddonRunes`. See [features/gear-config-api.md](features/gear-config-api.md) and
  [API.md](API.md).
- **Backpacks** (`backpack/`): `BackpackTier` (pure: rows and colour of each), `BackpackItem` (use opens it;
  its contents live in vanilla's `minecraft:container` component, so it keeps them wherever it goes) and
  `Backpacks` (what may go inside, the worn one in the `wildercord:backpack` attachment, synced only to its
  owner, with `wildercord:backpack_look` for everyone else; opening; the death drop). `menu.BackpackMenu` is an
  open backpack: it holds the very stack it opened and where it was, writes every change back into it at once,
  locks that slot, refuses swaps onto it, and takes no clicks (and closes) once the stack is anywhere else, which
  is what keeps an open backpack from being duplicated. `menu.BackpackSlot` is the worn slot, after the gear slots
  on the tray (`GearLayout.traySlots()`); `client.BackpackScreen` draws the menu; `client.render.GearLayer` draws the
  worn pack (`BackpackModel`) under a slotted staff. The bigger backpacks are `content.UpgradeRecipe`s, shared with
  the Cords; `mixin.ShulkerBoxBlockEntityMixin` keeps hoppers from feeding backpacks into shulker boxes.
- **Magical wildlife** (`wildlife/`; the design is DESIGN.md's [Magical wildlife](DESIGN.md#magical-wildlife)):
  - `WildlifeRules` is pure (and unit-tested): each `Kind` (its pool, weight, group size, rarity, crowding and biomes),
    the multiplier's effect on weight and chance, the moon's glow on a stag's antlers, a stag's `Stance` toward a player,
    gardens by biome, the intervals for scutes and membranes, the spark-bite, a skyray's cruise and bank, and `approach`
    (how poses ease on the client).
  - `Wildlife` registers the six entity types, their drops (`WildlifeItem`: a lore line and a use line in the tooltip;
    the Ember Tuft carries `COOKING_FUEL`) and spawn eggs, adds the Skyray Membrane to the elytra's `REPAIRABLE` through
    `DefaultItemComponentEvents`, remembers each player's last cast (`AFTER_CAST`, for the glimmerwings), and once a
    second per player writes a field-guide key for any guide creature in sight within 14 blocks (`Grimoire.unlock`).
    Sounds are the feel kit's `wildlife_*` events, looked up by `Wildlife.sound(name)`.
  - `WildlifeSpawns`: each kind joins its biomes through a Fabric biome modification (read as the world loads: switched
    off, it's not added at all; its weight grows with a multiplier above 1), and its `SpawnPlacements` rule decides every
    natural try live from `Config.get().wildlife()`: ground tag, light, the kind's chance and its crowding. Eggs, summons
    and spawners skip the config, chance and crowding.
  - The creatures: `Glimmerwing` and `Skyray` are `AmbientCreature`s that fly by steering their own velocity in
    `customServerAiStep` (no pathfinding, so a swarm costs little); a glimmerwing samples five spots for block light
    every two seconds, keeping the brightest, so a swarm finds a lamp over a few tries. `LumenStag`, `MossbackTortoise`
    and `Rimehare` are `Animal`s, `Cinderfox` a `TamableAnimal`. The shy ones run with `FleeGoal` (vanilla's avoid goal
    targets as for a fight, which Peaceful switches off). Synced data carries only what a model needs (a stag's pose and
    shed day, a tortoise's garden and hiding, a moth's colouring); each creature eases its poses on the client
    (`graze`, `hide`, `sit`, `air`, `flapPhase`...), with last tick's value beside it for the renderer to blend.
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

11. **Residues and reagents** (`residue_art.py`, called last): the ten residue blocks' textures (what glows on its own
   cut-out layer, lit by the model's `light_emission`, most of them animated), their models and blockstates (a void
   scar's per stage, some turned at random), the reagent icons, the loot tables, the `wildercord:residue_ground` and
   `wildercord:reagents` tags, the mining and bees' flower tags, and their English text (`residue_art.LANG`).
   `python tools/residue_art.py` renders a review sheet into `build/art-preview/`.

12. **The monsters of the wilds** (`monster_art.py`): the six skins (painted face by face on each model's box UV from
   materials: bark, moss, leaves, fur, feathers, chitin, crystal, frog skin, jelly), their glow layers, the frog's bubble,
   the drops' icons and the spawn eggs, their item models, the loot tables (`entities/<id>`), the brewing recipes for the
   Shadow Pelt and the Bog Gland, and the English text (`monster_art.LANG`). `python tools/monster_art.py` renders a
   review sheet into `build/art-preview/`.
12. **Magical wildlife** (`wildlife_art.py`): every creature's skin and glow layer, painted box by box onto the exact UV
   layouts of the models in `client/wildlife/` (each box is painted from a function of the point on it, so a stripe down
   a spine lands on every face it crosses; a box that would overlap another on the sheet is refused), the drops' icons,
   the spawn eggs, the frost print, the entities' loot tables, the `wildercord:spawns_on/<creature>` ground tags, the
   brews and recipes, and their English text (`wildlife_art.LANG`, with the creatures' sound subtitles).
   `python tools/wildlife_art.py` renders a review sheet into `build/art-preview/`.
13. **The world of aura** (`aura_world_art.py`, after `aura_art.py`): the ten duelists' skins and their glow layer, the
   fallen knight's skin and glow (painted on the layouts in `client/auraworld/`'s models), the manual pages (one model per
   method, picked by the component), the Aura Shard, the Breath Sash, the three forged weapons' looks (the glaive's with a
   spear's in-hand model), the spawn eggs, the smithing forgings and the other recipes, the knight's loot table, the
   `wildercord:knight_haunts` structure tag and the English text (`aura_world_art.LANG`). `python tools/aura_world_art.py`
   renders a review sheet into `build/art-preview/`. `tools/wiki_recipes.py` draws smithing recipes as the smithing table
   lays them out.

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

The feel kit (`tools/feel/`, built by `python tools/feel/build.py --only <part>`) holds a part per element and a
`monster` part: the monsters' own voices (`monster_<creature>_<sound>`), whose subtitles name the creature
(`subtitles.wildercord.kit.<creature>.<sound>`, their text in `monster_art.LANG`) rather than the four spell subtitles.
The creatures play them through `MonsterMagic.sound` and `kit`, from the hostile sound source.
The feel kit (`tools/feel/`, built with `python tools/feel/build.py --only <part>`) has one part that isn't an element:
`wildlife`, the creatures' voices (wings, calls, footfalls, a crystal antler falling). Magic's kit sounds share four
generic subtitles; a creature's names itself (`core.CREATURE_SUBTITLES`, their text in `wildlife_art.SUBTITLES`).
The feel kit (`tools/feel/`, built by `python tools/feel/build.py --only <part>` and checked with `--check`) holds the
rest, one part per element plus `neutral` and `aura` (the slash, the guard, the perfect guard, a breakthrough, backlash and
the stance's breath), registered by `WildercordSounds.kit(name)` and played through `cast.feel.Feels.sound`. The `duelist`
part holds the world of aura's: a blade drawn and sheathed, a bow, a yield, the clash, and the fallen knight's voice
(`duelist_knight_*`), with subtitles that name them (`aura_world_art.LANG`).

## 9. Testing

- **`AuraRulesTest`** covers aura's pure rules (stages, filling and waiting at a threshold, switching, what fills aura and
  what doesn't, the beat, the guard and its perfect moment, the Edge's pierce, the slash, backlash, experience and the
  tuning worked through, the trials, every passive growing, the colour, the ten methods, where manuals are found and the
  stage registry), and `WildercordConfigTest` the `aura` section. **`WildercordAuraTest`** plays it in a real world: the key
  and its default, learning and switching methods, aura from a real swing and not a half one, a dummy's quarter and cap,
  the stance, a breath on the beat and aura sense, the coat's 10% and an element, Flow's sweep from an axe, the guard and
  every perfect guard (a blow, an arrow, a spell), the Edge's reach, pierce and slash (by the real key), backlash, a
  simulated player's slash under the cap and the spellguard, and both breakthroughs; screenshots `aura_*`. `AuraRulesTest`
  also covers the top stages (all five open, their trials, the step, aura armour's floor, Intent's weaker-only rule, Dominion,
  the spellblade's falloff, the marks and that no method sets off its own), and `WildercordConfigTest` their keys.
  **`WildercordAuraMasteryTest`** plays the top stages: Aura Step by the real key (its distance, price with no slash, cooldown,
  a wall, its untouchable moment, backlash), aura armour's quarter and floor and its shell on the client, Intent on a weaker
  husk and not a stronger one (and a simulated rival's on the player, slight, and not from an equal), Dominion by the real key
  held (price, slow, weaker hits, a chain inside and never outside, the rest, its end), the spellblade (a fire spell cast
  sneaking riding the real slash onto a line of husks, both prices paid, a standing cast going out as usual, one left too long
  slipping off), a Rime mark set off by the player's fire spell (Shatter, with the config's chance raised for the test), the
  breakthroughs into Form (the tempest at a ley crossing) and Sovereign (a boss, and not one a spell touched), and against
  players (Dominion's weakening times the PvP scale, a rival's chain held to the cap, a carried spell meeting the spellguard);
  screenshots `aura_mastery_*`.

- **`AuraWorldRulesTest`** covers the world of aura's pure rules (the duel's stage, duelists and knights by stage and rank,
  yielding, mob guards, spawn chances, the forgings and the sash, the clash's meeting and a slash against a player), and
  `WildercordConfigTest` the `aura_world` section. **`WildercordAuraWorldTest`** plays it on a platform in the sky: duelists
  come to a camp (its borrowed fire going with them), a road and a village bell, never two together, and go into the field
  guide; a duel by the real use key (offer, accept, countdown, the fight at the challenger's stage), its guard's halving and
  perfect stagger, its slash at Edge cutting only its challenger, its yield teaching a method, experience and the Grimoire,
  a second victor handed the manual, a knocked-out challenger put back whole; every forging's recipe (keeping
  enchantments) and effect, pages bound into a manual (mixed ones refused), the sash's capacity and quicker stance; a knight
  rising in a dark spawner room, its telegraphed slash through the spell defences, dodged by stepping off its line and sent
  back by a perfect guard, its guard's stagger, halving and break by an axe, and its loot; a clash harming nothing; and the
  slash by day and night (`aura_world_slash_*`). Screenshots `aura_world_*`.

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
- **`WildercordPerformanceCastTest`** plays performed casting for real on a platform in the sky: a beam held to each
  overchannel stage hits a husk 1.2x, 1.4x and 1.6x as hard as a plain release and drains spare mana (and a starved
  channel stops climbing without touching the price), releases on the beat add their bonus, a forged trace is clamped
  to the steadying time, a glyph traced with the test's own mouse (sneak held, the camera still) scores high and
  steadies while an untraced release scores neutral, a client-side rival's incantation reads right and hides with the
  option, and a channel held too long tears loose without hurting a caster on one heart. Screenshots
  `performance_overchannel_stage_1..3`, `performance_trace_glyph`, `performance_incantation_rival` and
  `performance_incantation_third_person`.
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
- **`WildercordNewRunes2Test`** casts each of the second batch of new runes at husks (and a tamed wolf) on a
  platform in the sky and checks what it's for (two glaive hits, an imprint's wait, a latch's strikes, Kindred's
  share, Thirst's drink, Belated's delay, On Reaction and On Weakness firing only when they should, a brand's
  burst, a gash that stops healing, ore glowing, a seared blow, the soaked freezing, a sleeper waking, a spark
  lighting a lamp and going, Prolong, Umbra at noon and midnight, a sword snatched and given back), then
  screenshots three of their circles (`new_runes_2_circles`).
- **`WildercordFishingTest`** rolls the fishing tables thousands of times through the server's loot
  API with a real bobber (treasure runes and Torn Pages at their rates; a tangled rune never in plain
  water or out of open water, and at its rate under a mana storm, on a ley line and in a
  thunderstorm; Reeled In), then casts Tidehook at a husk and Current in water, in the rain and on
  dry land.
- **`WildercordMasteryTest`** checks spell mastery in a real world: real hits on husks earn experience and casts at
  nothing don't; training dummies stop at the practice cap; Practised offers three fitting traits, shows its toast, opens
  the choice in the Cord screen by itself, and clicking Thrifty Weave's card with the real mouse lowers the price in the
  readout and in a real cast; re-rolls and unbinding cost levels and are allowed once per rank; an edited spell starts
  its own record and changing back finds the old one; an inscribed scroll carries the spell's own traits, sigil and
  rank, a second player (a `FakePlayer`) studies it into Kindled with the traits borrowed, a second study is refused, a
  Kindled spell inscribes nothing, a forged scroll teaches nothing and a scroll read aloud teaches nobody; a named Adept
  spell's title goes to a second player added to the world beside the caster (for the moment of the cast) but not to
  one beyond 32 blocks, shows on the caster's screen, and not with spell titles off; a scroll's tooltip has its sigil.
  Screenshots `mastery_choice`, `mastery_cord_screen`, `mastery_panel_mythic`, `mastery_title` and `mastery_circles`
  (ranks I, III and V side by side). `MasteryRulesTest`, `MasteryTraitsTest` and `MasterySigilTest` cover the pure rules
  (thresholds, worth, the moment, repetition, practice, the tuning worked through, the catalogue and its filters,
  offers, determinism, the sigil's symmetry and variety), and `WildercordConfigTest` the `mastery` section.
- **`ResonanceTest`** and **`RuneReadingTest`** cover the pure parts: the draw is deterministic, a reroll salt and
  another seed change it, the first resonances never move with the count, every resonance is a clean affordable spell of
  crafted runes that fits its twist and collides with nothing, riddles name every rune, the privacy filter, quirks, the
  reading stages and the hints and glimpses of every rune.
- **`WildercordResonanceTest`** draws three worlds (two seeds alike, one not), casts a resonance for real (Grimoire, first
  finder, toast, announcement, price), reads a Torn Page riddle, checks the client holds nothing undiscovered, films a
  few twists (`resonance_twist_*`), and takes a rune learned from its item from unread to glimpsed to understood in the
  Codex (`resonance_codex_*`).
- **`WildercordFlightTest`** casts Soar for real on a platform in the sky: casting gives flight and a double-tap of
  jump takes off; a flight running out 64 blocks up warns, then lands the player with no damage and nothing left;
  creative players are left alone; an ally a Burst reaches flies and a stranger doesn't; a dungeon's ward (filed
  as a dungeon piece files its arena) won't let it lift anyone and sets down a flier who comes in; a monster's Pull
  and a Weigh ground a flier; a flight saved in a crash is tidied at login; death leaves nothing; and a real save
  and reload saves the player unable to fly, gives the flight back and lets it run out safely. Screenshots `soar_*`.
- **`WildercordWildlifeTest`** builds a lawn in the sky with a strip of each creature's ground and checks that every
  creature is in each of its biomes' spawn lists (read from the biome's `NATURAL_MOB_SPAWNS` attribute) and not in a
  wrong one; that a spawn rule passes on the right ground (a stag about a third of the time), fails on the wrong one and
  with `creatures.wildlife` off (reloaded), and never stops an egg; that a stag bolts from a player walking up, trusts
  one sneaking still beside it and sheds one antler a day for them, and curses its killer and frightens its kin; that a
  cinderfox tames with rabbit, sits, bites a polar bear 4.5 and sets it alight but a cow only 3, and gives one tuft a
  day to the brush; that two tortoises fed melon raise a baby and a struck one hides and takes 40%; that a rimehare bolts
  unless berries are held out; that a skyray climbs to its cruise and sheds a membrane; that a glimmerwing finds a
  lantern at night and comes to a player casting; that creatures seen go into the field guide (and the Grimoire page
  shows them, `wildlife_field_guide`); and, end to end, that glimmerwings spawn on their own once a stretch of the flat
  world is turned to forest at night with mob spawning on. Then it photographs each creature close up by day
  and night (`wildlife_<creature>_day`/`_night`) and a few poses (a stag grazing beside its shed antler, a tortoise in
  its shell, a cinderfox sitting). `WildlifeRulesTest` covers the pure rules and the field guide, `WildercordConfigTest`
  the `creatures` section.

## Hooks for other systems

Each world's own magic is built to be joined later (by spell mastery, by places of power, by residues). The API is in
`cast.WorldResonances`:

| Call | What it's for |
|---|---|
| `discovered(player, id)` | Whether a player has found resonance `id` (both sides: it reads the synced Grimoire key `resonance:<id>`) |
| `of(server)` / `quirks(server)` | This world's resonances and rune quirks (server side; never send them to a client) |
| `match(server, runes)` | The resonance a rune sequence spells out exactly, if any |
| `addWakeCondition(id, condition)` / `removeWakeCondition(id)` | An extra condition every resonance must meet to wake, asked as `(ServerPlayer caster, Resonance resonance) -> boolean`; a condition that doesn't concern a resonance answers true. A resonance held back isn't found and has no twist (the cast is only the ordinary spell, with a faint shimmer). A place of power can hold back the resonances it chooses unless cast inside it; a residue system can ask for a reagent in hand. |
| `onFound(listener)` | Told `(player, resonance, firstInWorld)` for each resonance a player finds, the first time they do |

Nothing in the draw changes when a condition is added: conditions only decide whether a cast wakes what it matched.
To give a resonance's riddle a line about its condition, a system can keep its own note against the resonance's id.
- **`WildercordMonstersTest`** plays each monster of the wilds for real on stages east of spawn: a Bramblewalker rears and
  roots the player and runs once set alight; a Gloomstalker hides in the dark, shows under glowing and stays shown after a
  fire spell, then crouches and pounces; a harpy is grounded by an earth spell, shrieks and dives (striking or crashing),
  and its lightning lands softer under the Potion of Warding and is held by the spellguard; a Geode Crawler curls, takes
  little from a plain blow, cracks under a pickaxe and a shock, and rolls; a Bog Witch-Frog swallows a chicken, swells,
  spits a bubble that poisons the player, and a bubble struck in the air pops; a Mana Ooze drinks a spell unharmed,
  grows, burns under fire and bursts in two when overfed. Then the biome entries (by the biome registry), the spawn
  rules and their switch (written to the config and reloaded), Runebound spells that compile, and Peaceful sending them
  all away. Screenshots `monster_<creature>` in each pose (held, from a three-quarter view) and `monster_*_live` mid-attack.
  `MonsterRulesTest` and `WildercordConfigTest` cover the pure parts.
- **`WildercordResidueTest`** casts a 60-mana spell of each element at grass (each leaves its residue), and checks a
  small spell, planks, the `residues.enabled` switch (reloaded) and a ward leave none, a reaction leaves one, everfrost
  is slick, a void scar draws a stick in and narrows, harvesting gives reagents and the grass back, a bottle takes an
  eddy, an Everfrost Shard halves a weave's XP and Geode Grit keeps the amethyst, a full moon makes Harm 15% stronger, a
  thunderstorm makes storm 25% stronger, and a ley crossing makes Fire 10% stronger and Burst · Explode 10% cheaper
  (and the `PowerPlaces` hook agrees); then a real save and reload keeps every residue, and `Residues.fastForward`
  fades them, one in unloaded ground waiting for its chunk. Screenshots `residue_gallery_day`/`_night`, each
  `residue_<kind>`, `residue_hud_full_moon` and `residue_ley_crossing`. `AltarReagentsTest`, `PowerTableTest` and
  `LeyCrossingTest` cover the pure rules.

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
- **Residues never grief.** Only natural ground or open air over it, never a built, placed or contents-holding block,
  a spell's passing block or a ward; a spell's own permission (a boss's: `spells_edit_blocks` and mob griefing); caps
  per chunk, area, dimension and caster; a saved fade that gives back the ground taken, read from a schedule, never a
  scan, and never loading a chunk for it.
- **Bosses are only ever slowed**, never frozen, swapped or held in place, so their fights can't
  break.
- **Aura can't be farmed or stacked.** Only a full swing on a real foe fills it or teaches; repetition fades a place, dummies
  give a quarter and teach 40 at most, one blow earns 6 at most, experience waits at each threshold for a trial, creative
  players earn nothing. Its blows are melee (armour applies); against players its bonuses sit under the spell-defence cap
  and the aura PvP scale, and projected aura meets the spell defences and the spellguard. Backlash never damages. Nothing
  about a player's aura leaves the server except to them; others see only a colour, a stage and a few flags. The top stages
  keep to the same: a step never passes anything solid or a ward's edge (and its move is the server's), aura armour never
  spends below its floor and is paid only for a blow that lands, Intent on a player is a vignette and a capped speed modifier
  that always lets go, Dominion's rest is saved in game time (a relog never resets it), and a carried spell is a spell paid
  for as one, through the cast engine (Shields, the spell defences, the PvP cap).
- **Spell mastery can't be farmed or stacked.** Experience comes from outcomes (`Mastery.afterDamage`, `healed`,
  `afterHit`), never from casting; repetition fades a place, dummies cap out, and one cast earns at most 20. Traits
  apply inside the ordinary cast and their damage sits under the spell-defence cap. Nothing about a player's records
  leaves the server except to them; others see only a spell's rank, sigil and looks. Inscriptions carry no experience
  and teach only a spell the reader has no record of.

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

