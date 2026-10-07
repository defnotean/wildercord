---
title: Cinnamon
nav_order: 1.1
parent: Companions and Style
previous_url: /companions/
previous_title: Companions overview
next_url: /companions/jobs-and-echoes/
next_title: Familiar jobs and event echoes
---

# Cinnamon

A small companion with a big personality. Cinnamon has her own model, an immortal heart, and one player to call home.

![Cinnamon in her pink bow, with the tip of her tongue out and her bell on her collar.]({{ '/assets/images/cinnamon-front.jpg' | relative_url }})

A pink bow, a gold bell and the tip of a tongue. Always by your side.
{: .caption }

## Choose her owner

Set one player's username or UUID in `config/wildercord-cinnamon.json`. Cinnamon arrives already tamed.

```json
{ "owner": "PlayerName" }
```

> Only her owner can ask her to sit or follow.

Start the game or server once to create the file. Its default owner is empty, so she waits until an owner is configured. The file reloads about every five seconds; a restart is unnecessary after editing it. Reloads validate the complete file first. Truncated JSON, an invalid owner value or a non-boolean `bell` keeps the entire last valid configuration.

In an integrated singleplayer development session, `"owner": "@singleplayer"` follows the world's owner even when Fabric changes its temporary dev username. Use an exact username or UUID on a dedicated server.

Cinnamon first appears at a safe, loaded position near that player. She then keeps one saved identity and body. An extra `/summon wildercord:cinnamon` is rejected; it cannot replace her, change her owner or duplicate her bow.

## Sit, follow and play

Interact with Cinnamon in a few simple ways.

| Action | Result |
|---|---|
| Click | Switch between sitting and following |
| Sneak-click | Pet her without changing her sit preference |
| Use Cinnamon's Red Bone | Wake her and play |
| Use Cinnamon's Bow | She wears it |
| Use shears | Untie her bow and get it back |

Her sitting preference is saved with her companion identity and survives rejoining, owner death and dimension travel. Greetings and petting bring an eager tail wag. She occasionally tilts her head, and after sitting quietly for ten seconds curls down and closes her eyes.

Craft her reusable Red Bone from a **bone, red dye and slime ball**. It never changes her owner or her saved sitting preference.

## Her bell, her bow and her temper

- **Her collar bell** jingles every 20 to 40 seconds, loud enough to hear from a good way off, so you can find her by ear. To quiet it, add `"bell": false` to `config/wildercord-cinnamon.json`.
- **Now and then** she sticks the tip of her tongue out for a few seconds, even in her sleep.
- **Cinnamon's Bow** is her armour, of a kind: craft it from **two string and pink dye** and use it on her. Like her sitting choice, it is saved with her companion identity, so she still wears it after rejoining, owner death or dimension travel.
- **She defends you.** Anyone who hurts her owner gets bitten, the way a tame dog would. She stays put while she's sitting, and leaves creepers, her owner's other pets and players you can't fight alone.

![Cinnamon curled up and resting after sitting quietly.]({{ '/assets/images/cinnamon-resting.jpg' | relative_url }})

A little rest between adventures.
{: .caption }

## Treats and temporary growth

Feed her any food in Minecraft's wolf-food tag, such as beef. Each accepted treat increases her size by **0.5×**, up to **3×** her ordinary size: about **1.74 blocks wide and 2.01 blocks tall**. Her model, shadow, collision box and navigation use the same physical scale. Growth does not increase bite damage.

The first treat starts **one 60-second window**. Extra treats never extend that deadline; feeding at the cap uses no food. Afterward she returns to normal. Growth requires loaded, clear, permitted space and solid footing across her entire larger footprint. If a new obstruction makes that size unsafe, she shrinks early; another treat still uses the original deadline. A narrow recall destination does not make her larger body pass through walls.

## Immortal, with time to recover

Ordinary damage never removes her health. Instead, **20 points of accumulated damage** send her into **30 seconds of recovery**. Hits have a half-second counting interval, and chip damage clears after ten quiet seconds.

While recovering she has a tired walking pose, follows you even if she had been asked to sit, and cannot attack. Her saved sitting preference returns when recovery ends, unless you use her whistle to ask her to follow. Feeding, toys, recalling, owner death, logout and dimension travel do not restart or shorten her saved deadline. Time spent online elsewhere still counts; time while the server is stopped does not.

## Call her with a whistle

Craft **Cinnamon's Summoning Whistle** from a **copper ingot, bone and string**. Only the configured owner can use it. Every copy shares the same saved **10-second cooldown** and calls the same Cinnamon.

The whistle asks her to follow and brings her to a clear, loaded, permitted landing beside you. It can retrieve her saved body after a chunk unload or owner dimension change. It refuses protected or warded cells, combat encounters, unsafe footing, walls, passengers and cramped spaces. If no safe landing exists, the message explains that she is blocked. Claim compatibility uses the mod's conservative read-only break-permission probe and may refuse travel on servers with restrictive claim rules.

Following Cinnamon also catches up when you move far away. If asked to sit in the same dimension, she stays parked and can unload normally until called.

## Keeping her safe

Her identity, owner, bow, requested sit and recovery/growth deadlines are saved. A separate owner UUID marker is saved and copied on owner death. If the marker and journal disagree, or only the marker remains, she is kept inactive and the whistle reports that identity repair is needed; it never silently chooses another UUID. Older worlds migrate the existing owner sit/bow choices when their first companion identity is created. She has no storage inventory and cannot be bred or used to multiply items.

Void or block entrapment recovery moves the original body to a safe nearby surface. If the owner is offline, dead or in a protected place, recovery waits. Once the owner is available, the normal checks apply again.

For an unloaded body, recovery may temporarily load **one registered chunk location globally**, for at most **10 seconds**. Minecraft also loads the finite neighbouring dependencies of that ticket. This is not permanent chunk loading or a moving trail through unexplored terrain. Tickets are removed on success, timeout, logout, configuration change and server stop; failed automatic recovery stays stopped until a new whistle call or login.

If the registered body cannot be found, Cinnamon is not replaced with a copy. The whistle reports the failed recovery. An explicit administrator `/kill`, discard or save edit is outside ordinary immortality; a verified removed body stays retired. Administrators should preserve the current save, check the owner/journal UUIDs in the server warning, and restore matching player, companion-journal and entity records from a consistent world backup. Unknown owned bodies are preserved while authority is uncertain. A missing owner marker can be restored from an intact matching journal; conflicting markers are never silently overwritten. If both identity records and the original entity data are lost or independently rolled back, automatic identity recovery is not promised. Do not use summon commands to manufacture a replacement. Clean save/restart is covered by the native test suite; abrupt crashes and independently rolled-back region/player/journal files are not a crash-atomic transaction.

Her round tan face with its little angry brows, dark saddle, white belly, floppy ears, fluffy legs and black nub of a tail use a separate entity, model and texture from Minecraft's wolf.

If she does not respond, check the configured owner and the [troubleshooting guide]({{ '/troubleshooting/' | relative_url }}).

> Development verification: the expanded growth, recovery, whistle and saved-world tests are included, but fresh native execution and visual acceptance are still pending. These mechanics are not yet a runtime-verified release claim.
