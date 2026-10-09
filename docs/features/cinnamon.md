# Cinnamon

Cinnamon is a custom small dog with her own model, texture, owner and saved identity.

See the [complete player guide](../../wiki/companions/cinnamon.md) for owner configuration, her bow and toy, temporary feeding growth, mobile recovery, whistle recipe and bounded chunk recovery.

- Set one owner in `config/wildercord-cinnamon.json`; malformed reloads retain the complete last valid configuration.
- Each treat adds 0.5× size, up to 3×, for one fixed 60-second window. Repeated feeding cannot extend it. Collision, support and claim checks use her real size.
- Ordinary damage cannot kill her. A 20-point damage budget starts 30 seconds of following without attacking; isolated chip damage clears after ten quiet seconds.
- Craft the reusable whistle from a copper ingot, bone and string. All copies share a 10-second owner cooldown and recall the same saved Cinnamon.
- A source-only, nonpersistent chunk ticket can recover her original saved body for at most ten seconds. It is removed on completion, timeout, logout and server stop.
- Extra `/summon wildercord:cinnamon` bodies cannot take over her owner or bow. Unknown saved identities are preserved and quarantined, not silently replaced. An explicit administrator removal stays retired.
- A separately saved owner UUID marker prevents missing or conflicting journal data from silently creating another dog. Follow the server's identity-repair diagnostic and restore consistent backup records when required.

## Check the feature

On an authorized Minecraft development environment, the existing focused suite remains:

```powershell
.\gradlew.bat -PcinnamonSuite runClientGameTest
```

It includes real growth/collision and exhaustion assertions, original-UUID whistle recovery, chunk unload, owner respawn, cross-dimension travel, full saved-world close/reopen, interrupted journal writes, conflicting identity markers, administrative retirement and bounded timeout. Giant and tired-following screenshots accompany the earlier companion captures.

The expanded native gameplay and visual checks have not yet been run on this source. Standalone Java compilation and pure unit tests do not establish native acceptance.
