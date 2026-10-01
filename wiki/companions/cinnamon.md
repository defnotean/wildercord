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

![Cinnamon, the small custom dog, waiting near her owner.]({{ '/assets/images/cinnamon-front.jpg' | relative_url }})

Small paws. A dark coat. Always by your side.
{: .caption }

## Choose her owner

Set one player's username or UUID in `config/wildercord-cinnamon.json`. Cinnamon arrives already tamed.

```json
{ "owner": "PlayerName" }
```

> Only her owner can ask her to sit or follow.

Start the game or server once to create the file. Its default owner is empty, so she waits until an owner is configured. The file reloads about every five seconds; a restart is unnecessary after editing it.

In an integrated singleplayer development session, `"owner": "@singleplayer"` follows the world's owner even when Fabric changes its temporary dev username. Use an exact username or UUID on a dedicated server.

Cinnamon appears at a safe, loaded position near that player. `/summon wildercord:cinnamon` also works: with the configured owner online in the same dimension, the summoned body binds to that player and replaces her previous body. Summoning does not transfer ownership to another player.

## Sit, follow and play

Interact with Cinnamon in a few simple ways.

| Action | Result |
|---|---|
| Click | Switch between sitting and following |
| Sneak-click | Pet her without changing her sit preference |
| Use Cinnamon's Red Bone | Wake her and play |

Her sitting preference is saved on her owner and survives replacement, rejoining, death and dimension travel. Greetings and petting bring an eager tail wag. She occasionally tilts her head, and after sitting quietly for ten seconds curls down and closes her eyes.

Craft her reusable Red Bone from a **bone, red dye and slime ball**. It never changes her owner or her saved sitting preference.

![Cinnamon curled up and resting after sitting quietly.]({{ '/assets/images/cinnamon-resting.jpg' | relative_url }})

A little rest between adventures.
{: .caption }

## Keeping her safe

Cinnamon takes no damage from combat, fire, falls or magic. If she falls into the void or becomes trapped in a solid block, she returns to a safe nearby surface. When no safe surface exists, she waits instead of spawning inside a hazard.

Her replaceable body returns near her owner after dimension travel, rather than staying behind. Her dark, rounded body, tan face and legs, floppy ears and shaggy paws use a separate entity, model and texture from Minecraft's wolf.

If she spawns but does not respond to a click, check the configured owner and use the [troubleshooting guide]({{ '/troubleshooting/' | relative_url }}).
