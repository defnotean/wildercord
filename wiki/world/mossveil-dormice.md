---
layout: default
title: Mossveil Dormice
parent: The World
nav_order: 29
permalink: /world/mossveil-dormice/
---
# A home for a quiet nose

> **New in 0.10.0-alpha.** The companion and cowl have passed focused native
> gameplay checks. The artwork below is an illustrated field plate; the separate
> game captures are labelled below.

Mossveil Dormice are little rounded cave companions with folded ear cups,
cream cheeks, twitching whiskers and a curling tail. Their sounds are tiny
sniffs, uneven bites, soft chirps and paws brushing moss. They never cast
magic, breed or produce bonus crafting materials.

![Illustrated Mossveil Dormouse and stitched cowl field plate — authored illustration, not native game evidence]({{ '/assets/mossveil/illustrated-field-plate.png' | relative_url }})

## Find a living fungal corner

Look in Overworld lush caves for dry moss or clay floors, low light,
structural cover and a nearby **mature Glowcap**. A prepared bud does not
qualify. Natural admission has a small local population cap and refuses a
saturated region. A species switch, the master creature switch and the
spawn multiplier control future natural admission.

An existing dormouse stays usable if natural spawning is later disabled.
The companion does not require a new structure or quest reward. A real
player-built fungal garden can provide food and a place to return to; the
normal biome and natural-spawn rules still apply.

## Earn her trust with food

| Action | Actual price | Waiting and conditions |
| --- | --- | --- |
| First wild feeding | 1 dried Glowcap Gill | Survival, visible and within 3 blocks |
| Second wild feeding | 1 Gill | Wait 5 seconds after the accepted feeding |
| Third wild feeding | 1 Gill | Another 5-second rest; the original feeder becomes her owner |
| Feed a wounded owned companion | 1 Gill | Rest 5 seconds; heal at most 2 health |
| Feed at full health | None | Refuses without eating the Gill |
| Owner curl or release | None | Interact with an empty hand |

The first accepted feeding reserves a **five-minute claim** for that
player. A foreign player cannot pay the later feedings or take the owner
slot. After that claim expires, a new first feeder begins again; the old
partial progress is not inherited. Progress, claimant, exact deadlines,
owner UUID and curl order survive world reopening.

Only her owner can change the curl order or feed her after taming. A
foreign player's Gills do not lure a tamed dormouse. When released, she
uses ordinary paths toward a live owner in the same loaded world. She
does not teleport to catch up, cross a portal automatically or continue
following a dead or departed owner. Bring her safely through the world
and leave her curled beside the garden when you travel far away.

## Stitch a Mossveil Cowl

Combine these three existing materials in an ordinary crafting grid:

- **Moonreed Floss** from an opened Moonreed, after a real moth visit.
- **Dried Glowcap Gills** from a mature cap, after an uninterrupted snail visit.
- **Mycelial Dew** from the existing Sporeback gathering interaction.

The resulting cowl occupies the **helmet slot**, provides **1 armour point**
and has **66 durability**. Moonreed Floss is its vanilla repair ingredient.
Repairs and replacement cowls preserve the player's saved filter rest;
they do not create an instant recharge.

### A prepared breath

While poisoned, wear the cowl and crouch still for **2 seconds** beside
your own living curled dormouse, within **3 blocks**. A real Fungal Nursery
must be nearby and supported over dry clear space and a solid floor.
Compatibility claim checks can refuse use of that home; no blocks are
broken or edited.

A completed breath pays **1 durability**, then shortens the active finite
Poison timer by the smaller of:

- **3 seconds**, or
- **one quarter of its remaining duration**.

The poison amplifier, visual flags and any pending weaker hidden poison
remain intact. The effect never becomes a full cure or immunity. Infinite
Poison does not qualify. A successful breath also causes **1 second of
Slowness**, with a small physical floss release and a stitched intake sound.

Both the wearer and that companion need a saved **10-second rest** after
commitment. Swapping helmets, repairing, changing dimensions, logging out
or reopening the world cannot renew that rest. Preparation itself is not
saved; after reopening, begin the stationary breath again when ready.

## Protect the preparation

Movement—including small steps that accumulate past the planted bound—
uncrouching, lost poison, changed equipment, an unsupported or changed
home, a foreign companion, death or departure cancels preparation.
Dense companion queries refuse rather than choosing a hidden eligible
animal from a truncated list.

The price and shared rests are reserved before the wear callback.
If another mod changes the wearer, worn stack, companion or effect after
that payment, the cowl refuses later success effects and gives no refund.
A duration edit already admitted before a later notification callback may
remain. It does not promise rollback of another mod's mutations or remove
their stronger replacement poison.

The offhand **Cave Breather** remains the faster emergency option: it
removes Poison with its existing wear/rest/Slowness tradeoff. The cowl is
the quieter, slower home-supported option and replaces a stronger helmet.
Neither protects the companion from ordinary damage. Strong poison,
physical attacks and being forced to move remain real vulnerabilities.

## The physical details

The original rig has four tiny paws, separate ear cups, a forward nose,
paired three-strand whisker fans and three tail hinges. Curling folds the
head and tail toward the belly. Sniffing and walking come from real
runtime behavior. The worn cowl is an original stitched head texture;
its filter uses small authored floss strips and no magical circle.

## From the game

These unchanged native captures use a supplied moss review floor. The companion's
sniff, owner curl and following are actual runtime behavior; this scene does not
show a naturally generated cave.

![Actual Mossveil Dormouse on supplied review floor]({{ '/assets/mossveil/native-idle.png' | relative_url }})

![Actual owner-ordered curl and folded ears, whiskers and tail]({{ '/assets/mossveil/native-curl.png' | relative_url }})

![Actual worn Mossveil Cowl from behind]({{ '/assets/mossveil/native-worn-cowl.png' | relative_url }})

The private filter emits eight small physical floss pieces at Full quality and
four at Minimal, then retires. Ordinary Poison particles can overlap those
small strips; the review shots do not isolate their appearance.

When several supported Nurseries are nearby, preparation selects a home within
three blocks of both you and your companion. An earlier out-of-range Nursery
does not prevent a later usable one from being selected.

Continue with [Glowcap Nurseries]({{ '/world/glowcap-nurseries/' | relative_url }}),
[Sporeback Snails]({{ '/world/sporeback-snails/' | relative_url }}), and
[Moonreed Gardens]({{ '/world/moonreed-gardens/' | relative_url }}).

Natural encounters are admitted only during ordinary server-thread spawning.
Chunk-generation attempts refuse; the species does not query live animal
populations through a world-generation region.
