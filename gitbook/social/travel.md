# Getting Around

## What it is

A full set of travel commands: **homes**, public **warps**, personal **waypoints**, **teleport requests** to and from
friends, and **/back**, **/spawn** and **/rtp**. Every teleport starts with a magic circle at your feet and a short
countdown.

## How to use it

Words in `<angle brackets>` are needed; words in `[square brackets]` can be left out.

| Command | Does |
|---|---|
| `/sethome [name]` | Sets a home where you stand. No name means **home**. The same name again moves it. |
| `/home [name]` | Teleports you to a home |
| `/delhome <name>` | Removes a home |
| `/homes` | Lists your homes. Click one to go there. |
| `/warp <name>` | Teleports you to a warp |
| `/warps` | Lists the warps. Click one to go there. |
| `/waypoint add <name> [x y z]` | Marks a waypoint here, or at the coordinates you give |
| `/waypoint remove <name>` | Removes a waypoint |
| `/waypoint list` | Lists your waypoints. Click one to track it. |
| `/waypoint track <name>` | Shows the way to it in the top-left corner of your screen |
| `/waypoint untrack` | Stops showing it |
| `/waypoint share <name> <player>` | Sends a waypoint to another player |
| `/tpa <player>` | Asks to teleport to a player |
| `/tpahere <player>` | Asks a player to teleport to you |
| `/tpaccept [player]` | Accepts a request (the newest, without a name) |
| `/tpdeny [player]` | Turns a request down |
| `/tpcancel` | Takes back the requests you've sent |
| `/tptoggle` | Stops anyone sending you requests. Again to allow them. |
| `/back` | Returns you to where you were before your last teleport, or where you died |
| `/spawn` | Teleports you to the world spawn |
| `/rtp` | Teleports you somewhere random in the Overworld |

### The warmup

Stand still for **3 seconds** while the circle forms. The teleport is cancelled if you **move** more than half a block
(looking around is fine), **take damage**, start a **duel**, die or change worlds. A cancelled teleport starts no
cooldown.

Operators and players in Creative mode teleport at once.

### Cooldowns

Each command rests after use: **30 seconds** for `/home`, `/warp`, `/spawn`, `/back` and `/tpa`, and **5 minutes**
for `/rtp`. Each has its own cooldown. Operators have none.

### Landing safely

If your destination has become unsafe (built over, lava, fire and so on), you land on the nearest safe ground close by.
If there is none, the teleport is called off.

### Homes

- Up to **3 homes**. Names use letters, numbers, `-` and `_`, up to 24 characters. Capitals don't matter.
- Homes work across worlds and are kept when you die.

### Warps

Public places anyone can visit. Only **operators** can make them with `/setwarp <name>` and remove them with
`/delwarp <name>`.

### Waypoints

Waypoints never teleport you. They point the way on foot.

- A tracked waypoint shows an arrow, its name and its distance in the top-left corner. Within **96 blocks**, a faint
  beam rises from it that only you can see.
- A shared waypoint arrives in chat with an **[Add]** button. You can share with the same player once every 10 seconds.
- Up to **50 waypoints**.
- Minecraft's own operator `/waypoint` list is now `/waypoint locator`; `/waypoint modify` still works.

### Teleport requests

- The other player gets **[Accept]** and **[Deny]** buttons. A request lasts **60 seconds**.
- A new request to the same player replaces the old one.
- When accepted, the traveller goes through the warmup and lands next to the other player.
- You can't send a request to someone in a duel. `/tptoggle` is remembered when you log out.

### /back, /spawn and /rtp

- `/back` counts as a teleport, so a second `/back` undoes the first. After you die, it takes you to where you died.
- `/spawn` goes to the world spawn, not your bed.
- `/rtp` lands you on solid surface ground up to **5,000 blocks** from the world spawn, never in water or lava.

## Tips and counterplay

- You can't teleport during a [duel](duels.md).
- Set a home right after an `/rtp`: your coordinates are shown in chat when you land.

### For server owners

Change these in the `travel` section of `config/wildercord.json`, then run `/wildercord reload`:

| Setting | Default | What it does |
|---|---|---|
| `enabled` | `true` | Turns the travel commands on or off |
| `max_homes` | `3` | Homes per player. 0 turns homes off. |
| `warmup_seconds` | `3` | Warmup length. 0 makes teleports instant. |
| `cooldown_seconds` | `30` | Cooldown for `/home`, `/warp`, `/spawn`, `/back` and `/tpa` |
| `rtp_cooldown_seconds` | `300` | Cooldown for `/rtp` |
| `rtp_radius` | `5000` | How far from spawn `/rtp` may send players |
| `tpa_timeout_seconds` | `60` | How long a request waits |

See [Controls](../controls.md).
