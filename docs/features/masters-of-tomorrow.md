# Masters of Tomorrow

Development build: `0.11.0-masters-dev`. Install the same development build on every client and the server. Back up a world before testing a new build.

## What's implemented so far

- Up to 100 absorbed Mana Crystals, each adding 10 maximum mana
- Heart Circles through twenty, with twelve additional progression gates
- Consent-based parties with friendly spell and Aura protection
- Three opt-in Sword Master trial disciplines: Ember, Gale and Stone
- Shared school-tuned cast-punish dashes with committed movement, a separate strike warning and an exposed recovery
- Ember's Cinder Wake, with a broad cut and a separately warned delayed afterburn
- Three new rebindable Aura combat actions and their original animations
- Authoritative windups and original motion for all ten existing first forms, plus Rising Cinders and Blossom Fall
- More active Normal/Hard pressure from Gloomstalkers and Bog Witch-Frogs

The complete combat overhaul is still being developed. This page describes the first integrated foundation. It does not claim a measured “20×” difficulty increase, a finished style roster, or a complete balance pass across ordinary creatures.

## Play together

Use `/party invite <player>`. The recipient joins with `/party accept <inviter>`, or refuses with `/party decline <inviter>`. Invitations expire after sixty seconds. `/party` lists the group, and `/party leave` leaves it. Leaders can use `/party kick <member>` and `/party disband`.

A party holds eight players. Party membership protects your allies and their owned pets from newly applied friendly damage and harmful spell/Aura effects. Healing and other helpful magic still work. Delayed hits use the membership in force when they land. Existing burns or status effects are not removed when joining.

An explicitly agreed duel keeps its existing exception. Party membership lasts across deaths, dimensions and reconnects in the same running server; restarting clears it. Invitations are cancelled on disconnect.

## Challenge a master

In Survival, reach Aura Form or Heart Circle VIII, then sneak-talk to a travelling Duelist twice to request an introduction. The teacher remains an ordinary teacher; a temporary Master waits nearby for one minute. This invitation does not enroll you. Talk to the Master to read the warning, then speak again within ten seconds to accept. The trial is lethal and voluntary. Other players must individually accept; being in the party does not enroll them.

The first challenger has thirty seconds to gather up to eight willing challengers, or may use `/master ready` to begin sooner. The prototype can also be reached through `/master challenge ember`, `/master challenge gale` or `/master challenge stone`; `/master join` explicitly joins an open nearby trial. Spawn eggs are available for controlled creative testing.

The roster and its difficulty scaling lock at the start. Leaving the 24-block arena, dying or disconnecting forfeits your entry. Remaining players do not receive an easier fight when someone leaves. Bystanders cannot help damage the master or be targeted by its attacks.

Watch the master's blade and the boss-bar action label. Attacks have a windup, commit their aim, and leave a recovery window. Frontal guards can cut hostile bolts, but flanking and forcing the master to spend Aura create openings. A tired master must stop to breathe. Simple hostile bolts may be redirected once; arbitrary linked spells are not reflected.

A legitimate first clear permanently records that school and teaches one existing technique part: Ember grants Echo, Gale grants Afterimage, and Stone grants Sunder. If you already know it, only the clear is recorded. Living, present opt-in teammates share the clear; bypass/admin kills and disabled-AI fixtures do not count. There is no repeatable item or XP payout. Use `/master victories` to view your record. The travelling Duelists who teach breathing methods remain separate.

### Answering a cast-punish dash

A free Master may pursue an enrolled player who has visibly held a real charge for at least 0.3 seconds. One-tick tap/cancel feints do not trigger it. The Master cannot cancel its own attack, guard, dash or recovery to answer your spell. Two runway lines warn for 0.4 seconds, then its actual body moves along that locked path for 0.3 seconds. On landing, a short outlined strike lane gives another full 0.4-second warning. A valid early opening can land within 28–29 ticks of starting a normal 30-tick charge; releasing earlier or casting faster can still beat it. It never turns the accepted dash or strike toward your later input.

Gale dashes up to 6.4 blocks for 26 Aura, at most once every 5 seconds; Ember up to 4.8 blocks for 28 Aura, every 7 seconds; Stone up to 3.6 blocks for 30 Aura, every 8 seconds. Slowness can shorten their travel. No dash crosses walls, fluid, dangerous ground, gaps, the arena edge or the world border. If the route becomes blocked, it stops rather than teleporting through.

Sidestep outside the 1.6-block-wide final lane, backstep beyond its 3.25-block reach, use solid cover or parry the strike. A landed hit that damages health or consumes absorption can break the original held spell, even if Mana Skin immediately heals the wound. A full guard, dodge, ward or other total damage prevention preserves the charge. Shared interruption protection still prevents repeated spell breaks for eight seconds after a successful break. Base damage is 28 for Ember/Gale and 30.8 for Stone before ordinary defenses, regardless of party size. Only the selected enrolled caster can be hit by this move.

Releasing the spell baits a paid cancellation, with no Aura or cooldown refund. The Master brakes and remains open for 1.5 seconds, just as it does after the strike. Restarting the spell cannot erase that opening or cause an immediate second dash. Sustained baiting, eight-player pressure and the new movement still need native playtest balance; this does not claim a measured difficulty multiplier. Its new body motion uses the original fallback rig, including when the optional articulated renderer is enabled.

### Reading Ember's Cinder Wake

