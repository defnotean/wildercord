# Stone Hinge: native feasibility decision

Status: **Do not implement or enable the proposed six-step form yet.** The damage/impulse observation seam is supported by the exact native bytecode. The gameplay movement transaction is not proved. This patch contains game-test instrumentation, native diagnostic cases, and a repeatable bytecode audit only. A green diagnostic run must not be presented as a shipped Stone Hinge, a successful six-step redirect, or approval to suppress knockback.

Baseline: local commit `2144eed610a3a075839e9d2fb389c0d8891a3d44`, tree `faf7fd22358263136859614eb2e3dadee8f703e9`. The lead identifies this as the published checkpoint tree; that remote commit is not present in this isolated clone and was not independently fetched.

## Why the current movement approach fails the gate

`WallTurn.move` teleports the body each step, writes `Vec3.ZERO`, and reconstructs fall debt. Those are intentional Wall Turn behaviors, but they do not preserve Stone Hinge's native Y/fall/ground physics. Reusing that method would silently replace the very impulse the new mechanic claims to redirect.

The 26.3 `ServerGamePacketListenerImpl.teleport(PositionMoveRotation, Set<Relative>)` path sets `awaitingPositionFromClient`. `handleMovePlayer` calls `updateAwaitingTeleport`; while acknowledgment is pending, normal position processing does not run. Merely restoring the server-side Y velocity/fall distance after a horizontal teleport therefore does not establish that the connected client continues normal vertical movement over six steps. This is a concrete missing proof, not a claim that every possible new movement implementation is impossible.

Rotating X/Z velocity by itself preserves native Y, but it consumes the original impulse before a controlled lateral movement step has actually succeeded. A positive collision query before a later client movement packet is not an accepted first movement step. If the lane becomes blocked or the movement is refused, that shortcut has removed ordinary knockback without paying the required lateral step. This implementation is rejected.

The diagnostic deliberately **never changes the captured impulse**. Its real wall/lava/powder-snow refusals establish that admission can inspect native collision geometry without changing position, velocity, fall distance, or on-ground state. They do not claim to execute a controlled movement step.

## Exact native ordering

Audited dependencies:

- Minecraft 26.3 client JAR SHA-256 `4508d006323f24fa02876310c192d739af56516eb259000ac50f0909a68c9a2d`
- Fabric entity-events 6.0.4+3434d6d95d JAR SHA-256 `a5a9e382e4f9875f7450dbf0d45221ca70afc00cf84aa02023104bb15a530ade`

The read-only `tools/check_stone_hinge_native_contract.py` emits dependency hashes and fourteen explicit checks, including exact audited dependency identities. It labels both `native_gameplay_executed` and `stone_hinge_movement_approved` false even when the bytecode check passes.

1. ServerPlayer and Player admission/PvP/difficulty checks precede the LivingEntity damage implementation. Wildercord's LivingEntity wrapper retains party protection, Aura Step, weakening, guard and armour behavior.
2. Fabric ALLOW_DAMAGE runs at the native `isSleeping` invocation, before damage. Foresight can reject the whole hit, or re-enter `hurtServer` with the same source for a remainder. Such nested calls must not share a receipt.
3. Player `actuallyHurt` applies armour and magic mitigation, writes absorption, then writes health. The receipt must observe this operation, not infer a wound from `hurtServer` returning true.
4. LivingEntity `hurtServer` calls default knockback after `actuallyHurt`, when applicable. A cooldown-rejected hit, a stronger hit during cooldown without its default impulse, full shield blocking, NO_KNOCKBACK, or resistance may leave no qualifying native write.
5. Native `knockback(DDD, DamageSource, float, boolean)` multiplies strength by `1 - KNOCKBACK_RESISTANCE` and returns for nonpositive strength before writing velocity. For positive strength it halves prior X/Z and adds the impulse. Grounded Y becomes `min(0.4, oldY / 2 + strength)`; airborne Y stays exactly oldY. The actual setDeltaMovement invocation is necessary evidence, but only the exact default-knockback callsite may associate it with this wound. Same-source callback knockback is not that default impulse. A changed aggregate velocity at the end of the tick is not equivalent evidence.
6. Default knockback precedes the second native death check and totem handling. Fabric's ALLOW_DEATH intercepts that second check. A lethal wound later saved by Reversal, spellguard, another death-save, or a totem remains lethal for this proposed trigger.
7. Fabric AFTER_DAMAGE runs at the LivingEntity tail for surviving bodies. Mana Skin and other restoration callbacks can hide net loss. The pre-restoration native receipt remains authoritative; a callback can also produce additional impulses or nested hits, in which case this conservative prototype refuses the association.
8. `Mob.doHurtTarget` and player attack paths may add knockback after `hurtServer` returns. An implementation that commits inside `hurtServer` before the caller finishes can accidentally suppress one part of a multicomponent hit. The diagnostic refuses multiple/orphaned native writes; support for extra knockback and player attacks is a separate integration gate.

