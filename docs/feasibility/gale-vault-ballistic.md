# Gale Vault: isolated native ballistic experiment

This is a GameTest-only feasibility gate. It adds one client GameTest entrypoint,
four test-only mixins and an exact-body owned context. It does not add an ordinary
attack, move ID, selector, Aura payment, cooldown, renderer animation or damage.
The accepted attack count and the production mod descriptor are unchanged.

## What runs natively

The fixture creates the registered Gale SwordMaster and waits for vanilla downward
collision and ground support. After a 14-tick harmless tell, the first eligible
vanilla `LivingEntity.travel` call receives one velocity write: 0.72 blocks/tick
east and 0.66 up. Original travel performs every translation, collision, gravity,
drag, landing, body push and fall-distance update. There is no repeated launch,
manual `move`, position correction, fall reset, collision immunity or teleport of
a launched Master. The recurrence is a preflight estimate, never a movement driver.

`Entity.move` HEAD/RETURN records requested velocity, actual coordinates,
`onGround`, `verticalCollisionBelow`, ceiling/wall collision and fall distance.
Landing requires prior observed airborne progress, a downward requested move,
actual downward collision, full safe support and agreement with the fixed pad.
`LivingEntity.aiStep` TAIL observes after native travel and pushing. Only a valid
landing can advance to six complete landing-warning ticks, a single harmless
future-release marker and recovery through both admission+80 and marker+36.
Aborts never regain eligibility. A cancelled airborne body must actually return
to ground before its final 36-tick recovery can finish.

## Precisely isolated AI inputs

For the one exact owned SwordMaster only, the test mixin cancels
`Mob.serverAiStep` at HEAD. It stops navigation and clears `jumping`, `xxa`, `yya`
and `zza`. Consequently this fixture does not execute goal/target-goal selectors,
navigation updates, `customServerAiStep`, sensing or queued move/look/jump controls.
This intentionally excludes production Master planner, encounter ownership,
roster, guard, attack, turn and navigation integration from the evidence.
No other entity is intercepted, and `isEffectiveAi` is unchanged. No native
physics method is cancelled or replaced.

NoAI cancels the attempt immediately, and unchanged vanilla behavior suspends
travel, gravity and fall accumulation while enabled. Resume allows native motion
but cannot revive the marker. This does not promise uninterrupted gravity under
NoAI. Removal and final cleanup clear test ownership; no scheduler survives it.

## Terrain and negative controls

The conservative fixed eastward corridor and both exit corridors require flat,
full, ordinary-friction support. Piecewise swept full native body boxes cover the
predicted arc and six settling ticks. Every collision-query halo must first have
completed resident chunks (`getChunkNow`) and fit build height and world border.
No tickets, chunk generation, scanning for another target or alternative route
are used. Fluids, climbables, hazardous and motion-changing blocks fail closed.
The same residency-first bounded body hazard/fluid scan covers each arc segment
and both side-exit volumes, including collisionless hazards over safe stone.
Current geometry is rechecked during the tell, flight and landing warning.

Cases include flat contact, target relocation after launch, preflight ceiling,
wall, slab, edge/void, support/body hazards, blocked exit, separate collisionless cobweb and water
controls on each side exit, border, actually absent
chunk, eight crowded full native bodies including a bystander, newly introduced
ceiling and wall with real collision, actual native midflight knockback, NoAI
suspension/resume, a removed pad with a lower native landing, and midair removal.
The first abort reason is sticky; later actual ceiling/wall collision observations
are retained even when a preceding sweep check already cancelled eligibility.

The lower-floor case preserves the real fall distance and records the native
fall-damage call and its result. Existing `SwordMaster.hurtServer` rejects
unattributed fall damage. This experiment adds no immunity, damage exception or
fall-distance repair. It does not establish that falling damages this Master.

## Counterplay and unresolved gates

The future reverse fan has radius 0.45–2.40, half angle 45 degrees and feet height
−0.05 through +0.65. Dry geometry checks cover both exits, takeoff underpass,
behind-pad space, jump height and radial edges. They never call projected damage.
There is no accepted 24-damage pulse, guard/parry proof, paid 28-Aura admission,
180-tick rest, eight enrolled-player roster proof, ordinary AI integration,
server/client phase protocol, new articulated pose or native observer-camera
presentation proof. Those require separate design and native evidence before a
production attack can be considered. The target relocation is a fixture action,
not a claim of connected player locomotion.

## Verification and evidence

The suite logs `GALE_VAULT_BALLISTIC` summaries and `GALE_VAULT_NATIVE_STEP` records
from the actual native callsites. The 17 movement ticks and approximately
2.815-block apex are assertions derived from the audited vanilla recurrence;
they are not measured successes until a native run passes. The phase marker is
explicitly logged with `paid_ordinary_attack=false damage_enabled=false`.

Independent Java compilation, JUnit isolation guards and vanilla bytecode checks
are separate from native runtime proof. The existing Loom Unix-socket host
restriction is unchanged and must not be bypassed. A source/compile pass alone
must never be presented as a successful ballistic experiment.
