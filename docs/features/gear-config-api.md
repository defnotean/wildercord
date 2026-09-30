# Casting gear, the server config and the add-on API

Three pieces that landed together: a gear chase alongside Cords, a config file for server owners, and a
stable API for add-on mods.

## Casting gear

Items you wear in the inventory's gear slots (or, while a slot is empty, hold) when you cast. The server
reads your slots and hands at the moment of casting; the Cord screen's readout shows what your gear does to
the spell being edited (its cost line already includes it), and the mana badge lists everything that counts.
See [Gear slots](#gear-slots) below.

| Item | Slot (held instead) | What it does | How to get it |
|---|---|---|---|
| **Elemental staff** (Fire, Frost, Storm, Wind, Earth, Life, Void, Arcane, Time, Blood) | Staff (either hand) | Effects of its element: +20% power. A spell with an effect of its element: 10% less mana. A charged cast (20% or more) of its element leaves it with the element's flourish | Craft: 2 sticks (Fire: 2 blaze rods), 2 of the element's material, a Mana Crystal |
| **Greater staff** (every element) | Staff (either hand) | As a staff, at +35% power; set in gold, with a halo of sparks | The Archivist (50%), the Wither (Void, Blood), the Warden (Earth, Storm), the Elder Guardian (Frost, Life) and the Ender Dragon (Void, Arcane, Time), 35-50%; Archive vaults |
| **Tome of the Fifth Page** | Tome (off-hand) | A fifth spell: thread it, select it (V steps on to it) and cast it while the tome counts. The Cord screen shows its row | Archive vaults and libraries, stronghold libraries |
| **Focus of Haste** | Focus (off-hand) | Charged casts fill 40% faster | Craft: Mana Crystal, feather, 2 sugar, gold ingot; Archive, strongholds, ancient cities, mansions |
| **Focus of Thrift** | Focus (off-hand) | Spells cost 15% less mana, and hit 10% softer | Craft: Mana Crystal, emerald, 3 gold ingots; same places |
| **Focus of the Deep Well** | Focus (off-hand) | +50 max mana | Craft: Mana Crystal, lapis block, 3 polished deepslate; same places |
| **Focus of Echoes** | Focus (off-hand) | A 10% chance a spell echoes: it goes off again half a second later, free | Craft: Mana Crystal, echo shard, 3 amethyst shards; same places |
| **Focus of Resolve** | Focus (off-hand) | Incoming spells hurt 20% less, but your spell effects are 15% weaker | Craft: Mana Crystal, 2 amethyst shards, 2 iron ingots; same places |

Staff materials: Fire blaze powder, Frost packed ice, Storm a lightning rod, Wind wind charges, Earth
mossy cobblestone, Life glistering melon, Void ender pearls, Arcane amethyst shards, Time clocks, Blood
nether wart.

Rules: the same piece twice counts once; two staffs never discount one spell twice (the better
counts); a staff's power only reaches effects of its element, Thrift's reaches every effect. Gear is its
own factor on cost and power, applied after everything else (Heart Circles, enchantments, charge).

Spell 5 is the tome's slot whatever your Cord: without the tome it's locked (its runes are kept), and a
"Cast spell 5" key can be bound.

## Gear slots

Every player has three gear slots in their inventory, after the Cord slot: **Staff** (any staff, greater
ones too), **Focus** (any focus) and **Tome** (the Tome of the Fifth Page). One piece each; only the kind a
slot is for goes in.

- **The rule.** A piece in its slot works with nothing held, and *takes the place of* held pieces of its
  kind: a staff in the staff slot and staffs in the hands, only the slotted one counts; a focus in the
  focus slot, a held focus is ignored; a tome in the tome slot, a held tome is ignored. A slot that is
  empty lets held gear work exactly as before (staffs in either hand, foci and the tome in the off-hand),
  so nothing that worked stops working. The slots are separate, so a tome and a focus now apply together.
- **Where.** In the survival inventory the slots sit in a tray on the panel's top edge, above the armour and
  the paper doll (the recipe book button and the crafting grid leave no room beside the Cord slot); the
  tray moves with the window when the recipe book opens, and a click on it isn't a click outside the window
  (which would throw what the cursor carries). In the creative inventory's Survival Inventory tab they're a
  block beside the Cord slot. Shift-click sends a piece to its slot (if empty) and back out; number keys swap.
- **Storage.** The `wildercord:gear` attachment (slot id to piece), saved with the player and synced to
  everyone who can see the wearer. It is not `copyOnDeath`: on death the pieces drop like the inventory
  (Curse of Vanishing destroys its own) unless `keepInventory` is on, when they go with the player. A piece
  is never in two places: the slots are emptied as they drop, and the respawned player only receives what
  the old one still had (`ServerPlayerEvents.COPY_FROM`).
- **On the wearer.** Everyone sees it, and it shows in the inventory's paper doll: a staff strapped across
  the back with its head over the right shoulder, a focus hovering off the left shoulder (bobbing and
  turning, with a faint glimmer circling it), the tome hanging from a belt at the left hip. Each is drawn
  with the item's own model. Nothing is drawn for an invisible or spectating wearer, or for a piece that is
  also held in a hand (no second staff on the back); the back and the belt stand clear of a chestplate,
  a cape and elytra, and follow a sneak.
- **Adding a slot.** `gear/GearSlot.java` is the list: one line gives a kind of gear its slot (its id, the
  `GearKind`s that fit, and a `Look` for the wearer). The menu slot, its place on both inventory screens
  (`GearLayout`), the empty-slot icon (`container/slot/gear_<id>`, drawn in `tools/gear_art.py`), its
  hover text (`gear_slot.wildercord.<id>` and `.hint`) and the rule all follow from that line; a new
  `Look` needs a case in `client/render/GearLayer`. The survival tray grows with the list; the creative
  block holds four.
- **The API.** `dev.wildercord.gear.GearSlots` reads and changes a wearer's slots (`get`, `equipped`,
  `set`, `clear`, `slotFor`, `fits`; reads work on any avatar, a mannequin too), and
  `WildercordApi.gearSlots()`, `equippedGear`, `equipGear`, `unequipGear` and `gearSlotFor` expose the
  same to add-ons (see [API.md](../API.md#casting-gear)). Writes belong on the server.

## Server config

`config/wildercord.json` is written with every default the first time the server starts, and read again by
`/wildercord reload` (operators), which lists anything wrong with the file. A missing or broken value
falls back to its default and a value out of range is clamped, each with a warning, so a bad edit never
stops a server. Every default is the number the mod used before.

| Setting | Default | What it does |
|---|---|---|
| `casting.max_creatures_per_cast` | 64 | Creatures one cast may touch (links and echoes included; repeating shapes per strike) |
| `casting.max_blocks_per_cast` | 32 | Blocks one cast may change |
| `casting.spells_edit_blocks` | true | Whether spells may change blocks at all |
| `casting.pvp_damage_scale` | 0.6 | Rune damage between players |
| `mana.regen_multiplier` | 1.0 | Mana regeneration (the HUD and mana badge show it) |
| `mana.cost_multiplier` | 1.0 | Every spell's mana (and Blood Price) cost, and passive upkeep (the readout shows it) |
| `world.runebound_chance_multiplier` | 1.0 | How often monsters spawn as Runebound |
| `loot.rune_chance_multiplier` | 1.0 | Runes in structure chests and from mobs (a boss's sure drop stays sure) |
| `loot.crystal_chance_multiplier` | 1.0 | Mana Crystals in structure chests |
| `loot.page_chance_multiplier` | 1.0 | Torn Pages in structure chests |
| `loot.gear_chance_multiplier` | 1.0 | Casting gear in chests and from bosses |
| `imbuing.max_items` | 6 | Imbued items one caster keeps |
| `imbuing.max_glyphs` | 12 | Glyphs one caster keeps, over every dimension together |
| `features.world_events` | true | World events (mana storms, fallen stars, rift sieges). When off, none starts, not even from `/wildercord event`; one already under way runs its course |
| `features.duels` | true | Switch for duels |
| `features.wild_magic` | true | Switch for wild magic |
| `features.world_changing_magic` | true | Magic that changes the world (fire lighting grass, frost freezing water, life making it bloom...). When off, spells change no blocks this way, though the steam, shocks through water and gusts still come |
| `features.creature_affinities` | true | Creature affinities: weaknesses (+50%), resistances (half) and immunities to elements, the callouts and the Bestiary (see [affinities.md](affinities.md)). When off, every creature takes every element alike, and vanilla's fivefold freezing damage on blazes, striders and magma cubes comes back |
| `features.elemental_climate` | true | Elemental climate: where a player casts nudges each element's damage (the Nether, a thunderstorm, snow...). When off, it's the same everywhere and the HUD shows no climate marks |
| `travel.enabled` | true | The travel commands (`/home`, `/warp`, `/waypoint`, `/tpa`, `/back`, `/spawn`, `/rtp`). Read when commands are built (start, `/reload`); the commands also refuse at once after `/wildercord reload` turns it off |
| `travel.max_homes` | 3 | Homes per player (0 turns homes off) |
| `travel.warmup_seconds` | 3 | Standing still before a teleport (operators and creative players skip it) |
| `travel.cooldown_seconds` | 30 | Before `/home`, `/warp`, `/spawn`, `/back` or `/tpa` works again (each its own; operators skip it) |
| `travel.rtp_cooldown_seconds` | 300 | Before `/rtp` works again |
| `travel.rtp_radius` | 5000 | How far from world spawn `/rtp` may land, in blocks |
| `travel.tpa_timeout_seconds` | 60 | How long a teleport request waits for an answer |
| `defence.spellguard` | true | The spellguard: one spell hit can't take a player from high health straight to dead; it leaves them on one heart (and for the rest of that spell, half a second, no spell finishes them), then recharges. Goes before totems, Reversal, Rebirth, Second Wind and a duel's knockout, so none is spent on it; never against `/kill` or the void |
| `defence.spellguard_health` | 0.8 | The share of full health a player needs for the spellguard to hold |
| `defence.spellguard_recharge_seconds` | 60 | How long the spellguard takes to come back after it holds (counted from when it held, so a shorter value applies to one already recharging) |
| `defence.max_bonus` | 2.5 | The most a hit's bonuses together (Execute, Trial Key, reactions, hexes, Decree, Drowse, Veil, Fortune, affinities, add-on reactions...) may multiply a spell against a player; creatures are uncapped |
| `defence.armour_rate` | 0.55 | How much of armour's worth against a blade counts against spells that bypass armour (magic, frost, sonic boom), players only; vanilla's armour formula times this |

`world_events`, `world_changing_magic`, `creature_affinities` and `elemental_climate` take effect at once on `/wildercord reload`. The `duels` and
`wild_magic` switches are there for those features to read (`Config.get().duels()` and so on).
The travel settings are described in [travel.md](travel.md). The defence settings are read by `cast.SpellDefence` (the
one path every spell's damage takes; the pure numbers are in `cast.SpellDefenceRules`), and are sent to clients for the
Cord screen's spell-defence badge. Loot chances apply when loot tables load (world start or `/reload`). Structure spacing isn't in the file:
it's data, in `data/wildercord/worldgen/structure_set/archives.json`, which a datapack can override. The
cost and regeneration multipliers are sent to each player on joining and after every reload, so what the
screens show is what the server charges.

## Add-on API

`dev.wildercord.api`, documented in [API.md](../API.md): add-ons implement `WildercordAddon` under the
`wildercord` entrypoint and register runes of every family with their traits and behaviour, Codex
categories and element reactions; listen to `BEFORE_CAST` (can stop it), `AFTER_CAST`, `SPELL_HIT`,
`SPELL_BLOCKED` and `IMBUE_RELEASED`; and read a player's mana, learned runes and spells. An example
add-on lives in the test sources.

## For other features

- **Cost and power**: casting gear is one factor in `Heart.manaCost` / `healthCost` (`gearCost`) next to
  the server's (`serverCost`), and one in `Effects.applyEffect`'s power (`cast.gearPower(element)`).
  Add other multipliers as factors of their own beside them.
- **Bosses**: `GearLoot.bossStaff(random, chance, elements)` rolls a greater staff for a new boss's
  loot.
- **Spell slots**: `SpellSlots` and `Gear.spellOpen(player, tier, spell)` say which spells are open; a
  spellbook now keeps five.
- **Reading gear**: `Gear.of(entity)` (and `Gear.tome(player)`) is the one place that combines the
  slots and the hands; use it, not `getMainHandItem`, for anything gear does.
