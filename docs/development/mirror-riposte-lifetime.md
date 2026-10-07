# Glacier Mirror and Static Riposte released lifetimes

This first, separately reviewable change repairs only the two existing arts' released effects. It does not add their fixed-release profiles or claim native acceptance.

Glacier Mirror captures the original connected player body and ServerLevel before its first damage callback. Its fifty-tick projectile ward and bounded cosmetic field share that original lifetime. Field eviction does not cancel the ward. Expiry or stale field cleanup removes only its own ward identity, never a newer ward. The pane follows live facing, matching the original live-facing reflection rule. The one-tick ownership transfer of an already admitted reflection survives ordinary ward expiry, but cannot survive retirement of the original owner or removal/world change of that exact projectile. Reflection aim, quarter-faster speed with the original cap, and all unrelated wards are unchanged.

Static Riposte captures each exact conductor, UUID and world. Every one-tick hop revalidates the original owner, the original prior conductor, the current target, current party/team/duel permission, and the original strict distance below five blocks. The original four hops, nearest distinct selection, damage decay and lack of chain LOS are unchanged. A lethal source remains a conductor while that exact dead body remains loaded. Removed, replacement or foreign-world bodies cannot conduct. Ordinary weapon or method changes and physical interruption do not retire already released electricity.

The subsequent presentation slice supplies native lifecycle/callback tests together with the earned-guard input and fixed-timeline acceptance tests. Offline compilation or pose images alone are not native gameplay/render acceptance.
