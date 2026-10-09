# World events

## What it is

Three events start on their own in the Overworld: **mana storms** over ley lines, **fallen stars** at night and **rift sieges** near bases and villages.

## How to get it

Events only start near players, in loaded ground.

- Every 30 seconds, the area around each player gets a chance to start one.
- Fallen stars and rifts never start on Peaceful. Mana storms still do.
- When each event can next happen is saved with the world, so restarting doesn't reset it.
- Monsters an event brings never pick up items, and they leave when the event ends.
- Server operators can start an event with a command (see [Controls](../controls.md)), or turn events off.

## How to use it

### Mana storms

![A meadow under a violet mana storm](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/mana-storm.jpg)

**When:** A storm forms over a [ley line](../progression/ley-lines.md) within 48 blocks of a player.
- It happens about once every three in-game days.
- After a storm, the same region waits a day and a half.
- At most two storms can be active at once, and they never overlap.
- A storm lasts 3 to 5 minutes and reaches 96 blocks from its center.

**Under a storm:**

| | Effect |
|---|---|
| Mana regeneration | Twice as fast |
| Spell cost | 25% less |
| Surges | Each spell has a 10% chance to surge |

| Surge | Share | What happens |
|---|---|---|
| Swell | 35% | The spell gets 60% more power |
| Stray element | 25% | A second spell rides along. It uses your shape (a Bolt if you cast Self or Touch) with a random effect: Fire, Frost, Shock, Windcut, Pelt, Hex or Harm |
| Echo | 25% | The spell casts again half a second later, free |
| Backfire | 15% | You take up to 3 damage (never below 1 heart), get knocked back and lose 10 mana |

**Storm runes:**
- Your 10th cast under a storm and every cast after it can surge into a rune. Each surge has a 1 in 3 chance.
- The rune is [Manaburn](../runes/world.md#manaburn) 5 times in 7, otherwise [Manatide](../runes/world.md#manatide).
- It goes straight into your pack.
- You can get one per storm.

Fishing under a storm can also pull up runes. See [Fishing](runes-of-the-world.md#fishing).

**Feat:** *Stormcaller*, for 20 casts under one storm.

### Fallen stars

![A falling star streaking across the night sky](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-star-falling.jpg)

**When:** Stars fall at night, about once every five nights for each player. Only one star can lie in a world at a time. After one falls, the next waits half a day.
- A star lands 60 to 150 blocks from a player, on dry, solid ground where that player could build.
- If something gets built under a falling star, it burns up in the air instead.

**What happens:**
1. Players within 220 blocks are told which way the star fell.
2. It lands with a boom. If mobGriefing is on, it blasts a small, shallow crater in natural ground only. Otherwise it just leaves scorch marks.
3. The Fallen Star can't be broken. A column of starlight rises from it for 5 minutes.

![A column of starlight rising from a Fallen Star, with Runebound guards](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-star-column.jpg)

**Guards:** When anyone comes within 40 blocks, 2 to 4 [Runebound](runebound.md) rise. The first is always a vindicator Adept. The star won't open until every guard is dead. Using the star tells you how many are left.

**Opening it:** Right-click the star to get:
- **A rune.** Usually a Tier III, most often [Starshard](../runes/world.md#starshard). Sometimes a Tier IV. The rune is chosen when the star lands.
- **A Mana Crystal.**
- **30 experience.**
- **The *Stargazer* feat.**

After it's opened, the guards vanish and the crater fills back in. Anyone standing in it is lifted out first. If nobody opens a star, it fades after 20 minutes.

### Rift sieges

![A violet rift with monsters pouring out at night](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-riftcaller.jpg)

**When:** Rifts open at night near a settled place, about once every six nights for each player.
- A place counts as settled if the area around you has at least 6 storage or work blocks, such as chests, furnaces, lecterns or signs.
- Only one rift can be open per world, with a full day between rifts.
- A rift opens 14 to 26 blocks from a player.

**Waves:** Runebound step out of the tear in three waves. A boss bar tracks them.

| Wave | Arrives | Monsters (1 player) | Per extra player | Adepts |
|---|---|---|---|---|
| 1 | 3 s | 3 | +1 | none |
| 2 | 45 s | 4 | +1 | 1 |
| 3 | 90 s | 4 + the Riftcaller | +2 | 1, plus 1 per 2 extra players |

- A wave holds at most 8 monsters, not counting the Riftcaller.
- Clear a wave early and the next one comes 5 seconds later.

**The Riftcaller** is a tough vindicator Adept, about four times as healthy as a normal one, and it resists knockback. It casts **Splitting Blackflame Bolt**: three bolts of black fire that water can't put out.

If a player kills it, it drops:
- [Unstable](../runes/world.md#unstable)
- a 50% chance of a second rune, either Riftcall or Unstable
- 1 to 3 Mana Crystals

**Closing it:**
1. **Beat every wave.** Kill every monster, the Riftcaller included.
2. **Seal it.** After the second wave arrives, hit the tear with spells of **three different elements**. Any player's spells count.

If nobody closes it, the rift closes itself 90 seconds after the last wave. It also closes after 30 seconds with nobody within 48 blocks. Either way it gives nothing.

**Rewards** drop where the tear stood. They depend on how many waves you fully beat:

| Waves beaten | Runes | Blank Runes | Mana Crystal | Experience |
|---|---|---|---|---|
| 0 (sealed early) | none | 1 | no | 15 |
| 1 | 1 | 1–2 | yes | 30 |
| 2 | 1 | 1–3 | yes | 45 |
| 3 | 2 | 2–4 | yes | 60 |

Most rift runes are Tier II, some are Tier III and a few are Tier IV. Tier III ones are usually [Riftcall](../runes/world.md#riftcall) or Unstable.

**Feat:** *Riftwarden*. Everyone who fought and is within 64 blocks when the rift closes gets it. To count as fighting, you must have hurt one of its monsters, been hurt by one, or struck the tear with a spell.

## Tips and counterplay

- **Storms:** cast your most expensive spells under a storm. Sitting on the ley line stacks its bonus with the storm's.
- **Storms:** watch out for backfire in a tight spot.
- **Stars:** race the column of light. Kill the guards before you try to open the star.
- **Rifts:** beat the waves for loot and seal the tear for safety. Sealing still pays for the waves you already beat.
- **Rifts:** kill the Riftcaller yourself, since its runes need a player kill.
- **Rifts:** one spell with three elements, like `Bolt · Fire · Chill · Shock`, can seal a rift in one cast.
