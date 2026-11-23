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
| Use her (empty hand, unharnessed) | Switch between sitting and following |
| Use her (empty hand, harnessed) | Mount Cinnamon (owner steers; party up to 4 riders) |
| Sneak and use her (unharnessed) | Pet her |
| Sneak and use her (harnessed) | Open her 54-slot twin side chests |
| Use **Cinnamon's Red Bone** | Play with her |
| Use **Cinnamon's Bow** | She wears it |
| Use **Cinnamon's Grand Bow** | Permanently grows to 3× size and empowers her stats & magic |
| Use **Cinnamon's Flying Harness** | Equips twin side chests, 4-seat riding, and 3D flight |
| Use shears | Take her Grand Bow, regular bow, or harness off (spilling chest contents safely if harness removed) |
| Feed her dog food (such as beef) | She grows for a while |

Her sit or follow choice, bow, Grand Bow, harness and saddlebag contents are saved. They stay through logging out, your death and dimension travel.

### Crafting

| Item | Recipe |
|---|---|
| Cinnamon's Red Bone | Bone, Red Dye, Slime Ball |
| Cinnamon's Bow | 2 String, Pink Dye |
| Cinnamon's Grand Bow | 4 Gold Ingots, Cinnamon's Bow, 2 Amethyst Shards, Mana Crystal |
| Cinnamon's Flying Harness | 4 Leather, Saddle, 2 Chests, Gold Ingot, Feather |
| Cinnamon's Summoning Whistle | Copper Ingot, Bone, String |

### Her bell and habits

- Her **collar bell** jingles every 20 to 40 seconds, so you can find her by ear. To silence it, add
  `"bell": false` to the config file.
- She sometimes sticks the tip of her tongue out, even asleep.
- After sitting quietly for ten seconds she curls up and closes her eyes.
- When you or any member of your party drops below **65% health**, Cinnamon walks or flies over and **licks** you back to health (healing 6 HP normally, or 10 HP when big or wearing the Grand Bow) and grants **Regeneration I** for 4 seconds.

### She defends you & casts passive magic

Anyone who hurts you gets bitten, like with a tame wolf. She does not attack while sitting, and leaves alone
creepers, your other pets, Sword Masters, and players you aren't allowed to fight.

Even at normal size, Cinnamon passively casts fire, frost, shock, and arcane bolts at nearby hostile targets.
While temporarily grown or permanently empowered with her **Grand Bow**:

- **Cinnamon's Grand Bow** makes her permanently **3×** size with **80** max health, **15** attack damage, **+10** armour, **+4** toughness, stronger knockback resistance, faster speed (**+0.11**), **60** damage budget before tiring, **24-block** spell range, **1.6×** spell power, and faster casting every **2.75 seconds**.
- Below half health, she sends a healing beam at you or injured party members every 15 seconds.
- **Hit pause balance:** whenever an enemy lands a hit on Cinnamon, she briefly pauses attacking, casting, and moving for **2 to 3 seconds** before jumping back into action.

### Flying Harness & Multi-Rider Saddlebags

Equipping **Cinnamon's Flying Harness** unlocks shared mount travel and storage:

- Up to **4 riders** (her owner plus same-party members) can ride Cinnamon at once, with one seat always reserved for her owner. Only the owner can steer and fly her.
- Jump to take off and climb through the air, steer in 3D along your look pitch, and touch down to walk on land. Both Cinnamon and dismounting airborne riders avoid fall damage (**Slow Falling** applies on midair dismount).
- Press your inventory key while riding—or Sneak + Right-Click while on foot—to open her **54-slot twin side chests**, shared with your party.

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
