# Reed-Crowned Ferryman encounter

Status: implemented on the development branch, opened 2026-10-13. This document
is not a release announcement. The real singleplayer encounter, all three moves,
phase transition, abandonment, replay-safe bell identity and save/reopen rewards
have native coverage. The registered locator also admits its basin on unmodified
default-preset Reedlands terrain in a bounded survey; this does not measure random
structure frequency. The Drowned
Road route, Reedkeeper conversations and coastal books remain peaceful.

## Place in the world

An optional challenge at the Last Crossing, an old ferry basin in the Luminous
Reedlands. The coast remembers a boatman who counted his passengers twice. The
encounter is a reed-and-timber guardian carrying that title; discovering it does
not establish that every village's historical boatman was the same person.

The player deliberately rings an authentic crossing bell. Walking into a survey
house, reading a book or completing the peaceful coastal route never starts a
fight. A shore entrance and continuous dry walkways give sword users a route to
the guardian. The player can leave the encounter. No temporary water placement,
player-build destruction or forced drowning is part of the first implementation.

## Body and motion

Build an original low raft with reed pontoons, a central woven mantle, a crown
of uneven upright reeds, a suspended front lantern and a long articulated oar.
Its silhouette must be distinct from the Tide Scribe and Bell-Warden. The hull
rocks while moving, the oar leads its sweep, the mantle opens around the lantern
for the wake, and an anchor hangs visibly before its cast. A dedicated renderer
and model expose the same timing state used by server mechanics.

## Encounter contract

- Server authority owns the home, challengers, move, locked aim, tell and recovery.
  The client receives presentation state and never decides whether a hit landed.
- Three moves: a locked oar sweep, a marked anchor landing with a short pull, and
  a lantern wake that marks unsafe lanes while leaving a broad continuous crossing.
  Every damaging event follows a visible tell and has a recovery interval.
- At half health the guardian changes the order and orientation of its crossings.
  The second phase does not shorten a tell already in progress or invalidate a
  previously shown safe lane.
- Each move can hurt a given player once. Spectators, creative observers and
  players outside the arena are ineligible. A target leaving retires its pending
  damage; another player cannot inherit an unannounced locked attack.
- Nearby challengers and world progression may scale the encounter using existing
  tempering rules. The base encounter remains independently testable.
- Abandonment clears pending attacks and resets the guardian after a finite quiet
  interval. Unload/restart must not replay an impact or create a second guardian.
- The authentic bell stores its guardian identity and completion. Copied decorative
  bells cannot summon an encounter. A failed spawn does not consume the challenge.
- Rewards require participation, are attributed once, and introduce a travel or
  observation utility rather than replacing existing combat equipment.

The implemented reward is the Compass of the Last Crossing, a named compass with
an untracked lodestone target pointing to its own bell, plus 80 XP and the field
note. Contributors must have dealt an accepted hit during that attempt and still
be alive in the arena at victory. Resetting or reloading clears participation.
The implementation keeps at most 64 contributors and announced recipients.

## Integration and verification

Implement pure hit geometry and phase rules before the server entity. Register the
entity, attributes, model, renderer, authentic bell and new structure as one
playable slice. Keep generated art and language in owned generator modules.

The native proof must use the actual bell interaction, all three normal attacks,
movement into and out of their marked regions, a real phase transition and death.
Check retirement on target loss, repeated bell use, copied-bell refusal, restart
identity, loot attribution and finite query/entity counts. Inspect the original
model and tells in first and third person with minimal effects as well as full
effects. Existing coastal story progression must remain completable peacefully.

Natural generation, all four structure orientations, chunk-order replay, declared
bounds, entrances and an old-world non-retrofit policy need separate evidence.
An authored test arena alone is not proof of natural placement frequency.
