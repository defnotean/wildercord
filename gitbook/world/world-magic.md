# Magic that changes the world

## What it is

Your spells change the world where they land. Fire lights grass and boils water, and frost freezes ponds and crusts lava. Storm runs through water, wind turns arrows, life makes flowers bloom and time ripens crops. A fight by a pond or in the rain plays differently from one on bare stone.

## How to get it

Every **harmful** spell does this, whatever its element. **Life** and **Time** also do it with their heals and blessings, so Heal makes flowers spring up and Accelerate ripens a field.

Some runes leave the world as it is:
- World runes that change blocks their own way, like Grow, Harvest, Icepath, Smelt and Break.
- Bubble, Tidehook, Tidecall, Tidewrit, Root, Weigh, Shackle, Kindling, Stasis and Rewind.
- Mire, Sinkhole, Fossilize, Tremor, Monolith, Thunderquake, Magma, Sandstorm, Basalt Surge and Pelt.

To check a rune, look at its tooltip in the Cord screen. A rune that changes the world has a dark green line starting *"Where it lands:"*.

A spell cast with Self changes nothing around you.

## How to use it

The change happens wherever the effect lands, on a creature or on the ground, every time it lands.

### Careful: TNT and creepers

- **Fire lights TNT.** A fire spell within 2 blocks of TNT primes it. The blast is yours and breaks blocks.
- **Storm can charge a creeper.** Each creeper a storm spell hits has a **1 in 4** chance to become charged. A charged creeper explodes twice as big.

### Fire

![Tall grass burning where a fire spell landed](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-magic-fire.jpg)

- **Sets fires:** up to **3** within **2 blocks**, on anything that burns. They spread and burn out normally.
- **Lights things:** up to **4** candles, campfires or TNT within 2 blocks. Anything standing in water stays dark.
- **Melts:** up to **6** snow, powder snow or ice blocks within 2 blocks.
- **Boils water into steam:**
  - The cloud lasts **4 seconds** and spreads about 2 blocks.
  - Enemies inside are **blinded** for 2 seconds and left wet.
  - You get at most two clouds per cast.
- **Dries up puddles:** a puddle of **4 water blocks or fewer** boils away completely.

![Steam rising from a pond hit by a fire spell](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-magic-steam.jpg)

### Frost

![A pond frozen over by a frost spell](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/frozen-pond.jpg)

- **Freezes water:** still water within **2.5 blocks** turns to walkable ice, up to **16 blocks**.
  - The ice melts in light, and always within about **30 seconds**.
  - It never freezes around a swimming creature.
- **Widen:** put Widen on the frost effect itself, not the shape, and the freeze reaches up to twice as far.
- **Puts out fire:** up to **6** fires, campfires or candles.
- **Crusts lava:** lava within 2.5 blocks becomes walkable basalt, up to **12 blocks**.
  - The crust lasts about **25 seconds**.
  - In its **last 5 seconds** it glows and turns to magma. That's your cue to get off.
  - Breaking it gives you nothing.
  - It never forms around a creature standing in the lava.

### Storm

![Lightning arcing through a pond to creatures in it](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/storm-water.jpg)

- **Conducts through water:** the shock runs up to **8 blocks** through connected water and hits up to **8** enemies standing in it.
  - It deals **4** times the spell's power close in, fading to **2.4** at the edge.
  - Your pets and friends are safe.
  - It conducts at most **3** times per cast.
- **Scrapes copper:** up to **4** copper blocks lose one stage of rust. Waxed copper is skipped.
- **Powers lightning rods:** up to **2** rods give a redstone pulse.
- **Charges creepers:** a 1 in 4 chance, as above.

### Wind

![Arrows turned aside by a gust](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-magic-wind.jpg)

- **Knocks back projectiles:** up to **8** within **3.5 blocks**. Yours and your allies' are never hit.
- **Blows out fires:** up to **4** fires and candles.
- **Scatters loot:** up to **16** loose items and experience orbs.

### Earth

![Slabs of turf heaving up around creatures](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-magic-earth.jpg)

- **Heaves the ground:** **5** slabs tilt up within **2 blocks**. No real block moves.
- **Throws enemies up:** enemies on the ground within 2 blocks are tossed upward. A stronger spell tosses them higher.

### Life

- **Grows plants:** up to **3** flowers or grass tufts within **2 blocks**. A crop or sapling it hits grows a stage.
- **Cures zombie villagers:** a life spell on a zombie villager that has **Weakness** starts the cure.
  - It turns back into a villager **3 to 5 minutes** later.
  - Weaken it first with Silence or a splash potion of Weakness.

### Void

- **Pulls loot in:** up to **16** loose items and experience orbs within **4 blocks** are drawn to where it lands.
- **Anchors endermen:** an enderman it hits **can't teleport for 5 seconds**.

### Time

- **Grows up young animals:** up to **4** baby animals within **3 blocks** age 2 minutes.
- **Ages crops and copper:** up to **3** blocks within **2 blocks** of where it lands.
  - Crops and saplings grow a stage.
  - Copper rusts a stage. Waxed copper is skipped.
- **Speeds up furnaces:** up to **2** furnaces, smokers or blast furnaces that are smelting jump **5 seconds** ahead.

### Arcane

- **Reveals the invisible:** up to **8** invisible enemies within **4 blocks** glow for **3 seconds**.
- **Makes shelves shimmer:** up to 8 bookshelves and enchanting tables within **3 blocks** shimmer. This is just for show.

### Blood

- **Grows Nether plants:** up to **3** nether wart or crimson fungi within **2 blocks** grow a stage.

### Wet creatures

A creature is **wet** if any of these apply:
- It is in water or rain.
- Tidebreath or a steam cloud hit it in the last **5 seconds**.
- A popped Bubble or a soaking rune drenched it.

| Element | On a wet creature |
|---|---|
| Storm | Triggers Conduct: +50% damage, and the shock arcs to two more enemies |
| Fire | Deals 25% less damage, and dries the creature |
| Frost | Freezes it solid at once, for at least 3 seconds |

See [Reactions](../spellcraft/reactions.md).

### Feats

- **Conductor:** shock five creatures at once through water.
- **Icebridge:** walk across water you froze before it thaws.

See [The Grimoire and Feats](../progression/grimoire.md).

### Rules and limits

- Only a player's spells change blocks, and only where that player may build. Spawn protection, claims and Adventure mode all stop it. Servers can turn block changes off.
- A monster's spells never change blocks. Their storm, wind, earth, steam and void effects still work.
- One cast changes at most **24** blocks this way. Grown-up animals count toward this limit too.
- A passive spell renewing itself changes nothing.
- Every change is natural or temporary, and nothing it lends you can be kept.

## Tips and counterplay

- **Cross water:** freeze a path across a river with frost, and keep casting as you go.
- **Cross lava:** frost crusts a path over it. Move on when the crust starts to glow.
- **Hide:** cast fire into a pond between you and a crowd. The steam blinds everyone inside it.
- **Use the rain:** in rain everything is wet, so every storm spell conducts. Fire is weaker, though.
- **Shock a crowd:** one storm spell into a pond hits everything wading in it.
- **Stop a volley:** a wind spell sends a skeleton's arrows back.
- **Pin an enderman:** hit it with void first.
- **Cure a zombie villager:** Silence it, then hit it with any life spell.
- **Copper:** storm scrapes rust off and time adds it.
- **Farming:** time ripens crops and speeds up a smoker.
- **Watch out** for your own TNT, and for creepers in a storm fight.
- **See it as a boss fight:** the Tide Scribe's flooded pit is built on these rules. See [The Drowned Scriptorium](drowned-scriptorium.md).
