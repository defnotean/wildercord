---
layout: default
title: Village Tournaments
parent: Growing Stronger
nav_order: 17
---

# Village Tournaments

A village with **at least three living residents near its bell** can host the **Three Bows**. Stewards gather at a timber registration stand, with embroidered cloth standards marking the fighting ground. Three different breathing methods make each circuit a chance to practise different counters.

![The timber registration stand, stitched standards and waiting stewards in game]({{ '/assets/images/village-tournament.png' | relative_url }})

## Find a gathering

Daytime visitors periodically look for clear, flat ground twenty to forty-four blocks from an inhabited village bell. The square must be dry, open to the sky, and seventeen blocks across. The visitors place their stand and four standards into empty space; they leave the existing ground intact. Hillside villages may have no suitable square. A nearby stand prevents duplicate gatherings.

The first gathering opens when a player is near the stand during daylight. It lasts **one Minecraft day** in game ticks. Later gatherings can open every **three Minecraft days**, while players and residents are nearby. These are optional encounters, with no entry fee or required attendance.

## Enter the Three Bows

Bring an Aura blade in Survival. Finish any existing duel or spar. Use the stand once to read the rules, then again within ten seconds to accept. Stand on its **arena side** before accepting: the fighting centre is six blocks north of the stand.

Each of the three stewards fights at the Aura stage you have when that bout starts. Each bout has a three-second countdown, an **eight-block arena radius**, and a two-minute limit. Knockouts are nonlethal. The duel returns health taken by your opponent and removes only their newly inflicted harmful effects. Other damage is not refunded.

After winning a bout, return to the stand within **one minute** to continue. Losing, leaving the radius, logging out, an interruption, or a draw ends the run. You can retry while the gathering remains open. A spell touching your opponent disqualifies the Aura run, even if the spell wins the bout. There is no prize for a partial circuit.

Watch prepared cuts and move around their line. An axe can break a raised guard. The stewards' methods change their Aura flavour; the next opponent is announced before the countdown.

## Choose your prize

Three clean knockout wins award one claim for this gathering:

- **One technique scroll of your choice** from the parts allowed on scrolls. The first selection favours a part you do not know.
- **Three Aura Shards**.
- **The Three Bows**, a three-page village history and steward's record.

At the stand, **sneak-use** to cycle the displayed scroll choice. **Use normally** to claim that choice once. The saved ledger retains an unclaimed prize across saves and later gatherings. Claiming it prevents repeated rewards. Winning also grants Aura experience, records the discovery, and completes the ordinary pure-Aura duel trial.

## Server controls and limits

![The awarded Three Bows history open in the game's illustrated book reader]({{ '/assets/images/three-bows-lore.png' | relative_url }})

`aura_world.tournaments` controls stand creation and hosted bouts. Creation also respects mob spawning and mob griefing game rules. Existing stands retain their ledger when the feature is disabled. No chunks are forced to load. Village checks and terrain searches are bounded, and one stand hosts only three stewards and one challenger at a time.

An active bracket does not resume after a reload. A missing steward cannot advance a round or earn a prize. An obstructed arena refuses to start. Each stand's current gathering and outstanding reward ledgers are capped at 128 players each.

The tournament is part of the wider Aura world. See [the Highland Beasts]({{ '/progression/aura-beasts/' | relative_url }}) for materials and field tools from the mountain passes.
