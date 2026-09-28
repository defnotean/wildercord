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
                                   Vfx / TechniqueVfx ──▶ Fx.send ──▶ particles for every player
                                   Sigils ──▶ sigil, spell circle and light particles
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
  `hint:` for a riddle read), the sixteen feats, and how much mana each first discovery condenses.
- **`Leaning`**: which element a caster leans toward, from their cast counts (40+ casts and 1.25x
  the runner-up), and its 10% bonus.
- **`SpellNames`**: a readable name made from a spell's runes ("Splitting Frost Bolt"), and
  cleaning of custom names.
- **`SpellCodes`**: a spell as a `wc:bolt.frost.split` code and back (add-on runes as
  `namespace~path`), and the pattern that finds codes in chat.
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
Circle to pay), `Rhythm` (casts on the beat), elemental leaning (counted in `countElements`),
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
  cast through `child()`, so no chain of links can run away;
- shapes that strike repeatedly (Domain, Zone, Totem, Orbit, Wall, Trail, Rain, Barrage, Orb) take a fresh creature/block budget per strike with `pulse()`, while the Siphon cap and
  `once(...)` costs still count for the whole cast.

`Cast.alive()` is false once the caster leaves, dies or changes dimension (or, for a passive, once
it's switched off, or once `cancel()` is called, which is how a Domain that loses a clash
shatters), and every scheduled part checks it, so a spell never outlives its caster.

`Cast.Info` records what was cast: the plan (Mirrorfrost casts it back), its rune count (a kill
with six or more is a feat) and the caster's leaning element (+10% for effects of it).

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
the caster's leaning (+10% for effects of that element) and, for innate runes, +6% per circle.
All spell damage goes through `Effects.hurt`, which skips invulnerability frames (so stacked
effects all land), applies Execute, Fortune and Unison, scales damage to players, and records the
hit for spell-kill counting and the innate runes that react to hits. After each effect,
`Effects.apply` tells `RuneSeals` its element and where it landed.

### Lasting magic: `Wards`, `Reactions`, `Spirits`, `Scheduler`

- **`Scheduler`**: delayed tasks, On Land watchers, and On Hurt / On Low Health watchers. Ticked
  once per server tick; cleared when the server stops.
- **`Reactions`**: short-lived marks (FROZEN, WINDSWEPT, PULLED, SOAKED, RESONANT) and the bonuses
  they set off (Shatter, Conduct, Wildfire, Implode, Collapse).
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
  **`Archivist`** is an illager with three phase
  spell lists, a boss bar that names the spell being cast, the rewrite at two thirds and one third
  health, and its own drops; the Archive Lectern's block entity wakes it.
- **`RuneSeals`**: a player's spell of the right element lights every seal of that element in the
  door it touched (found by flood fill); when no seal in the door is unlit, the door dissolves.
  Fire also lights unlit campfires.
- **`LeyWalker`**: sends the ley seed at login and sets the `on_ley` attachment every 5 ticks;
  `Mana.of` and `HeartCircles.tick` read it. The Wellstone's block entity checks the line under it
  once a second and renews `well_until` on players within 12 blocks.
- **`TrainingDummy`**: a `LivingEntity` that heals back to full after every hit, floats each hit
  up as a text display, and keeps its last 5 seconds of hits for the DPS in its name.
- **`SpellChat`**: a chat message decorator that turns `wc:` codes into hoverable spell cards.

### Visuals: `Vfx`, `TechniqueVfx`, `Fx`, `Sigils`, `BlockFx`

Visuals are sent from the server, so everyone sees the same show. Most are vanilla particles and
sounds. `Vfx.Theme` gives each element two colours, a mote, a spark and two sounds; shapes and
effects are built from primitives (`ring`, `radial`, `helix`, `stream`, `shockwave`).

