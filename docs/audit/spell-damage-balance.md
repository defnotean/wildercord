# Spell damage audit — 2026-10-03

## Confirmed reproduction

A real Survival Echo Cord accepts Touch, nine Vows and Harm (11 sockets). The original Vow
rule was 2^count power at unchanged mana price; cooldown clamps at 60 seconds while power
continues growing. Native baseline depleted all 1,000 dummy health in one paid hit below 200
mana. Base Harm/Touch/Vow arithmetic requests 7 × 1.3 × 512 = 4,659.2 before caster bonuses.
The dummy records its available health, not uncapped attempted damage.

The matching Bleed recipe requests an initial 2 × 1.3 × 512 plus eight 1 × 1.3 × 512 ticks:
6,656 before caster bonuses or movement. This explains how the same stack can exceed 5,000
over time. The original Bleed total is calculated from the production routine, rather than a
second native baseline. Execute grows by 2 for a 1.3 price multiplier; Focus by 1.5 for 1.2;
Trial Key by 1.6 for 1.3. Extend doubles duration for 1.4 mana, increasing damage-ticker work.
Knot expansion can put far more modifiers into a spell than its visible socket count suggests.

## Changes

- Compiler limits per target rune: Vow/Execute/Trial Key one, Focus two, Extend three. Duplicate
  warnings and unattached markers keep the creation screen honest; ignored copies add no cost.
  Pure numeric helpers also cap manually constructed plans.
- A Paid object owns a single SpellDamageAllowance. Children, pulses, repeats, again(), circle
  effect views and reflections keep it. It is per target UUID and never evicts old targets to
  restore allowance. Bookkeeping admits at most 256 distinct targets per payment.
- Player-owned Cast damage: maximum 96 per admitted hit; lifetime allowance min(512,12+2*mana).
  Cord casts provide their effective price after discounts and before refunds. Health-paid/free
  casts use the equivalent effective mana price. Other player Cast paths lazily use their plan
  weight; monster-only spells keep their existing difficulty scaling.
- Effects reserves the allowance before hit callbacks, screen punch, mastery and follow-up damage;
  SpellDefence also reserves it for direct Cast-aware damage. An internal admitted path prevents
  double charging. Old copies and delayed effects hold the same Paid object.
- Production Venom exits its scheduler when no allowance remains. Existing-world spell lists and
  Knots still load; extra modifiers become ineffective with a warning. No migration is required.

## Verification and practical scope

Native before: 1,000 dummy health removed by the paid nine-Vow cast. Native after: 18.2 Harm,
26 Bleed over its entire actual sequence, and a production Venom scheduler with exaggerated tick
power admitting exactly 212 damage at a 100-mana price then stopping. The same suite verifies
children/pulses/repeats/copies/reflections together do not renew the allowance; a new payment
can hurt again. Unit tests cover every integer price below 200, malformed/fractional amounts,
per-hit/whole-cast limits, separate targets/payments, bounded maps and Knot modifier expansion.

This is a deliberate reduction of extreme damage builds. Area spells keep a separate allowance
for each target. Healing/control and terrain interactions are not charged against damage. Damage
prevented by armor/resistance still uses the raw allowance. External add-ons directly calling
vanilla hurtServer without the Cast damage path, independent familiar/creature bites, accumulated
damage from many paid casts released by Stasis, and environmental hazards are separate systems.
This evidence does not claim every possible mod combination or multiplayer balance is exhausted.

Final verification: damage suite 39s, parry 53s, defense 43s; full build 15s, 963 tests, zero failures/errors/skips.
114 guide pages; CRC clean; 4864 resources/1632 classes byte-match.
SHA-256: a6155f8b12c975637ac2641e9248adf6e785a4efc11f7823bf906155794e9096. Four original inspected captures and logs in artifacts/review/spell-damage-balance.
