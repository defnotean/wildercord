# Stationary owner: negative movement control

Entrypoint (deliberately not registered in the ordinary test descriptor):
`dev.wildercord.aura.StoneHingeStationaryOwnerNegativeTest`

This bounded diagnostic tests the rejected composition, not a playable Stone Hinge:

1. Create two fresh real connected Survival scenarios with neutral input, resident flat stone, no pending native teleport acknowledgment, identical client/server position, and verified zero pre-hit horizontal velocity. Setup arrival happens before observation. Midnight prevents the ordinary Zombie fixture introducing sun-fire effects.
2. In each, use the full actual Mob.doHurtTarget receipt to establish a frontal nonlethal wound and its native default impulse. The first scenario leaves all movement and packet delivery ordinary.
3. In the second, execute exactly one native server `move(PLAYER, .3 lateral)` with the complete native post-hit velocity still installed. Assert the actual displacement and unchanged full velocity, Y, fall and ground state. Only then subtract the recorded horizontal impulse and send the proposed ordinary native motion packet. Zero pre-hit X/Z therefore produces zero delivered X/Z.
4. Observe actual packet sends, owning-client motion processing, client tick physics, genuine LocalPlayer.sendPosition sends, and their ordered matching native server position-processing result. Bind processed motion to the actual expected server packet/vector (including its native wire encoding), and require a post-motion positional send whose changed Y cannot alias an older stationary ground packet. Keep automatic synchronization flags unchanged, so duplicate native motion deliveries remain observable. Corrections are explicitly labeled and can never supply a successful controlled-step witness. Require nonempty pre-correction owner observations and measure absolute lateral displacement, including the opposite direction.
5. Require the expected incompatibility: no uncorrected owner lateral step, followed by loss of the server-only offset or a correction. Compare the matching native vertical inputs/impulses and first owning-client vertical motion; report both observed rise profiles and packet counts without declaring later vertical profiles equivalent.

All observations are bounded to 256 events per case and a fixed short post-delivery window. Hooks forward original operations exactly once. Snapshot/logging/overflow faults are recorded as deferred fixture failures; they never skip native operations, and teardown runs even if a scope assertion fails. No movement packet, teleport acknowledgment or input record is fabricated. No production protocol, controller, teaching, counter or six-step movement is implemented.

Even an expected-negative pass ends with:
`movement_gate=NOT_PROVEN gameplay_enabled=false`

The conditional form-policy shared rest is **120 ticks from the paid brace**, not the earlier 160-tick proposal. This diagnostic does not implement payment, rest or availability.

After this experiment, review either an explicit owner-predicted/server-validated movement protocol or a newly specified velocity-only redirect as separate proposals. Neither is approved by a negative control. Neither may silently substitute teleport corrections, reset falling physics or present a server-only displacement as the owning player's step.

The complete class is registered only in the development GameTest descriptor and may run as `diagnostic-stone-hinge-owner-negative`. The diagnostic requires both distinct observed world seeds, full setup/run/cleanup/return, and the exact `movement_gate=NOT_PROVEN gameplay_enabled=false` terminal. It is excluded from the Masters and articulated release groups, while remaining in the complete aggregate descriptor. Passing this expected negative control does not enable or validate a movement ability.
