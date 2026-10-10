---
title: Guilds and Covens
parent: Friends and Rivals
nav_order: 5.7
description: Standing groups for swordsmen and mages that outlast a restart and train faster together.
---

# Guilds and Covens

A [party]({{ '/social/parties/' | relative_url }}) lasts until the server restarts. A guild or coven lasts until its members
end it. A **guild** is for swordsmen: you need a breathing method to join one. A **coven** is for mages: you need a Cord or a
Heart Circle. You can be in one guild and one coven at the same time.

## Training together

When fellow members are within 32 blocks of you, your training counts for more:

- In a guild, every bit of aura experience you earn counts 5% more for each member nearby.
- In a coven, every bit of mana you spend counts 5% more toward your next Heart Circle for each member nearby.

Up to three members count, so the bonus tops out at +15%. Practice doesn't get it. The bonus makes you learn faster, not hit
harder: a guild or coven makes no one stronger in a fight.

## Commands

Use `/guild` for guilds and `/coven` for covens. Both take the same commands:

| Command | Does |
| --- | --- |
| `/guild` | Shows your guild, its members (the leader is marked with a star, and members who are away are greyed out) and how many are near you |
| `/guild found <name>` | Founds a guild for 32 emeralds. Names take 3 to 24 letters, digits, spaces, apostrophes or hyphens, and no two guilds or covens can share one |
| `/guild invite <player>` | Leader only. The invitation lasts a minute |
| `/guild accept` | Joins the guild you were last invited to |
| `/guild leave` | Leaves. If the leader leaves, the longest-standing member takes over, and when the last member leaves the guild ends |
| `/guild kick <member>` | Leader only |
| `/guild disband` | Leader only. Ends the guild for everyone |

A guild or coven holds up to 12 members. It is saved with the world and can name members who are offline.

Guilds and covens don't stop friendly fire. Join a party for that.