Ember commits its direction when the first outline appears. After 1.2 seconds it makes a broad, four-block cut. Backstep beyond that cut, then watch the new narrow, crossbar-marked lanes: they ignite once after another 1.3 seconds. Step sideways out of a lane, move beside or behind the blade, or take solid cover. The second strike cannot turn to follow you. Each beat deals 26 base damage before your ordinary defenses.

One or two challengers see one afterburn lane; three to five see two separated lanes; six to eight see three. Every lane stays 1.4 blocks wide and ends seven blocks from the original blade position. More players never multiply one player's hit count or damage. The master spends 24 Aura and remains open for 1.5 seconds after the ignition. Lower health makes this pattern more frequent without speeding up either warning. Interrupted or ended trials leave no harmful fire behind.

## New Aura controls

Rebind these under Options → Controls → Key Binds → Wildercord: Master's Arts. The Aura page shows your current bindings. Fresh defaults avoid Minecraft 26.3's Quick Actions and debug bindings; existing saved bindings are preserved, so check Controls if upgrading from the earlier G/H/J development defaults.

| Default | Move | Unlock | Cost | Rest |
|---|---|---|---:|---:|
| U | Spellcut | Edge | 14 Aura | 3 s |
| Y | Rising Break | Form | 20 Aura | 5 s |
| J | Driving Cut | Form | 18 Aura | 4 s |

Spellcut severs up to three incoming hostile bolts in front and sweeps nearby enemies. Rising Break attacks upward, wearing extra stance and lifting ordinary enemies. Driving Cut is a narrow thrust with five-block reach; a successful hit interrupts a caster for one second, respecting existing player recovery protection.

Every action has a windup before its hit and a recovery afterward. During that commitment you cannot layer another ordinary swing, Aura action or spell over it. Damage, interruption, a weapon change or leaving the world can cancel its pending hit. Ordinary held-slot and selected-stack changes latch immediately, so switching away and back does not restore it; direct server-side equipment changes are additionally checked each tick and at impact. The paid cost and recovery remain. The server decides targets and timing; pressing a key does not itself confirm a successful hit.

## Existing style first forms

All ten first forms retain their original input strings, prices and cooldowns. Their first physical release now follows a committed server windup with an original body/weapon motion:

| Style | First form | Windup | Recovery |
|---|---|---:|---:|
| Ember | Kindling Draw | 6 ticks | 12 ticks |
| Rime | Frostbite | 6 | 12 |
| Thunder | Crackle | 4 | 12 |
| Gale | Cutting Breeze | 4 | 10 |
| Stone | Rockbreaker | 10 | 18 |
| Verdant | Thorn Lash | 6 | 12 |
| Hollow | Void Cut | 6 | 12 |
| Starlit | Star Needle | 4 | 10 |
| Hourglass | Echo Cut | 6 | 20 |
| Crimson | Bloodletting | 6 | 12 |

Aim is fixed at acceptance, while the camera can look around freely. Crackle's later physical cuts stop on interruption, lost reach or cover. Echo's already-released afterimage keeps its original position and facing and can still strike after the live swordsman's motion stops. Existing wounds and fields likewise keep their intended post-release lifetime; they still recheck ownership and party admission when they affect a creature.

## Two second-form releases

Rising Cinders and Blossom Fall keep their existing leap → low input, Flow unlock, 8-Aura base cost and four-second cooldown. Rising Cinders now has an 8-tick windup and 16-tick recovery; Blossom Fall has an 8-tick windup and 18-tick recovery. Their cut, payment, interruption and committed-aim rules follow the first forms above.

Cinders scoops its blade upward from the outside hip and opens into a high diagonal finish. Its existing enemy lift releases with the cut; the cinder rain follows twelve ticks later. Blossom gathers over the shoulder and cuts down into a planted stance, releasing its existing four-second petal field. Neither art adds a jump, lunge or forced camera movement. Cancelling after release stops the body motion while the released rain or field keeps its own lifetime and checks who it can currently harm or help.

The other thirty-eight method arts have their existing custom effects but are not yet covered by this new complete body/weapon timeline.

## Ordinary encounters

Gloomstalkers spend less time stalking or retreating on Normal and Hard, close their old unreachable-distance gap, and pursue around cover. Bog Witch-Frogs use their ready tongue more decisively and approach targets outside their useful ranged band. Their health, damage and readable tells are unchanged; Easy keeps its original cadence. Both revalidate targets and cover before committed releases. This is a bounded two-species behavior pass, not a completed rebalance of every mob and boss.

## Growing beyond eight circles

The first eight thresholds and milestone perks are unchanged. Later circles require both more lifetime mana expenditure and achievements across the existing rune, reaction, secret and boss systems. The highest circle requires 1,220,000 lifetime condensed mana, 1,000 spell-killed monsters, 100 Runebound kills and all ten built-in secret spells.

At twenty working circles, circle bonuses total +300 maximum mana, +10 mana regeneration per second and +60% spell power. Existing capstone perks are not granted repeatedly. With 100 crystals, an Echo Cord, Reservoir III and a Focus of the Deep Well, the documented maximum is 1,725 mana. Other gear/configuration may alter the breakdown.

## Verification and tuning

Pure rules and asset consistency checks are part of this change. Native client test suites cover the intended payment, timeline, party mutation, roster and visual behavior. Consult the development verification report for which suites actually ran; authored tests alone are not evidence that runtime behavior passed.

Still to do: native multiplayer tuning against endgame armor, broader ordinary-mob and dungeon-boss difficulty, the remaining thirty-eight full-body style timelines, and richer Master encounters and rewards.
