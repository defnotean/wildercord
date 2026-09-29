# Travel commands

Homes, warps, waypoints, teleport requests, `/back`, `/spawn` and `/rtp`, for multiplayer servers. All
server-side, in `dev.wildercord.travel`, wired up by `Travel.init()` from `Wildercord.onInitialize`. The
player guide is [wiki/social/travel.md](../../wiki/social/travel.md).

## Commands

| Command | Who | Handler |
|---|---|---|
| `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/homes` | everyone | `Homes` |
| `/warp <name>`, `/warps` (and `/warp` alone) | everyone | `Warps` |
| `/setwarp <name>`, `/delwarp <name>` | operators | `Warps` |
| `/waypoint add <name> [pos] [dimension]`, `remove`, `list`, `track`, `untrack`, `share <name> <player>` | everyone | `Waypoints` |
| `/tpa`, `/tpahere`, `/tpaccept [player]`, `/tpdeny [player]`, `/tpcancel`, `/tptoggle` | everyone | `TeleportRequests` |
| `/back`, `/spawn`, `/rtp` | everyone | `Teleports` |

`TravelCommands` builds the tree, and only while `travel.enabled` is on (read in the
`CommandRegistrationCallback`, so at start and on `/reload`). Every handler also checks the switch, so
`/wildercord reload` turning it off stops the commands at once, before they leave the tree.

Names (`TravelRules.name`) are trimmed, lower-cased, 1 to 24 characters of `a-z 0-9 - _`. Suggestions come
from the player's own homes and waypoints, the warps, and for `/tpaccept`/`/tpdeny` whoever has asked.

