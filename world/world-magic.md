---
title: Magic that Changes the World
parent: The World
nav_order: 7
---

# Magic that changes the world

Spells don't just hit creatures: where they land, the world answers. Fire lights the grass, candles and TNT and boils
water into steam, frost freezes a pond you can walk across and cools lava into a crust, storm runs through water,
scrapes copper clean and sets lightning rods humming, wind knocks arrows back, earth heaves the ground, life makes
flowers bloom and cures the sick, void draws loose things in and pins endermen down, time makes crops grow and
furnaces hurry, arcane shows what hides, and blood feeds the Nether's red growth. A fight by a pond, in a meadow, on
a lava lake or in the rain plays differently from one on bare stone.

## Which spells do it

**Harmful spells** of all ten elements change the world where they land. **Life** and **Time** are the exceptions:
their heals and blessings do it too, not just their harmful spells. So Fireward won't set the grass round your friend
alight, but Heal will make flowers spring up at their feet, and Accelerate cast on a field ripens it.

A few runes leave the world as it is:

- **World runes** that already change blocks their own way (Grow, Harvest, Icepath, Smelt, Break and the rest),
- **Bubble** (it soaks, it doesn't freeze),
- **Root, Weigh and Shackle** (they hold creatures down rather than heave them up),
- **Stasis and Rewind** (they stop time or turn it back, never on),
- and **Kindling**.

The effect does its world change wherever it lands, on a creature or on the ground, every time it lands (a Zone that
strikes every second changes the world every second, within the limits below). A spell cast on yourself with Self
changes nothing round you.

You can tell from a rune's tooltip in the Cord screen: a rune that changes the world has a dark green line starting
*"Where it lands:"*, such as *"Where it lands: freezes water into ice you can walk on, cools lava into a crust that
melts back after 25 seconds, puts out fires, campfires and candles"* on a frost rune.

## Careful: TNT and creepers

Two of these can turn on you.

- **Fire lights TNT.** A fire spell that lands within 2 blocks of TNT primes it, just as flint and steel would. The
  blast is yours, and it breaks blocks like any TNT. Don't fight with fire near your own TNT.
- **Storm can charge a creeper.** Every creeper a storm spell strikes has a **1 in 4** chance of becoming charged, as if
  lightning had hit it. A charged creeper blows up twice as big. Kill it fast, or use another element on creepers.

Both warnings are in the rune tooltips too.

## What each element does

### Fire

<img src="{{ '/assets/images/world-magic-fire.jpg' | relative_url }}" alt="A patch of tall grass burning in a meadow where a fire spell landed, poppies around it" class="shot">
<span class="caption">Fire lights what will burn.</span>

**Which runes:** Ember, Fire, Flashfire, Explode, Meteor, Inferno, Primer; the runes of the world Blazecall,
Cinderbrand, Soulfire and Sunscorch; and the fused runes of fire (Firestorm, Steam, Hellmouth, Starfire, Everburn,
Conflagration).

- **Sets fires.** Up to **3** fires within **2 blocks** of where it lands, on or beside anything that burns: grass,
  flowers, leaves, logs, wool, planks. These are ordinary fires: they spread and burn out by the game's usual rules.
  Fire is only lit where fire can spread and burn out on its own (the game only lets fire burn within a certain
  distance of players; outside it, no fire is lit, since it would never go out).
- **Lights candles, campfires and TNT.** Up to **4** within 2 blocks: unlit candles and candle cakes catch, unlit
  campfires (and soul campfires) flare up, and **TNT is primed** (see the warning above). Candles and campfires
  standing in water stay dark.
- **Melts** up to **6** snow layers, blocks of powder snow and ice within 2 blocks. Ice turns to water (in the Nether it
  simply vanishes).
- **Boils water.** Landing in water, it throws up a cloud of **steam** that hangs for **4 seconds**, about 2 blocks
  round, thick enough to hide what's behind it. Every creature you could harm inside it is **blinded** for 2 seconds,
  again each second, and left **wet**. A cast makes at most **two** steam clouds.
- **Boils puddles away.** A puddle of **4 water source blocks or fewer** boils away completely. A pond only steams.

<img src="{{ '/assets/images/world-magic-steam.jpg' | relative_url }}" alt="White steam billowing up from a small pond where a fire spell struck the water" class="shot">
<span class="caption">Fire meets water: a cloud of blinding steam.</span>

### Frost

<img src="{{ '/assets/images/frozen-pond.jpg' | relative_url }}" alt="A pond seen from above, its surface frozen into pale ice under a frost spell's circle" class="shot">
<span class="caption">Frost freezes the water into ice you can walk on.</span>

**Which runes:** Chill, Icicle, Frost, Coldsnap, Freeze; the runes of the world Tidecall, Undertow, Hoarfrost, Drowning
Word and Tidewrit; and the fused runes of frost (Hail, Glacier, Blizzard, Black Ice, Rime Seal, Frostbite, Absolute Zero).

- **Freezes water.** The surface of still water within **2.5 blocks** of where it lands turns into **frosted ice** you
  can walk on, **16 blocks** at most. It works even when the spell sank to the bottom (bolts fly on through water):
  it freezes the top, up to 4 blocks above.
- **Widen reaches further only on the frost effect itself.** A Widen that attaches to the frost effect makes the
  freeze (and the crust below) reach up to twice as far, but only frost effects with a radius (Coldsnap, Blizzard,
  Rime Seal, Tidecall, Tidewrit) take one. A Widen on the spell's shape (a wider Burst of Frost, say) leaves the
  freeze as it is.
- **The ice always melts.** It melts in the light like Frost Walker's ice, and anything still standing after about
  **30 seconds** melts back anyway, even in the dark. If the server stops, or nobody is near when the time comes, the
  ice melts as soon as its ground is loaded again, so frozen water always comes back. The ice
  [Icepath]({{ '/runes/effects/frost/' | relative_url }}#icepath) makes thaws the same way, in caves and at night too.
- **Never round a swimmer.** It won't freeze water where a creature is swimming (it would be trapped in the ice).
- **Puts out fire.** Up to **6** fires, lit campfires and lit candles nearby go out.
- **Cools lava into a crust.** The surface of lava in the same reach (2.5 blocks) cools into a crust of **basalt**
  you can walk across, **12 blocks** at most. A path over a lava lake, a bridge across a Nether river.
  - **It holds for about 25 seconds.** For its **last 5 seconds** it glows and cracks: the basalt turns to **magma**,
    cracks spread over it, and lava spits from it. Magma burns to stand on (unless you sneak). That's your warning:
    get off. Then it melts back into lava.
  - **It always melts back.** If the server stops, or nobody is near when its time comes, it melts as soon as its
    ground is loaded again (after a restart it may melt without its warning glow, so don't linger on it).
  - **It's only lent.** Breaking a block of the crust gives you nothing: the lava comes straight back. Blown up by an
    explosion, it drops nothing either, and its lava is back within a second. Pistons can't push it, and other spells
    can't change it.
  - **Never round a creature in the lava.** A strider wading in lava, or anything else standing in it, keeps the lava
    round it (it would be stuck in the rock).

### Storm

<img src="{{ '/assets/images/storm-water.jpg' | relative_url }}" alt="Lightning arcing across a pond from where a storm spell struck the water to creatures standing in it" class="shot">
<span class="caption">Storm runs through water to everything in it.</span>

**Which runes:** Shock, Jolt, Ripple, Thunderclap, Lightning; and the fused runes of storm (Tempest, Plasma, Magnetize,
Riftbolt, Stormweave, Stormclock, Thunderhead).

- **Conducts through water.** Landing in water (or striking a creature standing in it), the shock runs through all the
  water joined to that spot, up to **8 blocks** away, and strikes up to **8** creatures in it. Only creatures you could
  harm: your pets and friends in the water are safe from your own shock.
- **Damage:** **4** times the spell's power within 3 blocks of where it met the water, fading to **2.4** at the edge of
  its reach. Creatures the spell already hit directly aren't struck a second time.
- **Three conductions per cast** at most, so a Zone in a lake doesn't shock it forever.
- Branching arcs skitter across the surface from the strike to every creature shocked, and the water flashes.
- **Scrapes copper.** Up to **4** copper blocks within a block of where it lands lose **one stage** of oxidation, as
  lightning does: oxidized goes back to weathered, weathered to exposed, exposed to plain copper. Doors, trapdoors,
  bulbs, grates, rods and the rest of copper's family too. Waxed copper is left alone.
- **Sets lightning rods humming.** Up to **2** lightning rods within a block give off a redstone pulse, just as when
  lightning strikes them, so a storm spell can open a door or ring a bell wired to a rod. (A Blank Rune lying by the
  rod still needs a real storm to become Lightning.)
- **Lightning and Tempest** call down a bolt of lightning of their own, which scrapes the copper and powers the rod it
  strikes the game's own way.
- **Charges creepers.** Each creeper it strikes has a **1 in 4** chance of becoming charged. See the warning above.
- On a **wet** target, storm also sets off the **Conduct** reaction (below).

### Wind

<img src="{{ '/assets/images/world-magic-wind.jpg' | relative_url }}" alt="Arrows turned aside in mid-air by a gust, white slashes of wind curving past them" class="shot">
<span class="caption">Wind knocks arrows out of the air.</span>

**Which runes:** Push, Windcut, Dash, Launch, Levitate, Repel, Cyclone; the rune of the world Summit Wind; and the
fused runes of wind (Downdraft, Updraft, Recoil).

- **Knocks projectiles back.** Up to **8** projectiles in flight within **3.5 blocks** of where it lands are flung back
  the way the wind blows: arrows, tridents, fireballs, even another caster's bolts. Never yours, and never an ally's.
- **Blows out** up to **4** fires and lit candles nearby.
- **Scatters** up to **16** loose items and experience orbs.

### Earth

<img src="{{ '/assets/images/world-magic-earth.jpg' | relative_url }}" alt="Slabs of turf heaving up out of the ground round two creatures, with a ring of light at their feet" class="shot">
<span class="caption">Earth heaves the ground and throws foes up.</span>

**Which runes:** Pelt, Aftershock, Tremor; the runes of the world Infest, Sandstorm, Mire, Stalactite and Basalt Surge;
and the fused runes of earth (Magma, Sinkhole, Fossilize, Bonespur, Monolith).

- **Heaves the ground.** **5** slabs of the ground tilt up round where it lands, up to **2 blocks** out, and settle back
  about a second later. They're only a picture of the ground: no block is moved or broken.
- **Throws foes up.** Creatures you could harm, standing on the ground within 2 blocks, are thrown upward: a small hop,
  higher for a stronger spell (up to twice as high).

### Life

**Which runes:** Heal, Regrowth, Cleanse, Venom, Nourish, Bramble, Haven, Restore, Reversal; the runes of the world
Vinelash, Remedy, Moonpetal, Sporebloom and Rootsnare; the fused runes of life (Bloom, Soulbond, Second Wind,
Lifebloom); and the innate rune Fortune.

- **Makes things grow.** Up to **3** flowers and tufts of grass spring up within **2 blocks**, on grass, dirt and
  other ground plants grow on: poppies, dandelions, cornflowers, azure bluets, oxeye daisies and short grass.
- **A crop or sapling it lands on grows a stage**, like bone meal (that counts toward the 3).
- **Starts curing a zombie villager.** Any life spell that lands on a zombie villager with **Weakness** starts its cure,
  just as a golden apple would: it shakes, and **3 to 5 minutes** later it's a villager again (keep it out of the sun
  meanwhile), and it remembers who cured it when it trades. Weaken it first: a **Silence** spell does it, or a splash
  potion of Weakness. A zombie villager without Weakness is left as it is.

### Void

**Which runes:** Blind, Hex, Pull, Banish, Gravity Well, Blackspark, Blackflame, Hollow, Wither, Sonic Boom, Dragon
Breath; the runes of the world Echolocate, Resonant Shriek, Portalfall, Hush, Eclipse, Starmaw and Riftcall; and the
fused runes of void (Entropy, Devour, Singularity).

- **Draws things in.** Up to **16** loose items and experience orbs within **4 blocks** are drawn in toward where it
  lands, into a little knot of darkness. Handy for gathering the drops of a fight.
- **Anchors endermen.** An enderman a void spell strikes **can't teleport for 5 seconds**: it can't blink away from
  you, or out of the rain. A ring of darkness turns at its feet while it's held. Strike it again to hold it longer.
  (Arrows still can't hurt it; spells can.)

### Time

**Which runes:** Countdown, Foresight, Accelerate; the fused runes of time (Timesteal, Reckoning, Chronoshift); and the
innate rune Borrowed Time. Not Stasis or Rewind: they stop time or turn it back.

Time makes the world age where it lands.

- **Young animals grow up.** Up to **4** baby animals (and villager children) within **3 blocks** grow **2 minutes**
  older (growing up takes 20), so a few casts raise a calf. One kept young with a golden dandelion stays young.
- **Crops grow.** Up to **3** blocks within **2 blocks** of where it lands age a stage: crops ripen one stage
  (wheat, carrots, potatoes, beetroots, torchflowers, pitcher plants), saplings grow a stage (and a grown sapling into a
  tree, as bone meal would), sweet berry bushes, cocoa and melon and pumpkin stems grow as bone meal would grow them.
- **Copper weathers.** Copper in the same reach (and counting toward the same 3) weathers **one stage** greener: plain to
  exposed, exposed to weathered, weathered to oxidized. Waxed copper is left alone. (Storm scrapes it back.)
- **Furnaces hurry.** A furnace, smoker or blast furnace within a block of where it lands, burning and smelting, jumps
  **5 seconds** ahead: half an item in a furnace, a whole one in a smoker or blast furnace. It burns its fuel as far,
  and it finishes the item itself, so the result lands in its output as usual. Up to **2** furnaces.

### Arcane

**Which runes:** Harm, Reveal, Smite, Starfall, Silence, Resonance, Decree; the runes of the world Fangs, Starlight
Tether, Starshard and Manaburn; and the fused runes of arcane (Nullify, Prismatic Burst).

Arcane changes no blocks: it lets you see.

- **Shows the invisible.** Up to **8** invisible creatures within **4 blocks** of where it lands (ones you could harm)
  glow for **3 seconds**, so you can see them through walls. Invisible friends stay hidden.
- **Shelves shimmer.** Bookshelves, chiseled bookshelves and enchanting tables within **3 blocks** shimmer with
  glyphs (up to 8). Only a sight, but a pretty one in a library.

### Blood

**Which runes:** Cleave, Dismantle, Leech, Rend, Bleed; the rune of the world Blood Moss; the fused runes of blood
(Lifesteal, Bloodboil, Heartstopper, Crimson Mist, Sanguine Rite, Hemomancy); and the innate rune Blood Thread.

Blood feeds the Nether's red growth.

- **Nether wart ripens.** Up to **3** nether wart and crimson fungi within **2 blocks** of where it lands grow: nether
  wart ripens a stage, and crimson fungus on crimson nylium grows as bone meal would (now and then into a huge fungus).

## Wet

A creature is **wet** while it's in water or rain, for **5 seconds** after Tidebreath or a steam cloud, and while it's
**soaked**: by a popped Bubble, or by a soaking rune such as Mire, Undertow, Tidecall, Drowning Word or Tidewrit (each
soaks for a few seconds).

On a wet creature:

| Element | What happens |
|---|---|
| **Storm** | It **conducts**: the Conduct reaction, +50% damage, arcing to two more enemies |
| **Fire** | It hits **25% softer**, and dries the creature (unless it's standing in water) |
| **Frost** | It **freezes solid** at once, for at least 3 seconds, even from a light Chill: ready for fire to Shatter |

More on reactions: [Reactions]({{ '/spellcraft/reactions/' | relative_url }}).

## Feats

- **Conductor**: shock five creatures at once through the water they stand in. (Advancement *Conductor*.)
- **Icebridge**: walk across water you froze with a spell, before it thaws. (Advancement *Icebridge*.)

Each is worth 250 mana toward your next Heart Circle. See [The Grimoire and Feats]({{ '/progression/grimoire/' | relative_url }}).

## Rules and limits

- **Only a player's spells change blocks**, and only where that player may build: spawn protection, land claims and
  Adventure mode all stop it. Some servers turn this magic's block changes off in their settings (the steam, the
  shocks through water and the gusts still happen), or block changes by spells entirely.
- **A monster's spells never change blocks.** A Runebound's fire won't light your grass or your TNT. But the changes that
  aren't blocks still happen for monsters: their storm runs through water, their earth heaves the ground, their wind
  turns your arrows, their fire steams, their void draws items and their arcane shows you if you drank a potion of
  Invisibility.
- **Only a player's spells change creatures for good**: charging a creeper, starting a cure and growing up young
  animals. A server that turns this magic off turns these off too. (An enderman's anchor and the glow on the
  invisible last only seconds, and work either way.)
- **A cast changes at most 24 blocks this way** in all, links, pulses and echoes included, and every block it changes
  also comes out of the spell's own allowance of blocks per cast. Young animals grown count toward the 24 too.
- **A passive renewing itself changes no blocks** (an Orbit of fire won't set a trail of fires as you walk), and
  grows no young animals.
- **Every change is the game's own, and temporary or natural**: fire, frosted ice, a crust of basalt on lava that melts
  back, grass and flowers, growth, and copper weathering or being scraped. Nothing is placed that wasn't already part
  of the world, and nothing lent can be kept.

## Using it

- **Make a bridge.** A frost spell on a river freezes a path; cast again as you go. Get across before it thaws.
- **Cross the lava.** A frost spell on a lava lake crusts a path over it. When the crust starts to glow, move: in 5
  seconds it's lava again.
- **Hide in steam.** A fire spell into a pond between you and a crowd blinds everything in the cloud for as long as it
  lasts.
- **Fight in the rain.** Everything is wet: every storm spell conducts. Fire is weaker.
- **Lure foes into water.** One storm spell into a pond shocks everything wading in it. Five at once earns *Conductor*.
- **Turn arrows.** A wind spell at a skeleton's volley sends the arrows back the way they came.
- **Put out a fire** with frost or wind, before it spreads to your house.
- **Tidy up after a fight** with a void spell on the ground: the drops come to you.
- **Pin an enderman.** Strike it with void first, and it can't blink away from the rest of your spells.
- **Cure a zombie villager.** Silence it (it's weakened), then any life spell: Heal, even Nourish.
- **Clean your copper, or age it.** Storm scrapes a stage off; time adds one. Both stop at waxed copper.
- **Wire a rod.** A lightning rod on a door or a trap answers any storm spell, not just the sky's.
- **Hurry the farm and the furnace.** A time spell over a field ripens a few crops at a time, and one on a smoker
  cooks the next steak at once.
- **Light the candles** with a fire spell across the room, and snuff them with frost or wind.
- **Find the hidden.** An arcane spell into a crowd shows any invisible foe lurking in it.
- **Mind the TNT and the creepers** (see the warning at the top).
- **The dungeons use it.** The Tide Scribe's flooded pit is this page turned into a boss fight; see
  [The Drowned Scriptorium]({{ '/world/drowned-scriptorium/' | relative_url }}).
