# Collapse and Red Rain ground releases

Collapse reserves presentation ID 17 with an 8-tick paid windup and 18-tick
recovery. Red Rain reserves ID 18 with an 8-tick paid windup and 16-tick recovery.
Both keep their Flow admission, original leap/low string, base 8 Aura payment and
80-tick individual rest. Acceptance pays once; a cancelled windup retains its
paid rest and recovery and creates no field or completion hook. Successful
release invokes the existing performer and completion hooks once, preserving the
original string marks, observed victim and acceptance time. Neither performer
uses that observed victim. IDs 0–16 keep their existing dispatch and timing;
shared-key requests still admit only IDs 0–2.

## Explicit ground policy and counterplay

These untargeted forms use the explicit `GROUND_AHEAD` policy. Their center is
computed from the original owner's feet at release plus accepted horizontal
facing. Movement during windup changes the launch position; free-look does not
change the accepted direction. This deliberately differs from Hailfall's
accepted ground receipt. Once released, the field center stays fixed.

The original floor search remains in use: sample ahead plus one block vertically,
search up 1.5 and down 3, and retain the ahead point if there is no floor.
Collapse stays 2.5 blocks ahead, pulls within radius 4.5 on ages 2/4/6/8/10/12/14/16,
and crushes within radius 2.2 at age 16. Red Rain stays 2 blocks ahead with its
radius 3.5 immediate burst and 40-tick field. Its field continues to bleed on
ages 10/20/30/40. Original target counts, falloff, damage factors, pull strength,
status lengths, shared per-performance PvP/stance caps and current strike inputs
are unchanged.

Red Rain's immediate burst now requires owner line of sight. The query applies
visibility before its existing target cap and each selected target is checked
again before striking. This is an intentional counterplay change alongside the
paid windup and recovery. The delayed rain and Collapse pull/crush keep their
existing cover behavior; neither acquires a caster-to-target visibility gate.

The immediate burst and all Red Rain pulses share the original Drink instance:
30% of actual health taken, at most 4 health across the performance and still
within the common mending bucket. Legitimate kill damage can heal; there is no
new target-survival condition. Owner or field retirement suppresses subsequent
field-specific Drink healing. The common hit path can still finish an already
admitted immediate burst hit, including its separate Crimson on-hit passive.
Current party, team, duel and Master trial rules continue to be checked at the
existing damage/status/physics sinks, including changes inside damage callbacks.

## Opt-in released ownership

Only these two forms opt into the new `ArtFields.openReleased` entrypoint. A
`ReleasedArtOwner` captured before the first effect binds the release to the
exact original connected body and level. Death, respawn, disconnect and world
change permanently retire the token, including a same-call world round trip.
Current body/level checks supplement lifecycle events. The rain cannot open a
new field after an immediate damage callback has retired that release.

Ordinary weapon changes, method changes, physical interruption and recovery
completion do not retract an already committed field. Existing callers of
`ArtFields.open` retain their previous lifetime. Field caps, replacement order,
end callbacks and silent server-stop cleanup remain unchanged.

An invalid released owner also retires its currently running field. The field's
existing exact ephemeral Effects source-scope identity stops remaining damage,
status and physics mutations while that pulse unwinds. Nested independent
source scopes keep their own admission. Both performers recheck field activity
after damage before continuing their field-specific work.

## Presentation and acceptance

Collapse uses a braced downward drive; Red Rain uses a separate descending rake.
Both author rigid fallback, articulated body and first-person hand motion with
left/right mirroring. Existing held-item, armor and funded-shell socket paths
remain in use. Animation samples the announced server timeline and never
shortens it to fit a cosmetic beat. The articulated renderer remains opt-in.

The focused native entrypoints are
`dev.wildercord.party.MastersGroundFieldTimelineTest` and
`dev.wildercord.aura.GroundFieldReleasedOwnerTest`; registration and CI wiring
belong to the integration lead. Existing ArtFieldMutationSafetyTest retains its actual
party join/disband, duel-ending, retired-pulse and nested-source callback probes.
New fixtures cover paid release timing, owner movement, floor fallback, cover,
pulse/healing receipts and original-body lifetime. Compile/JUnit and offline
motion/composition evidence do not constitute native execution, GPU rendering,
material-quality or release acceptance. Native and pixel acceptance remain
pending the actual CI run.
