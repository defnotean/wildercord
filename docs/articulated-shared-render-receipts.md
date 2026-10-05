# Passive shared-player render receipts

These observers belong only to the client GameTest mod. Register
`articulated-shared-receipt-gametest.mixins.json` in that mod's `fabric.mod.json`.
The existing `masters-capture-gametest.mixins.json` still registers the body submission
observer. Neither configuration belongs in the production mod.

Only a `TestScreenshotOptionsImpl.name` starting with `articulated_shared_` arms a
receipt. The outer Fabric screenshot operation and its existing client-thread
lambda share a token by options-object identity. An unmatched nested operation
suspends the active scope. The original options, update, extraction, render,
command submission, screenshot call, consumer and returned path stay unchanged.
There is no additional render, phase sampling, input, freeze, wait or retry.

The exact Fabric screenshot call arms the copy observer after rendering has
finished. The copy observer also requires the exact target and consumer identities.
A thumbnail or other screenshot cannot consume this capture's receipt. The token
records the texture's identity at the actual `copyTextureToBuffer` enqueue, before
the asynchronous callback. The callback captures the token, never a mutable game
state or a lookup of whichever player/frame is current later.

## What the receipt establishes

- Separate extraction, render, copy and capture identities, simulation tick,
  world/session, player UUID and entity ID, accepted move/start/windows/aim,
  options/tracker partial values, and actual entity-extraction partial arguments
  after the production HitStop hook
- The owner body's actual submission, or the main-hand view skin and actual item
  submission, with observed activation, phase, weight, handedness, appearance and
  palette hashes
- Deferred skin geometry preparation for the same model and state. Attachment
  setup calls are recorded separately and cannot satisfy this requirement.
  `ModelFeatureRenderer.prepareModel(Submit)` scopes its actual
  `Model.setupAnim(Object)` call. The player observer runs after the full original
  method, including production TAIL hooks.
- Actual skin model/texture and rig width; armor/item identifiers and foil flags;
  actual viewport and GUI dimensions; observed rigid/segmented visibility
- Render-target/color-texture generation agreement, returned image path, PNG
  bytes/SHA-256, and callback-versus-decoded-image pixel agreement

`requestedPhase` comes from the existing trial name. `renderedPhase` comes from
the visible deferred palette, even if it is different. A phase miss is diagnostic
evidence and a coverage miss, without changing time or the filename. Paused/frozen
captures cannot satisfy unpaused phase coverage. ACTIVE is an observed palette,
not proof of an exact hit/contact pixel. Every report retains
`exactImpactPixelCoverage: "unverified"` and `nativePixelReviewRequired: true`.
Only native skin widths actually present in the receipts count as observed;
the fixture's synthetic two-width bridge assertions do not supply that evidence.

The pixel digest is SHA-256 of ASCII `argb32-be-row-major-v1`, a zero byte,
big-endian 32-bit width and height, followed by top-to-bottom, left-to-right
big-endian ARGB32 pixels. `NativeImage.getPixel` and the decoded PNG use the same
ARGB ordering. Alpha participates in the hash. PNG bytes are never modified.

## Failure and artifact rules

Receipts are written to
`screenshots/articulated-shared-receipts/<run UUID>/<capture>-<kind>-<sequence>.json`.
The existing image paths and images are preserved, including rejected images.
An original screenshot exception is rethrown unchanged. Observer errors cannot
skip the original consumer or strand its completion future. When a normal
screenshot completes, an observer rejection is reported only after its image
has been written. If no callback arrives, the existing Fabric deadline/failure
unwinds; this observer adds no waiting loop.

Every diagnostic file for a token is authoritative. A late or duplicate callback
can invalidate an earlier immutable verified receipt and creates another JSON
record. Never accept only files containing `-receipt-` in their names. A receipt
whose PNG path was overwritten later is invalid, even if a newer image reused
the same trial name. Multiple capture tokens for one trial are rejected by the
verification tool rather than hiding a retry.

Run the artifact check against one selected run's **complete** receipts directory
and the corresponding original game directory or extracted artifact equivalent.
The native process must have finished, or the artifacts must be a sealed snapshot
after callbacks can no longer append diagnostics. A scan of a live run is only a
provisional observation and cannot establish terminal acceptance:

```sh
python3 tools/verify_articulated_render_receipts.py \
  --receipts GAME_DIR/screenshots/articulated-shared-receipts/RUN_UUID \
  --game-dir GAME_DIR --expected-count 120 \
  --expected-trials-file planned-shared-trials.txt
```

The planned manifest is optional for image association, but use it for an exact
planned trial matrix. Expected distinct counts are 24 for shared body and 96 for
shared HUD; 120 applies when both ran in that native process. A missing receipt,
missing image, conflicting/late record, duplicate trial, changed PNG or pixel
digest mismatch makes association verification fail. The output preserves phase
misses and lists only actually observed skin coverage. A passing association
check is not a visual acceptance verdict or proof of complete phase coverage.

## Checks and native gate

```sh
JAVA_HOME=/path/to/jdk-25 python3 tools/check_articulated_render_receipts.py
```

