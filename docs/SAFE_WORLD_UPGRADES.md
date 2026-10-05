# Bounded existing-world upgrades

## Scope

This first adapter adds one original open **Wayfarer Training Pavilion** to an
explicitly reviewed location in an existing Overworld. It does not regenerate chunks,
run dungeon post-processing, populate all old terrain, retrofit boss arenas, or spawn
entities. Existing natural wildlife, wandering Duelists/Master introductions, wisps and
runtime events already work in older terrain; large dungeons and gardens remain
world-generation-only.

The pavilion is 7×7, with a 49-block mossy-stone-brick floor, four four-high stripped-oak
columns (16 blocks), a 24-block stone-brick roof border, 25 smooth-stone roof blocks and
nine raised smooth-stone roof blocks: **123 writes**. A 9×9×9 snapshot covers the unchanged
foundation and eight blocks of headroom (729 cells). Everything fits inside one chunk.
All write cells must be air; the full 9×9 foundation must be flat and from a narrow
natural-material allowlist. Nothing is dug, cleared, replaced with loot, or given a block
entity. Two writes maximum per tick, one active transaction per server, 128 retained sites.
The 12-block Cairn exists only in recovery tests; it is not a player-facing building.

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
"force" override. The first adapter is Overworld-only.

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
reconciles an APPLIED manifest whose chunk save lagged behind. There is no whole-server
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

`UpgradeRecoveryTest` is a native Fabric client fixture that creates an isolated save,
places five real pavilion blocks with an injected interruption, closes/reopens the actual
integrated server, reauthorizes/resumes the remaining 118, unloads the actual chunk, proves
recovery does not reload it, and checks native mixin-detected ABA-safe rollback. It does not
open a real user world. Compiling it is not evidence of executing it; record its actual run
separately before declaring this feature native-verified. No new CI job variant is added.

Further building/entity adapters need independent budgets, native tests and review. Bosses,
loot, encounters, crash-safe entity UUID receipts and encounter tombstones remain outside
this initial inert structure adapter. Never adapt destructive dungeon `postProcess` directly.
