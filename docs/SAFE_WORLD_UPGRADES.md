# Bounded existing-world upgrades

## Scope

Four individually reviewed adapters can each add one bounded site to an explicitly reviewed
location in an existing Overworld: the inert **Wayfarer Training Pavilion** and three small
encounter sites (Sleeping Blade rest, Battlefield memorial, Sword Tomb duel ring). None of them
regenerates chunks, runs dungeon post-processing, populates all old terrain or spawns an
entity. Existing natural wildlife, wandering Duelists/Master introductions, wisps and
runtime events already work in older terrain; large dungeons and gardens remain
world-generation-only. See "Catalog decisions" below for every worldgen family.

The pavilion is 7×7, with a 49-block mossy-stone-brick floor, four four-high stripped-oak
columns (16 blocks), a 24-block stone-brick roof border, 25 smooth-stone roof blocks and
nine raised smooth-stone roof blocks: **123 writes**. A 9×9×9 snapshot covers the unchanged
foundation and eight blocks of headroom (729 cells). Everything fits inside one chunk.
All write cells must be air; the full 9×9 foundation must be flat and from a narrow
natural-material allowlist. Nothing is dug, cleared, replaced with loot, or given a block
entity. Two writes maximum per tick, one active transaction per server, 128 retained sites.
The 12-block Cairn exists only in recovery tests; it is not a player-facing building.

## Catalog decisions

`UpgradeCatalog` lists every structure and placed feature under
`data/wildercord/worldgen/structure` and `feature`, plus runtime-only content. A unit test fails
if a new worldgen file has no individual decision. `/wildercord-upgrade catalog` prints them.

- **Bounded adapter**. Each has a reviewed `UpgradeBlueprints` layout that fits in one chunk,
  with at most 256 writes and a guard of at most 1024 cells. The foundation must be natural and
  every other guard cell must be air.
  - `wayfarer_training_pavilion` v1: 123 writes, 9x9x9 guard, no anchor (inert).
  - `sleeping_blade_rest` v1: 38 writes, 7x7x6 guard. The anchor is a Sleeping Blade stone.
  - `battlefield_memorial` v1: 58 writes, 9x9x5 guard. The anchor is a battlefield memorial.
  - `sword_tomb_duel_ring` v1: 182 writes, 13x13x6 guard (1014 cells). The anchor is a north-facing
    tomb reliquary. Its keeper arena, 8 blocks south, sits inside the guard with three cells of
    headroom. The Gravekeeper only rises through the existing reliquary trigger after placement.
- **World-generation only**:
  - the archive, ember sanctum, astral observatory, drowned scriptorium, rootbound maze, storm
    spire, clockwork crypt, living greenhouse, moving sky ruin and belowkeeper drainhouse dungeons;
  - the full old battlefield, sword tomb and sleeping blade structures. Their multi-chunk
    jigsaw and post-processing is never replayed; the small adapters above stand in for them;
  - breathmark sites and herb patches.

  Their size, loot, entities or destructive post-processing cannot fit the bounded,
  compare-and-set model.
- **Runtime only, no adapter needed**: Master training grounds, wandering Masters/Duelists and
  village tournaments are spawned or opened at runtime and already reach old terrain.

Worldgen has no Master training-ground structure today, so there is nothing to adapt.

## Encounter adapters and duplicate prevention

The preview lists the anchor block, which is placed like any other compare-and-set write.
Its block entity is marked authentic (the same flag natural generation sets) only after
**every** write is observed. This happens through the engine's activation hook, before APPLIED
is persisted. If the anchor is missing or the chunk unloads, activation defers and the manifest
stays APPLYING. A resumed or repeated step re-runs activation idempotently.

Sleeping Blades, Battlefields and Sword Tombs are unique encounters. Each allows at most one site
per random-spread region of its shipped structure set, and none within the separation distance
in a neighbouring region. The spacing/separation values are 92/36, 64/24 and 76/28 respectively,
and a unit test checks them against the JSON. A site is refused at preview, approval,
reauthorization and before every write when any of the following exists:

