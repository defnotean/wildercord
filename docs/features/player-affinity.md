# Player affinities

Every player has an affinity with each of the ten elements, grown by what they do and worth a little with
that element: more power, from level III a resistance, at V cheaper spells. The player-facing page is
[wiki/progression/affinity.md](../../wiki/progression/affinity.md); this is how it's built.

| Piece | Where | What it does |
|---|---|---|
| Rules | `spell.PlayerAffinity` (pure, unit-tested in `PlayerAffinityTest`) | Levels and thresholds, what a level gives, every source with its points and daily allowance, the allowance maths (`grant`), reaction elements, the night watch, seeding from old casts |
| Cost shares | `spell.SpellCompiler.elementShares` | Each element's share of a spell's effect costs, priced as `cost` prices them |
| Leaning | `spell.Leaning` | The deepest affinity, at level I or more and 1.25x the runner-up (colour only) |
| Runtime | `cast.PlayerAffinities` | `gain` (allowances, the gain multiplier, level-ups), every hook, the regular check, seeding, the `Rise` payload |
| State | `player.WildercordAttachments.AFFINITY`, `AFFINITY_TALLY` | Points per element (synced to the owner, kept on death); the day's allowances (server only, kept on death) |
| Both sides | `player.Heart.affinity`, `affinityLevel`, `leaning`, and the price in `rawCost` | Read by the server, the Cord screen and the HUD alike |
| UI | `client.CordScreen` (Grimoire section, readout line, heart badge), `client.GrimoireToast.affinity` | |
| Settings | `features.player_affinity`, `affinity.gain_multiplier` | `Config.playerAffinity(player)` answers on both sides (the switch travels in `Config.Sync`) |

## Levels and what they give

| Level | Points | Power (`PlayerAffinity.power`) | Others' spells of it (`damageTaken`) | Price |
|---|---|---|---|---|
| I | 100 | x1.03 | x1 | x1 |
| II | 600 | x1.06 | x1 | x1 |
| III | 1,800 | x1.09 | x0.90 | x1 |
| IV | 4,500 | x1.12 | x0.85 | x1 |
| V | 10,000 | x1.15 | x0.80 | 10% off that element's share |

- **Power** is one factor in `Effects.applyEffect`'s `power` (where leaning's +10% used to be), so it reaches
  every kind of effect (damage, heals, shields, pushes) and everything that takes `power` from there: techniques,
  innate runes, fused effects, add-on runes. `PlayerAffinities.power(cast, element)` is 1 unless the cast
  carries the caster's affinities (`Cast.withAffinity`, copied by `again`): `SpellCaster.cast` (and so its
  echoes, chorus, Twin Star, Focus of Echoes and wild surges), `Imbuing`'s releases and Mirrorfrost. Scrolls,
  passives and a mana storm's surge stay at their plain strength, the scope leaning's bonus always had.
- **Resistance** is in `Affinities.affinity`, the same factor as a creature's affinity (so it's inside the one
  `Affinities.multiplier` line in `Effects.hurt`): for a `Player` target it returns
  `PlayerAffinities.resistance`, 1 when a reaction went off with this hit (`Reactions.reactedWithin(target, 0)`),
  when the caster is the target, or with affinities off. There's no callout. It applies whoever cast the spell,
  and before `pvp_damage_scale`.
- **Price** is a factor in `Heart.rawCost`, next to gear and the server's multiplier: `PlayerAffinity.costFactor`
  on `SpellCompiler.elementShares(root)`. It's read only when some element is at V (`anyMastered`), so most
  players never price a spell's shares. Mana and Blood Price health both use it, `Heart.manaCost`'s "a discount
  saves at least one" rule covers it, and passives' upkeep doesn't (as with gear).

## Growth

Everything goes through `PlayerAffinities.gain(player, source, element, units)`:

