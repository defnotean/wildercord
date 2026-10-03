# Glowcap Nursery native audit

## Scope

This increment connects an actual living Sporeback visit to a persistent three-clue investigation, two harvested materials spent in native crafting, a placed canopy and finite offhand equipment. It supplies three original discoverable journals. It remains a fungal ecosystem foundation: a connected threat, authored ruin, restoration relic and wider population/seasonal behavior are still required.

Production sources are the `Fungal*`, `Glowcap*` and `Breathmark*` classes under `src/main/java/dev/wildercord/wildlife`, the existing Sporeback visit hooks, and authored `tools/fungal_art.py` / `tools/feel/fungal.py`.

## Actual failures and fixes

1. `native-first.log`: correlated feature fixture seeds produced the same clue kind. Seed sampling was corrected; authentic feature provenance was retained.
2. `native-second.log`: an ordinary mushroom competed with the prepared Glowcap. The obsolete observation mushroom was removed from the dedicated garden fixture; normal wildlife food selection stayed intact. The player guide explains food competition.
3. `native-third.log`: diagnostic helper compilation error. Fixed test-only diagnostic scope.
4. `native-fourth.log`: second visit failed after a Grow hook. The visitor stood at a different height; this did not establish a Grow collision bug. Actual Grow also landscapes vegetation, so this remains a separate integration observation.
5. `native-fifth.log`: second visit also failed with Heal, with the snail at Z=-4.101 while the cap was at Z=0. Its anchored radius-four search could no longer include the cap. This established roaming beyond the local garden search as the reproducible acquisition failure. The fixture now has physical low walls and accessible garden paths from the start. The same visitor completes the second cycle without teleportation, clock edits or replacement.
6. Visit search used to consume the same 260 ticks as travel/browse. An 81-column scan can use about 200 ticks before discovering the final corner. Selecting the initial valid path now gives a separate 260-tick journey. Search and journey are each finite; path retries do not renew the journey. Native far-corner testing then exposed a second cause: in 26.3, Mob.setSpeed also sets the forward input. The old .13 attribute and .6 visit modifier produced about .00608 blocks/tick of effective ground movement. Native trace showed only 1.58 blocks of approach within 260 ticks. The purposeful visit modifier is now 1.25, while the .5 wander modifier remains. The finite native corner visit succeeds at tick 325, with 136 journey ticks left.
7. `native-terrain-first.log`: real normal terrain on seed -7775421970293783948 failed to supply a cap and both authentic clue kinds within 81 lush-cave-neighborhood chunks. Tiny vertical sampling plus rarity starved discovery. Feature sampling now checks 12 local columns over the finite Y 47..-64 band, up to 1344 candidate cells each; decorators have two finite attempts, cap rarity 3 and marker rarity 4. The same failed seed is used for before/after validation, with the same 81-chunk limit.

## Passed native acquisition and restart

`native-sixth.log`: terminal **BUILD SUCCESSFUL in 3 m 16 s**, exit 0. Four original frames are preserved in `sixth/`; all were visually inspected.

The actual client suite passed:

- Living first observation and both authored feature-provenance clues; replicated hand-placed markers cannot mint rewards.
- A cutting placed through the native client interaction; dry habitat refusal and Life preparation.
- A real uninterrupted first browse, cap maturity, permission refusal, native harvest, dropped gill pickup and spending that earned gill in actual inventory-menu crafting.
- Actual earned Nursery placement above removable scaffolding.
- Gathering the same visitor's dew, preserving its forage deadline, waiting its real 1205 ticks and completing a second native approach/browse. No replacement visitor or shortened rest in the second cycle.
- A second native harvest/pickup; actual earned gills and dew consumed in the Cave Breather recipe.
- Native crafted-filter return, original conclusion journal and three Blank Runes exactly once.
- Real offhand Poison clearing with equipment wear and Slowness; another Poison application remains during the shared rest. The final use breaks finite equipment while keeping the rest deadline.
- Full world close/reopen preserves exact visitor clocks, authentic marker provenance, claimed investigation, filter deadline and restored cooldown indicator. Repeated sites/returns cannot renew rewards.
- Shift input is restored in an outer finally, including failed paths through either world.

Life habitat preparation exercises the actual `CastEngine.onHit` + SPELL_HIT effect hook using TOUCH/GROW for the first preparation and TOUCH/HEAL for the second cycle. It is a direct effect-hook fixture, **not proof of a paid player cast**. Vanilla sticks, paper, leather and copper are ordinary fixture supplies; fungal gills and dew are actually acquired and spent.