- another manifest of the family in the journal that is not PREVIEW or ROLLED_BACK
  (APPLYING, APPLIED, ROLLING_BACK and CONFLICT all count);
- a saved `upgrade_sites` record (SavedData) of a natural or upgraded site:
  - Natural records come from valid Wildercord structure starts and authentic generated anchors
    seen whenever a chunk loads, and from a scan of resident chunks across the nearby regions at
    admission.
  - Upgrade records mirror every manifest's family, chunk, content version and phase.
  - The record store holds 4096 entries. Once full it saturates and refuses every unique family;
- a live Gravekeeper loaded anywhere in the nearby regions (Sword Tomb only);
- a natural start at the region's candidate chunk in the generator's own placement, or one that
  cannot be verified (`CHUNK_LOAD_NEEDED`). If the world's generator does not place the set at all
  (a flat or custom preset), this check is skipped. If it places the set with a different spread,
  the site is refused.

Limits:

- An old natural site is unknown when its chunk has not loaded since this version and the
  generator check cannot see it (for example, a preset without the structure set).
- Rollback is deferred while the encounter is live: a guardian bound to the reliquary, or a
  Sleeping Blade draw in progress.
- Rollback removes the anchor block together with its saved state. The mod never deletes or
  spawns encounter entities itself.

`/wildercord-upgrade sites` lists the records.

Operator commands: use `/wildercord-upgrade preview_site <family> <chunk_x> <floor_y> <chunk_z>`
for the encounter families. The pavilion also keeps `preview`. Everything after the preview
(approve, reauthorize, resume, rollback, inspect) is unchanged.

## Important limits

**Historical provenance is UNKNOWN.** A natural-looking floor and missing edit records
never prove old terrain is untouched. There is no perfect detection of old player edits,
private claims, commands/mods that bypass normal hooks, or changes made while Wildercord
is absent. The operator must inspect the exact region and surrounding builds and accept
that uncertainty. Do not run this against a valuable world without a tested offline
whole-world backup.

A command permission check is not a claim query. Read-only claim adapters report CLEAR,
CLAIMED, UNKNOWN/error, or NO_PROVIDER_CONFIGURED. Every configured adapter must return
CLEAR; one CLAIMED, UNKNOWN, exception, or invalid status vetoes placement. Adapters must
inspect their claim inventory without loading chunks and supply a revision that changes
with configuration/authority semantics. Register/unregister also changes the policy.
No third-party claim adapter is bundled in this first slice.

For genuinely no-claims installations only, an operator may deliberately enable manual
no-provider mode. This explicitly lacks automatic claim verification. It cannot bypass a
configured adapter, even if that adapter is broken. Installed mod IDs/versions are hashed
into the declaration and policy; this is not detection of every possible claim system.
The declaration, write-enable setting and active job are session-local, default off, and
must be reviewed again after restart. Every enable/disable or observed provider revision
change invalidates outstanding approvals. The historical-provenance acknowledgment is
separate from this claim policy.

Default exclusions cannot be waived by approval: any inhabited-time evidence, known
future player edits, any block entities in the chunk, local valid structure starts or
structure references, the 256-block spawn buffer, build/world-border limits, unsafe guard
blocks, fluids, entities inside the guard, and players within 160 blocks horizontally. A seen nearby player also
starts a 60-second quiet period in the current session. Containers/redstone/custom blocks
are excluded in the guarded volume; this is not a scan of arbitrary surrounding terrain.
Strict inhabited-time exclusion makes this intentionally conservative: use an uninhabited,
already generated edge-of-view-distance chunk, not a visited settlement. There is no
"force" override. All adapters are Overworld-only. The only block entity tolerated in the
chunk is the plan's own anchor at its blueprint position, during resume and rollback.

## Operator workflow

Commands require owner-level permission (level 4). Use the server console or an operator
far outside the guard's player buffer. The scanner only examines an already resident full
chunk. It does not load or generate a chunk for you.