1. Nothing for creative or spectating players, a non-element, or with `player_affinity` off.
2. `offered = units * source.points`; the day's tally (`AFFINITY_TALLY`, keyed by `source.tallyKey(element)`,
   reset when `PlayerAffinity.day(overworld game time)` changes) says what it offered already;
   `PlayerAffinity.grant` lets through the rest of its daily allowance in full, and `tail` of anything past it
   (0 for every source but casting, which slows to a tenth). The tally counts what was offered, so the
   allowance measures what was done, not what it was worth.
3. The granted points times `affinity.gain_multiplier` are added: whole points to `AFFINITY`, the fraction kept
   in memory per player and element (like `HeartCircles`' condensing; lost on leaving, always under one).
4. Each level crossed calls `rise`: a chat message saying what it gives, two chimes, the `Rise` payload (the
   client shows `GrimoireToast.affinity`, whose token is `GrimoireToast.token(element, level)`), and at level I
   `Grimoire.unlock(player, "affinity:<element>", false)`: the entry condenses `PlayerAffinity.REWARD` (100) with no
   second toast.
5. If the leaning changed, the leaning message and, the first time, the Leaning feat.

Game days are the overworld's game time over 24,000, which sleeping doesn't advance, so a bed never resets an
allowance.

### Sources

| Source | Element | Points per | Daily | Hook |
|---|---|---|---|---|
| `cast` | any | mana spent on its effects (0.1) | 100, then 10% | `SpellCaster.cast` → `onCast` (spent x `elementShares`) |
| `reaction` | its elements | reaction set off (3) | 45 | `Grimoire.reaction` → `reaction` (one a second) |
| `bestiary` | the weakness's | new weakness (25) | 100 | `Grimoire.unlock` → `discovered` |
| `smelt` | fire | item taken (0.25) | 30 | `FurnaceResultSlotMixin` (`checkTakeAchievements`) |
| `fire_kill` | fire | kill while burning or by fire (2) | 30 | `AFTER_DEATH` (killer: attacker, else kill credit) |
| `burning` | fire | check on fire, no Fire Resistance (1) | 15 | check |
| `lava` | fire | check within 3 blocks of lava in the Nether (0.5) | 15 | check |
| `fish` | frost | fish caught (3) | 30 | check: `Stats.FISH_CAUGHT` |
| `cold` | frost | check in `Climate` SNOW, or on `#ice` (0.5) | 15 | check |
| `frozen` | frost | check fully frozen (1) | 15 | check |
| `thunder` | storm | check in `Climate` THUNDER (1) | 30 | check |
| `struck` | storm | real lightning survived (40) | 80 | `AFTER_DAMAGE` (lightning, no attacker; once per 2 s) |
| `rod` | storm | rod struck within 16 blocks (8) | 24 | `LightningRodBlockMixin` |
| `glide` | wind | 100 blocks of elytra flight (1) | 30 | check: `Stats.AVIATE_ONE_CM` |
| `fall` | wind | fall of 6+ damage survived (4) | 20 | `AFTER_DAMAGE` |
| `heights` | wind | check above y 200 in the Overworld (0.5) | 15 | check |
| `stone` | earth | `#base_stone_overworld`/`_nether` with the right tool (0.1) | 15 | `PlayerBlockBreakEvents.AFTER` |
| `ore` | earth | `#c:ores` with the right tool (1) | 30 | `PlayerBlockBreakEvents.AFTER` |
| `deep` | earth | check in `Climate` DEEP (0.5) | 15 | check |
| `harvest` | life | ripe `CropBlock` or nether wart (0.25) | 25 | `PlayerBlockBreakEvents.AFTER` |
| `breed` | life | animals bred (3) | 30 | check: `Stats.ANIMALS_BRED` |
| `heal` | life | health restored to another by a spell (0.25) | 30 | `LivingEntityHealMixin` (`Effects.applying()`) |
| `tame` | life | animal tamed (10) | 20 | `TameAnimalTriggerMixin` |
| `end` | void | check in `Climate` END (0.5) | 20 | check |
| `void_kill` | void | enderman, endermite or shulker killed (2) | 30 | `AFTER_DEATH` |
| `pearl` | void | ender pearl thrown (1) | 15 | check: `Stats.ITEM_USED` ender pearl |
| `deep_dark` | void | check in the deep dark biome (1) | 20 | check |
| `enchant` | arcane | item enchanted (5) | 30 | check: `Stats.ENCHANT_ITEM` |
| `meditate` | arcane | check meditating (0.5) | 20 | check |
| `page` | arcane | new riddle read (10) | 30 | `Grimoire.unlock` (`hint:`) → `discovered` |
| `rune` | arcane | rune learned (15) | 60 | `RuneItem.use` → `learnedRune` |
| `ley` | arcane | check on a ley line (0.5) | 20 | check |
| `night_watch` | time | a night 4/5 out under the night sky (20) | 20 | check (`NIGHT` counted, paid at the first day check) |
| `age` | time | thing aged by time magic (1) | 20 | `WorldMagic.age` → `aged` |
| `clock` | time | check with a clock in hand (0.25) | 10 | check |
| `melee_kill` | blood | kill by `player_attack` from the killer (2) | 30 | `AFTER_DEATH` |
| `blood_price` | blood | health paid (1) | 40 | `SpellCaster.cast` → `onCast` |
| `heavy_hit` | blood | 8+ damage from an attacker, survived (3) | 21 | `AFTER_DAMAGE` |

The **check** runs every `PlayerAffinity.SAMPLE_TICKS` (100) ticks per player, each player on a tick of their
own (`(tick + uuid hash) % 100`), so a full server's checks are spread out. It reads `Climate.conditions` (cached
per second anyway), a few block and biome lookups (the lava search only in the Nether: 7x5x7), and the game's own
statistics for what vanilla already counts, so fishing, breeding, enchanting, pearls and elytra flight need no
mixins at all. The first check after joining only takes note of the statistics.

