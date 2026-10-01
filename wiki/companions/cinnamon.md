---
title: Cinnamon
nav_order: 1.1
parent: Companions and Style
---

# Cinnamon

Cinnamon is a custom small dog inspired by the supplied photographs: a dark, rounded body, tan face and legs, long floppy ears, and shaggy paws. Her entity, model and texture are separate from Minecraft's wolf. Her in-game name is **Cinnamon**.

## Choose her owner

Start the game or server once to create `config/wildercord-cinnamon.json`, then set `owner` to the one player's exact Minecraft username or UUID:

```json
{ "owner": "PlayerName" }
```

The default value is empty, so Cinnamon waits for an owner. On an integrated singleplayer dev session, `"owner": "@singleplayer"` follows that world's owner even if Fabric changes its temporary dev username between launches. A dedicated server should use a username or UUID. The file is checked again about every five seconds, so a restart is unnecessary after editing it.

Cinnamon appears at a safe, loaded position near her owner already tamed. `/summon wildercord:cinnamon` also works: when the configured owner is online in that dimension, the summoned Cinnamon is bound to that player and replaces her previous body. Only her owner can click her to switch between sitting and following. That choice is saved on her owner and survives direct replacement, rejoining, death, and dimension travel. Her replaceable body returns near her owner after travel; it does not stay behind in another dimension.

She takes no damage from combat, fire, falls or magic. A companion that falls into the void or gets trapped in a solid block is moved back to a safe nearby surface. If there is no safe surface, she waits for one instead of appearing inside a hazard.

Sneak-click to pet her without changing her sit setting. Her greeting and petting have an eager tail wag, and she occasionally tilts her head while idle. After sitting quietly for ten seconds she curls down and closes her eyes. Click her with Cinnamon's Red Bone to wake her and play; the toy is reusable and does not change ownership or her sitting preference. Craft it from a bone, red dye, and a slime ball.

## In game

![Cinnamon rests after sitting quietly; her owner can wake her to play.]({{ '/assets/images/cinnamon-resting.jpg' | relative_url }})

Cinnamon rests after sitting quietly; her owner can wake her to play.

## In game

![Cinnamon has a small, round body, dark coat and tan face, with her own model.]({{ '/assets/images/cinnamon-front.jpg' | relative_url }})

Cinnamon has a small, round body, dark coat and tan face, with her own model.
