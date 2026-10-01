# World events

Now and then the world's magic stirs by itself. Three events come on their own: **mana storms** over the ley lines,
**fallen stars** at night, and **rift sieges** beside the places people live.

## The rules for all three

- **Overworld only**, and only **near players**: every 30 seconds, each player's surroundings may start one.
- **Only in loaded ground.** Nothing happens in chunks nobody is near.
- **Peaceful stops the hostile ones.** Fallen stars and rifts never start on Peaceful. If the world turns Peaceful
  while one is going, an open rift closes (giving nothing) and a fallen star won't open. Mana storms still come.
- **Nothing is permanent.** A fallen star's crater fills back in, and any monster an event brought is taken away when
  the event ends.
- **Event monsters don't pick things up.** A star's guards and a rift's monsters never pick up items, so they can't walk
  off with a fallen player's gear. One that's taken away drops anything it was carrying.
- **A monster that changes stays the event's.** A zombie that drowns, or a skeleton that freezes in powder snow, is
  still one of the rift's (or the star's) own, and still has to be killed.
- **Cooldowns are saved.** When each kind of event may next come, and where a star is lying, is saved with the world,
  so a restart doesn't reset them. (A storm or a rift in progress simply ends with a restart.)
- Server operators can start any event on the spot, in the Overworld; see [Controls](../controls.md).
- **A server can switch world events off** in its settings. Then none of the three ever starts, not even from the
  command, though one already going runs its course.

## Mana storms

![A meadow under a mana storm: the sky and fog tinted violet, violet lightning crawling across the sky and arcing along the ground](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/mana-storm.jpg)
<span>Under a mana storm the sky turns violet and arcs crackle along the ley line.</span>

### When and where

- **Over a ley line.** A storm gathers over the strongest point of a ley line within 48 blocks of a player. No ley line
  near you, no storm. (See [Ley Lines](../progression/ley-lines.md).)
- **About once every three in-game days** for a player near a ley line.
- **A region that just had one won't get another for a day and a half.** Regions are squares 512 blocks across.
- **At most two storms** rage at once, in all the worlds together, and **never on top of each other**: a storm won't
  gather where its reach would overlap one already raging.
- It lasts **3 to 5 minutes**, and covers everything within **96 blocks** of its heart (at any height).

### What happens

A great ring of violet light rolls out along the line from the storm's heart. While you're under it:

- The **sky and fog** take a faint violet cast.
- The **ley line surges**: faint violet arcs jump between points of the line near you, motes of light lift off it,
  and now and then violet lightning crawls across the sky overhead.
- You're told when you enter (*"A mana storm rages overhead: mana flows twice as fast, spells cost a quarter less, and
  any may surge"*), when you leave (*"You leave the mana storm"*) and when it passes (*"The mana storm passes"*).

And your magic changes:

| | Under a mana storm |
|---|---|
| **Mana regeneration** | **Twice as fast** (+100%, on top of everything else) |
| **Spell cost** | **25% less** (your HUD shows the lower cost) |
| **Surges** | Every spell you cast has a **10% chance to surge** |

### Surges

When a spell surges, a violet ring flashes round you and one of these happens:

| Surge | How often (of surges) | What happens |
|---|---|---|
| **Swell** | 35% | *"Surge! Your spell swells with the storm's mana"*: the spell has **60% more power** |
| **Stray element** | 25% | A second spell rides along with yours: your spell's shape (a Bolt if yours was Self or Touch) carrying a random element's effect: Fire, Frost, Shock, Windcut, Pelt, Hex or Harm |
| **Echo** | 25% | *"Surge! Your spell echoes"*: the whole spell goes off again half a second later, **for free** |
| **Backfire** | 15% | *"The spell backfires"*: the storm's mana kicks back. You take up to 3 damage (never enough to drop you below 1 heart), get shoved backward and lose 10 mana. No damage in Creative |

A cast surges one way at most.

### The storm's runes

