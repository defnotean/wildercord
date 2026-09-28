---
title: Magic that Changes the World
parent: The World
nav_order: 7
---

# Magic that changes the world

Spells don't just hit creatures: where they land, the ground answers. Fire lights the grass and boils water into
steam, frost freezes a pond you can walk across, storm runs through water to everything in it, wind knocks arrows
back, earth heaves the ground, life makes flowers bloom and void draws loose things in. A fight by a pond, in a meadow
or in the rain plays differently from one on bare stone.

## Which spells do it

**Harmful spells** of seven elements change the world where they land. **Life** is the exception: its heals and
blessings bloom too, not just its harmful spells. So Fireward won't set the grass round your friend alight, but Heal
will make flowers spring up at their feet.

Arcane, Time and Blood spells leave the world as it is. So do a few runes of the other elements:

- **World runes** that already change blocks their own way (Grow, Harvest, Icepath, Smelt, Break and the rest),
- **Bubble** (it soaks, it doesn't freeze),
- **Root, Weigh and Shackle** (they hold creatures down rather than heave them up),
- and **Kindling**.

The effect does its world change wherever it lands, on a creature or on the ground, every time it lands (a Zone that
strikes every second changes the world every second, within the limits below). A spell cast on yourself with Self
changes nothing round you.

You can tell from a rune's tooltip in the Cord screen: a rune that changes the world has a dark green line starting
*"Where it lands:"*, such as *"Where it lands: freezes water into ice you can walk on, puts out fires and campfires"*
on a frost rune.

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
  freeze reach up to twice as far, but only frost effects with a radius (Coldsnap, Blizzard, Rime Seal, Tidecall,
  Tidewrit) take one. A Widen on the spell's shape (a wider Burst of Frost, say) leaves the freeze as it is.
- **The ice always melts.** It melts in the light like Frost Walker's ice, and anything still standing after about
  **30 seconds** melts back anyway, even in the dark. If the server stops, or nobody is near when the time comes, the
  ice melts as soon as its ground is loaded again, so frozen water always comes back.
- **Never round a swimmer.** It won't freeze water where a creature is swimming (it would be trapped in the ice).
- **Puts out fire.** Up to **6** fires and lit campfires nearby go out.
- **Lava is left alone.**

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
- On a **wet** target, storm also sets off the **Conduct** reaction (below).

### Wind

<img src="{{ '/assets/images/world-magic-wind.jpg' | relative_url }}" alt="Arrows turned aside in mid-air by a gust, white slashes of wind curving past them" class="shot">
<span class="caption">Wind knocks arrows out of the air.</span>

**Which runes:** Push, Windcut, Dash, Launch, Levitate, Repel, Cyclone; the rune of the world Summit Wind; and the
fused runes of wind (Downdraft, Updraft, Recoil).

- **Knocks projectiles back.** Up to **8** projectiles in flight within **3.5 blocks** of where it lands are flung back
  the way the wind blows: arrows, tridents, fireballs, even another caster's bolts. Never yours, and never an ally's.
- **Blows out** up to **4** fires nearby.
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

### Void

**Which runes:** Blind, Hex, Pull, Banish, Gravity Well, Blackspark, Blackflame, Hollow, Wither, Sonic Boom, Dragon
Breath; the runes of the world Echolocate, Resonant Shriek, Portalfall, Hush, Eclipse, Starmaw and Riftcall; and the
fused runes of void (Entropy, Devour, Singularity).

- **Draws things in.** Up to **16** loose items and experience orbs within **4 blocks** are drawn in toward where it
  lands, into a little knot of darkness. Handy for gathering the drops of a fight.

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
  Adventure mode all stop it. Some servers turn off block changes by spells entirely.
- **A monster's spells never change blocks.** A Runebound's fire won't light your grass. But the changes that aren't
  blocks still happen for monsters: their storm runs through water, their earth heaves the ground, their wind turns your
  arrows, their fire steams and their void draws items.
- **A cast changes at most 24 blocks this way** in all, links, pulses and echoes included, and every block it changes
  also comes out of the spell's own allowance of blocks per cast.
- **A passive renewing itself changes no blocks** (an Orbit of fire won't set a trail of fires as you walk).
- **Every change is the game's own, and temporary or natural**: fire, frosted ice, grass and flowers. Nothing is
  placed that wasn't already part of the world.

## Using it

- **Make a bridge.** A frost spell on a river freezes a path; cast again as you go. Get across before it thaws.
- **Hide in steam.** A fire spell into a pond between you and a crowd blinds everything in the cloud for as long as it
  lasts.
- **Fight in the rain.** Everything is wet: every storm spell conducts. Fire is weaker.
- **Lure foes into water.** One storm spell into a pond shocks everything wading in it. Five at once earns *Conductor*.
- **Turn arrows.** A wind spell at a skeleton's volley sends the arrows back the way they came.
- **Put out a fire** with frost or wind, before it spreads to your house.
- **Tidy up after a fight** with a void spell on the ground: the drops come to you.
- **The dungeons use it.** The Tide Scribe's flooded pit is this page turned into a boss fight; see
  [The Drowned Scriptorium]({{ '/world/drowned-scriptorium/' | relative_url }}).