`Fx.send` is the one place ordinary particles leave the server. It **skips any particle that would
appear right in front of a player's own eyes**, which keeps your own spells from blocking your
view. `Fx.sendAll` skips that check (for Heart Circle rings while you meditate or form a circle),
`Fx.sendOthers` sends to everyone but one player (the ring spin on every cast, which only onlookers
need), `Fx.sendFar` reaches 512 blocks (for a big Domain's dome), and `Fx.quietly(...)` mutes
everything inside it (passive renewals).

Magic circles are the mod's own particle, `wildercord:sigil` (`SigilOption`: a style, colour,
size, facing, lifetime and spin). The styles are `CIRCLE`, `RING`, `STAR`, `TARGET`, `CRACKED`,
`GLOW` and `BAND` (a thin plain ring, drawn in straight pieces so its line is the same width at any
radius). A spell's whole circle is one particle, `wildercord:spell_circle` (`SpellCircleOption`: the
spell's rune ids, a colour, a radius, a facing and a lifetime); each client builds it from the runes
(see `SpellSigil`), using every rune's own emblem and ring pattern
(`textures/particle/circle/<rune>_mark.png` and `_band.png`, drawn by `tools/circle_art.py`; add-on
runes without art fall back to their family's). `Sigils` builds full circles, ground reticles and
spell circles (`Sigils.spell`) and sends them to players within 128 blocks.

A third particle, `wildercord:light` (`LightOption`), is shaped light: a `RING` (a shockwave
racing out), a `RAY` (a beam, its pieces turned to face the viewer), a `SLASH` (a tapered crescent
sweeping across) and an `ORB` (a glow wrapped in turning rings), each a soft halo (`sigil_beam`)
under a hot core. `Light` sends them; every shape's visuals in `Vfx` and `TechniqueVfx` are built
from them, the circles and glows. `SigilOption.glow` is a camera-facing flash; it replaced vanilla's firework flash, which
showed up close as a pale square. The circle under every cast (`Vfx.castCircle`) is the spell's own
circle, flat on the ground, with enchanting glyphs rising from it and nothing flying outward; a Runebound's
telegraph is the same circle held out in its right hand.

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
| `cooldowns` | list of long | no (not saved) | When each spell is ready |
| `crystals` | int | yes | Mana Crystals used |
| `circles`, `condensed` | int | yes | Heart Circles and mana condensed |
| `spell_kills`, `boss_slain` | int, bool | yes | Heart Circle breakthroughs |
| `runebound_slain` | int | yes | Runebound slain, for the 6th Circle |
| `grimoire` | list of string | yes | Discoveries: reactions, secrets, feats and riddles read |
| `innate` | string | yes | The caster's innate rune |
| `element_casts` | map of string to int | yes | Casts per element, for leaning |
| `cracks` | count, mend time | yes | Circles cracked by overcasting, and when they mend |
| `meditating` | bool | no (not saved) | Worked out by the server each tick |
| `rhythm` | stacks, window | no (not saved) | The rhythm chain and the next beat |
| `charge` | spell, start, colours | no (not saved) | A spell being charged; synced to **everyone** nearby, who draw its circle |
| `on_ley`, `well_until` | bool, long | no (not saved) | On a ley line; near an awake Wellstone until |
| `spirit_until`, `frozen_until` | long | on the mob | End times for summons and frozen mobs |
| `runebound` | list of string | on the mob | A Runebound's spell |

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

Two notices go the other way: `Discovery(key)` (a new Grimoire entry; the client shows a toast)
and `LeySeed(seed)` (sent at login: a one-way hash of the world seed that ley lines grow from).
Everything else travels through synced attachments; `CHARGE` is synced to everyone nearby so they
can draw the circle.

## 6. The client

- **`CordScreen`**: the whole Cord UI, drawn in local coordinates inside a scaled pose so it stays
  crisp at every GUI scale. The header holds the Spells/Passives/Grimoire switch and the heart,
  mana and help badges; below are the rows, the family tabs and search box, category chips (only
  for categories you've learned something in), the Codex, and the plain-English readout, whose
  small tool buttons rename the spell (`RenameSpell`), copy or paste its spell code, and inscribe
  a scroll (`InscribeScroll`). It edits a local copy of the spells and sends `EditSpell` /
  `EditPassive` after each change. The Grimoire page replaces the rows, Codex and readout with
  everything discovered. It turns on SDL text input while open (see the 26.x notes).
- **`SpellHud`**: the panel beside the hotbar: selected spell, its runes and cost, the mana bar
  with a cost mark, cooldown, and passive drain (or the charge, while charging); above it the
  spell's name, rhythm notes and cracked circles (✦), and a beat ring that closes on the badge as
  the beat comes. Laid out by measuring, and shrinks to fit.
- **`WildercordKeys`**: R (tap casts, hold charges), V (tap selects, hold opens the
  **`SpellWheelScreen`**), K and four unbound "cast spell N" keys.
- **`fx/`**: `SigilParticle` (the magic circle: a flat, tinted, double-sided quad at full
  brightness) drawn in its own `SigilGroup`, which culls anything implementing `SigilGroup.Extent`
  by its whole extent;
  `SpellCircleParticle` (a spell's whole magic circle, built from its runes and opening in stages);
  `LightParticle` (rings, beams, slashes and orbs); `ChargeCircles` (the spell's circle in front of a
  charging caster's hands, opening as the charge builds, read from the synced charge);
  `AimPreview` (the reticle or dotted line while charging); `LeyMotes` (violet motes along ley
  lines, worked out on the client from the ley seed).
- **`GrimoireToast`**, and the Grimoire page inside `CordScreen`.
- **`render/`**: `ArchivistRenderer` (the evoker model, scaled 1.25x, with its own skin) and
  `TrainingDummyRenderer` with its own `DummyModel`.
- **Mixins**: the Cord slot is added to the inventory menu on both sides (`InventoryMenuMixin`,
  menu index 46), synced from creative mode (`ServerGamePacketListenerImplMixin`), drawn in the
  survival inventory (`InventoryScreenMixin`, with `SlotWell` for the slot's frame) and placed in
  the creative inventory tab (`CreativeModeInventoryScreenMixin`). On the server side,
  `LightningRodBlockMixin` turns a Blank Rune by a struck rod into Lightning, and
  `MannequinAccessor` sets up Phantom's afterimage (a mannequin wearing the caster's skin).

## 7. Content: items, loot, effects

- **Items** (`WildercordItems`): one `RuneItem` for every rune (the rune id is a data component,
  `wildercord:rune`, and the item model picks the texture from it; an innate rune's item can't be
  learned from), the Blank Rune, the four Cords, and the Mana Crystal.
- **Cord tiers** (`CordTier`): sockets, spell slots, highest rune tier, base mana and regeneration.
- **Loot** (`WildercordLoot`): extra pools on vanilla chest tables, weighted by tier (a Tier I rune
  is 8x as likely as a Tier IV one in the same pool), Torn Pages in old libraries and ruins, plus
  mob and boss drops. Only vanilla's own tables are touched, so a datapack that replaces a table
  keeps full control of it. Runebound and the Archivist drop their loot in code.
- **Entities** (`cast.WildercordEntities`): the bolt (`RuneBolt`, drawn by nothing but its
  particles), the Archivist and the Training Dummy.
- **Particles** (`WildercordParticles`): `wildercord:sigil` (a `SigilOption`),
  `wildercord:spell_circle` (a `SpellCircleOption`) and `wildercord:light` (a `LightOption`).
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
   blocks and the two entity skins from `world_art.py`.
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

Run `python tools/item_art.py` on its own to render review sheets of every icon into
`build/art-preview/`.

## 9. Testing

- **`src/test`**: JUnit 5 tests for everything in `spell/`: reading rules, attachment, costs,
  cooldowns, Blood Price, Vow, passive rules, circle and enchantment maths (`SpellCompilerTest`,
  `HeartAndPassivesTest`); and spell names, spell codes, secret spells, leaning, innate runes, the
  breakthrough table and `world.LeyLines` (`DiscoveryTest`). Fast and headless.
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

## 10. Rules that keep it safe

- **The server decides everything.** Edits and casts are validated in `SpellCaster`; the client
  only draws.
- **Friendly fire is off.** `Targets.canHarm` / `canHelp` decide every effect's targets, and
  respect the `pvp` game rule. Damage to players is scaled down (`Effects.PVP_DAMAGE`).
- **Every cast has budgets** (creatures, blocks, link depth), and spirits are capped per player.
- **Monsters never change blocks.** Every block edit checks `Casters.mayBuild`, which is false for
  anything but a player allowed to build there.
- **Nothing temporary is permanent.** Summons and frozen mobs carry saved end times; Rampart
  blocks crumble when the server stops and drop nothing when broken; block-display visuals are
  removed as their chunk loads; wards aren't saved at all.
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
- **Fabric events used**: `ServerLivingEntityEvents.ALLOW_DAMAGE` / `AFTER_DAMAGE` /
  `ALLOW_DEATH` / `AFTER_DEATH`, `ServerTickEvents`, `ServerEntityEvents.ENTITY_LOAD` /
  `ENTITY_UNLOAD` (Runebound, block-display cleanup), `ServerPlayConnectionEvents.JOIN` (the ley
  seed), `ServerMessageDecoratorEvent` (spell cards in chat), `ServerLifecycleEvents`,
  `LootTableEvents.MODIFY`, `PlayerBlockBreakEvents.BEFORE`, `HudElementRegistry`.
- **Dev client**: Loom's `runClient` loads classes from `build/`; rebuilding while it runs mixes old
  and new classes. The build supports `-PaltBuild` (outputs to `build-alt/`) for compile checks
  while a client is open.
