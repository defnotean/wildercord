# Physical magic and custom presentation expansion

Implemented after the first audit pass. All changes remain uncommitted. Player instructions are in `../features/physical-magic.md`; the verification ledger below distinguishes completed checks from gates still running.

## Implementation scope

1. Replace vanilla spell decoration through the shared send path with original material sprites and distinct particle motion: ember, frost, storm, wind, stone, petal, void, arcane, clockwork, blood, water and vapour. Preserve the existing authored per-rune sequences and add explicit scripts and emblems for every new rune. Audit direct particle send bypasses too.
2. Add Strata Rise, Tidal Lift and Wind Steps as physical world interactions, plus six specific fusion variants with different mechanics and authored casting/impact sequences.
3. Exact woven effects now combine further into canonical, flat compositions of up to eight leaves. Larger weaves require higher tiers and XP. Unit coverage iterates the complete elemental effect roster, including innate soul weaving. Valid Knots remain the composition route for shapes, modifiers and links.
4. Verify terrain restoration, collision, protected blocks, overlapping casts, water sources, no item duplication, cancellation, save/expiry behaviour and defensive counterplay. Improve relevant QoL/readouts and fix concrete audit findings.
5. Regenerate art/resources, run focused gameplay/visual/shader checks and the relevant regression gates, archive screenshots, rebuild the release/profile packs, and restart the dev client with the final changes.

## World interaction design

* Strata Rise raises a real temporary wall in stages, with collision and a finite lifetime; it never overwrites occupied or protected spaces.
* Tidal Lift borrows a bounded set of nearby real water sources, lifts and directs them into a visible attack, and restores them. Borrowed cells use a temporary reservation to prevent uncontrolled fluid duplication.
* Wind Steps creates clearly outlined collision platforms with a finite lifetime; different fusion variants offer frost stability or storm counterattacks. Terrain and player collision stay authoritative on the server.
* Every interaction obeys existing build permissions, loaded-chunk boundaries, cast budgets and ownership/team targeting rules. Support spells are useful on terrain as well as creatures.

## Previous verified release

Before this new scope, the build passed 460 unit tests. The full client gate passed in 44m 40s, followed by expedition, 39-shape turning and enabled-shader checks. That is historical evidence, not verification of the later expansion. The dev client was closed before compilation and testing.

## Implemented changes

- Three physical base runes and six signature variants, each with an explicit three-gesture script, illustrated emblem, ring and item icon; 338 registered runes, 297 shapes/effects, 77 named fusions including 22 signatures.
- Twelve original material sprites with two frames each and distinct client motion; the shared spell packet boundary converts legacy decorative calls, and client aim/trails/formations use the same owned materials. Decorative vanilla lightning entities were removed without changing the associated spell damage.
- Walls and steps have actual collision; water borrows and reserves real sources, moves a custom translucent water body along a bounded trajectory, and restores sources. Identity-checked cleanup cannot remove a later replacement construct. Saved undo checks complete block state rather than only block type.
- Elemental terrain reactions preserve expiry. Placement respects permissions, occupied spaces, loaded chunks and budgets. Construct blocks have no obtainable item or loot.
- Exact weaves flatten existing weaves, preserve duplicates and charge full summed mana. Their tier and XP grow with leaf count. The shard route refuses to discard woven contents.
- Every elemental effect, including the ten innates, has an exact weaving route. Survival soul weaving requires the owner's awakened innate; checking ownership happens before learning, mana payment or cooldown. Only one distinct innate identity fits a soul weave. Crouch-use a Blank Rune on an altar to imprint it for one blank and three levels.
- The native sneak-imprint handler is on the held item because Minecraft bypasses normal block use while sneaking. Native item-on-block tests cover payment, result and insufficient-XP refusal.
- Spellguard snapshots health at the start of a same-cast, same-tick burst, closing a concrete bypass where several small effects lowered health below the threshold before the lethal effect.
- Caster rune strokes use short beats from the current rear pose; a second fixed rear circle no longer remains at the old position after a turn.
- Span and Rampart crumbling also use original material sprites instead of vanilla block-break decoration.

## Verification ledger

- `logs/physical-magic-verification.log`: passed in 55 seconds. All nine terrain runes captured; collision, cancellation, expiry, obstruction preservation, water return, bounded water damage/Soaked, frost platform reaction, extended real altar weaving, foreign soul refusal, eight-hit spellguard burst, native imprint and payment refusal passed.
- `build/test-results/test/`: 464 tests in 55 suites, zero failures/errors/skips. Added canonical multi-weave, size/tier/payment limits, malformed/nested refusal, full effect coverage and innate restrictions.
- `logs/expanded-magic-verification.log`: passed in 3m 12s; 39 shapes and turning, 190 screenshots archived.
- `logs/expanded-shader-verification.log`: passed in 52 seconds with Iris/Sodium and the test shader enabled; 12 comparisons archived.
- `logs/expanded-final-assets-1.log` and `logs/expanded-final-assets-2.log`: consecutive final regeneration passed; all 3,623 resource hashes are identical.
- `logs/expanded-rune-ownership-verification.log`: passed in 25 seconds; owned innate behaviour and explicit foreign-Phantom refusal. Older Survival fixtures were updated to use the matching awakened heart rather than bypass ownership.
- `logs/expanded-complete-suite.log`: full integrated gate passed in **45m 6s**. The 59-entrypoint descriptor has **56 passed and 3 skipped** suites with the full animation gallery enabled. Baseline shader, extra fire/blood close-ups and staged showcase are the three skips; enabled shaders passed separately. `artifacts/review/test-manifest.json` records the exact basis and counts.
- `logs/expanded-final-build.log`: final jar and game-test compilation passed in 4 seconds; the 464-test, 55-suite unit gate remains green.
- `logs/expanded-distribution-profiles.log`: Performance, Balanced and Cinematic packs built from the final jar. Dependency consistency, archive CRC and exact bundled-jar equality checks passed. Fresh launcher import remains unverified.
- The full run archived 840 PNGs, including 186 labelled timing frames covering release for all 297 shape/effect runes and impact for all 258 effects. The merged review index has 666 searchable captures, 20 equipment icons, nine new physical rune icons and twelve original material silhouettes.
- Source diff cleanup restored original line endings in 148 Java files whose normalized contents exactly matched HEAD, retaining every semantic change. `logs/expanded-whitespace-check.log` records the clean whitespace gate.

Full real multiplayer combat/performance sessions, arbitrary shader packs, fresh launcher import and natural dungeon placement over many seeds remain outside the completed local gates. Screenshots capture moments, not proof of smoothness under every camera or travel condition.
