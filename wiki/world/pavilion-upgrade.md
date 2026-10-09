---
title: Pavilion Upgrade for Old Worlds
parent: The World
nav_order: 40
---

# Pavilion Upgrade for Old Worlds

This page is for **server owners**. Players don't need to do anything.

## What it is

An optional, owner-approved tool that places one small **Wayfarer Training Pavilion** in an existing Overworld. The pavilion is 7 by 7 blocks: a mossy stone brick floor, four stripped oak columns, and a stone and smooth stone roof. That is 123 plain blocks, all inside one chunk.

You don't need it for most new content. Wildlife, wandering Duelists and Master introductions, wisps and world events already appear in old terrain. Large dungeons and gardens still only appear in newly generated land.

## What it never does

- Load or generate chunks.
- Dig, clear or replace blocks. Every block it places must go into air, on flat natural ground.
- Add loot, chests or other block entities.
- Spawn creatures or bosses, or rebuild dungeons and arenas.
- Run by itself. It is off by default, every approval lasts for the current session only, and it must be reviewed again after a restart.

It also refuses a spot, with no way to force it, when any of these apply:

- A player is within 160 blocks. A nearby player also starts a 60-second wait.
- The spot is within 256 blocks of spawn.
- The chunk has been lived in, or has player edits, chests or other block entities, structures, fluids or creatures nearby.
- The spot is outside the Overworld, or the chunk isn't already loaded.

It cannot tell for certain whether old terrain was edited by players or claimed by a land-claim mod. You check that yourself.

## How to use it

All commands need owner permission (level 4). Run them from the server console or while standing far away from the spot.

1. **Back up the whole world.** Stop the server, make and test a full offline backup, including the `data/wildercord-upgrades` folder. Note a name for that backup. Restart with the same mods.
2. Run `/wildercord-upgrade status`. It shows your installed-mod fingerprint.
3. Confirm how land claims are checked:
   - With a claim checker installed: `/wildercord-upgrade enable_adapters <fingerprint>`
   - With **no** land-claim system at all: `/wildercord-upgrade declare_no_external_claims <fingerprint>`
4. Preview a spot: `/wildercord-upgrade preview <chunk_x> <floor_y> <chunk_z>`. The pavilion is centered in that chunk at that floor height. Nothing is built yet. The command gives you a plan hash.
5. Read `data/wildercord-upgrades/<site>.preview.txt` on the server. It lists every block that would change. Look at the area in game too.
6. Approve the exact plan:
   `/wildercord-upgrade approve <hash> accept_unknown_history_and_confirm_offline_backup <backup_name>`
7. Watch progress with `/wildercord-upgrade inspect <hash>` or `status`. Building waits while players are close.

To stop all building, use `/wildercord-upgrade disable`. Recovery records are kept.

## After a crash or restart

Nothing restarts automatically. Turn your claim mode back on (step 3), then run:

- `/wildercord-upgrade reauthorize <hash> accept_unknown_history_and_confirm_offline_backup <backup_name>`
- `/wildercord-upgrade resume <hash>`

Resume only finishes blocks that are still exactly as expected. Anything else stops as a conflict for you to review.

## Undoing it

Run `/wildercord-upgrade rollback <hash>`. After a restart, first run `reauthorize_rollback` with the same words as `reauthorize`. Rollback only removes pavilion blocks that are still unchanged. Anything a player has since changed is left alone for you to check.

## Tips

- Pick an untouched, already generated chunk near the edge of explored land, not a village or base.
- Never edit or delete the `.wcu` files in `data/wildercord-upgrades`. They are the record used to resume and undo.
- The backup name is your note to yourself. The mod cannot check that the backup is real or complete.