The existing `MasterHitReceipt` is scoped to Master spellbreaks and Driving Cut. It is not changed or promoted into a global melee registry here. The diagnostic has a separate per-invocation receipt, exact source identity, nested-call contamination, and finally cleanup. Association requires all three native scopes: `hurtServer -> dealDefaultKnockback`, that method's exact five-argument `knockback` call, and the first matching six-argument invocation reached from it. Every six-argument invocation installs its own scope, so a nested or callback invocation cannot inherit the outer native-write association. All original operations run exactly once. An AFTER_DAMAGE callback remains unassociated even if it uses the exact source object and is the only positive velocity write.

## Source classification

Proximity and a living direct entity do not identify melee. Ordinary Master SWEEP/THRUST, Crescent hits, afterburn/field hits and other projected attacks all use Aura damage with the Master as direct entity and owner.

The test-only prototype grants provenance only at two exact callsites:

- The vanilla `Mob.doHurtTarget -> Entity.hurtServer` operation, with its actual source.
- The current Master `tickAttack -> projected` invocation while the real released attack is SWEEP or THRUST. `AuraFighter.projected -> MasterHitReceipt.source` exposes the just-created exact source before AuraElements can re-enter damage.

A generic Aura hit, a close Master.projected call outside that release, a handcrafted mob-attack source, reflected or projectile-tagged damage, explosions and nested same-source damage are excluded. The frontal test uses `AuraGuard.facing`, not `faces`: the latter includes Bulwark's all-direction guard and would improperly admit rear hits. Coincident/degenerate horizontal origins refuse.

The diagnostic snapshots legal hostility and frontal geometry at damage admission, and requires an original connected Survival body, nonlethal accepted health/absorption loss, exactly one associated positive native impulse, and unchanged dimension, position and final native velocity at synchronous admission. It never changes damage. Full resistance/Anchor cannot purchase distance; partial resistance records the smaller actual impulse.

This conservative prototype does not classify player attack methods, custom add-on melee, Master BREAK_CAST/Pursuit, Cinder Wake, Reprise or Stone Fracture. Those need separately reviewed exact provenance; do not add them by broadening the Aura damage-type check.

## Native diagnostic and its limits

Entrypoint: `dev.wildercord.aura.StoneHingeFeasibilityTest`

Test-only Mixin configuration: `stone-hinge-proof-gametest.mixins.json`

The fixture uses the actual connected client Survival body from the disposable test world. The server PlayerList is only read to resolve/verify that body. It creates real native mobs and a real enrolled Stone Master; it never creates or inserts fake players and never installs synthetic CHARGE. Health, equipment, attributes and progression needed by a specific defence are declared fixture setup, not claimed acquisition evidence.

The intended native cases are:

