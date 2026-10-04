# Tideward crossing kit development milestone — 2026-10-04

## Accepted additions

Three craftable wetland items with original icons, worn equipment, a forked held spool, physical braid/peg/notch effects and three dedicated sound voices. Reedwater Waders occupy feet with one armour point: shallow supported non-sprinting movement receives a modest aid, actual moving ticks spend wear, and accumulated progress/shared saved rest survives replacement and reopening. Dewglass Spectacles occupy head without armour: half a second crouching with a clear view reads an actual nearby Reedback warning, spending lens wear and shared rest. Bank Surveyor's Line uses two actual visible supported banks to read a short route, spending two wear and shared rest; its private guidance lasts five seconds and edits no terrain or water.

Read admission checks exact whole-route footing/body snapshots, actual player authority, loaded terrain, border and conservative break-based claim compatibility. Per-player and cell leases precede callbacks; recursive or mutated observations are refused. Adventure use reaches the real callback without requiring magic editing. This compatibility query is not a universal claim-plugin reading API.

Client playback is bounded independently of frozen world time. Only an actual held source, matching world/nonce and valid whole geometry can draw the route. A paid cue may wait up to five local client ticks for vanilla block replication. The original ingress deadline remains authoritative; CLEAR, removal, departure and expiry cannot resurrect it. Initial emission and periodic bursts remain capped at ten. Physical materials are world-lit, use Full/Minimal budgets, and avoid magical circles.

## Native tests

Root serialized the real Minecraft/Fabric client runs. All nine final focused gates pass; logs and unchanged captures are retained under artifacts/drafts/tideward-crossing.

| Gate | Result | Evidence |
|---|---|---|
| ReedwaterWadersTest | PASS 32s | Actual craft/equip/input displacement, wear, rest, replacement and complete saved-world reopen |
| ReedwaterBoundaryTest | PASS 26s | Dry/deep water, actual jump/landing, worn effects and vanilla break |
| DewglassSpectaclesTest | PASS 27s | Actual craft/head use, living warning, occlusion, lens wear and reopened shared rest |
| TidewardWarningBoundsTest | PASS 24s | Saturated thirteen-creature refusal and an ordinary paid Minimal warning |
| BankSurveyLineTest | PASS 39s | Actual crafting/two UseBlock packets, hole refusal, two wear, saved rest, route reception and blocked-head cancellation |
| TidewardReadAdmissionTest | PASS 27s final | Real Adventure input, disabled editing, whole-cell callback changes, recursive admission, spectator/body departure |
| TidewardStalledClockTest | PASS 39s final | Actual frozen server clock, finite local route lifetime, burst/query ceilings, source removal and dimension cancellation |
| TidewardPresentationTest | PASS 45s | Actual paid Full/Minimal route and crouched warning, retained particles, strict paired pixel differences and expiry |
| TidewardPendingReceiverTest | PASS 44s | Actual receiver reached while support replication is missing; five-read expiry, CLEAR/source/world cancellation and no revival |

The pending-receiver gate injects a registered cosmetic route to isolate delayed client geometry, with explicit receiver-read witnesses. BankSurveyLineTest and PresentationTest separately prove genuine paid packets. Supplied equipment, habitat and hostile warnings are labelled fixtures; these gates do not prove natural wildlife distribution.

Fourteen clean presentation originals are preserved: ten drawn/expired frames directly viewed by root and four matched backgrounds used by strict native pixel comparisons. Eight earlier equipment/crafting/interaction captures were also directly viewed; they retain water occlusion and GUI clutter rather than being presented as clean visual acceptance. The illustrated guide uses three unchanged final originals.

## Corrections exposed by testing

The draft used a removed save method; fixtures now reopen the actual WorldSave. Buried floor-centre ray tests rejected a legitimately visible bank top after ordinary current movement; both admission and bank visibility now target the actual top face while retaining obstruction checks. A real paid cue arrived before a client gap changed from AIR to STONE; bounded pending admission fixes that ordering. The initial visual test counted particles queued before the ordinary engine flush and failed with zero drawn pixels; it now waits one ordinary tick and counts retained particle groups, preserving the strict visual assertions. A missing held-model particle material was corrected. Historical failed logs/screens remain in drafts.

## Build, guide and artifact

Final build PASS 15s: 1,000 unit tests, zero failures/errors/skips. All 5,392 generated paths reproduce, and the audio manifest/file check passes. The 128-page web guide validates and exports to GitBook.

Checked JAR: artifacts/review/tideward-crossing/wildercord-tideward-review+mc26.3.jar. SHA256 **d9f5f9d37fec7fdbb2db415b09b092710c54f097d2c742c0bd329758b5f9d9c3**. It matches 5,392 processed resources and 1,802 compiled classes exactly, has no duplicate ZIP entries or unexpected Wildercord classes, and passes CRC checks. Embedded version remains 0.9.1-alpha.1+mc26.3; this development filename distinguishes it from the public release.

No human audio listening, shader review, sustained TPS measurement, natural ecosystem completion, public release or VPS upgrade is claimed. Genuine cooperative Unity across two real local TCP clients is separately accepted in two-client-unity.md; the complete multiplayer roster remains open.

## Goal ledger

Functional items become 27: relics2/equipment4/tools5/placeables7/materials9. Category minima still require six relics, four equipment and one tool: at least eleven further items and 38 total. Creatures8, abilities3, newly authored signatures18, lore15, investigations2, locations2 and complete ecosystems0 remain unchanged. Runtime370 runes/Life31/40 total signatures/95 named fusions. The living-world goal remains active.
