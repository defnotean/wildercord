# Magic that changes the world

Spells don't just hit creatures: where they land, the world answers. A fight by a pond, in a
meadow, on a lava lake or in the rain plays differently from one on bare stone. In the Cord screen,
a rune that does this says so in its tooltip, in a dark green line under its description (the lang
keys `tooltip.wildercord.world.<interaction>`, from `WORLD_MAGIC_LANG` in `tools/generate_assets.py`):

| Element | Interaction | Tooltip line |
|---|---|---|
| Fire | `IGNITE` | Where it lands: sets grass and leaves alight, lights candles and campfires, melts snow and ice, boils water into blinding steam. It lights TNT too: stand clear |
| Frost | `FREEZE` | Where it lands: freezes water into ice you can walk on, cools lava into a crust that melts back after 25 seconds, puts out fires, campfires and candles |
| Storm | `CONDUCT` | Where it lands: in water, shocks every foe in the same water; scrapes a stage of oxidation off copper and powers lightning rods. It may charge a creeper: careful |
| Wind | `GUST` | Where it lands: knocks arrows and fireballs away, blows out small fires and candles, scatters loose items |
| Earth | `HEAVE` | Where it lands: the ground heaves up, throwing foes standing on it |
| Life | `BLOOM` | Where it lands: grass and flowers bloom, crops grow, and a weakened zombie villager starts to be cured |
| Void | `DRAW` | Where it lands: draws loose items and experience in; an enderman struck can't teleport for 5 seconds |
| Time | `AGE` | Where it lands: crops and saplings grow, copper weathers, young animals grow up faster, and a furnace jumps ahead in its smelting |
| Arcane | `SHIMMER` | Where it lands: bookshelves and enchanting tables shimmer, and invisible creatures show for 3 seconds |
| Blood | `FEED` | Where it lands: nether wart and crimson fungus grow |