This callable command runs the existing pure Java checks, all artifact verifier
tests, and the launcher/packaging regressions. Importing any of these test modules
through ordinary unittest discovery does not run javac, Java, or a native process.
The standalone Java checks use only standard Java and compile the pure receipt
class from GameTest sources. Python tests require Pillow, already used by the
repository's image tooling. None of these tests claims to launch a native client.

### Bounded runner-side gate

For the articulated CI job, replace only its existing native launch command with:

```sh
python tools/run_articulated_receipt_gate.py \
  --log articulated-native.log \
  --output artifacts/review/articulated-native-receipts-RUN_ID-RUN_ATTEMPT
```

The wrapper invokes the unchanged `tools/run_client_ci.py --suite articulated`
launcher, inheriting the job's graphics environment. It requires Python 3.10+ and
Pillow; use the repository's existing setup-python and `pip install pillow`
pattern. The native launcher still requires its existing Java 25, Gradle, virtual
display, and Mesa setup. No native process is started by the test command above.

The wrapper creates a fresh nonce and retains it in memory for its one owned
launch. GameTest receipts carry this nonce at the top level, including failures
before the copy stage. The wrapper waits for its launcher and requires that
fresh log's matching native-process exit marker, which the launcher emits only
after waiting for or stopping the actual native process. A killed launcher or
missing completion marker cannot establish a sealed capture run.

Only after confirmed exit does the wrapper require one fresh UUID receipt run,
all matching nonce values, and the exact 120 planned shared trial names. It
rejects stale/preexisting/multiple/changing directories, copied provenance,
missing receipts, altered associations and ambiguous retries. A fresh output
directory and separate, fresh native log are required. The output cannot overlap
the original game/evidence tree. No cleanup, rerender, timing change, or retry is
performed to make a rejected run pass.

Upload the wrapper's entire output directory as the small metadata artifact:

- `association-report.json`: exact planned names, actual phase/skin observations,
  preserved phase misses, native exit and source/CI provenance, and raw-record
  byte counts and SHA-256 values
- `receipts/`: every complete original receipt record for the selected run,
  including failed/late records, copied byte-for-byte

The uncompressed output is at most 24 MiB: each raw record is limited to 128 KiB,
at most 256 records are accepted, all raw records share a 23 MiB allowance, and
the report has a 1 MiB allowance. Exceeding any bound fails the gate; records are
never truncated. If only the observation summary exceeds its allowance, the
wrapper keeps complete raw records and writes a small explicit failure report.
Earlier validation/budget failures can leave an incomplete metadata package;
`package.complete` makes this explicit. All original receipts, PNGs and native
logs remain where the existing full evidence upload expects them. PNGs are never
copied into, resized for, or rewritten by this metadata artifact.

Association or native failure exits nonzero. The report distinguishes image
association from `gatePassed`, which additionally requires native success and
`phaseCoverageVerified` for all 120 planned unpaused, unfrozen phases. Phase
misses fail this stricter gate, remain reported misses and do not fabricate missing phase or skin
coverage. Visual review and exact impact-pixel proof remain separate requirements.

### Ordinary unwrapped runs

The full/sharded native launcher can still run the same fixtures without this
wrapper. A missing nonce is recorded as `unbound`; it does not reject otherwise
valid local image receipts and does not skip or change any original capture or
assertion. Such receipts can describe their local PNG association, but cannot
claim a bound, completed wrapper launch. The wrapped articulated gate rejects
unbound provenance. Both routes are covered by the pure and wrapper tests.

The required later native gate must prove Mixin application against Minecraft
26.3 and Fabric client GameTest 6.0.7, actual body and main-hand view/item/deferred
passes, original PNG association, repeated frames, delayed callbacks, pause and
resume, replacement/cancellation, and original screenshot failures. The pure
checks and descriptor/compile review do not establish GPU behavior. No production
renderer or shared-player screenshot fixture changes are needed for registration.

### Native observation and capture correction

In ae9d57d8's PR run 37384979391, all four native suites and all 120 image
associations passed. The actual checkout was PR merge `31ba23fbb0feb337c287336a76e99373805805ee`;
its tree `f22ae8470ba51701511706cdc1428f8fbe605a13` exactly equals the feature head's tree.
All 40 requested ACTIVE Rising Break/Driving Cut images actually rendered RECOVERY;
the other 80 windup/recovery requests matched. Only slim live skins were observed.
The complete receipt artifact is 11379853489, SHA-256
`0a9f7ebf0b19644f2304bd51f3dc9bed28f29e5508563b5cc6b577965492256e`.

Receipts and the pinned Fabric API establish why: default screenshot delta ticks
are 1.0, while the fixture admitted its state at 0.5. At the accepted impact tick,
1.0 projects into recovery. The fixture now explicitly uses the supported
`withDeltaTicks(0.5F)` option, sharing that constant with its state extraction and
logged age. The observer, game time, accepted timeline, pose and original 120-trial
matrix are unchanged. API/projection regressions reproduce the former miss and
check the correction; the runner now separately requires actual phase coverage.
A fresh native run is still required before claiming the corrected ACTIVE images.
