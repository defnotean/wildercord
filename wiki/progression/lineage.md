---
title: Sparring and Lineage
parent: Growing Stronger
nav_order: 2.48
description: "Consent-based blade spars, safe knockouts, timed clashes, masters and disciples, daily lessons, graduation, and the Lineage tab."
---

# Sparring and lineage
{: .no_toc }

Aura grows through practice with other swordsmen. A spar lets you test your blade safely; a master can teach a disciple over a longer road. Both begin with deliberate gestures, and either person can leave.

## Challenge someone to a spar

Hold a blade, **sneak, and use it on the other player**. They receive your salute. They must return the same gesture within **20 seconds** to accept. An ordinary attack never accepts a spar.

Both players need a breathing method, a blade, at least half their health, and a moment clear of combat. Creative and spectating players cannot participate. End an awakening before accepting. Wait ten seconds after taking damage, and thirty seconds after player combat or a completed bout.

Four cloth standards mark the grounds. After a **three-second count-in**, fight with blades and Aura. Spells, projectiles, and pets cannot hurt your sparring partner. The grounds remain freely walkable.

| How it ends | Result |
| --- | --- |
| Bring your partner to one heart | A safe knockout; they stay alive |
| Walk beyond the grounds | You concede |
| Fight for two minutes | An even result |
| Outside harm interrupts | The spar is called off |

Your partner's damage and harmful effects are undone afterward. Spent Aura is restored, while an awakening's cost remains paid. Momentum starts and ends at zero, and an awakening ends with the spar.

A spar teaches experience only after **ten seconds of fighting with both people landing a blow**. By default, three spars per partner per Minecraft day can teach experience. Further spars are still available for practice. Spars do not grow bonded blades, technique ranks, or mastery, and do not count as ordinary combat trials.

The **Lineage** tab on the Aura page records your wins, losses, and even results.

## Answer a clash on the beat

Two opposing Aura crescents can lock where they meet. Certain opposing arts can also enter a clash. The small rhythm panel above the hotbar shows three beats, both participants' grades, and the score. It leaves the centre of your view clear.

**Tap Attack once as each pair of ticks meets.** A perfect press earns two points; a good press earns one. Missing earns nothing. Extra presses and presses outside a window subtract a point, so frantic clicking makes the result worse.

The Way of the Blade gives slightly wider timing windows and wins an equal score against a side without that edge. It still needs to answer the beats. Duelists and other Aura fighters have their own timing, with tighter rhythm at higher stages.

The winner's crescent continues with reduced damage, normally **80%**. An equal clash breaks both crescents. The server judges presses with a bounded connection allowance; the client cannot submit a score or a winning result.

<img src="{{ '/assets/images/aura-clash-rhythm.png' | relative_url }}" alt="Two Aura crescents locked ahead of the swordsman, with a compact three-beat rhythm panel above the hotbar and a clear crosshair" class="shot">

## Ask for a master

A master must have reached **Form**. A new disciple must stand at least **two stages below** them; someone who has not learned Aura can ask too. A master has three active disciples by default, and a disciple has one master.

The master **holds a blade and sneaks while standing still** until the breathing stance settles. The disciple kneels close in front of them, facing them. Both receive the request, then hold the posture through the eight-second ceremony. **Stand, turn away, or release the stance to cancel.** A broken ceremony creates no bond.

The ceremony teaches an untrained disciple the master's method. A disciple with a different method receives its manual and can decide whether to change.

## Learn together

- **Stay nearby:** within 24 blocks in the same world, ordinary Aura experience grows 25% faster by default. Practice experience does not receive this bonus.
- **Return for a lesson:** kneel before your breathing master again. A five-second lesson teaches one technique part they know and you do not, helps an eligible disciple find their crossroads, or becomes a session of practice if there is nothing new to teach. The next lesson is available after a Minecraft day.
- **Best your master:** a knockout in a spar can meet a waiting breakthrough trial for a stage below your master's own. Leaving the grounds does not count. Reaching their stage still asks you to face a trial of the world.
- **Give back through progress:** your breakthroughs teach your master a share of the road you just walked, 25% by default. Shares earned while the master is away remain saved for them.
- **Graduate:** when you reach your master's stage, the active bond ends with honour. Their record remembers you.

## See and release your bonds

Open the Aura page's **Lineage** tab to see your master, the next lesson, active disciples, and honoured graduates. Gold markers identify nearby people; green marks people online; dim markers identify people away. Long names are shortened to keep controls readable, and longer records have pages.

Either participant can release a bond at any time, even when the other is offline. Press **Release**, then **Confirm** within five seconds. The first click only asks for confirmation. Releasing an active bond cancels its ongoing lesson; already-earned shares remain saved.

<img src="{{ '/assets/images/aura-lineage.png' | relative_url }}" alt="The Lineage tab shows a nearby disciple, spar records, lesson information, honoured records and a Release control" class="shot">

## Server settings

The `aura` config section controls `sparring`, `spar_ring_radius`, `spars_per_day`, `spar_xp`, `mentorship`, `max_disciples`, `disciple_gain`, `master_share`, `clashes`, and `clash_carry`. Turning a feature off does not delete saved lineage records. Bond release remains available.

See [Aura]({{ '/progression/aura/' | relative_url }}), [Ways]({{ '/progression/ways/' | relative_url }}), [Techniques]({{ '/progression/techniques/' | relative_url }}), and [The Bonded Blade]({{ '/progression/bonded-blade/' | relative_url }}) for the rest of the swordsman's path.