## Visual evidence

- `sixth/fungal_glowcap_snail_visit.png`: actual mature layered cap and feeding snail in the contained garden.
- `sixth/fungal_open_nursery_rest.png`: woven open-frame canopy and live garden visitor.
- `sixth/fungal_root_field_notes.png`: earned original root field journal in Minecraft's real book screen, readable without clipping.
- `sixth/fungal_three_breathmarks_conclusion.png`: earned conclusion, three authored pages; first page readable in the actual book screen.

No screenshot pixels were edited.

## Still pending in this review

`native-bounds-sixth.log`: terminal BUILD SUCCESSFUL in 1 m 10 s, exit 0. The real far-corner path and 40-tick browse produced dew. A genuinely Fire-hidden incumbent refused another visitor's Nursery admission. A nonintersecting physical wall ring blocked an initially valid path: remaining journey 256 became 156 after 100 ticks, then expired without reserve. Retry checks did not renew the deadline. Earlier obstruction fixtures intersected the moving snail's body and correctly triggered damage/hiding; their failures are retained in bounds-first through bounds-fifth logs.

`native-terrain-seventh.log`: terminal BUILD SUCCESSFUL in 42 s, exit 0. On the original failed seed, the same 81-chunk sample contains 6 natural Glowcaps,2 authentic root marks and 1 authentic air mark. Postgeneration counts:1735 supported exposed moss/clay floors,216 moist,115 covered moist,100 covered moist in Lush/Dripstone biome. Distinct current-chunk 4-by-3 strata improve useful coverage while preserving 12 columns/1344 candidate bound, actual moisture/cover/biome checks, no terrain replacement and no query-driven chunk loading. Per-feature temporary diagnostic logs were removed.

The natural original image is preserved at `terrain-seventh/fungal_glowcap_natural_cave.png`; it was inspected. The close cave bank/overhang partly obscures the cap, so this documents the actual generated habitat rather than serving as a clear model showcase. The contained garden frame remains the clearer plant model review.

`native-eighth.log`: terminal **BUILD SUCCESSFUL in 2 m 28 s**, exit 0. Model and collision posts extend from -16 to 12 model units, reaching the lower walking floor with a 12-unit central gap. The same actual snail rests with its footing exactly equal to roof.below() after real navigation. First preparation uses the real Grow hook with nearby collision-state diagnostics; second preparation uses Heal. No grown collision blocks, forage clocks or rewards were edited to pass. A seventh acquisition attempt exposed a test gather-packet race with the now-faster first shelter entry; the final suite awaits actual first entry then finite rest exit before gathering.

Eighth originals are preserved in `eighth/`. Their grounded supports are visible, but real Grow tallgrass and player Night Vision particles obscure the resting visitor. Gameplay acceptance is retained; clear presentation captures are queued separately with quiet camera particles and unobstructed actual viewpoints. No image pixels or accepted acquisition evidence are altered. Four functional item credits, three original lore texts and one connected investigation are accepted. No full ecosystem count, general performance improvement or multi-seed population balance is claimed from these suites.


## Final clear native presentation

- `native-canopy-presentation.log`: terminal BUILD SUCCESSFUL in 50 s, exit0. Original `canopy-presentation/fungal_grounded_canopy_visitor.png` shows grounded posts, the woven canopy, live resting visitor and opened cap without occluding vegetation or player particles. The suite uses supplied habitat/plant/canopy for presentation; the actual snail navigates, browses and enters shelter, with no pose, timer or reserve fixtures. Earned acquisition remains the separate eighth full chain.
- `native-terrain-presentation.log`: terminal BUILD SUCCESSFUL in 36 s, exit0. Original `terrain-presentation/fungal_glowcap_natural_cave.png` shows the actual generated plant, water, clay footing, cave walls and vines. A bounded choice of24camera positions checks real collision/outline visibility and quiets only player Night Vision particles. No world blocks or images are edited to improve the view.
- The final same-seed81-chunk sample contains6caps,1authentic root mark and1authentic air mark; floor counts1726supported,215moist,114covered moist,99eligible cave-covered moist. These counts differ slightly from the previous same-seed run because natural neighbor decoration/plant evolution occurs during loading; both actual runs supply the full discoverable chain. No deterministic population-density or multi-seed claim is made.
- Both clear original frames were inspected and copied unedited into `wiki/assets/images/glowcap-grounded-nursery.png` and `glowcap-natural-bank.png`, with accurate chapter captions.

