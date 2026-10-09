# Lifeline: Circle XIV cooperation

Second lesson of the authored pack, using the same architecture as Tollgate.

## Use case
Pull a friend out of trouble: off a ledge, out of a mob, or across lava you cannot cross yourself. Swap trades places with whatever it hits, and Pull drags foes. Lifeline moves only a consenting ally, and only to safe ground beside you.

## Discovery and lesson
- Feat **Starbreaker** (Star Eater) makes *The Thread Between Stars* retrievable.
- A three-page live reading with active Circle XIV teaches the rune. Old saves are covered by the feat.
- Practice is recorded on the first reel.

## Rules
- The row must be exactly Beam + Lifeline.
- The first press threads one ally (party member, own or team pet or summon) in sight within 16 blocks, for 8 s. This costs 28 base mana once, with a 10 s shared rest.
- A second fresh press reels the ally free of charge. They land on the nearest safe cell beside you (`Effects.safeSpot`) that has no fluid, is buildable by you and is in your dungeon ward.
- The thread snaps beyond 24 blocks, or on:
  - death or a world change
  - disconnecting
  - the ally stopping being an ally, such as leaving the party
- Sneak + Cast lifts it.

## Counterplay and consent
- A player ally who crouches or sleeps refuses the pull.
- Riders and mounts cannot be reeled.
- The reel needs sight.
- Claims and wards are checked at both ends, so nobody can be pulled out of a protected area or into a ward.
- Enemies can never be threaded.

## Delayed ownership
The thread is bound to the original connected caster body (`ReleasedArtOwner`). It retires when that body is replaced by respawn or reconnect, or when the server stops. Nothing is restored on rejoin.

## Presentation
- Beam pose.
- Lead-tied sound.
- A thin strand drawn every 4 ticks.
- On reel: an amethyst chime and a bright pull ray.
- Lead-break sound when the thread snaps.
- Arcane beam signature.
- HUD reads "press <key> to reel".

## Verification
- Unit tests cover the shared rules.
- The native test threads and reels an owned tamed wolf, checking cost, the free reel and practice.
- Party members, the crouch refusal and claim checks need a second connected player and are left for CI.
