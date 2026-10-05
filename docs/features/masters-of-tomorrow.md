# Masters of Tomorrow

Development build: `0.11.0-masters-dev`. Install the same development build on every client and the server. Back up a world before testing a new build.

## What's implemented so far

- Up to 100 absorbed Mana Crystals, each adding 10 maximum mana
- Heart Circles through twenty, with twelve additional progression gates
- Consent-based parties with friendly spell and Aura protection
- Three opt-in Sword Master trial disciplines: Ember, Gale and Stone
- Three new rebindable Aura combat actions and their original animations
- Authoritative windups and original motion for all ten existing first-form style arts
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

## New Aura controls

Rebind these under Options → Controls → Key Binds → Wildercord: Master's Arts. The Aura page shows your current bindings.

| Default | Move | Unlock | Cost | Rest |
|---|---|---|---:|---:|
| G | Spellcut | Edge | 14 Aura | 3 s |
| H | Rising Break | Form | 20 Aura | 5 s |
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

The other forty method arts have their existing custom effects but are not yet covered by this new complete body/weapon timeline.

## Ordinary encounters

Gloomstalkers spend less time stalking or retreating on Normal and Hard, close their old unreachable-distance gap, and pursue around cover. Bog Witch-Frogs use their ready tongue more decisively and approach targets outside their useful ranged band. Their health, damage and readable tells are unchanged; Easy keeps its original cadence. Both revalidate targets and cover before committed releases. This is a bounded two-species behavior pass, not a completed rebalance of every mob and boss.

## Growing beyond eight circles

The first eight thresholds and milestone perks are unchanged. Later circles require both more lifetime mana expenditure and achievements across the existing rune, reaction, secret and boss systems. The highest circle requires 1,220,000 lifetime condensed mana, 1,000 spell-killed monsters, 100 Runebound kills and all ten built-in secret spells.

At twenty working circles, circle bonuses total +300 maximum mana, +10 mana regeneration per second and +60% spell power. Existing capstone perks are not granted repeatedly. With 100 crystals, an Echo Cord, Reservoir III and a Focus of the Deep Well, the documented maximum is 1,725 mana. Other gear/configuration may alter the breakdown.

## Verification and tuning

Pure rules and asset consistency checks are part of this change. Native client test suites cover the intended payment, timeline, party mutation, roster and visual behavior. Consult the development verification report for which suites actually ran; authored tests alone are not evidence that runtime behavior passed.

Still to do: native multiplayer tuning against endgame armor, broader ordinary-mob and dungeon-boss difficulty, the remaining forty full-body style timelines, and richer Master encounters and rewards.