Only **harmful** spells of an element do this (Fireward won't burn the grass round your friend),
except Life and Time, whose helpful spells do it too (`WorldRules.of`). Runes that already change
blocks their own way (Grow, Icepath, Smelt and the other World runes) are left as they are, and so
are Bubble, Root, Weigh, Shackle, Kindling, Stasis and Rewind (`WorldRules.QUIET`).

The code is `cast/WorldMagic` (called from `Effects.apply` after every effect), with every rule and
number in `spell/WorldRules` (pure, unit-tested in `WorldRulesTest`) and the game tests in
`WildercordWorldMagicTest` and `WildercordWorldMagic2Test`.

## What each element does

**Fire** (Fire, Explode, Meteor, Inferno, Ember, Flashfire, Primer...)
- Sets up to **3** fires within **2 blocks** of where it lands, on or beside anything that burns:
  grass, flowers, leaves, logs, wool, planks. These are ordinary fires, so they spread and burn out
  by vanilla's rules. No fire is lit where fire can't spread (the `fire_spread_radius_around_player`
  game rule), since it would never go out there.
- Lights up to **4** candles, candle cakes and unlit campfires (`KINDLE_MAX`, not waterlogged ones)
  in the same reach, and **primes TNT** there (`TntBlock.prime` with the caster as its source, so the
  `tnt_explodes` game rule still applies). The TNT is the caster's risk: the tooltip and the wiki say so.
- Melts up to **6** snow layers, blocks of powder snow and ice (ice turns to water).
- Landing in water, it boils: a cloud of **steam** hangs for **4 seconds**, **2 blocks** across,
  hiding what's behind it. Anyone you could harm inside is blinded for 2 seconds, again each second,
  and left wet. A puddle of **4 water blocks or fewer** boils away completely; a pond just steams.
  Two steam clouds per cast at most.

**Frost** (Frost, Freeze, Chill, Icicle, Coldsnap)
- Freezes the surface of water within **2.5 blocks** into frosted ice, **16 blocks** at most, even
  when the spell sank to the bottom (up to 4 blocks deep). Widen reaches further (up to 5 blocks)
  only when it attaches to the frost effect itself, which only frost effects with a radius take
  (Coldsnap, say); a Widen on the spell's shape leaves the freeze as it is. You can walk on the
  ice. It melts in the light like Frost Walker's ice, and anything still standing after **30 seconds**
  melts back anyway, even in the dark or at night. The thaw is saved with the world: if the server
  stops, or nobody is near when the time comes, the ice melts as soon as its chunk is loaded again,
  so frozen water always comes back. Never around a creature swimming there.
- Puts out up to **6** fires, lit campfires and lit candles nearby.
- **Cools lava into a crust.** Source lava with open air above, in the same reach as the freeze,
  becomes **basalt**, **12 blocks** at most (`CRUST_MAX`), never where a living creature's box touches
  the lava block. Each block is written down in `TemporaryBlocks` (placed basalt, replaced the lava
  state, due in `CRUST_TICKS` = 500 ticks plus up to 20). `CRUST_WARN_TICKS` (100) before it's due,
  the basalt turns to **magma** (re-recorded as magma, so a crash from then on still melts it) and
  crack overlays (`destroyBlockProgress`, a negative id per block) spread over it every second;
  magma burns to stand on, the warning to get off. At its time it melts back into the lava it was.
  A scheduled task does all this; `TemporaryBlocks` is the safety net for a crash or an unloaded
  chunk (it melts it 40 ticks past due, as the chunk loads, without the warning). The crust is one
  of `Effects.isTemporary`'s blocks (`WorldMagic.isCrust`, which reads the saved record): pistons
  can't push it and `Casters.mayEdit` refuses it, so no other spell touches it. A crust block broken
  by a player (`PlayerBlockBreakEvents.BEFORE`, registered in `WorldMagic.init`) drops nothing: the
  lava comes back. The record is saved, so this holds after a restart too. An explosion breaking a
  crust block drops it as any explosion would (the lava isn't put back); basalt and magma are cheap
  and renewable, so that isn't worth a mixin.

**Storm** (Lightning, Shock, Thunderclap, Ripple, Jolt)
- Landing in water, the shock runs through all the water joined to it, up to **8 blocks** away,
  striking up to **8** creatures in it (not those the spell already hit) for **4 damage** × the
  spell's power within 3 blocks, fading to **2.4** at the edge. **3** conductions per cast at most.
- Storm on a wet target sets off **Conduct**: +50% damage, arcing to two more enemies.
- In a cube **1 block** out from where it lands (`STORM_REACH`): up to **4** copper blocks lose one
  stage of oxidation (`WeatheringCopper.getPrevious`, as `LightningBolt` does; waxed copper isn't
  `WeatheringCopper`), then up to **2** unpowered lightning rods pulse for 8 ticks. The pulse is done
  by hand (power, update the attached block's neighbours, schedule the switch-off), not through
  `LightningRodBlock.onLightningStrike`, whose `LightningRodBlockMixin` would turn Blank Runes by the
  rod into Lightning: that takes a real storm. Lightning and Tempest (`WorldRules.callsLightning`)
  summon a visual lightning bolt, whose vanilla strike already scrapes and powers what it lands on,
  so they skip this.
- Each creeper the spell strikes has a **25%** chance (`CREEPER_CHARGE_CHANCE`) of being charged
  (`CreeperAccessor`: the powered flag, without `thunderHit`'s fire and 5 damage). A player's spell
  only, and not with `features.world_changing_magic` off.

**Wind** (Push, Launch, Dash, Levitate, Windcut, Cyclone, Repel)
- Knocks up to **8** projectiles in flight within **3.5 blocks** back the way the wind blows:
  arrows, tridents, fireballs, even another caster's bolts. Never yours or an ally's.
- Blows out up to **4** fires and lit candles, and scatters up to **16** loose items and experience orbs.

**Earth** (Tremor, Aftershock, Pelt)
- Heaves **5** slabs of the ground up round where it lands (**2 blocks** out), which settle back
  after a second. They're only a picture of the ground: no block is moved.
- Throws foes standing within 2 blocks upward (0.45, more for a stronger spell).

**Life** (Heal, Regrowth, Venom, Restore, Haven...)
- Up to **3** flowers and tufts of grass spring up within **2 blocks**, on grass or dirt, and a crop
  or sapling it lands on grows a stage, like bone meal.
- A zombie villager it strikes that has Weakness starts converting (`ZombieVillagerAccessor`'s
  `startConverting`, 3600 to 6000 ticks as a golden apple gives, with the caster as the one who
  cured it). A player's spell only, and not with the switch off.

**Void** (Pull, Wither, Gravity Well, Hollow, Banish...)
- Draws up to **16** loose items and experience orbs within **4 blocks** in toward where it lands.
- An enderman it strikes (one the caster may harm) is **anchored** for **100 ticks**
  (`ANCHOR_TICKS`): `EndermanMixin` fails `Enderman.teleport(x, y, z)`, which every way it
  teleports goes through. A dark ring turns at its feet every 10 ticks while it holds. Kept in
  memory only (it lasts seconds); cleared when the server stops.

**Time** (Countdown, Accelerate, Foresight, Timesteal, Reckoning, Chronoshift, Borrowed Time)
- Up to **4** babies (`AgeableMob.canAgeUp`, so not one age-locked with a golden dandelion) within
  **3 blocks** grow **120 seconds** older (`WorldRules.agedBaby`, straight onto their age, so no
  breeding cooldown comes with it). A player's spell only, not from a passive, not with the switch
  off; each counts toward the cast's 24 world changes (not its block budget).
- Up to **3** blocks within **2 blocks** age: a `CropBlock` one stage, a sapling `advanceTree` (a stage,
  or into a tree), sweet berry bushes, cocoa and the other `minecraft:crops` (stems, pitcher crops) a
  bone meal, and `WeatheringCopper` one stage on.
- Up to **2** furnaces, smokers or blast furnaces within a block, lit and with something to smelt,
  jump **100 ticks** ahead (`WorldRules.furnaceSkip`: never onto the last tick, which the furnace
  finishes itself so the result goes where it belongs, and never past its fuel, which burns as far;
  `AbstractFurnaceBlockEntityAccessor`).

**Arcane** (Harm, Smite, Silence, Reveal, Starfall...)
- Changes no blocks. Up to **8** invisible creatures the caster could harm within **4 blocks** get
  Glowing for **60 ticks**; bookshelves (`enchantment_power_provider`), chiseled bookshelves and
  enchanting tables within 3 blocks (up to 8) shimmer. Works for monsters too.

**Blood** (Cleave, Leech, Bleed, Lifesteal, Crimson Mist...)
- Up to **3** nether wart (one stage riper) and crimson fungi (a bone meal, so sometimes a huge
  fungus) within **2 blocks**.

## Wet

A creature is wet while it's in water or rain, and for **5 seconds** after Tidebreath, a popped
Bubble or a steam cloud. On a wet creature:
- **storm** conducts (+50%, arcing to two more),
- **fire** hits **25% softer**, and dries it,
- **frost** freezes it solid at once, for at least 3 seconds, even a light Chill, ready to Shatter.

## Feats

- **Conductor**: shock five creatures at once through the water they stand in.
- **Icebridge**: walk across water you froze with a spell, before it thaws.

## Rules

- Only a player's spells change blocks, and only where they may build: every block change goes through
  `WorldMagic.edit`, so `Casters.mayEdit` (spawn protection, claims offered the change as a break,
  Adventure mode, and never a spell's own temporary block), the cast's block budget
  (`casting.max_blocks_per_cast`, 32 per strike) and the per-cast allowance (**24** world changes a cast,
  links and echoes included). A server config with `features.world_changing_magic` (or
  `casting.spells_edit_blocks`) set to false stops them. A monster's spells never change blocks, but still
  conduct through water, heave the ground, blow arrows away, anchor endermen and show the invisible.
- Changes to creatures that outlast the spell (a charged creeper, a cure begun, a baby grown) come only
  from a player's spell, and not with `features.world_changing_magic` off.
- A passive renewing itself changes no blocks (and grows no babies).
- A spell cast on yourself with Self changes nothing round you.
- Every change is vanilla's own and temporary or natural: fire, frosted ice, a crust on lava that melts
  back, grass and flowers, growth and copper's weathering.
