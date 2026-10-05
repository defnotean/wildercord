# Mana Skin: resolved health damage

Mana Skin now restores at most 20% of the current hit's **nonlethal red-health loss**, after armour, enchantments, Resistance and absorption. It charges the existing two mana per health actually restored. The existing minimum 0.25-health recovery and available-mana limit remain. No Master damage, attack cadence, player armour, regeneration or death-save values change.

## Damage order and ownership

Inspection of the official Minecraft 26.3 `Player.actuallyHurt` bytecode confirms this order: armour, magic damage reduction, absorption consumption, health subtraction. `LivingEntity.hurtServer` performs death prevention afterward. Fabric's installed entity-events callback runs after that flow and passes an amount that does not represent the final health wound.

`PlayerMasterHitReceiptMixin` retains its existing Master interruption observation, finishing it before any restoration. A separate `ManaSkinDamage` frame spans only one `Player.hurtServer` invocation. The frame records health immediately around that invocation's exact player/source native damage operation. HeartCircles consumes this wound once at its original `AFTER_DAMAGE` callback position. The Master receipt is never reused as a general damage cache. Nested hits get separate frames, and `finally` restores the previous frame after success, rejection or exceptions.

Keeping the callback position preserves the earlier Scheduler low-health trigger and Imbuing responses. For example, a player at 8/20 health taking 2.4 native health damage still crosses the low-health threshold at 5.6, then receives a 0.48 rebate to 6.08.

## Edge behavior

- Full absorption and genuinely prevented/rejected hits restore no health and spend no mana. Partial absorption only permits a rebate on the remaining red-health loss.
- A pre-existing wound never increases the rebate. An earlier healing callback can reduce or remove the remaining recoverable wound; Mana Skin cannot raise health past this hit's starting health.
- Gash and other healing suppression still apply through `heal`. Zero actual restoration spends zero mana. Partial restoration pays only for the actual increase.
- The passive heal runs under the defender's own non-spell source using the existing `Effects.withSource` scope. It cannot award the incoming attacker Life affinity or healing mastery, and the exact enclosing attack source/cast is restored before later damage callbacks. Genuine helpful spells keep their normal healing credit.
- A native wound reaching zero health is ineligible, even when a totem, Reversal or another death save later restores life. Mana Skin cannot revive a player or supplement the death save.
- The source's existing bypass-invulnerability exclusion remains. Circle formation still uses its original event-based interruption rule.
- Native health/absorption loss still admits a matching Master interruption before healing. Party guards, source attribution, charge identity and interruption immunity are unchanged.

## Evidence and remaining native acceptance

The prior native P4 netherite receipt logged a raw 28-point strike causing 4.273926 native health loss, followed by complete health restoration and 11.2 mana spent. That is measured baseline evidence from the earlier Master receipt run, not a new sustained-fight measurement. For the same resolved wound, the corrected rule predicts approximately 0.854785 recovery, 3.419141 remaining health loss and 1.709570 mana spent. Those corrected numbers are arithmetic predictions until the new native fixtures run.

Pure boundary tests cover finite values, lethal/nonlethal loss, existing wounds, earlier restoration, thresholds, insufficient mana, healing suppression payment and the recorded endgame wound. Native `ManaSkinChecks`, included in `WildercordHeartCirclesTest`, adds real damage, P4 control, full/partial absorption, Resistance, Gash, rejected damage, lethal/totem/Reversal, callback order, earlier healing and nested source/target/rejection/exception cases. Existing Master receipt and all-caller consistency checks now assert the corrected rebate while retaining their interruption and source tests.

`ManaSkinSustainChecks` is an independently selectable, observation-only benchmark. It uses a fresh fixed-seed world, a connected player with 20 health, P4 netherite, Circle 20, an Echo Cord, 600 starting mana and normal 18 mana/second regeneration. An unpaused Gale Master selects and executes its actual attacks for 400 server ticks or until death. The player stands still without casting or guarding. There are no attack replays, combat teleports, manual AI steps or resource resets during observation. Logs report attack timing, the actual callback snapshots, health, absorption, mana payment, observed regeneration and survival. This is one specified Gale encounter, not a general difficulty claim.

For a valid before/after comparison, run the identical benchmark source on each revision with `-PfocusedSuite=dev.wildercord.aura.world.ManaSkinSustainChecks`, then compare the `[mana-skin-sustain]` logs and settings. The baseline build needs only that benchmark and descriptor registration, not the correction-dependent assertion changes. The corrected benchmark also runs in the Masters suite. Local verification compiles native test source but cannot execute Loom/native clients in this environment; CI must establish actual runtime outcomes. No corrected native or sustained-survival result is claimed here before that run.
