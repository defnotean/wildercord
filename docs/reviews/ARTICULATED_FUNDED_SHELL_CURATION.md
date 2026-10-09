# Bounded funded Aura-shell native curation

`tools/curate_masters_frames.py --suite articulated` admits the exact 36 PNG
names from `ArticulatedAuraShellPresentationTest`. The run marker and manifest
identify all four native source suites. Full native evidence and its independent
test verdict remain authoritative; this packager does not establish a passing
test or visual acceptance.

Names join `articulated_funded_shell_`, one of `spellcut`, `rising_break` or
`driving_cut`, one of `third_person_front`, `third_person_back` or `first_person`,
then `right_stage4` or `left_stage5`, then `skin` or `netherite`, with underscores
and a `.png` suffix. Right/stage5, left/stage4, other stages and alternate names
are rejected. Each `fundedShellCoverage` row identifies one expected filename
and separately reports `captured`, `selected`, `missingCapture` and
`omittedForBudget`. Absent screenshots never become budget omissions.

Camera, configured hand, equipment and stage are filename/fixture facts.
Stage4/Stone and stage5/Gale start with 100 Aura, use real accepted input and
ordinary shell upkeep under the native fixture contract. Equipment `skin` does
not identify actual skin geometry. Armored trials equip all four netherite
pieces with Protection IV and gold Sentry trim, including in first person;
only the chestplate arm geometry is submitted in that camera. The diamond
sword, empty offhand and visible HUD are also fixture facts. Unencoded viewport
and UI scale stay null.

Every selected funded frame keeps `phase` and `renderedPhase` unknown and
`nativePixelReviewRequired` true. There is no PNG-bound sidecar. The packager
does not ingest `ARTICULATED_FUNDED_SHELL` logs or nearby JSON files:
`preCaptureReceipt.status` stays `not_ingested`; activation, shell ARGB, actual
skin and pre-capture age remain null, and pre-capture phase remains unknown.
The exact log-name search key points into the preserved full evidence artifact.
Even a matching pre-capture log would not establish the screenshot's render
phase.

Selection begins with the same six finalized NPC PNG/JSON representatives.
When funded captures are present, budget opportunities alternate one shared-art
representative, one original owner comparison pair, then one funded pair. Old
core comparisons retain left/right, body, full-armor front/back and reference
HUD skin/chestplate ordering and atomic costs. The six funded priority pairs
are:

- Armored first person, both hands, for each of the three moves: six PNGs
- Skin front, both hands, for Spellcut and Driving Cut: four PNGs
- Armored back, both hands, for Rising Break: two PNGs

Present funded priority pairs are charged together, and an unaffordable pair
is not retried as individual images later. A missing mate stays missing; its
real counterpart may be selected. Shared request breadth and existing temporal,
idle and equipment/viewpoint comparisons precede the other funded configurations.
Early-school NPC extras remain last. Without funded captures, the old
four-shared-representatives-then-owner allocation order is retained.

The total cap remains 14,000,000 bytes, including NPC sidecars and manifest.
The manifest reserve grows from 640,000 to 768,000 bytes: the complete fixture
contains 287 selected PNG records, 15 sidecars and all expected coverage rows,
and its manifest exceeds the old reserve. That reduces image space by 128,000
bytes; a near-cap old-only fixture can retain one fewer PNG. It does not split
comparison pairs. PNGs and NPC sidecars remain byte-identical; existing exact
run/head provenance, pre-run freshness, safe-path, duplicate-name, signature,
SHA-256 and atomic-publication checks are unchanged.

Verification: `python -m unittest discover -s tools -p
'test_curate_masters_frames.py' -v` passes 102 tests. Added checks cover exact
names and impostors, unknown receipts despite nearby logs/JSON, every missing
and budget omission, representative fairness, atomic pairs, finalized NPC
priority, the full manifest reserve, byte identity and the unchanged total cap.
These use synthetic packaging bytes and do not launch or validate native play.
