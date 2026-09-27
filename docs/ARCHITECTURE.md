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
```

Three layers, each only knowing about the ones below it:

| Layer | Package | Knows Minecraft? | Tested by |
|---|---|---|---|
| Spell engine | `spell` | No | JUnit (`src/test`) |
| Runtime | `cast`, `player`, `content`, `net`, `mixin` | Yes (server side) | Client game tests |
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

## 2. Casting (`cast/`)

### The gate: `SpellCaster`

`SpellCaster.cast(player, spell)` is the only way a spell starts. It checks, in order: a Cord is
worn, the spell slot is unlocked, the spell compiles to something, the cooldown is over, and the
player can pay (mana, or health for Blood Price). Only then does it charge, start the cooldown,
condense mana toward the next Heart Circle, and call `CastEngine.cast`.

`activeSockets` decides which threaded runes actually fire: inside the Cord's sockets, learned,
still loaded (a rune from a removed add-on stays threaded but *silent*), and a tier the Cord can
hold. Runes outside those rules stay threaded but quiet, so swapping Cords never deletes a spell.

`SpellCaster.edit` / `editPassive` validate edits the same way before saving.

### A cast in flight: `Cast`

A `Cast` is the context every part of a spell carries: the caster, the level, the link depth,
the caster's bonuses (power and duration from Heart Circles and enchantments), whether it's a
passive renewing itself, and a **budget**:

- at most **64 creatures** and **32 blocks** touched, and **8 links** deep, shared by the whole
  cast through `child()`, so no chain of links can run away;
- shapes that strike repeatedly (Domain, Zone, Totem, Orbit, Wall, Trail, Rain, Stand, Barrage,
  Orb) take a fresh creature/block budget per strike with `pulse()`, while the Siphon cap and
  `once(...)` costs still count for the whole cast.

`Cast.alive()` is false once the caster leaves, dies or changes dimension (or, for a passive, once
it's switched off), and every scheduled part checks it, so a spell never outlives its caster.

`Cast.Trigger` is where a segment starts (the caster, or the thing that set a link off).
`Cast.Hit` is what a shape hit: creatures, the point, the direction, the block and face if any.

### Shapes: `CastEngine.deliver` and `ShapeRunners`

`CastEngine.runSegment` delivers each group, then handles the segment's link. `deliver` switches on
the shape: instant shapes (Self, Touch, Beam, Burst, Cone) compute their hits right away; moving or
lasting shapes (Bolt and Arc via the `RuneBolt` entity; Trail, Wall, Orbit, Ring, Pillar, Wave,
Mine, Totem, Stand, Domain, Crescent, Barrage, Orb, Blitz in `ShapeRunners`) schedule their work
with `Scheduler` and call `onHit` as they go. Split fans shapes out (`CastEngine.fan`,
`CastEngine.spread`); Volley repeats them (`CastEngine.volley`).

### Effects: `CastEngine.onHit`, `Effects` and `Techniques`

`onHit` takes the creature budget, then applies each effect in the group (Stasis always first, so
it can hold everything after it), repeats lingering effects, pays out Siphon, and fires an anchored
On Hit / On Kill link.

`Effects.apply` is one `switch` on the rune's path. It splits the hit into `helped` (allies) and
`harmed` (fair game) lists with `Targets`, so friendly fire is impossible by construction. Simple
effects are a few lines inline; bigger ones live in helpers in `Effects` or, for the newer
techniques, in `Techniques`. All spell damage goes through `Effects.hurt`, which skips
invulnerability frames (so stacked effects all land), scales PvP damage, applies Execute, and
records the hit for spell-kill counting.

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

### Visuals: `Vfx`, `TechniqueVfx`, `Fx`

Every visual is vanilla particles and sounds sent from the server, so everyone sees the same show
without a resource pack. `Vfx.Theme` gives each element two colours, a mote, a spark and two
sounds; shapes and effects are built from primitives (`ring`, `radial`, `helix`, `stream`,
`shockwave`).

`Fx.send` is the one place particles leave the server. It **skips any particle that would appear
right in front of a player's own eyes**, which keeps your own spells from blocking your view.
`Fx.sendAll` skips that check (for Heart Circle rings around your chest), `Fx.sendFar` reaches 512
blocks (for a big Domain's dome), and `Fx.quietly(...)` mutes everything inside it (passive
renewals).

## 3. Player state (`player/`)

All per-player state is a Fabric **data attachment** (`WildercordAttachments`): saved with the
player, synced to that player only, and copied through death where noted.

| Attachment | Type | Kept on death | Purpose |
|---|---|---|---|
| `cord` | ItemStack | yes | The worn Cord |
| `spellbook` | `Spellbook` | yes | Learned runes, the four spells, passives and their switches |
| `mana` | float | no | Current mana |
| `cooldowns` | list of long | no (not saved) | When each spell is ready |
| `crystals` | int | yes | Mana Crystals used |
| `circles`, `condensed` | int | yes | Heart Circles and mana condensed |
| `spell_kills`, `boss_slain` | int, bool | yes | Heart Circle breakthroughs |
| `meditating` | bool | no (not saved) | Worked out by the server each tick |
| `spirit_until`, `frozen_until` | long | on the mob | End times for summons and frozen mobs |

`Spellbook` is an immutable record with `withSpell`, `withPassive`, `learn`... Every edit returns a
new instance, which is what makes the attachment save and sync it. Rune ids are kept as strings
even when no such rune is loaded, so removing an add-on never deletes anything.

`Mana.of(player)` computes max mana and regeneration from every source in one place, and runs on
both sides (the server to regenerate, the client to draw the HUD). `Heart` does the same for
circles, breakthroughs and the spell bonuses (`manaCost`, `cooldownTicks`, `upkeep`), so the HUD,
the Cord screen and the server always agree on what a spell costs.

## 4. Heart Circles and passives

- **Condensing**: `SpellCaster.cast` calls `HeartCircles.condense` with the mana spent.
- **Forming**: `HeartCircles.tick` (every 5 ticks, from `SpellCaster`) tells the player once when
  the heart is ready, then, while they meditate, counts up to `Circles.FORM_TICKS`; taking damage
  resets it. `HeartCircles.form` adds the circle, refills mana and plays the breakthrough.
- **Perks**: Mana Skin is an `AFTER_DAMAGE` hook; Flow, Overflow and Archmage are folded into
  `Heart.bonuses`.
- **Spell kills**: `Effects.hurt` records the last spell hit on each creature; an `AFTER_DEATH`
  hook counts a hostile mob that dies within 5 seconds of it.

`PassiveCaster.tick` runs each passive slot the heart has opened: every second it charges the
upkeep (or marks the passive as faltering), and whenever the passive is due it casts it with
`passive = true`. Self passives renew every 2 seconds, quietly after the first time; Orbit and
Stand passives renew when their shape runs out, and their `Cast.alive()` turns false the moment
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

Server-to-client state travels through the synced attachments.

## 6. The client

- **`CordScreen`**: the whole Cord UI, drawn in local coordinates inside a scaled pose so it stays
  crisp at every GUI scale. The header holds the Spells/Passives switch and the heart, mana and
  help badges; below are the rows, the family tabs and search box, category chips (only for
  categories you've learned something in), the Codex, and the plain-English readout. It edits a
  local copy of the spells and sends `EditSpell` / `EditPassive` after each change.
  It turns on SDL text input while open (see the 26.x notes).
- **`SpellHud`**: the panel beside the hotbar: selected spell, its runes and cost, the mana bar
  with a cost mark, cooldown, and passive drain. Laid out by measuring, and shrinks to fit.
- **`WildercordKeys`**: R, V, K and four unbound "cast spell N" keys.
- **Mixins**: the Cord slot is added to the inventory menu on both sides (`InventoryMenuMixin`,
  menu index 46), synced from creative mode (`ServerGamePacketListenerImplMixin`), drawn in the
  survival inventory (`InventoryScreenMixin`) and placed in the creative inventory tab
  (`CreativeModeInventoryScreenMixin`).

## 7. Content: items, loot, effects

- **Items** (`WildercordItems`): one `RuneItem` for every rune (the rune id is a data component,
  `wildercord:rune`, and the item model picks the texture from it), the Blank Rune, the four
  Cords, and the Mana Crystal.
- **Cord tiers** (`CordTier`): sockets, spell slots, highest rune tier, base mana and regeneration.
- **Loot** (`WildercordLoot`): extra pools on vanilla chest tables, weighted by tier (a Tier I rune
  is 8x as likely as a Tier IV one in the same pool), plus mob and boss drops. Only vanilla's own
  tables are touched, so a datapack that replaces a table keeps full control of it.
- **Effects and potions** (`WildercordEffects`): Clarity and Mana.
- **Enchantments**: data-driven JSON under `data/wildercord/enchantment/`, read in code through
  `Mana.enchantLevel` with the keys in `Mana` and `Heart`.

## 8. The asset pipeline (`tools/`)

Nothing under `src/main/resources/assets` or `data` is edited by hand except the mod metadata and
mixin configs. `python tools/generate_assets.py` rebuilds it all from the code:

1. **Reads the roster** by parsing the `shape(...)`, `effect(...)`, `modifier(...)`, `link(...)`
   lines in `Runes.java`.
2. **Textures**: rune icons from `item_art.py` (animated for Tier III-IV), Cords, badges; GUI and
   HUD sprites from `gui_art.py`.
3. **Item models**: a `select` on the `wildercord:rune` component picks each rune's model.
4. **Language**: rune names and descriptions from `Runes.java`, UI strings from the `lang` dict,
   and each rune's "Craft:" and "Found:" tooltip lines.
5. **Recipes and recipe-book unlocks**: from `RUNE_RECIPES` plus the tier costs in
   `TIER_CATALYSTS`; it asserts that every Tier I-III rune has a recipe and no Tier IV rune does.
6. **Mana data**: enchantments, brewing recipes, the Mana Crystal recipe.
7. **`docs/RECIPES.md`**, including where every rune drops, parsed from `WildercordLoot.java`.

Run `python tools/item_art.py` on its own to render review sheets of every icon into
`build/art-preview/`.

## 9. Testing

- **`src/test`**: JUnit 5 tests for everything in `spell/`: reading rules, attachment, costs,
  cooldowns, Blood Price, Vow, passive rules, circle and enchantment maths. Fast and headless.
- **`src/gametest`**: `WildercordScreenshots` is a Fabric client game test. It builds a world,
  gives the player an Echo Cord and every rune, and:
  - screenshots the HUD and the Cord screen at GUI scales 1-4 and three window sizes;
  - casts every newer rune at husks (an exception anywhere fails the test);
  - asserts real mechanics (Stasis holds every damage type, Reflect, Foresight, Reversal,
    Blood Price, Combo, passives draining mana, forming a Heart Circle);
  - checks that the Cord screen enables text input.

## 10. Rules that keep it safe

- **The server decides everything.** Edits and casts are validated in `SpellCaster`; the client
  only draws.
- **Friendly fire is off.** `Targets.canHarm` / `canHelp` decide every effect's targets, and
  respect the `pvp` game rule. Damage to players is scaled down (`Effects.PVP_DAMAGE`).
- **Every cast has budgets** (creatures, blocks, link depth), and spirits are capped per player.
- **Nothing temporary is permanent.** Summons and frozen mobs carry saved end times; Rampart
  blocks crumble when the server stops and drop nothing when broken; wards aren't saved at all.
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
  `ALLOW_DEATH` / `AFTER_DEATH`, `ServerTickEvents`, `LootTableEvents.MODIFY`,
  `PlayerBlockBreakEvents.BEFORE`, `HudElementRegistry`.
- **Dev client**: Loom's `runClient` loads classes from `build/`; rebuilding while it runs mixes old
  and new classes. The build supports `-PaltBuild` (outputs to `build-alt/`) for compile checks
  while a client is open.
