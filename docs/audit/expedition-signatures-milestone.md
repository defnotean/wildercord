# Expedition signature development milestone (2026-10-03)

Focused gameplay, affected-family regression and combined package verification are complete. This audit records the checked development JAR and remaining acceptance work. Public release, server deployment and the full Linux descriptor remain separate gates.

## Content and count boundaries

Twelve new tier-III signature recipes extend the six accepted field signatures. Scoped acceptance brings the living-world goal to **18 newly authored signatures out of 18**, with **40 total signatures, 95 named fused recipes and 369 runtime runes**. Source inventory has 277 effect declarations; aliases mean declaration counts are not the runtime-rune count.

This meets the numeric new-signature target only. It does not close all supported-pairing behavior, the full spell lifecycle/presentation audit, real multiplayer, the twelve separate new-ability target, or the whole goal. No creature, functional-item, lore or investigation credit is added here. Those counts remain 6 creatures, 21 functional items, 2 abilities, 12 lore texts and 1 investigation. Larger Life105 and Rootmolt/Drainhouse files remain isolated drafts with no accepted credit.

## Exact recipes and authored gameplay

| Signature | Fusion ingredients | Base mana | Distinct role and tradeoff |
| --- | --- | ---: | --- |
| Nullcatch | Reflect + Collect |20| One front hostile projectile capture; rear/allied/reflected/ownerless shots pass; no stored ammunition/refund. |
| Second Bell | Shock + Countdown |16| Two delayed electrode beats; movement avoids the second; at most two targets/eight final damage per shared payment. |
| Red Ledger | Bleed + Reveal |18| Ordinary movement crosses three blood gates; standing still counters; teleports are excluded; six final damage per payment. |
| Quietus | Manaburn + Stasis |20| Short visible projectile escrow taxes a newly cast hostile player's current mana; no hit, mana reward or free ammunition. |
| Blood Escrow | Leech + Barrier |14| Actual health buys an ally a finite absorption chamber; survival floor/crouch/existing ward refuse; one gift per payment. |
| Frost Molt | Frostward + Cleanse |14| Actual frozen condition pays for one finite small-projectile plate; stronger/bypass attacks pass; generic debuffs are not frost resources. |
| Pulse Ferry | Regrowth + Time Skip |18| Separate sap beats visit two different wounded eligible allies; at most three actual recovery each/six total; no overheal. |
| Last Lantern | Light + Rewind |18| Records safe allied ground; a fresh crouch requests one bounded safe return; no health rewind or forced movement. |
| Pocket Current | Bubble + Collect |16| Ready permitted loose items enter the caster's tracked chest; both halves need permission/ownership; full/locked/loot-table destinations refuse safely. |
| Wayline | Span + Grapple |18| Forward input earns bounded collision-respecting tow toward owned safe ground; crouch releases; no flight grant or moving others. |
| Night Seam | Light + Zipper |12| Inspects beyond at most three ordinary permitted stone cells for an open-ground/threat clue; no wall destruction or relocation. |
| Shard Compass | Prospect + Treasure Sense |12| A successful accessible matching deposit clue spends one real raw-metal sample; refusal preserves it; no mining or invented reward. |

Base mana is not a final whole-spell quote: shapes, modifiers and other groups retain their ordinary costs. The twelve use original authored preparation/travel/impact arrangements, inventory art and material sound events. Reusing the existing material rendering engine is not twelve new particle engines or proof of every delivery.

## Completed evidence

Evidence root: artifacts/review/signatures-next.

| Gate | Authoritative result | Scope |
| --- | --- | --- |
| Deterministic generator |5158 reproduced paths| Asset reproducibility; not a native readability result. |
| Unit tests |978 passing units| Pure rules/contracts and repository unit coverage; no claim of remote multiplayer. |
| NextCounterTest |counter-native-final.log,42s successful| Real paid counter scenarios, parry/capture provenance, ordinary movement versus teleport, shared-payment final caps, cancellation and reopened-world cleanup. |
| NextSupportTest |support-native-third.log,42s successful| Actual native airborne jump refuses Molt and preserves its real cold resource; grounded admission spends that cold; frost/debuff isolation, consent/refusal, finite support outcomes and reopened-world cleanup. Some allied-player actors are controlled fixtures. |
| NextTrailTest |trail-native-second.log,36s successful| Actual Pocket ownership survives real saved-world reopen; staged item admission rechecks a newly protected destination before deposit. Resource/movement/probe outcomes are isolated controlled fixtures. |
| NextSignaturePresentationTest |presentation-native-third.log,62s successful| All12 actual client Fuse packets consume exact ingredients and produce exact rune; paid Full/Minimal casts exercise all12 preparation and flight identities in both profiles; mixed fallback preserved. |
| Final bounded NextCounterTest |counter-bounded-native-first.log,45s successful| Native256-arrow fixture enumerates65 entries and returns a saturated empty candidate set rather than processing an incomplete scan; a complete13-projectile provenance snapshot stays separate from nearest12 handling and later paid capture; prior cancellation, final shared-payment caps and reopen checks still pass. Structural budgets are not frame-time measurements. |
| PulseFerryNoHealTest |ferry-zero-native-first.log,43s successful| Real Gash blocks actual healing: no SAP and exactly zero emitted ferry voices. The positive case performs capped actual healing and exactly two real voices. |
| Affected Void38 Full/Minimal regression |void38-native.log,2m13s successful| Complete38-identity preparation and moving-body checks in Full/Minimal;153 original native PNGs retained in void38/screenshots. This is stage acceptance, not38 complete delivery lifecycles. |
| Affected FrostFlightTest34 Full/Minimal |frost34-native.log,1m59s successful| Complete34-identity paid moving-body acceptance in both profiles;69 native PNGs retained. Full impact/alternate-delivery lifecycles remain separate. |
| Affected LifeFlightTest30 Full/Minimal |life30-native.log,1m48s successful| Complete30-identity paid moving-body acceptance in both profiles; 121 original native PNGs retained. Larger Life105 drafts receive no credit. |
| Preserved presentation originals |38 native PNGs| Twelve altar frames, inventory/fallback, twelve preparation and twelve flight frames in presentation/screenshots. They are actual game captures, not image edits. Per-profile visual assertions are broader than the single retained representative frame per phase. |

