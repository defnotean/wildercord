# Life owner camera correction

This patch is local implementation and test coverage, not a claim of new native visual acceptance.
The available standalone Java 25 verifier checks compilation and JUnit; native client execution
and the supervised two-process comparison remain separate required gates.

## Production boundary

`LifeOutcomeTransport` may deliver one bounded recipe packet to its known source owner even
when every point is near that player's eye. The server cannot know the owner's camera mode.
Unknown and other sources keep the existing eye gate. Existing world/range, loaded-anchor,
128-piece recipe, 512-piece recipient-window, refusal and quiet-renewal gates are unchanged.

`LifeOutcomeClient` retains whole-recipe quality admission, queue age/world checks, the
512-piece total client limit and Full/Balanced/Minimal limits of 512/256/96. It uses the
actual initialized native camera. Only a detached owner camera belonging to the real local
player can relax the player-eye check; the 1.1-block actual-camera check always applies.

The normal `ClientLevel.addParticle` path remains in place, including vanilla particle
settings. Its synchronous provider call receives scoped source/owner metadata. Only owner
outcome particles consult `LifeOwnerClearance` on every quad extraction. Switching to first
person immediately reapplies eye safety even before the camera's next update. Camera clipping,
free-look and later movement use the current extraction camera. A view change never re-emits,
deletes, extends or duplicates the particles. Life recipes retain their ten-tick lifespan and
support materials retain eight ticks. Other Life/material particle sources are unchanged.

No damage, status, target selection, party rules, rune recipes, packet schema or gameplay
scheduler was changed.

## Preserved connected-owner fixtures

`LifeOutcomeGallery` resolves the rendered client UUID to its exact current `PlayerList`
entry and `connection.player`. That connected body pays in Survival and remains the native
camera entity. Camera mode and cast orientation are prepared before payment. Capture queues
the original event, including its original stage and recipient. It never teleports an observer,
restores a pre-cast position, or re-aims a cast during its three-tick preparation/release window.
Real Bloomstep movement remains authoritative; only the native front/back view may change
after arrival to select an unobstructed camera ray.
Pending captures are cleared at client-world boundaries; capture additionally requires the
same actual client/server world pair and an event age from zero through five ticks.

Preserved budgets and assertions:

- Core: 14 paid calls, the original wait sequence totaling 386 ticks. Bolt still requires
  actual hostile damage and Poison. Its obsolete spectator-overlap claim was removed;
  `SPECTATOR_BOLT_VENOM` remains the separately registered genuine two-client proof.
- Stateful: 22 paid calls across 18 cases, the original wait sequence totaling 1937 ticks,
  plus the existing learned-foreign-innate payment refusals. Bloom keeps its original APPLY
  rather than substituting a later pet PULSE.
- Spore support: five scenarios, 325 ticks. Original flat/slab/airborne/protected/no-effect
  conditions remain, with loaded block and body rays from the real owner camera.
- Pulse Ferry: four scenarios, 344 ticks. Allied recipients may remain passive fake bodies;
  only the caster is required to be the exact connected owner. Existing party, crouch,
  no-heal, removed-recipient and movement assertions remain.

## Native registration

The native descriptor registers both new entrypoints. The focused Masters group now has
38 entries: its original 36 remain in the same relative order, with both additions immediately
before the final presentation entry. Existing workflow behavior and permissions are unchanged;
only the displayed focused/paired counts change to 38/46.

The two exact entrypoints are:

1. `dev.wildercord.cast.LifeOwnerCameraTest`: six separately named paid connected-owner
   scenarios, native first/front/back, Full/Minimal, reduced flash, and a real blocking wall
   that clips the native camera. It checks actual extracted quads, source identity, original
   particle identities/lifetimes, third-to-first-to-third transitions, post-release look
   changes, eventual expiry and native screenshots. It is not a substitute for manual
   inspection of those screenshots.
2. `dev.wildercord.cast.LifeOutcomeTransportTest`: transport-negative diagnostics only.
   Its explicitly manufactured diagnostic events prove unknown/foreign source eye gates,
   refusal, mute, range, exact full-recipe recipient cost and budget exhaustion. They are
   not paid-cast or visual evidence.

