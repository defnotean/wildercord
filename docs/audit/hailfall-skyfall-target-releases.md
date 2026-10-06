# Hailfall and Skyfall target-bearing releases

This change adds original player choreography and an authoritative accepted-target
timeline for two existing Flow arts. It does not add a damage art, dependency,
asset pack, movement system, or default-on renderer. Native visual acceptance is
separate from source previews and offline geometry checks.

## Timeline and admission

Hailfall reserves presentation ID 15 with 8 ticks of paid windup and 16 ticks of
recovery. Skyfall reserves ID 16 with 6 ticks of paid windup and 16 ticks of recovery.
The original `leap low` strings, base 8 Aura price, base 80-tick individual rest,
discounts, damage factors, status lengths and damage admission remain in effect.
The shared-key client request still accepts only IDs 0–2. IDs 0–14 keep their
original dispatch and timings.

The real accepted server string creates an art-specific receipt after clash
resolution. Its immutable fields copy marks, acceptance tick, owner body/world,
feet, eye, flat aim, full view, and ground point. Targets retain the original body,
UUID, level and copied accepted bounds, never a client-supplied identity. The
public string context keeps its original observed victim for hooks; the receipt
alone authorizes a target-bearing release.

Hailfall can accept the observed victim as a cloud anchor at strict 3D distance
less than 7. It separately earns direct-hit priority only below 5.5. Losing that
narrower direct privilege during windup does not cancel a still-valid cloud
anchor. A body accepted outside 5.5 never gains priority later. The ordinary cut
still selects up to four fresh lawful recipients in its original 3-block,
120-degree cone with half-width allowance and 2.2 vertical bound.

Skyfall records exactly one of three routes: observed victim at strict 3D
distance less than 8.5, nearest forward-cone victim at reach 6 plus its accepted
half-width in 90 degrees and 2.2 vertical bound, or explicit ground. A cone
receipt cannot acquire the observed-victim radius exception at release. Both
cone selection and release use the accepted flat aim and the same current owner
body's feet; free-look never changes the accepted attack direction.

At release an accepted selected body must still be alive, unremoved, registered
as the exact original body in the same world, currently harmable, within its
recorded route, and visible. Its position is then sampled once. Invalid selected
anchor means no cut, cloud, bolt, replacement victim, ground fallback or success
hook. Payment, individual cooldown and the readable accepted whiff/recovery
continue. Invalid caster/equipment/world commitment uses the established physical
interruption behavior and likewise retains paid rests.

## Explicit cover and ground counterplay change

These previously immediate forms now require visible target selection and
same-target release visibility. Hailfall's immediate cut also requires LOS. This
is an intentional gameplay counterplay change, alongside the added windup and
recovery; it is not described as merely cosmetic.

An explicitly accepted no-target ground point is computed once using the
original ahead distance (Hailfall 3.6; Skyfall 4.0) and floor search (up 1.5/down 3).
It remains fixed if the owner moves or looks elsewhere. Acceptance and release
require the same captured world, Hailfall strict 3D distance less than 7 or Skyfall
3D distance at most 6 from the current owner body, and clear collider LOS from
the eye to the saved point plus 0.1 height. Target receipts never convert to this
ground route when they fail.

Cover behavior after a successful release is preserved. Hailfall's fixed cloud
continues its seven original stone impacts at release +4/+6/+9/+11/+14/+16/+19;
the final stone is not shortened to the 16-tick body recovery. Each shared
per-foe counter still admits at most three stone attempts. Skyfall answers exactly
six ticks after release and alone may follow its original body at strict distance
less than 4 from the released point. Dead, removed, replaced, wrong-world or newly
friendly bodies disable tracking; the already-released bolt stays at its released
point. There is no new delayed caster-to-victim LOS test. Both additional Skyfall
arcs still search from the strike origin, not from the prior arc recipient.

Current party, team, PvP, duel and trial admission is rechecked at each existing
damage/status sink. Selection does not cache hostility. The original shared Hits
objects, art attribution, caps and live strike inputs remain unchanged.

## Narrow released-owner repair

The separate first commit binds Hailfall's outer rays and nested +2 stone callbacks
to a `ReleasedArtOwner`. The nested stone uses the captured level after validating
the owner. This closes the gap where a player could die or leave between the ray
and stone, or the stone could reinterpret old coordinates in a different world.

The owner binding is opt-in for these effects. It requires the alive, unremoved,
original connected player body in the original level. Actual death, respawn,
dimension-change and disconnect events retire its token permanently, including a
same-call dimension round trip. Callback equality checks supplement those event
latches. Ordinary weapon removal, method changes, interruption, recovery ending
and physical-continuation cancellation do not retract a released cloud or bolt.

The weak-key token map stores no player, world or server inside its values.
Callbacks own their bounded references until they execute; server stop retires
all tokens and clears the map. No generic scheduler or other arts are migrated.

## Presentation and verification boundaries

Hailfall uses a folded Rime guard and upward hail release. Skyfall raises and
points its blade skyward, holds the call, and has a cosmetic answer at release +6.
That answer remains recovery, not a second physical ACTIVE frame. Both forms
author the articulated body, mirrored independent first-person hands, and rigid
fallback. Existing armor, funded-shell, item socket and unknown-layer fallback
paths remain in use; the experimental renderer remains off by default.

Focused native helpers exercise real observed string requests, original-body
lifecycle transitions, target loss/cover/range/admission changes, payment,
completion hooks and unchanged damage receipts. They are authored separately from
registration and CI wiring. Offline tests and source-driven contact sheets are
not native execution, GPU rendering, material-quality or release acceptance
evidence. The missing-body count drops by two only after those acceptance gates.