1. Stop the server and make/test an offline backup of the **whole save**, including
   `data/wildercord-upgrades`. Restart with the same mods. Keep the backup reference.
2. `/wildercord-upgrade status` reports the installed-mod fingerprint and mode.
3. If real claim adapters are installed, use
   `/wildercord-upgrade enable_adapters <installed_mod_fingerprint>`.
   Otherwise, only after confirming that no external claims system exists, use
   `/wildercord-upgrade declare_no_external_claims <installed_mod_fingerprint>`.
   This is an explicit declaration, not proof of unclaimed land.
4. `/wildercord-upgrade preview <chunk_x> <floor_y> <chunk_z>` chooses the chunk's center
   (local x=8, z=8) at the specified floor y. It performs no world writes and reports the
   exact dimension, guard bounds and full SHA-256 plan hash.
5. Inspect the server-side `data/wildercord-upgrades/<site>.preview.txt`: every WRITE and
   unchanged GUARD cell is listed with original and desired state. Visually inspect the
   region too, without entering the exclusion buffer during placement. Review claims with
   the relevant server tools. Natural-looking blocks alone are not evidence.
6. Only after accepting the exact region and unknown historical provenance, issue
   `/wildercord-upgrade approve <full_hash> accept_unknown_history_and_confirm_offline_backup <backup_reference>`.
   This persists approval and schedules that exact plan. A changed guard, new edit, provider
   policy change or missing safety condition prevents further writes.
7. Use `/wildercord-upgrade inspect <full_hash>` and `status` for progress.
   `/wildercord-upgrade disable` stops scheduling and disables writes; it never deletes
   recovery records. Unload/player approach defers without requesting chunk tickets.

The backup reference is an operator assertion identifying the tested offline backup; the
mod cannot verify the backup's completeness. The `.preview.txt` is readable review output.
The checksummed `.wcu` manifest is the recovery authority and contains every original state,
site/blueprint identity, approval/policy, current write intent and later-edit fences. Do not
edit or delete it, or discard it after the structure becomes visible.

## Restart, crash reconciliation and rollback

World chunks, SavedData and entities are not one atomic transaction. `setDirty()` does not
prove anything reached disk. This implementation uses a separate bounded write-ahead
journal; normal SavedData is used only for conservative future player-edit evidence.

For each block:

1. Persist the immutable original snapshot, operator/backup/policy authorization and exact
   next-write intent to a temporary manifest; force the file.
2. Atomically replace the manifest and force its directory. Newly created journal directory
   entries are forced too. Filesystems without these primitives fail closed; there is no
   weaker non-atomic fallback.
3. Recheck loaded status, absence of nearby players, claims/policy, the entire guard and
   per-cell expected state, then perform at most one compare-and-set block write.
4. Keep the manifest permanently. APPLIED means all desired blocks were **observed** in
   memory; it does not claim the chunk and journal were atomically flushed together.

After a crash, any intent can correspond to its original or intended state. A restart does
not write automatically. The operator reviews the retained manifest, re-enables the current
claim mode, then explicitly uses:

`/wildercord-upgrade reauthorize <full_hash> accept_unknown_history_and_confirm_offline_backup <backup_reference>`

and `/wildercord-upgrade resume <full_hash>`.

Resume accepts only exact original/intended states and unchanged guards, with no later-edit
fences. It reapplies only still-original cells. Anything else stops as a conflict. This also
reconciles an APPLIED manifest whose chunk save lagged behind, and re-runs anchor activation
if the anchor's saved flag lagged behind. A player edit is never overwritten and there is no
override. The only explicit way to build over a changed region is for the operator to preview
and approve a different site that is still clean. There is no whole-server
forced save or pretend cross-file commit in the bounded placement loop.

