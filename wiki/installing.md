---
title: Installation and Updates
nav_order: 2.1
description: "Install the matching Minecraft, Fabric and Java versions, choose a launcher profile, and update a world safely."
---

# Installation and updates

Wildercord runs on **Minecraft Java 26.3** with **Fabric**. Install the same Wildercord version on the server and on every player's game.

The current version is **0.11.0-alpha**. Players still on 0.10 must update before they can join a 0.11 server.

## What you need

| Requirement | Version |
|---|---|
| Minecraft Java | 26.3 |
| Java | 25 or newer |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | 0.161.0+26.3 or newer |

## Pick your download

Download from [GitHub Releases](https://github.com/defnotean/wildercord/releases). The mod file for this version is `wildercord-0.11.0-alpha+mc26.3.jar`. The `-sources` jar is for developers; don't put it in your mods folder.

| Download | Use it when | Also includes |
|---|---|---|
| Mod jar | You already have a Fabric instance, or you run a server | Nothing: install Fabric API yourself |
| Performance `.mrpack` | You want lighter effects | Fabric API and Sodium |
| Balanced `.mrpack` | You want the default look | Fabric API and Sodium |
| Cinematic `.mrpack` | You want fuller effects and shader support | Fabric API, Sodium and Iris |

All profiles hold the same Wildercord jar. Only the visual defaults differ, never the gameplay. No shader pack is included: Iris alone doesn't turn shaders on.

## Install by hand

1. Make a **Minecraft 26.3** instance with **Java 25** and **Fabric Loader 0.19.5** or newer.
2. Add **Fabric API** for 26.3.
3. Put the Wildercord jar in the instance's `mods` folder.
4. For a dedicated server, install the same Wildercord and Fabric API there. Sodium and Iris are client-only.
5. Start the game and check the Mods list. Then follow [Getting Started]({{ '/getting-started/' | relative_url }}).

## Update a world

1. Close the game or stop the server.
2. Back up the world and the `config` folder.
3. Remove the old Wildercord jar, then add the new one. Keep only one version in `mods`.
4. Start again.

Your existing chunks stay as they are. Wandering Duelists, wildlife and world events still turn up in old terrain, but large dungeons and the 48 new sites (farmsteads, Master halls, mines, roadside, water and wild places) only appear in newly generated land. Explore new ground to find them. Server owners can add a training pavilion to an old world: see [Pavilion Upgrade]({{ '/world/pavilion-upgrade/' | relative_url }}). Check [What's New]({{ '/whats-new/' | relative_url }}) for changed rules.

## If it won't start

| Problem | Check |
|---|---|
| Dependency error | Minecraft, Fabric Loader, Fabric API and Java versions match the table above |
| Duplicate mod error | Remove the old jar and any `-sources` jar from `mods` |
| Server refuses you | Your Wildercord version matches the server's. A 0.10 game can't join a 0.11 server |
| Low frame rate | Open Magic visual settings and lower other players' effects first: see [Performance]({{ '/performance/' | relative_url }}) |

For other problems see [Troubleshooting]({{ '/troubleshooting/' | relative_url }}). When you report a crash, include your Minecraft and Wildercord versions and `logs/latest.log`.