**`/waypoint` and vanilla.** Minecraft has an operators-only `/waypoint` (the locator bar's `list` and
`modify`). Brigadier merges a second literal of the same name into the first and keeps the first's
requirement, so ours would have been operators-only. `TravelCommands.detach` takes vanilla's node out of the
root by reflection (Brigadier's `children` and `literals` maps), then ours is registered with vanilla's
`modify` branches grafted under an operators-only `modify` (not a redirect: the per-player tree sent to
clients drops redirects to nodes outside it) and its `list` as `/waypoint locator`. If the reflection ever
fails, a warning is logged and ours merges in as operators-only.

## One path for every teleport: `Teleports.begin`

`begin(player, kind, where, destination, operator)`:

1. travel on, not in a duel (`Duels.inDuel`);
2. the kind's cooldown, unless `operator` (`Travel.operator`: `Commands.LEVEL_GAMEMASTERS`);
3. `destination.resolve(player)` once up front, so an unreachable place fails before the wait;
4. a warmup of `travel.warmup_seconds`, skipped for operators, creative and spectators.

During the warmup `Teleports.tick` (every server tick) cancels on moving more than 0.5 blocks
(`TravelRules.moved`), a duel starting, death, disconnect or a world change; an `AFTER_DAMAGE` hook cancels
on damage. At the end the destination is resolved **again** (a visited player moves, a home gets built
over), `/back` is set to the departure point, the player is dismounted and moved with
`ServerPlayer.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true)` (which handles changing dimension),
fall distance is cleared, and the kind's cooldown starts. Warmups and cooldowns are in memory only
(server ticks, keyed by UUID).

The commands pass `Travel.operator(player)` for `operator`; tests pass `false` to exercise the warmup with
the singleplayer host, who is always an operator.

### Safe landing

`SafeSpots` (pure) decides whether a column is standable (floor below, feet open or shallow water, head
open, nothing dangerous) and finds the nearest standable block within 3 to the side, 6 down and 4 up. A
strict search (rtp) also refuses water and leaves. `Landing` classifies blocks into `SafeSpots.Cell`s, tries
the exact saved position first (bounding box clear, support within 0.3 below, no danger, head out of
water) so a spot on a slab or carpet stays exact, then falls back to the search with a final bounding-box
and world-border check (`Grid.fits`). Creative and spectator travellers land exactly where they asked
(except rtp, which is always strict).

- `/spawn`: `server.findRespawnDimension()` and `server.getRespawnData()`, then the heightmap top there if
  the spawn block itself isn't standable.
- `/rtp`: random columns spread evenly over a disc of `travel.rtp_radius` round the overworld spawn
  (`TravelRules.randomPoint`), oceans and rivers skipped by biome without loading anything, at most
  `RTP_CHUNK_LOADS` (12) chunks loaded out of `RTP_TRIES` (64) columns, the surface from
  `MOTION_BLOCKING_NO_LEAVES`. Found once before the warmup, re-checked at the end.

### Visuals (`TravelFx`)

Arcane palette (`ElementFx.ARCANE`), built from existing particles only: a flat `CIRCLE` and counter-turning
`RING` re-sent each second (each lives 26 ticks, so a cancelled circle fades at once), a `STAR` in the last
second, a closing `groundRing`, rising motes; a `CRACKED` circle and `magic_break` on cancel; a vertical
`Light.ray` pillar, an inward ring, a flash and motes on departure; a flash, a ground shockwave, a star seal
and a mote burst on arrival, with `blink` and an amethyst chime at both ends.

## Data

| Where | What |
|---|---|
| `Travel.DATA` (`wildercord:travel` attachment, persistent, `copyOnDeath`, not synced) | `TravelData`: homes and waypoints (name to `Spot`), the back location, `requests_off`, the tracked waypoint's name |
| `Warps` (`wildercord:warps` saved data on the overworld) | name to `Spot` |
| memory | pending warmups, cooldowns, teleport requests (`TravelRules.Requests`) |

`Spot` keeps the dimension as a string id, so a place in a removed dimension still loads (and refuses to
be reached). Every `TravelData` field is optional in the codec. Deaths set the back location
(`AFTER_DEATH`); the respawned player gets a clickable "/back" hint (`AFTER_RESPAWN`).

## Teleport requests

`TravelRules.Requests` holds `Request(from, to, here, made)`. One request per (from, to): a new one replaces
it. `/tpaccept`/`/tpdeny` without a name take the newest. Requests expire after `travel.tpa_timeout_seconds`
(checked once a second, and before every answer), and are dropped when either side disconnects or the
target turns requests off; both sides are told in every case. Accepting starts the traveller's
`Teleports.begin` (kind `TPA`) with a destination that looks the host up by UUID at the end of the warmup
(players are replaced on respawn, so no references are kept).

## Waypoints, the HUD and the beam

`Waypoints.Track(name, dimension, x, y, z)` is a clientbound play payload (registered in `Travel.init`, received
in `client.WaypointHud`), sent on track/untrack, when the tracked waypoint is moved or removed, and on join. The
client draws one line top-left (hidden with F3): an arrow turned by `TravelRules.bearing(dx, dz, yaw)`, the name
and the distance, or the dimension's name elsewhere (`Spot.dimensionName`). Every 10 ticks the server sends each
tracking player within 96 blocks of their waypoint (horizontally) a rising `LightOption.RAY` (once a second) and
motes, to that player only (`sendParticles(player, ..., overrideLimiter = true, ...)`).

## Config (`travel` section)

| Setting | Default | Range |
|---|---|---|
| `travel.enabled` | true | |
| `travel.max_homes` | 3 | 0-1000 (0 turns homes off) |
| `travel.warmup_seconds` | 3 | 0-60 |
| `travel.cooldown_seconds` | 30 | 0-86400 (`/home`, `/warp`, `/spawn`, `/back`, `/tpa`, each its own timer) |
| `travel.rtp_cooldown_seconds` | 300 | 0-86400 |
| `travel.rtp_radius` | 5000 | 16-1000000 |
| `travel.tpa_timeout_seconds` | 60 | 5-3600 |

A config file written before the section existed reads as these defaults (the file isn't rewritten; add
the section by hand, or delete the file to have it written again with every key).

## Tests

- `src/test/.../travel/TravelRulesTest` (names, home limits, the default home, drift, cooldown seconds, the
  arrow's bearing, rtp's disc, request replace/answer/expire/cancel/forget) and `SafeSpotsTest` (standable
  rules, strict mode, the nearest-spot search, the world's last word); `WildercordConfigTest` covers the
  section's defaults, an old file and clamping.
- `WildercordTravelTest` (client game test): warmup broken by moving and by harm, `/home` after its warmup
  and the cooldown after it, the homes limit, warps, `/back` both ways, request bookkeeping (with a
  `FakePlayer` stand-in), `/rtp` on safe ground, and a tracked waypoint reaching the HUD. Screenshots:
  `travel_warmup`, `travel_waypoint_hud`, `travel_waypoint_nether`.