Focused native work for this milestone is complete. Final combined regeneration/build, parent review and delivery remain parent-owned. The full fungal ecosystem still needs its threat, ruin, relic/equipment breadth and further population balance work.

## Subsequent Linux full-suite regression gate (2026-10-03)

GitHub run37157807760 at commit c30ae028 initialized Mesa/OpenGL and reached actual FungalNurseryTest gameplay, then failed the combined assertion "Gathering the same visitor preserves its real forage rest". Retained full log: artifacts/review/life-fieldcraft-milestone/ci-c30ae028-failed.log. The prior focused Windows acceptance does not make this Linux full-suite gate green.

Source inspection establishes that mobInteract does not modify forageReady. The second gather used an arbitrary diagonal player offset near the now-real grounded canopy corner posts and Grow vegetation, while server admission checks actual line of sight, range, visible/open pose, crouching and the existing reserve/rest. Geometry or admission is a plausible explanation; the old combined failure did not retain those state values, so its exact cause is not proven.

The patched fixture selects at most eight actual supported, collision-free approach positions, preferring cardinal open sides, while real native crouch input is held. At most four real entity-interaction packets follow normal moving-visitor admission; no canopy/vegetation, clocks, reserve or entity pose is changed to pass. Admission diagnostics include real player/snail coordinates, visibility, crouching, pose, reserve and independent deadlines. Reserve spending and exact forage-rest preservation are separate assertions. The player also waits for the actual emitted dew pickup and verifies a Survival inventory increase before leaving, retaining real ingredient provenance. Focused native execution and a clean subsequent Linux full-suite run remain acceptance gates.

Patched focused Windows native verification: native-ci-gather-first.log, session 87618, exit 0 / BUILD SUCCESSFUL in 2 m 28 s. Both actual crouch admissions and real emitted-dew inventory pickups passed, as did the same visitor's exact forage-rest preservation, second real rested browse/harvest, native paid-ingredient crafts, connected one-time reward and full restart. Four original screenshots preserved in ci-gather-first. Linux full-suite rerun is still required before closing that CI gate.

A subsequent full Linux run at f4d77883, run37160112771, failed the new emitted-dew inventory-pickup assertion during the FIRST mushroom gather, before Grow or canopy construction. Retained log: artifacts/review/void-material-milestone/ci-f4d77883-failed.log. Its admission recorded Shift=true, visible=true, open pose0 and the ready dew reserve. The gather geometry correction therefore admitted the packet, but standing beside the visitor still assumed a randomized real drop would land inside the pickup box. Local focused success does not establish portable pickup geometry.

The revised collection fixture tracks the actual new emitted item UUID, excluding pre-existing local dew drops. If the inventory has not already received it, the native client physically walks toward its live position while crouched, using at most180 ticks and at most three short lateral detours after measured movement stalls. The fixture does not teleport either item or player onto a reward, alter its owner/target/pickup delay, set clocks/resources, or insert inventory. Up/Left/Right input states restore in finally. Bounded diagnostics retain real item coordinates, velocity, age, delay, target/thrower, player collision admission and inventory. The earned-material gate remains required. Native verification and a new Linux full-suite result are pending.
Tracked-drop focused native run: native-ci-pickup-first.log, session 91986, terminal exit 0 / BUILD SUCCESSFUL in 2 m 23 s. Both actual randomized drops had pickup delay and no target/thrower at first observation; native crouched movement acquired each into Survival inventory. The same visitor retained its exact first forage deadline, completed its second actual rested browse/harvest, supplied earned native crafts and one-time completion, and survived full world restart with exact saved clocks. Four untouched screenshots preserved under ci-pickup-first. A clean Linux full-suite outcome remains required.
Root review also found a pickup observation race: a preceding inventory check and a later entity-position/log callback could straddle ordinary auto-pickup. The corrected helper atomically rechecks earned inventory inside initial-drop, position and logging callbacks. Observed inventory success exits normally; a vanished tracked drop without the required inventory increase still fails. Exact UUID provenance and native movement are retained. Root owns the final focused rerun and Linux acceptance.

Parent-reviewed final pickup callback race correction passed the complete focused native suite in 2 min 20 s (exit 0): `native-ci-pickup-final.log`. Original captures are in `ci-pickup-final/`. Inventory and tracked-item presence are inspected atomically in each lookup/log callback, preserving success if normal pickup occurs between callbacks. Full Linux descriptor acceptance remains open.