A mana storm keeps two runes of the world, [Manaburn](../runes/world.md#manaburn) and
[Manatide](../runes/world.md#manatide), and gives them to those who cast under it:

- **From your 10th cast under a storm**, every surge (of any kind) has a **1 in 3 chance** to crystallise the storm's
  mana into one of its runes.
- The rune goes **straight into your pack** (or drops at your feet if it's full). It's **Manaburn** 5 times in 7 and
  **Manatide** 2 times in 7.
- You're told in chat, *"The storm's mana crystallises in your hands: Manaburn"*, and a violet ring falls in on your
  hands with a flash everyone near can see.
- **One rune per storm** for each player. Your casts count for the whole storm, even if you step out and back in.

With a surge on one cast in ten, that's about one rune for every 30 casts after your 10th, so a caster who keeps
casting through a whole storm will usually come away with one.

**Fish under it, too.** A fishing line cast into open water under a storm brings up a rune tangled in the line, on top
of the catch, 12% of the time (17% where the ley line itself runs under the water). See
[Fishing](runes-of-the-world.md#fishing).

### Feat

**Stormcaller**: cast 20 spells under a single mana storm. Worth 250 mana toward your next Heart Circle, and the
advancement *Stormcaller*.

### Tips

- **Cast big.** A quarter off every spell and double regeneration make a storm the best time for your most expensive
  spells, and for the feat.
- **Keep casting.** The storm's runes and Stormcaller both come from casting under it, and cheap spells count as much as
  dear ones.
- **Mind the backfire** in a tight spot: it shoves you back and costs 10 mana.
- **Sit on the line.** A ley line already doubles your regeneration; the storm's bonus adds on top.

## Fallen stars

![A falling star streaking down across the night sky, trailing white light](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-star-falling.jpg)
<span>A star falls. Everyone within 220 blocks is told which way.</span>

### When and where

- **At night** (from dusk to dawn on the day clock), and **rarely**: about once in five nights for each player.
- **At most one star lying in a world at a time**, and after one falls, none falls in that world for half a day.
- It lands **60 to 150 blocks from a player**, on solid, dry ground where the world is running (close enough to a
  player that nothing there is paused), clear of water and lava, and somewhere that player would be allowed to build
  (spawn protection and claims are respected).
- **It never crushes what's built.** If someone builds where it's coming down while it falls, the star burns up in the
  air instead.

### What happens

1. **It falls.** A star streaks down over a couple of seconds, speeding up, with a sound you can hear from far off.
   Everyone within 220 blocks reads *"A star falls to the northeast!"* (or whichever way it is from them).
2. **It lands with a boom**, a flash and a shockwave that shakes the ground for anyone near.
   - If the mobGriefing rule is on, it blasts a **small, shallow crater** out of the natural ground: dirt, stone, sand,
     gravel, sandstone, snow and the plants on them, never anything built or any block with things inside. The crater
     is about 7 blocks across and 2 deep at most, with a scorched floor of blackstone and smooth basalt.
   - Otherwise it only leaves dark scorch marks on the ground.
3. **The Fallen Star** lies in the middle: a lump of glowing starstone. It can't be mined, and not even the Wither can
   break it. For **5 minutes** a column of starlight climbs from it, seen from up to 400 blocks away, so you can race
   to it.

![A column of starlight rising from a Fallen Star in a grassy clearing at night, two Runebound approaching it](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-star-column.jpg)
<span>The column of starlight, and the star's guards rising.</span>

### Its guards

When anyone comes within **40 blocks** of the star, **2 to 4 Runebound** rise round it: *"Runebound rise to guard the
Fallen Star"*. The first is always a vindicator **Adept**; the rest are skeletons and zombies, each with a spell from its
usual list (see [Runebound](runebound.md)).

The star **won't open until every guard is killed**:

- A guard you lead far away, or out of loaded ground, still stands. Using the star tells you how many are left:
  *"The star's guardians still stand (2)"*.
- A guard that goes without being killed (sent away by Peaceful, say) is replaced by a fresh one the next time someone
  comes near.
- After a restart, a star that was guarded wakes fresh guards when someone comes near.
- If its guards haven't woken yet when you use the star, they wake then.

### Opening it

With every guard dead, **use the star** (right-click it) to break it open: *"The Fallen Star breaks open and crumbles to
dust"*. It gives:

| Reward | |
|---|---|
| **A rune** | Three times in four a **Tier III** rune, usually [Starshard](../runes/world.md#starshard) (the star's own rune: about 3 in 4 of those) and otherwise any craftable Tier III. One time in four, a **Tier IV** rune instead |
| **A Mana Crystal** | |
| **30 experience** | |
| **Stargazer** | The feat, for whoever opens it (250 mana toward your next Heart Circle), and the advancement *Stargazer* |

The rune is decided when the star lands, so waiting doesn't change it.

Then the star crumbles, **every** remaining guard vanishes (even one led out of loaded ground, which goes as soon as
its ground loads), the scorch marks go and **the crater fills back in**, block for block (except where someone has
built since). Anyone standing in the crater is lifted clear first, so nobody is buried. A star nobody opens **fades
after 20 minutes**, the same way. One left where nobody goes still counts as gone after 20 minutes, so it never holds
up the next star, and crumbles as soon as its ground loads again. One taken away some other way (by an operator's
command, say) cleans up after itself just the same.

![A Fallen Star, a dark block glinting with white light, under its column of starlight with a Runebound zombie nearby](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/fallen-star.jpg)
<span>A Fallen Star, ready to open once its guards are down.</span>

## Rift sieges

![A tear of violet light with a spell circle turning on its face, a witch, burning zombies and a vindicator pouring out of it at night](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/world-riftcaller.jpg)
<span>A wave pours out of the rift.</span>

### When and where

- **At night, near a settled place**: a base or a village. A place counts as settled when the loaded ground around you
  (about 80 blocks across) holds at least 6 chests, barrels, furnaces (of any kind), lecterns, crafters, brewing
  stands, enchanting tables or signs.
- **Rarely**: about once in six nights for each player.
- **At most one open rift in a world**, and none for a whole day after the last.
- It opens **14 to 26 blocks from a player**, on level, dry ground with open air above.

### What happens

A **tear of violet light** opens, about 3.5 blocks tall, darkness down its middle and a spell circle turning on either
face. It lands with a flash and a shake; everyone within 64 blocks reads *"A rift tears open to the east!"* (or
whichever way). A boss bar, *Rift siege · wave 1 of 3*, shows to everyone within 48 blocks, with the wave's monsters
left as its fill.

Three **waves** of Runebound step out of the tear, onto one side or the other:

| | Comes | Monsters (one player) | Each extra player adds | Adepts | Its monsters, in order |
|---|---|---|---|---|---|
| **Wave 1** | 3 seconds after it opens | 3 | 1 | none | zombie, skeleton, zombie, husk, skeleton |
| **Wave 2** | 45 seconds after it opens | 4 | 1 | 1 | vindicator, skeleton, zombie, pillager, stray, zombie |
| **Wave 3** | 90 seconds after it opens | 4, and **the Riftcaller** | 2 | 1, and 1 more for every 2 extra players | vindicator, pillager, skeleton, witch, zombie, skeleton |

- **Players** means those within 48 blocks of the rift when the wave comes.
- A wave holds **at most 8** monsters (the Riftcaller comes on top).
- The Adepts are the first monsters of their wave (so wave 2's Adept is a vindicator).
- **Beat a wave early** (every one of its monsters killed) and the next comes **5 seconds** later instead.
- Every monster carries a spell from its usual list; see [Runebound](runebound.md).

### The Riftcaller

With the last wave, *"The last wave pours out of the rift, and something worse with it"*, and then *"The Riftcaller steps
through"*. The **Riftcaller** is a Runebound vindicator **Adept** carrying **Splitting Blackflame Bolt** (`Bolt ·
Blackflame · Split`: three bolts of black flame that water can't put out, 3 damage a second for 6 seconds). It has
about **100 health**, four times a vindicator's, and resists knockback. While it lives, the boss bar turns pink and
shows its health as *Riftcaller*.

A Riftcaller killed by a player drops its own spoils, on top of the rift's:

| Riftcaller's spoils | |
|---|---|
| **[Unstable](../runes/world.md#unstable)** | Always |
| **Another rune** | 50% chance: Riftcall or Unstable |
| **1 to 3 Mana Crystals** | |

### Closing it

There are two ways to close a rift:

1. **Beat every wave**: kill every monster of all three waves (the Riftcaller too). *"The rift collapses: every wave is
   beaten!"* A monster that wanders off, or is sent away, doesn't count: it has to be **killed**.
2. **Seal it with spells**: once the **second wave** has come, strike the tear with spells of **three different
   elements**. Any player's spells count toward the same three; each strike tells everyone near,
   *"Fire strikes the rift (1 of 3 elements)"*. A spell strikes the tear if it hits it, or lands within 3 blocks of its
   middle. Before the second wave it's too raw: *"The rift is too raw to seal yet: hold it until the second wave"*.
   Sealed, *"The rift is sealed!"*, and any monsters still out go back in.

If nobody closes it, the rift **closes itself** 90 seconds after its last wave, or after 30 seconds with nobody within
48 blocks. It gives nothing then, and takes its monsters back with it: *"The rift closes on its own, and takes its
monsters with it"*.

### Rewards

Closing a rift drops its rewards where the tear stood. How much depends on how many waves were **beaten**: every
monster of that wave killed (sealing a rift doesn't count as beating the waves still out).

| Waves beaten | Runes | Blank Runes | Mana Crystal | Experience |
|---|---|---|---|---|
| 0 (sealed early) | none | 1 | no | 15 |
| 1 | 1 | 1 to 2 | yes | 30 |
| 2 | 1 | 1 to 3 | yes | 45 |
| 3 | 2 | 2 to 4 | yes | 60 |

Each rune is a **Tier II** rune 60% of the time, **Tier III** 35%, and **Tier IV** 5%. A Tier III rune is usually one
of the rift's own runes, [Riftcall](../runes/world.md#riftcall) or
[Unstable](../runes/world.md#unstable) (about 3 in 4), and otherwise any craftable Tier III.

**Riftwarden**: everyone who fought the rift and is within 64 blocks when it closes earns this feat (250 mana toward
your next Heart Circle) and the advancement *Riftwarden*. Fighting means you hurt one of its monsters, were hurt by one,
or struck the tear with a spell. Standing by isn't enough.

The rift's boss bar goes as soon as you leave its world, or move more than 64 blocks away.

### Tips

- **Beat the waves for the loot, seal for safety.** Sealing after the second wave is quick, but only beaten waves pay:
  all three beaten gives two runes and the most Blank Runes. If a fight is going badly, sealing still pays for every
  wave already beaten.
- **Kill the Riftcaller yourself.** Its Unstable rune only drops if a player kills it.
- **Bring friends.** More players mean bigger waves, but everyone who fights gets the feat and the rewards are shared on
  the ground.
- **Carry three elements.** Any three different elements seal the tear, so a spell like `Bolt · Fire · Chill · Shock`
  (three effects on one bolt) can seal it in one cast once the second wave is out.
