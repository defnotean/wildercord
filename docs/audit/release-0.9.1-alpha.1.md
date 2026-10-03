# 0.9.1-alpha.1 publication audit — 2026-10-03

## Published artifacts

- GitHub release: https://github.com/defnotean/wildercord/releases/tag/v0.9.1-alpha.1
- Release code commit: 9195e475 (learnall implementation d761a9e0; generated tooltip correction 0a302610).
- Five GitHub assets: ordinary mod, sources and Performance/Balanced/Cinematic launcher profiles.
- Mod SHA-256: cdd7741d6a48f4a12cd2d1f04e9b61e7b202ed68f024b42fb297020d1283fd1a.
- CurseForge upload accepted as file 9050277, project 1716203. Final inspected status: Approved.
  Author portal screenshot confirms moderation approval.
- Initial 0.9.1 upload 9050083 is superseded by this distinct corrected build.
- CurseForge summary/description updated with Aura, habitats, creatures, authored spells,
  damage limits, fusion command behavior, screenshots and current installation links.
- Wiki/GitBook update deployed successfully in GitHub Pages run 37144088485.
  Live home and What's New both contain 0.9.1-alpha.1; live Controls includes the fusion command rules.

## Verification

Native LearnAllFusionTest passed in 47s, including actual command execution, repeats,
client attachment synchronization and a complete saved-world restart. All 350 registered rune
results and 77 named recipe keys are present. Saved spells/names, missing-addon knowledge,
unrelated discoveries, condensed mana and innate ownership remain intact.

Generated reproducibility check passes across 4867 paths. Corrected full build passes in 15s:
963 tests, no failures/errors/skips. JAR CRC clean; 4864 resources and 1632 classes byte-match
build outputs. GitHub asset digests match local release and all three profile archives.
The GitHub Build job passed asset generation and unit/build gates. Its separate full native
CI game-tests job was still running when inspected; the entire workflow is not claimed green.

The initial CI failure was five stale generated modifier descriptions. Those were regenerated
from source, committed and verified. A distinct alpha.1 artifact prevents confusing the first
upload with the corrected release. No gameplay change was needed to correct that gate.

## VPS deployment status — pending player departure

SSH access confirmed to the user's configured Windows VPS. Live server root: C:\wildercord.
Four players remained online at the last check; installed mod was still 0.9.0-alpha.
The corrected JAR is staged as a .pending file, with matching remote SHA-256.

Scheduled task WildercordUpdate091 checks every two minutes. Its last invocation returned 0
and wrote state waiting with the actual RCON player list. It has not installed the update yet.
The task requires an empty-server RCON response twice, saves/stops cleanly, makes an offline
world/config/mods backup, retains the old JAR, installs exactly the staged checksum, then checks
loader version and RCON health. It does not force-kill Java. Success removes the update task;
failures disable it and write a diagnostic status. Existing watchdog state is restored.

Authoritative future checks:
- Scheduled task WildercordUpdate091 and Get-ScheduledTaskInfo.
- C:\wildercord\staging\update-091-status.json.
- Installed mod file/hash in C:\wildercord\mods.
- C:\wildercord\logs\latest.log and RCON list.

This is a queued deployment, not proof of a successful server update. Backup/startup behavior
has not executed yet and is not established by reading the script. The release upload and
wiki update are complete; server activation remains outstanding. The full living-world goal
stays active; the proposed Reed Rattle remains design work and is not in these builds.