Other anti-farm details: kills of players never count; a hopper emptying a furnace counts for nobody (the slot
hook only fires for a player taking by hand); lightning from a spell has its caster behind it and doesn't count;
stone and ores need the right tool; the reaction hook counts one reaction a second, so an area spell setting off
twenty Shatters is worth one.

## Leaning

`Leaning.of(points)` is the element with the most affinity points, at level I or more and at least 1.25x the
runner-up; `Heart.leaning` returns "" with affinities off. It colours the Heart Circles' rings and a charging
circle with no effect of its own, and earns the Leaning feat, but adds no power: its old +10% on one element is
the affinity's own levels now. `SpellCaster` no longer counts `element_casts`; the attachment stays registered so
old saves keep it, and `PlayerAffinities.seed` (on join, once: until `AFFINITY` exists) turns it into a start,
`PlayerAffinity.seed` (2 points a cast, never past level II), writing each first level into the Grimoire quietly.

## Config

```json
"features": { "player_affinity": true },
"affinity": { "gain_multiplier": 1.0 }
```

Both are in `WildercordConfig.KEYS` and `toJson`, so `addMissing` adds them to an older file. With the switch off,
`gain` earns nothing, `power` and `resistance` are 1, `Heart`'s price factor is 1 (the client reads the synced
switch), and `Heart.leaning` is empty. The multiplier (0 to 100) scales what's granted, not the allowances.

## Tests

- `src/test/.../PlayerAffinityTest`: thresholds and levels, power/resistance/price, the cost shares, the
  allowances (a 400-block stone farm earns exactly the day's 15), every source sensible, every reaction's
  elements, the night watch, seeding, the Grimoire reward, leaning.
- `WildercordConfigTest.playerAffinitiesHaveASwitchAndAPace`.
- `src/gametest/.../WildercordPlayerAffinityTest`: casting, mining with the cap, the level-up toast and entry,
  power on the right element only (and not on a scroll's cast), resistance from III against a Runebound (and a
  Shatter through it), the switch; screenshots `affinity_toast` and `affinity_grimoire`.
