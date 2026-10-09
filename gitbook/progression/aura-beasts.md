# The Highland Beasts

## What it is

Two highland animals that shrug off spells but take full damage from blades and Aura: the **Stonehorn**, a rock-shouldered
grazer, and the **Galeclaw**, a feathered ridge runner. Feed them to collect materials for field gear.

| Damage | Stonehorn (normal / recovering) | Galeclaw (normal / recovering) |
|---|---|---|
| Spells | 12% / 40% gets through | 20% / 65% gets through |
| Blades and Aura | Full damage | Full damage |

Slowing and holding spells still work on them. Save damage spells for when they're recovering.

## Stonehorn

![Stonehorn's curved horns, rock shoulders and cloven feet in game](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/stonehorn-grazer.png)

**Where:** dry, open ground in meadows, windswept hills, windswept gravelly hills and stony peaks, at least twelve blocks above
sea level.

**Danger:** walk within four blocks without sneaking or holding wheat and it gets territorial. It **stamps for two seconds**,
then charges straight ahead. **Step sideways.** After a charge it recovers for a while, longer if it hit a wall.

**Feed it:** offer wheat to a calm Stonehorn for a **Stonehorn Plate**. It can shed again after two minutes.

**Bastion Poultice** (plate, clay ball, wheat): Resistance I and Slowness I for ten seconds. Single use, thirty-second
cooldown.

## Galeclaw

![Galeclaw's hooked beak, banded feathers and talons in game](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/galeclaw-ridge-runner.png)

**Where:** windswept hills and forests, jagged and frozen peaks, snowy slopes and groves. It hunts Rimehares and scavenges
dropped raw rabbit or chicken. Casting spells within eighteen blocks draws its attention.

**Danger:** it **crouches for two seconds** and marks its landing spot in stone dust. **Get out of the marked area** before it
leaps. The landing doesn't follow you. After landing it has an exposed recovery.

**Feed it:** offer raw rabbit or chicken before you provoke it for a **Galeclaw Plume** (two-minute wait between plumes).
A fed Galeclaw leaves prey and scraps alone for **one minute**. Feeding is not taming: it still defends itself.

**Ridge Whistle** (plume, copper ingot, string): distracts stalking Galeclaws you can see within sixteen blocks for ten
seconds. Reusable, ten-second cooldown. It can't stop a leap that's already coming or an animal you've hurt.

## Windreed

![Three Windreed growth stages on a controlled field platform](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/highland-windreed-stages.png)

Small patches grow in **meadows and windswept hills, forests and gravelly hills**. Use a mature plant to gather **two
tassels**; the roots regrow. Plant a tassel on dirt or grass. It doesn't spread by itself.

Stonehorns and adult Rimehares graze mature plants back a stage (not with `mob_griefing` off). Life magic on a patch grows it
one stage.

| Item | Made from | What it does |
|---|---|---|
| **Draft Kite** | Windreed, Galeclaw Plume, leather, string, stick | **8 seconds of Slow Falling**, using one tassel from your inventory. Reusable, **20-second** cooldown. It slows a fall; it doesn't lift you |
| **Windreed Braid** | Windreed, string, sweet berries | Single use: **30 seconds of Downwind**. Hares spook less, Stonehorns stay calm, and Galeclaws don't notice your casting. You move **10% slower**. Sprinting gives you away |

![The Draft Kite held above a controlled descent platform, with its native finite lift effect active](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/highland-draft-kite.png)

## Tips and counterplay

- Stonehorns shelter at night, Galeclaws at midday, and both during storms. Hurting one wakes it up.
- Let a mage slow or hold them while a swordsman closes in.
- Downwind doesn't stop an attack that's already started or an animal defending itself. Milk removes it.
- Only two of each kind live within 96 blocks. They don't breed.
- They appear in the Grimoire's bestiary once you've observed them.

## Server settings

`aura_world.aura_beasts` turns natural spawning and aggression on or off. Existing animals and materials stay. Peaceful
stops their attacks.

Related: [Aura](aura.md) · [The Sleeping Blade](sleeping-blade.md)
