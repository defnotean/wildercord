# Cinnamon

Cinnamon is a custom small dog with her own model, texture, owner and saved identity.

See the [complete player guide](../../wiki/companions/cinnamon.md) for owner configuration, her bow and toy, temporary feeding growth, mobile recovery, whistle recipe and bounded chunk recovery.

- Set one owner in `config/wildercord-cinnamon.json`; malformed reloads retain the complete last valid configuration.
- **Party Lick Healing:** When Cinnamon's owner or a same-party teammate drops below 65% health within 18 blocks, Cinnamon moves/flies in to lick-heal them (6 HP normal, 10 HP when big or wearing the Grand Bow) plus 4s Regeneration I on a 5s cooldown.
- **Universal Passive Magic:** Cinnamon passively casts Bolt+Fire, Bolt+Frost, Bolt+Shock and Bolt+Harm against nearby eligible hostile targets at any size (including while ridden by her owner), obeying friendly-fire, Sword Master, duel and party rules.
- **Grand Bow (`wildercord:cinnamon_grand_bow`):** Permanently keeps Cinnamon at 3× scale with +60 max health (80 total), +12 attack damage (15 total), +10 armour, +4 toughness, +0.6 knockback resistance, +0.11 speed, +40 damage budget (60 total), 24-block spell range, 1.6× spell power and 55-tick casting cadence.
- **Flying Harness (`wildercord:cinnamon_harness`):** Allows up to 4 riders (owner + same-party teammates, always reserving a seat for the owner) with owner-only 3D flight steering, autonomous sky-following, fall-damage immunity (Slow Falling on midair dismount), and a persistent 54-slot twin side chest UI (`ChestMenu.sixRows`).
- **Hit Pause Balance:** Receiving non-ally/non-passenger damage pauses Cinnamon's movement, attacks and spellcasting for 40 to 60 ticks (2–3 seconds) and clears active cast windups without unseating riders.
- Each treat adds 0.5× temporary growth up to 3× for a fixed 60-second window (unless already permanently grown with her Grand Bow).
- Ordinary damage cannot kill her. Exhaustion starts 30 seconds of following without attacking; isolated chip damage clears after ten quiet seconds.
- Craft the reusable whistle (`wildercord:cinnamon_whistle`) to recall her safely when she is not carrying riders; nonpersistent bounded chunk recovery respects claims, wards and duel locks.

## Check the feature

On an authorized Minecraft development environment, the existing focused suite remains:

```powershell
.\gradlew.bat -PcinnamonSuite runClientGameTest
```

It includes real growth/collision and exhaustion assertions, original-UUID whistle recovery, chunk unload, owner respawn, cross-dimension travel, full saved-world close/reopen, interrupted journal writes, conflicting identity markers, administrative retirement and bounded timeout. Giant and tired-following screenshots accompany the earlier companion captures.

The expanded native gameplay and visual checks have not yet been run on this source. Standalone Java compilation and pure unit tests do not establish native acceptance.