The bounded counter query accepts complete snapshots through64 sources, aborts on enumeration65, and handles at most12 nearest candidates. Quietus retains the full complete snapshot for provenance: the native13-old-bolt case records all13 while only12 enter handling, so the unhandled old bolt cannot become a later newly cast attack. This is a structural-work fixture, not a timed benchmark or general ecosystem simulation result.

Initial compile/unit/native failures remain retained alongside the corrected passes. Successful focused tests do not erase earlier failures or prove the complete descriptor. Runtime sources include NextSignatureRules/Payments/Safety, CounterSignatures, SupportSignatures, TrailSignatures, PocketChestOwnership, bespoke server FX and client NextSignatureForms; actual teleport/placement ownership seams are integrated rather than presentation-only substitutes.

## Verified combined development package

Review JAR: `artifacts/review/signatures-next/wildercord-expedition-signatures-review+mc26.3.jar`

- SHA256: `1b4709a3f1a625d9f8fc3432b1927d7cd1abc75517490f3362e72d3b26d6632c`.
- Combined build passed22s;978 unit tests, zero failures/errors/skips.
-5158 generated paths reproduce;5157 processed resources and1711 compiled main/client classes byte-match the package.
- ZIP CRC, duplicate entries and unexpected/missing compiled classes checked.
-122 player-guide pages pass navigation/asset validation; sound check exits0.
-381 original native PNGs across four retained presentation/regression groups:38 signatures,153 Void38,69 Frost34 and121 Life30. These totals do not count unpreserved frames or invent separate screenshots for each asserted profile.

## Delivery and CI boundaries

The source and documentation are delivered together in the development milestone. The review artifact is separate from the public alpha.1 installation.

- The full Linux client descriptor is not green. On prior commit fb2c07cb9fb7ee3091538c842fb67aee283899cb, the ordinary build passed and Linux native gameplay advanced to a later gravity-flight assertion with zero visual pixels. Retained log: artifacts/review/grow-pickup-milestone/ci-fb2c07cb-failed.log. This is distinct from the earlier nursery pickup issue, and the new twelve signatures were not included in that committed CI run.

The client test runtime now defaults ALSOFT_DRIVERS=null only when there is no explicit environment override. This Linux-oriented test audio setup is implemented in build.gradle; a fresh Linux initialization/full-descriptor pass has not been established. It is not a production gameplay audio change.

## Larger acceptance still open

All delivery shapes, ranks/modifiers and supported combinations need the complete lifecycle matrix. Full beam/rain/self/construct/field/summon behavior, impact/aftermath readability, shader/reduced-flash review, simultaneous real clients, dedicated-server/crash/death/dimension upgrades and sustained natural ecosystems remain on the goal. No hostile/root-control immunity, universal damage cap, complete ecosystem or blanket performance speedup is claimed.

## Changes in this development milestone

- Added twelve distinct expedition, counterplay and support signature fusions with exact altar recipes, original material preparations/travel, effects, voices and inventory art.
- Added limited front capture, movement-countered delayed damage and ordinary-movement ledgers with final shared-payment caps; hostile projectile escrow keeps ownership/payment provenance and cannot manufacture refunds or attacks.
- Added resource-paid ally shielding, frozen-condition plates, finite actual-healing parcels and player-consented safe-ground return.
- Added owner/permission-safe chest gathering, input-driven ground towing, nondestructive seam investigation and sample-funded deposit clues.
- Verified all12 real client altar commits and paid Full/Minimal preparation/flight checks; retained381 original signature/affected-family review images across four groups.
- Delivered a locally checked development JAR with978 passing units,5158 reproducible generated paths,122 validated guide pages and verified resources/classes/ZIP integrity; public release and deployment remain separate.
- Verified bounded projectile-source scanning and complete small-set provenance/capture behavior, and corrected recovery presentation to emit no SAP or voices when actual healing is zero.
- Completed the numeric18-new-signature expansion target without crediting baseline signatures, additional creatures/items/lore, or isolated future drafts. The full living-world objective remains active.
