# Ordinary encounter pressure audit

Base: ebb2887f7d7becccf339918c00a9741721606fb1. This is a source audit, not playtest evidence.

## Observed budgets
- Gloomstalker: 24 health, 4 attack, 6 pounce damage; initial stalking 40–79 ticks and 50–99 ticks after retreat. Signature pounce requires distance 3.2–9.5 and line of sight. Beyond 10 blocks it uses ordinary navigation at 1.1 speed.
- Bog Witch-Frog: 32 health, 5 attack, 2 tongue damage; a bubble reserves an 80–129 tick interval. It retains readable swelling and mouth-open tells.
- Geode Crawler: 26 health, 4 attack, 6 roll damage; its curled defense already rewards cracking tools/elements instead of uniform damage.
- MonsterSettings currently configures spawn rate and species enablement, not encounter rank or advanced threat budgets.

## Implications
The requested 100-crystal and 20-circle ceilings expand player resources and power far beyond these early-game budgets. A higher ceiling does not make every player advanced, so increasing every spawn's damage to endgame levels would damage early progression. Health increases alone do not fix navigation, attack uptime, spell pressure or coordinated enemy behavior.

## Next bounded implementation proposal
1. Retain beginner creatures and teaching duelists. Add explicitly recognizable veteran/elite encounters in dangerous locations or opt-in trials.
2. Reuse species tells and weaknesses, but add a second attack or a reposition response after repeated ranged damage. Never let the response erase a recovery window or damage through cover.
3. Apply rank once when the encounter begins; do not heal, reroll rewards, or silently downscale when targets disconnect or change equipment.
4. Keep damage, health, posture and behavior tuning separate. Cap ordinary movement buffs; faster traversal must not remove telegraphs.
5. Reward risk through mastery materials and technique access, rather than increasing player damage at exactly the rate enemy health rises.

## Required playtest matrix
- Early leather/iron melee and basic Cord, midgame Aura/caster, Protection IV netherite endgame.
- Stationary casting, backpedaling, pillar/roof/doorway exploitation, cover, aerial casting, melee trading, timed parry/dodge, mixed-party flank.
- Measure first successful enemy hit, time spent unable to path, damage after full mitigation, unavoidable damage, time to kill, stagger uptime and resource recovery.
- Compare 1, 2, 4 and 8 consenting Master challengers. Keep baseline ordinary fights independent of Master roster scaling.

## Scope status
The initial Masters of Tomorrow code adds encounter and animation foundations. A complete normal-mob/boss ecosystem rebalance remains unverified and must not be advertised as completed based on these source observations.
