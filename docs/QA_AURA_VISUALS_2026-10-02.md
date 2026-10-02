# Aura standards and physical effects verification

## Scope

Animated cloth crossroads and rally standards, bounded technique nameplates, and physical Aura ground feedback. Ground marks are
temporary fracture, crater-impression and blade-gouge visuals. Combat rules and terrain-changing arts retain their existing behavior.

## Passed checks

- Final `build compileGametestJava`: 849 unit tests, zero failures, errors or skipped tests; distributable JAR built.
- Generated assets: 4,568 paths verified, identical PNG pixels and exact bytes for other generated files.
- Player guide: all 101 pages, navigation, links, assets and GitBook exports checked.
- Full Ways client game-test suite; focused crossroads and Banner rerun after the physical-ground revision. Assertions cover four
  cached standards, texture availability, calm rendering, expiry after selection and the small rally standard.
- Full Aura FX client suite rerun with physical scars. Assertions check visible Dominion scars and reject unsupported open-air cues.
- Shader suite rerun with a daylight crater scene, alongside the plain rendering scene.
- All ten awakened Dominions and the plain Dominion rerun after replacing indirect elemental spell seals.
- Seven selected art scenarios rerun after that replacement: Hailfall, Glacier Mirror, Collapse, Event Horizon, Meteor Shower,
  Nova and Thousand Moments.
- Base Aura regression suite passed during this revision.
- Diff whitespace check passed.

## Visual review

Reviewed first- and third-person crossroads, daylight and night cloth, calm detail, rally standard, default Dominion, Rime Dominion,
Hailfall and shader/plain ground projection screenshots. Early cloth seams and the one-sided finial were corrected. The rightmost
crossroads flag faces inward to fit the first-person view. The Rime review exposed a spell seal inside `ElementFx.frostCreep`; Aura
now uses dedicated physical helpers for these indirect paths, and its Dominion scene was captured again.

## Performance bounds and limits

- Standard cache: at most 48 effects, updated in place and cleared on disconnect; cloth detail reduces on calm/off and at distance.
- Ground-scar cache: at most 24 effects, radius capped at eight blocks and lifetime at 600 ticks. Refresh cues reuse a scar and do
  not re-emit debris. Floor state and lighting refresh every five ticks outside the render loop.
- Ground fragments use the actual supporting block, with fewer on calm and none on off.
- Scars skip open air and narrow collision surfaces, hide if the supporting block changes, and fade naturally. Crater impressions
  do not excavate blocks. Stair surfaces use the top of their collision bounds.
- The selected art pass covers seven representative scenarios; it is not a claim that all fifty art scenarios were rerun.
- This build is unreleased and retains the existing 0.9.0-alpha version metadata.
