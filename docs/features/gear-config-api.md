# Casting gear, the server config and the add-on API

Three pieces that landed together: a gear chase alongside Cords, a config file for server owners, and a
stable API for add-on mods.

## Casting gear

Items you hold while you cast. The server reads your hands at the moment of casting; the Cord screen's
readout shows what your gear does to the spell being edited (its cost line already includes it), and the
mana badge lists everything in your hands.

| Item | Hand | What it does | How to get it |
|---|---|---|---|
| **Elemental staff** (Fire, Frost, Storm, Wind, Earth, Life, Void, Arcane, Time, Blood) | either | Effects of its element: +20% power. A spell with an effect of its element: 10% less mana. A charged cast (20% or more) of its element leaves it with the element's flourish | Craft: 2 sticks (Fire: 2 blaze rods), 2 of the element's material, a Mana Crystal |
| **Greater staff** (every element) | either | As a staff, at +35% power; set in gold, with a halo of sparks | The Archivist (50%), the Wither (Void, Blood), the Warden (Earth, Storm), the Elder Guardian (Frost, Life) and the Ender Dragon (Void, Arcane, Time), 35-50%; Archive vaults |
| **Tome of the Fifth Page** | off-hand | A fifth spell: thread it, select it (V steps on to it) and cast it while the tome is held. The Cord screen shows its row | Archive vaults and libraries, stronghold libraries |
| **Focus of Haste** | off-hand | Charged casts fill 40% faster | Craft: Mana Crystal, feather, 2 sugar, gold ingot; Archive, strongholds, ancient cities, mansions |
| **Focus of Thrift** | off-hand | Spells cost 15% less mana, and hit 10% softer | Craft: Mana Crystal, emerald, 3 gold ingots; same places |
| **Focus of the Deep Well** | off-hand | +50 max mana while held | Craft: Mana Crystal, lapis block, 3 polished deepslate; same places |
| **Focus of Echoes** | off-hand | A 10% chance a spell echoes: it goes off again half a second later, free | Craft: Mana Crystal, echo shard, 3 amethyst shards; same places |

Staff materials: Fire blaze powder, Frost packed ice, Storm a lightning rod, Wind wind charges, Earth
mossy cobblestone, Life glistering melon, Void ender pearls, Arcane amethyst shards, Time clocks, Blood
nether wart.

Rules: the same piece in both hands counts once; two staffs never discount one spell twice (the better
counts); a staff's power only reaches effects of its element, Thrift's reaches every effect. Gear is its
own factor on cost and power, applied after everything else (Heart Circles, enchantments, charge).

Spell 5 is the tome's slot whatever your Cord: without the tome it's locked (its runes are kept), and a
"Cast spell 5" key can be bound.

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
| `imbuing.max_glyphs` | 12 | Glyphs one caster keeps per world |
| `features.world_events` | true | World events (mana storms, fallen stars, rift sieges). When off, none starts, not even from `/wildercord event`; one already under way runs its course |
| `features.duels` | true | Switch for duels |
| `features.wild_magic` | true | Switch for wild magic |
| `features.world_changing_magic` | true | Magic that changes the world (fire lighting grass, frost freezing water, life making it bloom...). When off, spells change no blocks this way, though the steam, shocks through water and gusts still come |
| `travel.enabled` | true | The travel commands (`/home`, `/warp`, `/waypoint`, `/tpa`, `/back`, `/spawn`, `/rtp`). Read when commands are built (start, `/reload`); the commands also refuse at once after `/wildercord reload` turns it off |
| `travel.max_homes` | 3 | Homes per player (0 turns homes off) |
| `travel.warmup_seconds` | 3 | Standing still before a teleport (operators and creative players skip it) |
| `travel.cooldown_seconds` | 30 | Before `/home`, `/warp`, `/spawn`, `/back` or `/tpa` works again (each its own; operators skip it) |
| `travel.rtp_cooldown_seconds` | 300 | Before `/rtp` works again |
| `travel.rtp_radius` | 5000 | How far from world spawn `/rtp` may land, in blocks |
| `travel.tpa_timeout_seconds` | 60 | How long a teleport request waits for an answer |

`world_events` and `world_changing_magic` take effect at once on `/wildercord reload`. The `duels` and
`wild_magic` switches are there for those features to read (`Config.get().duels()` and so on).
The travel settings are described in [travel.md](travel.md). Loot chances apply when loot tables load (world start or `/reload`). Structure spacing isn't in the file:
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