Retain the existing `LifeOutcomeQualityTest`, `LifeOutcomeRecipeTest`,
`LifeOutcomeSurfaceTest`, `LifeOutcomeSourceTest`, `LifeOutcomeEdgesTest`, migrated four
fixture classes, `BloomstepArrivalCaptureTest`, party mutation/lifecycle tests, and existing
negative no-effect support tests. Quality adds an actual native camera switch for both Life
and Time particles plus the foreign-source third-person eye gate. Existing edge tests retain
quiet paid renewal, expiry, removal, cancellation, claims and cooldown checks.

## Four additional supervised comparisons

`LifeOwnerPairedCases.CASES` declares only these additional IDs:

1. `LIFE_SELF_HEAL_FIRST_PERSON`
2. `LIFE_SELF_HEAL_THIRD_PERSON`
3. `LIFE_SELF_HEAL_CAMERA_TRANSITIONS`
4. `LIFE_SELF_SECOND_WIND_REDUCED_FLASH`

The contract, exporter and supervisor preserve all existing 42 cases in their exact order
and append these four explicit IDs, for 46 total. Each additional case performs exactly one
ordinary paid self cast. Second Wind samples the genuine finite ward's original APPLY,
retaining both Life and Time material; the original Stateful suite still proves its lethal
TRIGGER. Both roles are distinct loaded connections in Survival. The owner uses own Full;
the second viewer uses others Minimal with an opposing own setting. Independent native
samplers retain exact source metadata, original object identities, counts, extracted quads,
lifetimes and reduced-flash alpha while particles are actually alive.
Screenshot readiness requires insertion into a native ParticleGroup, not merely an entry in
particlesToAdd. Front/peer screenshots have separate block/body ray checks. The rear-camera
transition proves clearance and extraction only, because the player's body can occlude a
recipe authored on its front surface.

The wired handshake for each of these four IDs is:

1. Host calls `runConnectedPair(context, server, hostUuid, peerUuid, preparePeer, observed,
   completed)` after the preserved cases. The `preparePeer` callback publishes the existing
   provenance envelope plus the supplied case/source/entity fields and waits for the real
   peer's acknowledgement. Do not advance payment until acknowledgement.
2. Peer handles prepare by verifying the envelope and invoking
   `armPeer(context, caseId, hostUuid)`, then writes its armed acknowledgement. Before waiting
   for the later ready message, call `capturePeer(context, caseId)` so its native screenshot
   is taken while the original material is alive.
3. Host performs the actual payment. Its `observed` callback waits for the peer
   captured acknowledgement before publishing ready. Peer calls `finishPeer(context, caseId, proof)` and includes the
   independently produced fields in its seen acknowledgement. It must not copy host
   measurements as peer evidence. Host verifies source identity, peer piece/unique counts
   equal `lifeMinimalPieces`, `lifePeerExtracted > 0`, native camera status, and positive
   Time/reduced samples for the fourth case before marking completion.
4. Call `abortPeer(context)` on failure to restore camera/quality settings. The helper also
   restores both real server modes and the host's prior innate setting on exit.

The existing 900-second supervisor ceiling is unchanged. Supervisor validation requires all
six named phase witnesses for each added case with exact nonce/source/role/PID/body identity,
original APPLY stage, one complete recipe and independent reduced-flash/Time measurements.
Tests reject a 42-only contract or terminal ledger, missing phases, substituted source/viewer
roles, malformed or duplicate measurements, queued-only capture claims and absent support.
Its owner/peer screenshots and sampler evidence are additional artifacts under the same
source/descriptor/connection provenance; they do not retroactively establish older cases.

## Verification status

Static comparison preserved all four existing wait/call budgets and `git diff --check`
passes. Independent review and the serialized standalone compile/JUnit receipt are recorded
by the integration lead. Native results, image inspection, and real paired comparison
results are pending execution in the supported native environment.
