---
title: Getting Around
parent: Friends and Rivals
nav_order: 6
description: "Homes, warps, waypoints, teleport requests, /back, /spawn and /rtp: every travel command, the short warmup before a teleport, cooldowns, and what server owners can change."
---

# Getting Around

Wildercord comes with a full set of travel commands for servers: **homes** you can return to, public **warps**,
personal **waypoints** that point the way, **teleport requests** to and from friends, and **/back**, **/spawn** and
**/rtp**. Every teleport is a little piece of magic: a circle forms at your feet, you stand still for a moment, and
you vanish in a pillar of light.

- [The commands](#the-commands)
- [How a teleport works](#how-a-teleport-works)
- [Homes](#homes)
- [Warps](#warps)
- [Waypoints](#waypoints)
- [Teleport requests](#teleport-requests)
- [Back, spawn and random teleport](#back-spawn-and-random-teleport)
- [For server owners](#for-server-owners)

## The commands

Anyone can use these, unless the server has turned travel off. Words in `<angle brackets>` are needed; words in
`[square brackets]` can be left out.

| Command | Does |
|---|---|
| `/sethome [name]` | Sets a home where you're standing. Without a name it's called **home**. Setting a home again by the same name moves it. |
| `/home [name]` | Teleports you to a home. Without a name: the one called **home**, or your only one. |
| `/delhome <name>` | Removes a home. |
| `/homes` | Lists your homes. Click a name to go there. |
| `/warp <name>` | Teleports you to a warp. |
| `/warps` (or `/warp`) | Lists the warps. Click a name to go there. |
| `/waypoint add <name>` | Marks a waypoint where you're standing. You can give coordinates after the name instead: `/waypoint add mine 120 40 -300`. |
| `/waypoint remove <name>` | Removes a waypoint. |
| `/waypoint list` | Lists your waypoints. Click a name to track it. |
| `/waypoint track <name>` | Shows the way to a waypoint in the corner of your screen. |
| `/waypoint untrack` | Stops showing it. |
| `/waypoint share <name> <player>` | Sends a waypoint to another player, who can add it with one click. |
| `/tpa <player>` | Asks to teleport to another player. |
| `/tpahere <player>` | Asks another player to teleport to you. |
| `/tpaccept [player]` | Accepts a request. Without a name: the newest one. |
| `/tpdeny [player]` | Turns a request down. |
| `/tpcancel` | Takes back the requests you've sent. |
| `/tptoggle` | Stops anyone sending you requests. Type it again to allow them. |
| `/back` | Returns you to where you were before your last teleport, or to where you died. |
| `/spawn` | Teleports you to the world spawn. |
| `/rtp` | Teleports you somewhere random in the Overworld, on safe ground. |

Operators also have `/setwarp <name>` and `/delwarp <name>` (see [Warps](#warps)).

Tab completion fills in names as you type: your homes, the warps, your waypoints, and for `/tpaccept` and
`/tpdeny`, whoever has sent you a request.

## How a teleport works

Every command that teleports you (`/home`, `/warp`, `/spawn`, `/back`, `/rtp` and an accepted request) follows the
same rules.

### The warmup

When you use one, a violet **magic circle** forms at your feet and a countdown appears above your hotbar:
*Teleporting to your home in 3... stand still*. Motes of light rise from the circle's edge, and in the last second a
star lights up inside it. When the count reaches zero you're gone.

The warmup is **3 seconds** unless the server says otherwise. It's cancelled, and nothing happens, if:

| You... | The game says |
|---|---|
| **move** more than half a block (turning to look around is fine) | *Teleport cancelled: you moved* |
| **take damage** | *Teleport cancelled: you were hurt* |
| **start a duel** | *Teleport cancelled: you're in a duel* |

Dying or changing worlds also calls it off. A cancelled teleport's circle cracks and fizzles out. It doesn't start a
cooldown, so you can try again straight away.

**Operators and players in Creative mode** skip the warmup and teleport at once.

### Cooldowns

After a teleport, **that command** rests for a while before you can use it again: **30 seconds** for `/home`,
`/warp`, `/spawn`, `/back` and `/tpa`, and **5 minutes** for `/rtp` (unless the server says otherwise). Each command
has its own cooldown, so using `/home` doesn't stop you using `/warp`. If you try too soon you're told how long is
left: *You can use /home again in 12 seconds*.

Operators have no cooldowns.

### Landing safely

The place you're going is checked again at the very end of the warmup. If something has changed, say someone built
over your home or lava has flowed over it, you land on the **nearest safe ground close by** instead. If there isn't
any, the teleport is called off and you're told why. Safe ground means something solid under your feet, room for
your head, and no lava, fire, magma, cactus, berry bushes, powder snow or cobwebs in the way.

(Players in Creative mode land exactly where they asked, since nothing can hurt them there.)

### Everything else

- **You can't teleport during a [duel]({{ '/social/duels/' | relative_url }}).** Finish it first.
- **Teleports work between worlds.** A home in the Nether takes you to the Nether.
- **Where you left from is remembered**, so `/back` can take you there again.
- Starting a new teleport while one is warming up replaces the old one.
- Everyone nearby sees the light and hears the chime where you leave and where you arrive.

## Homes

A home is a place of your own to come back to. Stand where you'd like it and type `/sethome`. Then `/home` brings you
back from anywhere, in any world.

- You can have **up to 3 homes** (the server can change this). Give them names to tell them apart:
  `/sethome base`, `/sethome mine`, then `/home mine`.
- Names can use **letters, numbers, `-` and `_`**, up to 24 characters. They don't care about capitals: `Base` and
  `base` are the same home.
- **Setting a home again by a name you already use moves it** there. This works even when you have the most homes
  you can have.
- `/homes` lists them all, with how many you have out of how many you can. Hover over a name to see where it is;
  click it to go there.
- If you have several homes and none of them is called **home**, a plain `/home` shows you the list to pick from.
- Your homes are yours alone, and they're kept when you die.

## Warps

Warps are **public places anyone can go to**: the spawn town, a market, an arena, the portal hub. Type `/warps` to see
them all (click one to go there), or `/warp <name>` to go straight there.

Only **operators** can make and remove warps:

| Command | Does |
|---|---|
| `/setwarp <name>` | Sets a warp where the operator is standing. Setting one again by the same name moves it. |
| `/delwarp <name>` | Removes a warp. |

Warps follow the same naming rules as homes, belong to the whole server, and can be in any world.

## Waypoints

Waypoints are **markers that never teleport you**. They're for finding your way back on foot: to a village you
passed, a cave entrance, the stronghold you're hunting, a friend's base.

- `/waypoint add <name>` marks where you're standing, and remembers which world you're in. Give coordinates to mark
  somewhere else: `/waypoint add village 340 70 -1200`.
- `/waypoint track <name>` shows the waypoint as one small line in the **top-left corner** of your screen:
  an arrow that turns as you look around and always points toward it, its name, and how many blocks away it is.
  When it's in another world, the line says which world instead.
- Within about **96 blocks**, a **faint beam of light** rises from a tracked waypoint so you can spot it. Only you can
  see it.
- `/waypoint untrack` hides the line and the beam. Tracking another waypoint switches to it. Removing the tracked
  waypoint stops tracking it.
- `/waypoint list` shows them all (the tracked one has a star). Hover over a name to see where it is; click it to
  track it.
- `/waypoint share <name> <player>` sends the waypoint to someone else. They get a message in chat with an **[Add]**
  button that adds it to their own waypoints, in the right world.
- You can keep **up to 50 waypoints**. Adding one by a name you already use moves it.

The corner line hides while the debug screen (`F3`) is open, since it uses the same corner.

Minecraft has its own `/waypoint` command for operators, which changes how players look on the locator bar. It's
still there for them: `/waypoint modify ...` works as before, and its list is now `/waypoint locator`.

## Teleport requests

To visit a friend, **ask first**: `/tpa Alex`. To bring a friend to you: `/tpahere Alex`.

- They hear a chime and see your request in chat, with two buttons: **[Accept]** and **[Deny]**. They can also type
  `/tpaccept` or `/tpdeny`.
- A request **lasts 60 seconds** (the server can change this). After that it runs out, and you're both told.
- **Sending another request to the same player replaces the first.** You can have requests out to several different
  players at once.
- `/tpcancel` takes back every request you've sent.
- You can't send a request to yourself, or to someone who's in a duel.
- **When a request is accepted**, whoever is travelling goes through the usual warmup (so they must stand still),
  and lands wherever the other player is **when the warmup ends**, on safe ground near them.
- Everyone is told what happened: sent, accepted, turned down, taken back or run out. If either player leaves the
  game, the request is dropped.

**Don't want requests?** `/tptoggle` turns them all away (anyone who tries is told you aren't taking requests) and
drops any waiting for you. Type it again to allow them. This is remembered even if you log out.

## Back, spawn and random teleport

### /back

`/back` returns you to where you were **just before your last teleport**, so after `/home` it takes you back to where
you were. Using `/back` counts as a teleport too, so a second `/back` undoes the first.

**When you die**, `/back` remembers where it happened instead. After you respawn, a message in chat reminds you, and
you can click it. If the spot is dangerous now (you died in lava, say), you land on the nearest safe ground close by,
or the teleport is called off if there's none.

### /spawn

`/spawn` takes you to the **world spawn** (not your bed), on safe ground.

### /rtp

`/rtp` sends you **somewhere random in the Overworld**, up to **5,000 blocks from the world spawn** (the server can
change this). It's a good way to find fresh land to settle.

- You always land **on the surface, on solid ground**: never in the sea, a river, lava, on top of a tree or inside a
  block.
- It works from any world, and always takes you to the Overworld.
- It has a longer cooldown than the other commands: **5 minutes**.
- Very rarely it can't find anywhere safe; you're told, and can simply try again.
- Your coordinates are shown in chat when you land, so you can write them down (or `/sethome` right away).

## For server owners

Everything on this page works out of the box. In the server's Wildercord settings file, owners can change:

- **Whether the travel commands exist at all.** Turned off, they stop working as soon as the settings are reloaded,
  and leave the command list the next time the server starts or reloads its data packs. Turned back on, they return
  at that same point.
- **How many homes each player may have** (3 at first). Setting it to 0 turns homes off.
- **How long the warmup lasts** (3 seconds at first). 0 means every teleport is instant.
- **The cooldown** for `/home`, `/warp`, `/spawn`, `/back` and `/tpa` (30 seconds at first), and **the cooldown for
  `/rtp`** on its own (5 minutes at first).
- **How far `/rtp` may send players** from the world spawn (5,000 blocks at first).
- **How long a teleport request waits** for an answer (60 seconds at first).

Operators always skip the warmup and cooldowns. Only operators can set and remove warps. See
[Controls]({{ '/controls/' | relative_url }}) for how an operator reloads the settings.

## Related

- [Duels]({{ '/social/duels/' | relative_url }}): nobody can teleport in or out of one.
- [Controls]({{ '/controls/' | relative_url }}): every key and command in one place.
