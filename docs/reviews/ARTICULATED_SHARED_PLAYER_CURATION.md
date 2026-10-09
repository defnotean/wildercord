# Bounded shared-player native curation

`tools/curate_masters_frames.py --suite articulated` includes the additive
`ArticulatedSharedPlayerChecks` captures in the existing curated artifact. The
shared capture paths, complete native evidence and authoritative test manifest
remain intact. The later [funded Aura-shell extension](ARTICULATED_FUNDED_SHELL_CURATION.md)
adds a fourth source suite and interleaves its representative budget opportunities
when those captures are present. This packager copies source PNG bytes without decoding,
rendering, resizing or retouching them. A screenshot's presence is not a native
test verdict or visual-acceptance verdict.

The exact names are `articulated_shared_<rising_break|driving_cut>_<third|hud>_`
`<width>x<height>_gui<scale>_<skin|netherite>_<left|right>_requested_`
`<windup|active|recovery>.png` (the lines join without spaces).
Third-person front captures use only 1280×720/GUI3. HUD captures use
854×480/GUI2, 1280×720/GUI3, 1280×960/GUI4 and 1920×810/GUI3. Each of the 40
configuration rows has three independently requested trials, for 120 expected
filename slots.

`sharedPlayerCoverage` is separate from the existing owner/armor/HUD `coverage`
and finalized `npcCoverage`. Every shared configuration lists captured requests,
selected labels, missing requests and budget omissions. Unknown or alternate
names do not substitute for an expected slot. A present `requested_active` PNG
means that requested capture exists; it does not prove active-phase pixels.

Frame records distinguish filename facts and fixture assumptions:

- `requestedPhaseBasis` and `viewportBasis` are `filename_only`.
- Camera and configured hand come from the exact filename. `skin` means the
  equipment configuration, not a claim about wide/slim skin geometry.
- Enchanted full netherite in third person and enchanted chestplate arms in HUD,
  the diamond sword/empty offhand and visible HUD are explicitly identified as
  the reviewed native fixture contract, not measured image facts.
- `phase` and `renderedPhase` are `unknown`; `verifiedRenderedPhases` stays empty.
  Exact-impact pixel coverage remains unverified and native pixel review is
  still required.
- The packager does not ingest the native log. `preCaptureReceipt.status` is
  `not_ingested`; accepted move, activation, windows, actual skin and pre-capture
  age remain null, and pre-capture phase remains unknown. The manifest gives
  the exact `ARTICULATED_SHARED_SAMPLE name=...` search key in the preserved
  `articulated-native-evidence` artifact, not a fabricated receipt. Even a real
  pre-capture phase receipt cannot establish the framebuffer phase during
  screenshot readback.

Selection retains the six finalized NPC priority beats first. Without funded captures, it then gives
each shared art/camera one representative opportunity before the old owner
matrix: third person then HUD, Rising Break then Driving Cut. It prefers a
requested-active capture, then windup/recovery, the reference viewport, skin
and left hand, using a different real candidate if an earlier one cannot fit.
These are at most four initial representatives, not a promise that unavailable
or oversized files fit. Original body/hand, full-armor front/back and HUD
comparison pairs retain their existing atomic budgeting. Shared request breadth
then alternates the two arts before the remaining old temporal/breadth samples.
All original coverage groups and finalized NPC receipt rules remain intact.

The total cap remains 14,000,000 bytes, including copied NPC sidecars and the
manifest. The articulated manifest reserve is now 768,000 bytes, accommodating
all 287 selectable PNG records and the funded coverage rows. The original 251
records and 40 shared configuration rows previously required 640,000 bytes,
exceeding the former 256,000-byte reserve in the complete small-file fixture.
Budget omissions remain explicit. Existing run-marker freshness, exact run/head
provenance, safe paths, duplicate-name rejection, byte hashes and atomic output
publication apply to the new captures unchanged.

Focused verification: `python -m unittest discover -s tools -p
'test_curate_masters_frames.py' -v`. These are synthetic packaging fixtures only;
they never establish native execution or visual acceptance.
