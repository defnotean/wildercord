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

![Cinnamon in her pink bow, with the tip of her tongue out and her bell on her collar.]({{ '/assets/images/cinnamon-front.jpg' | relative_url }})

A pink bow, a gold bell and the tip of a tongue.
{: .caption }

## What it is

Cinnamon is a small, one-of-a-kind dog with her own model. She belongs to one player, follows them everywhere, and
cannot die from ordinary damage. There is only ever one Cinnamon in a world.

## How to get it

Cinnamon's owner is set in the config file `config/wildercord-cinnamon.json`. Start the game or server once to
create it, then put a username or UUID in it:

```json
{ "owner": "PlayerName" }
```

- The file reloads about every five seconds, so you don't need to restart.
- If the file has a mistake in it, the last good settings are kept.
- She then appears, already tamed, at a safe spot near her owner.
- Summoning a second Cinnamon does nothing. It can't replace her or change her owner.

## How to use it

Only her owner can command her.

| Action | Result |
|---|---|
| Use her (empty hand) | Switch between sitting and following |
| Sneak and use her | Pet her |
| Use **Cinnamon's Red Bone** | Play with her |
| Use **Cinnamon's Bow** | She wears it |
| Use shears | Take her bow back |
| Feed her dog food (such as beef) | She grows for a while |

Her sit or follow choice and her bow are saved. They stay through logging out, your death and dimension travel.

### Crafting

| Item | Shapeless recipe |
|---|---|
| Cinnamon's Red Bone | Bone, Red Dye, Slime Ball |
| Cinnamon's Bow | 2 String, Pink Dye |
| Cinnamon's Summoning Whistle | Copper Ingot, Bone, String |

### Her bell and habits

- Her **collar bell** jingles every 20 to 40 seconds, so you can find her by ear. To silence it, add
  `"bell": false` to the config file.
- She sometimes sticks the tip of her tongue out, even asleep.
- After sitting quietly for ten seconds she curls up and closes her eyes.

### She defends you

Anyone who hurts you gets bitten, like with a tame wolf. She does not attack while sitting, and leaves alone
creepers, your other pets and players you aren't allowed to fight.

### Treats make her bigger

Each treat makes her **0.5×** bigger, up to **3×** her normal size. She shrinks back **60 seconds** after the first
treat; more treats don't extend that time. She only grows if there is clear, solid room for her bigger body.

### Big Cinnamon is stronger, and casts magic

While she's bigger than normal, each treat's growth also makes her stronger. At full **3×** size:

- Her bite does **11** damage instead of 3. She has **40** health instead of 20, plus armour, and she's harder to knock back.
- She tires only after **40 damage** instead of 20.
- She joins any fight you start, not only the ones started against you.
- She casts spells. Every few seconds she throws a fire, frost, lightning or arcane bolt at whatever she's fighting, up
  to 20 blocks away. If your health drops below half, she casts a healing beam at you, at most once every 15 seconds.

Her spells follow your own friendly-fire rules, so they never hurt you, your party or your pets. They also never hurt
other players unless you are allowed to fight them. When she shrinks back to normal size, the extra strength and the
magic go away.

### She can't die

Damage doesn't lower her health. Instead, after **20 damage** (more while she's big) in a short time she gets tired and rests for
**30 seconds**. Damage she has taken is forgotten after ten quiet seconds.

While tired she walks slowly beside you, even if told to sit, and can't attack. When she recovers she goes back to
what you last told her.

### The whistle

**Cinnamon's Summoning Whistle** calls her to a safe spot beside you and tells her to follow. It works even if she
was left in an unloaded area or another dimension. It has a **10-second cooldown**, shared by every whistle.

The whistle won't bring her into protected or warded places, during a combat encounter, or into walls and cramped
spaces. If no safe spot exists, it tells you she is blocked.

![Cinnamon curled up and resting after sitting quietly.]({{ '/assets/images/cinnamon-resting.jpg' | relative_url }})

A little rest between adventures.
{: .caption }

## Tips and counterplay

- Left far behind while following, she catches up on her own. Told to sit, she stays put until you whistle.
- If she gets stuck in the void or inside blocks, she is moved to a safe spot nearby once you are around.
- She has no inventory and can't be bred.
- If she doesn't respond, check the owner name in the config file, then see
  [Troubleshooting]({{ '/troubleshooting/' | relative_url }}).

### For server admins

Cinnamon is never replaced with a copy. If her body is lost (for example through `/kill` or a save edit), the whistle
reports that recovery failed. Restore her from a world backup with matching player and world data; don't summon a new
one. With `"owner": "@singleplayer"` she follows the owner of a singleplayer world. On a dedicated server, use an
exact username or UUID.
