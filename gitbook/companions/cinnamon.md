# Cinnamon

A small companion with a big personality. Cinnamon has her own model, an immortal heart, and one player to call home.

![Cinnamon in her pink bow, with the tip of her tongue out and her bell on her collar.](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/cinnamon-front.jpg)

A pink bow, a gold bell and the tip of a tongue. Always by your side.


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
| Use Cinnamon's Bow | She wears it |
| Use shears | Untie her bow and get it back |

Her sitting preference is saved on her owner and survives replacement, rejoining, death and dimension travel. Greetings and petting bring an eager tail wag. She occasionally tilts her head, and after sitting quietly for ten seconds curls down and closes her eyes.

Craft her reusable Red Bone from a **bone, red dye and slime ball**. It never changes her owner or her saved sitting preference.

## Her bell, her bow and her temper

- **Her collar bell** jingles every 20 to 40 seconds, loud enough to hear from a good way off, so you can find her by ear. To quiet it, add `"bell": false` to `config/wildercord-cinnamon.json`.
- **Now and then** she sticks the tip of her tongue out for a few seconds, even in her sleep.
- **Cinnamon's Bow** is her armour, of a kind: craft it from **two string and pink dye** and use it on her. Like her sitting choice, it's kept on her owner, so she still wears it after rejoining, death or dimension travel.
- **She defends you.** Anyone who hurts her owner gets bitten, the way a tame dog would. She stays put while she's sitting, and leaves creepers, her owner's other pets and players you can't fight alone.

![Cinnamon curled up and resting after sitting quietly.](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/cinnamon-resting.jpg)

A little rest between adventures.


## Keeping her safe

Cinnamon takes no damage from combat, fire, falls or magic. If she falls into the void or becomes trapped in a solid block, she returns to a safe nearby surface. When no safe surface exists, she waits instead of spawning inside a hazard.

Her replaceable body returns near her owner after dimension travel, rather than staying behind. Her round tan face with its little angry brows, dark saddle, white belly, floppy ears, fluffy legs and black nub of a tail use a separate entity, model and texture from Minecraft's wolf.

If she spawns but does not respond to a click, check the configured owner and use the [troubleshooting guide](../troubleshooting.md).