- Bare Mob.doHurtTarget, absorption-only loss, Mana Skin rebate, and an explicit AFTER_DAMAGE restoration callback.
- Partial and full native knockback resistance, Resistance V with no accepted loss, native invulnerability, rear hits and source-provenance refusals.
- Nested other-source/reused-source callbacks, a second actual native impulse during AFTER_DAMAGE, a stronger native-cooldown hit whose only positive write is a same-source AFTER_DAMAGE callback, and a real Knockback-enchanted weapon whose caller-supplied impulse occurs after hurtServer returns.
- Real perfect Aura Guard from connected sneak input, real paid Aura Step, a real fully stopping Foresight ward, its actual same-source reentrant remainder, actual Anchor, and actual Reversal/totem lethal saves.
- Real whole-body first-step geometry against wall, lava and powder snow, with original native motion preserved on refusal.
- A genuinely falling connected body whose native knockback leaves Y/fall state untouched.
- The existing Stone Master's naturally selected, fully warned THRUST against its real opted-in challenger. The test never writes attack/timer fields. A nearby call to the same Master's projected pipeline outside the exact melee release must remain unclassified.

Run the isolated native diagnostic through the reviewed native CI path; the lead owns suite/catalog wiring and publication. Standalone Java compilation proves types only. It cannot prove Mixin injection application, native callbacks, actual connected motion, or the above game assertions.

Even a successful run above proves **only the receipt, source, native impulse and refusal precursors**. It intentionally ends with `movement_gate=NOT_PROVEN gameplay_enabled=false`. No screenshots or new content art are required for this gate.

## Conditions for reopening the gameplay proposal

Before any form content is implemented, supply a movement transaction that:

1. Keeps ordinary damage, native resistance, vertical impulse, gravity, falling distance and real on-ground handling authoritative.
2. Associates exactly one accepted nonlethal melee wound with the native impulse it will redirect after all relevant callbacks and caller-supplied impulses are accounted for.
3. Does not consume or weaken that impulse until its first actual lateral movement step succeeds. A refused move, changed wall/hazard/ward, lost connection/body, callback cancellation or native movement rejection retains the original impulse.
4. Proves a maximum of 1.8 controlled lateral blocks across six swept steps on a real connected client, including falling, grounded lift, acknowledgment delay, changed collision geometry, hazards, control cancellation and later external impulses. No no-gravity flag, Y reset, regenerated fall debt, or client on-ground forgery is acceptable.
5. Defines the policy for failure after an accepted first step without continuing hidden knockback immunity or a free counter.

The proposed 20 Aura/eight-second shared rest, 6-tick preparation/12-tick catch, optional separately warned 8-tick one-target reply and 16-tick recovery remain proposals. Costs, weapon/control/ward restrictions, cancellation exceptions and reply eligibility have not been approved by this artifact.

Do not change the existing damage-cancels-form rule globally. Any future exception must name the exact successful receipt and the exact paid catch session; all unrelated damage keeps cancelling as before.

Do not implement teaching, animation or persistence yet. If this gate is later passed, migration must retain legacy Wall Turn learned/equipped state, shared ready-at time, airborne-use debt, practiced state, recovery and landing commitment. The one rebindable C/form slot must remain one slot; Stone Hinge competes with Wall Turn.

## Diagnostic integration boundary

The full descriptor appends Reweave and Stone Hinge after its existing entries. The fixed
`progression-feasibility` case selects only those two complete classes, in that order;
Masters 38, articulated 6 and paired 46 remain unchanged. A passive server-thread seed
receipt preserves the original world settings and RNG. Exact ordered runner cleanup and
return evidence is required for both classes. This does not open the movement gate above.

Held Shift is released in finally, and callback state, attacker disposal and all five
probe thread-locals are cleaned on the server thread before world closure. Idle assertions
run before reset so cleanup cannot hide an unbalanced proof. A native callback-exception
case requires the next real melee capture to remain independent; it is authored, not yet
observed natively. Master disposal also runs if probe teardown fails.
