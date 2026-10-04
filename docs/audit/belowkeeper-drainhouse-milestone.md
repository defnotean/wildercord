# Belowkeeper Drainhouse development milestone

Focused development acceptance recorded2026-10-04. This adds a connected cave investigation and two functional items; the larger living-world goal remains active.

## Playable content

Mara's Drainhouses are original three-room cave stations: an engraved threshold, a living Glowcap garden beside a contained water gutter, and a raised copper restoration alcove. A rigid ribbed vault, timber supports, moss, copper grates and distinct material ledgers establish the architecture. The19-by7 blueprint has808 cells, at most32 additional dry supports, and226 required AIR cells under the unchanged256 carving limit.

The optional investigation requires actual snail dew gathering, observing a living Rootmolt meal, and a subsequent physical or evasive counter. Three authentic ledgers provide three original lore books with three readable pages each and custom cover artwork. Ordinary placed replicas cannot establish provenance. Each player's observations and rewards persist independently.

Restoration spends two real dew, two gills and four copper and grants Mara's Empty Bell once per player. Its one-second stationary commitment interrupts one visible nearby Rootmolt's warning/rake/hold, consumes finite wear and leaves a short movement/mining tradeoff. Item changes, movement, obstruction and invalid sources cancel admission; another copy cannot bypass the saved rest. Repair uses one dew, one gill and one copper for up to eight wear, with a saved one-minute repair rest. A broken or lost bell is not replaced by restoration.

Rootbound Greaves are actual feet-slot armor crafted from leather boots, copper, a gill and dew. Thirty still crouching grounded dry ticks prepare one hold denial. The physical wound still lands; the denial costs additional wear and a saved ten-second rest. Jumping, movement, water and an actual equipped-stack change invalidate preparation. Their protection is specific to the cave creature's restraint.

Four original physical sound events support reading, restoration, ringing and bracing. They do not draw magical circles. The Rootmolt spawn egg now uses its own authored sprite and the actual26.3 item/generated model, replacing a removed vanilla template.

## Reliability and generation corrections

- Register the configured resource under26.3's worldgen/feature path.
- Admit an actual dry two-high existing cave mouth and harmless noncolliding vanilla plants. Align one canonical north entrance at the same height; retain the bounded room-assessment and column budgets.
- Preserve original solid ore in architecture and unused exterior doorways. Refuse ore blocking the primary doorway, interior, garden or other functional openings.
- Explicitly admit safe solid dry gravel only in the outer roof. Interior/floor gravel and unknown materials remain refused.
- Replace65 unused overhead AIR cells with a rigid tiled/chiseled vault instead of raising the excavation budget. Actual player standing clearance is verified through the arrival lane and above the raised floor.
- Add five gutter bottom and five north-side liner cells. Real server fluid ticks verify containment; a separate unsealed control actually flows.
- Capture the full original footprint before writing. Record touched cells before calling a writer; verify actual retained states and all three block entities before authenticating ledgers. False-after-mutation, exceptions and late removed-block-entity failures restore original state.
- Re-admit actual post-write restoration/repair state, actors, materials and permissions. Reentrant callbacks cannot duplicate payment or rewards.

## Native evidence

| Gate | Result | Practical scope |
|---|---|---|
| DrainhouseTerrainFitTest, final vault |37s pass|Actual226-rock excavation, final changed-write failure and exact rollback, body clearance, original ore, unsafe controls and water containment|
| DrainhousePlacementTest, embedded layout |22s pass|Complete transaction, actual Nether zero-write refusal, throwing/mutate-false writer and final removed-ledger rollback|
| DrainhouseTerrainTest, final natural entry |57s pass|Three seed worlds,243 scanned chunks, one authentic natural dependency site,246 total explicit sample/footprint requests, Survival input/landing and full saved-world reopen|
| DrainhouseAcquisitionTest, final vaulted layout |164s pass|Actual dropped-resource acquisition, living observations/counter, all nine native book pages, restoration/crafting/repair, refusals/reentry and full restart with41 byte-identical copied world files|
| BelowkeeperEquipmentTest |62s pass|Real equipped-stack swap, crouch/jump/creep invalidation, physical wound/wear, bell obstruction/rearming and saved rests/reopen|
| BelowkeeperPresentationTest, Rootmolt egg correction |41s pass|Native hand model, worn equipment, book/item icons and removed-template warning correction|

The first natural scan failed despite one real generator commit because the house was in a neighboring dependency chunk beyond the81 scanned chunks. The corrected fixture retains at most16 genuine accepted WorldGenRegion centers and verifies only their exact known footprints inside a fixed121-chunk coordinate border. It does not widen the scan, invoke placement or credit post-generation diagnostic fits. The accepted site is at(-40,9,85) in seed716843; its existing mouth is(-46,9,81). It retains three authentic ledgers and two supported damp AGE0 buds, and its provenance survives reopen.

The supplied-habitat acquisition fixture supplies habitats, witness creatures and ordinary vanilla crafting materials. The dew/gill drops, investigation flags, books and bell are genuinely earned. Its Life preparation uses the actual onHit owner seam, not proof of a full mana-paid Grow cast. Reentry checks use synchronous callbacks on an integrated server, not remote multiple-client evidence.

## Review package

- JAR: artifacts/review/belowkeeper-drainhouse/wildercord-belowkeeper-review+mc26.3.jar
- SHA-256:1046f769ff0fffb92145b5a93c3717413192a1b6d76365d7b02e3d61c39ff30a
- Exact package verification:5320 processed resources,1764 compiled mod classes, matching bytes/class inventory and valid ZIP.
- Final build:13s;988 unit tests, zero failures/errors/skips.
- Generated reproducibility:5321 paths, identical decoded PNG pixels and exact other bytes.
- Guide/GitBook:125 pages with valid navigation, templates and assets; actual natural-site image included.
- Gallery:index.html and screenshot-manifest.json contain14 byte-preserved native originals. Both natural views, final earned scene and all nine final journal pages were directly inspected. Two equipment views precede the ceiling revision and are labelled accordingly.
- Source metadata remains0.9.1-alpha.1+mc26.3. The distinct review filename and checksum identify these development bytes; no public release or server deployment is implied.

Decoded physical audio passes finite/no-clipping/spectral checks. Final native listening remains open. One site in this small fixed sample does not establish representative frequency. Full Linux gameplay, remote multiplayer, sustained mixed populations and a complete fungal ecosystem remain separate acceptance work. The latest58904bbc CI run was still in progress when this development audit was written.

## Conservative goal credit

This increment adds one relic, one equipment item, three lore texts, one investigation and one location. Accepted totals become23 functional items (relic2/equipment2/tools4/placeables6/materials9),15 lore texts,2 investigations and2 locations. Creature7, abilities2, new signatures18 and complete ecosystems0 remain unchanged. Content-category minimums still require at least14 additional functional items. Neither draft Ember content nor the Tideward crossing kit receives credit here.
