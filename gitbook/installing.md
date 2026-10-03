# Installation and updates

**This guide describes Wildercord 0.9.1-alpha for Minecraft Java 26.3.** Use the matching mod on the server and every player's client.

## Pick your download

Download the release from [GitHub Releases](https://github.com/defnotean/wildercord/releases). The ordinary mod file is **`wildercord-0.9.1-alpha+mc26.3.jar`**. The sources jar is for developers and does not go in your mods folder.

| Download | Use it when | Included dependencies |
|---|---|---|
| Mod jar | You already have a Fabric instance or manage a server | Install Fabric API separately |
| Performance `.mrpack` | You want restrained effects on your own machine | Fabric API and Sodium |
| Balanced `.mrpack` | You want the default presentation | Fabric API and Sodium |
| Cinematic `.mrpack` | You want fuller effects and optional shader support | Fabric API, Sodium and Iris |

Launcher profiles contain the same Wildercord jar. Their visual defaults differ; their gameplay rules do not. A third-party shader pack is **not included**. Installing Iris alone does not enable shaders.

## Manual installation

1. Create a **Minecraft 26.3** instance using **Java 25** and **Fabric Loader 0.19.5 or later**.
2. Install **Fabric API for Minecraft 26.3**. This release uses 0.161.0+26.3.
3. Put the Wildercord mod jar into that instance's `mods` folder.
4. For a dedicated server, install the same Wildercord and Fabric API versions there. Sodium and Iris belong on the client.
5. Start Minecraft and check the Mods list. Follow [Getting Started](getting-started.md) to equip a Cord.

## Updating an existing world

Close the game or stop the server, back up the world and configuration, remove the previous Wildercord jar, then install the new one. Keep only one version in `mods`. Existing chunks are preserved; new dungeons need newly generated terrain. Restarting loads the new practice dimension.

The published release is an alpha. Keep world backups when changing versions. See [What's New](whats-new.md) for additions and changed rules.

## Troubleshooting startup

| Symptom | Check |
|---|---|
| Dependency error | Minecraft version, Fabric Loader, Fabric API and Java version all match the release |
| Duplicate mod error | Remove the old jar and the sources jar from `mods` |
| Server rejects connection | Client and server have the same Wildercord version |
| Poor visual performance | Open Magic visual settings; reduce other players' effects first |
| Missing Cinnamon | Configure her owner separately; she does not default to the nearest player |

For gameplay problems, use the [Troubleshooting guide](troubleshooting.md). Include your Minecraft/mod versions and `logs/latest.log` when reporting a crash.