`/wildercord-upgrade rollback <full_hash>` reverses at most two changes per tick. On restart
or policy change, first use the same explicit acknowledgment with `reauthorize_rollback`.
Rollback only restores a cell that still equals this plan's placed state and has no external
edit fence. A changed or later identically replaced cell is preserved for manual review;
other safe cells can be restored. The ROLLED_BACK/CONFLICT tombstone survives and prevents
recreating the same site after a blueprint-version bump. A ROLLED_BACK manifest can still
reconcile a lagging chunk save through explicit rollback; it can never authorize placement.
Rollback-only reauthorization is not placement authority.

The lowest ordinary `LevelChunk.setBlockState` hook forces an edit fence **before** an
external change to a guarded cell, including commands, creative/survival building and
ABA (remove then identically replace) changes. Failed attempts may conservatively fence a
cell too. A one-use, exact-position/state permit exempts only the upgrade's own write;
nested edits are not exempt. If the journal fails, protected-region edits are refused rather
than silently losing conflict evidence; unrelated world edits continue. Restart and inspect
the server error and backup before attempting recovery. If retained manifests cannot be
read/forced at startup, server startup is refused so normal edits cannot silently lose their
fences; restore access or the complete offline backup. On a fresh server with no retained
recovery files, unsupported journal setup only disables this feature. Successful ordinary player block
placements and breaks also mark their chunk as edited for future eligibility; that SavedData
may lag disk and cannot prove historical safety. Evidence is capped at 65,536 distinct
chunks per dimension; reaching the cap conservatively excludes the whole dimension from
new placement instead of discarding edit history. Recent-activity cache saturation similarly
defers all candidate regions for the remaining quiet period.

## Verification and remaining release gates

`UpgradeEngineTest` uses real on-disk forced manifests and an in-memory block-world port to
cover each of the 12 Cairn apply and rollback crash boundaries, partial chunk saves, durable
intent ordering, failures, policy changes, idempotence, duplicate/version tombstones and ABA
conflicts. `UpgradeClaimsTest` covers distinct no-provider/unknown/claimed/error behavior.
These are deterministic fault-injection tests, not actual power-loss hardware testing.

`UpgradeCatalogTest` checks that:

- every worldgen structure and feature has a decision;
- unique families mirror their shipped spread;
- blueprints fit one chunk and the budgets;
- the region/separation duplicate model works for natural and upgraded sites;
- admission refuses approval and reauthorization, and activation defers APPLIED until the
  anchor is marked.

`UpgradeEncounterTest` is a native test in a disposable flat world.

1. It previews and approves a Sword Tomb with a test claim adapter. CLAIMED refuses approval,
   a claim that appears after approval stops writes, and a provider revision voids the
   approval until it is reauthorized.
2. It interrupts placement at 100 of 182 writes and refuses a second tomb in the same region.
3. It records a natural authentic Sleeping Blade and refuses an upgraded one against it.
4. It applies a Battlefield memorial, which stays inert until the last write, and refuses a
   tomb next to a live Gravekeeper.
5. After an actual server close and reopen it:
   - checks the saved records and the anchor flag;
   - resumes the tomb exactly once (authentic reliquary, no keeper spawned);
   - still refuses a second tomb;
   - preserves a later player edit through a CONFLICT rollback;
   - cleanly rolls back the memorial after rollback reauthorization.

The test world does not place the structure sets, so the generator check is not exercised
natively.

`UpgradeRecoveryTest` is a native Fabric client fixture that creates an isolated save,
places five real pavilion blocks with an injected interruption, closes/reopens the actual
integrated server, reauthorizes/resumes the remaining 118, unloads the actual chunk, proves
recovery does not reload it, and checks native mixin-detected ABA-safe rollback. It does not
open a real user world. Compiling it is not evidence of executing it; record its actual run
separately before declaring this feature native-verified. No new CI job variant is added.

Further building/entity adapters need independent budgets, native tests and review. Dungeons,
loot, spawned entities and crash-safe entity UUID receipts remain outside these adapters.
Never adapt destructive dungeon `postProcess` directly.
