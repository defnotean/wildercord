# Runes of the world

Fifty-three new runes that can't be crafted at all: each is found only in its own places. Go and
explore, and your spellbook grows. A rune of the world says where it comes from in its tooltip, and
the Grimoire (the third page of the Cord screen) lists every one by where it's found, with a hint
for the ones you haven't learned yet.

They come from five kinds of place:

- **Vanilla structures:** their chests have a chance of one of the structure's own runes, on top of
  their usual loot. Ocean monuments have no chests, so their Elder Guardians carry theirs. The
  server's rune loot multiplier (`loot.rune_chance_multiplier`) scales these chances like every
  other rune's.
- **Biomes, by Attunement:** hold a Blank Rune and meditate in the right land at the right moment.
- **Wildercord's dungeons and their bosses:** the Ember Sanctum, the Astral Observatory and the
  Drowned Scriptorium, and the Cinder Warden, the Star Eater and the Tide Scribe.
- **World events:** Fallen Star craters, Rift sieges (and their Riftcaller) and mana storms (a
  surge, once you've cast 10 spells under a storm, now and then crystallises one of its runes: see
  [World events](world-events.md)).
- **Fishing:** Tidehook and Current come up only on a fishing line in open water (see
  [Fishing](#fishing) below).

A few of them also turn up in Archive libraries, and a slain Runebound Adept sometimes drops one
(8%, times the rune loot multiplier).

## Fishing

Fishing in open water brings up runes, in two ways. Both need **open water** (vanilla's rule for
treasure: a clear stretch at least 5 blocks across with nothing in or just above the water round the
bobber), and both draw from the same list: the two runes found only by fishing, **Tidehook** and
**Current**, and 21 crafted runes that fit the water: Tidebreath, Chill, Icicle, Icepath, Shock,
Feather Fall, Night Eye, Swift, Heal, Collect and Leap (Tier I), Bubble, Frost, Thunderclap, Jolt,
Pull, Grapple, Levitate and Wave (Tier II), and Freeze and Lightning (Tier III, rare). Within the
list, tiers weigh 8, 5 and 2 as in a chest.

- **Treasure catches.** Vanilla's treasure pool (six treasures of weight 1) gains a rune (weight 4)
  and a Torn Page (weight 1): about **4 treasure catches in 11 are a rune** and **1 in 11 a Torn
  Page**. Treasure is 5% of open-water catches, about 11% with Luck of the Sea III, so a rune comes
  up about once in 55 catches with a plain rod and once in 24 with Luck of the Sea III. Tidehook and
  Current weigh three times a sea rune of their tier here: each is about 1 in 11 of the runes.
- **Magic waters.** Where magic runs strong at the bobber, a rune comes up **as well as** the catch:
  12% a catch under a mana storm, 5% on or near a ley line (a little wider than where you count as
  standing on one), 5% in a thunderstorm with its rain (or snow) falling on the bobber. They add up, to at
  most 20%. Tidehook and Current weigh six times a sea rune of their tier here (each about 1 in 6).
  The angler reads *"Something magical was tangled in your line!"*, and the bobber flashes with a
  glint and a ring of pale light.

The server's rune and page loot multipliers (`loot.rune_chance_multiplier`,
`loot.page_chance_multiplier`) scale these chances like every other (0 turns them off). The first
rune a player fishes up earns the feat **Reeled In** (250 mana toward the next circle, and its
advancement).

In code: `WildercordLoot` adds the rune (an inline table, tier-weighted) and the page into
`gameplay/fishing/treasure`'s own pool, and a conditional pool to `gameplay/fishing` for magic
waters, gated by the loot condition `wildercord:magic_waters` (`MagicWatersCondition`, reading the
bobber and where it floats); `wildercord:fished_rune` (`FishedRuneFunction`) on both grants the feat
and shows the cue. The odds are pure data in `content/FishingRules`; `cast/Fishing` reads the storm,
the ley line and the weather at the bobber.

## Attunement

Hold a **Blank Rune** in either hand and meditate (sneak and stand still with a Cord on) in a land
that holds a rune. If the moment is right, a circle opens under you in the rune's colour and motes
of the land rise into the blank. Keep still for **20 seconds**: the blank brightens through four
stages, each with a chime, and then becomes the land's rune. Moving, standing up or letting go of
the blank breaks it off. Each land's rune is written into your Grimoire the first time; until then
the Grimoire shows its riddle. A land that holds a rune, but not right now, tells you so.

A land gives each player its rune **once an in-game day** (20 minutes of play): meditate there again
sooner and it tells you how long it still rests, and the Grimoire's line for that land shows when
it's ready again.

| Land | Moment | Rune |
|---|---|---|
| Cherry Grove | At night, under a full moon, under open sky | Moonpetal |
| Ice Spikes | While snow is falling on you | Hoarfrost |
| Deep Dark | Within 4 blocks of sculk | Hush |
| Mushroom Fields | Any time | Sporebloom |
| Badlands (any kind) | Around noon, under open sky | Sunscorch |
| Swamp | While rain is falling on you | Mire |
| Lush Caves | Any time | Glowvine |
| Mangrove Swamp | Any time | Rootsnare |
| Dripstone Caves | Any time | Stalactite |
| Jagged Peaks or Frozen Peaks | Above height 200 | Summit Wind |
| Soul Sand Valley | Any time | Soulfire |
| Warped Forest | Any time | Warp Step |
| Crimson Forest | Any time | Blood Moss |
| Basalt Deltas | Any time | Basalt Surge |
| The End's outer islands | Any time | Starlight Tether |

## Every new rune

### Shapes

| Rune | Tier | Does | Found |
|---|---|---|---|
| Snare | II | Strings an unseen tripwire from your feet to where you look (up to 12 blocks). The first enemy to cross it within 30 seconds springs it on everything within 2.5 blocks. | Jungle temples |
| Vortex | III | A whirling vortex opens where you look for 3 seconds, dragging creatures within 5 blocks into its eye and striking everything in the eye twice a second. | Ominous vaults |
| Constellation | III | Joins up to 5 enemies within 12 blocks of you (that you can see: never through a wall) in a constellation of light and strikes them all at once. | The Astral Observatory |

### Effects

| Rune | Element | Tier | Does | Found |
|---|---|---|---|---|
| Treasure Sense | Arcane | I | For 60 seconds, Luck II, and every chest, barrel and suspicious block within 24 blocks sparkles now and then. | Buried treasure, Archive libraries |
| Ancient Seed | Life | I | Plants an ancient seed at the point: a torchflower or pitcher plant blooms, and crops within 4 blocks grow a stage. | Trail ruins (brushing) |
| Glowvine | Life | I | Glowing cave vines heavy with glow berries grow down from the ceiling around the point: a light that stays. | Attuned in lush caves |
| Echolocate | Void | II | A sonar pulse from the point: every enemy within 16 blocks glows through walls for 10 seconds, and those it hits are dazed for 3. Only what you could harm glows: never your allies or bystanders, and an invisible player only when they're your enemy in PvP. | Ancient cities, Archive libraries |
| Infest | Earth | II | Silverfish burrow out of the stone around each target: 1 damage every half second for 4 seconds, and Slowness I. | Stronghold libraries, Archive libraries |
| Vinelash | Life | II | A thorned vine lashes each target: 5 damage, and it's yanked 4 blocks toward you. | Jungle temples, Runebound Adepts |
| Remedy | Life | II | Washes away harmful effects, heals 4 and gives Regeneration I for 6 seconds. A zombie villager it touches is weakened, ready for a golden apple. | Igloo basements |
| Warcry | Blood | II | A war horn: you and your allies within 8 blocks of the target gain Strength I and Speed I for 12 seconds. | Pillager outposts, Runebound Adepts |
| Fangs | Arcane | II | A ring of evoker fangs snaps up around each target: 6 damage from below (and a bite for any other enemy standing on the fangs). | Woodland mansions, Archive libraries |
| Undertow | Frost | II | Drags each target down: Slowness III for 3 seconds and soaked. In water it's pulled under and takes 5 damage. | Shipwrecks, Runebound Adepts |
| Tusk Charge | Earth | II | You charge like a hoglin, up to 8 blocks the way you look, tossing everything in your path into the air for 5 damage. | Bastions |
| Blazecall | Fire | II | Three blaze fireballs fall on each target over a second: 2 fire damage each, setting it alight. | Nether fortresses, Runebound Adepts |
| Portalfall | Void | II | A portal opens under each target and drops it from 7 blocks up, with 2 damage on the way through. | Ruined portals, Runebound Adepts |
| Moonpetal | Life | II | A storm of moonlit petals at the point: 4 damage to every enemy within 3 blocks, and 3 health to you and your allies there. | Attuned in a cherry grove |
| Hush | Void | II | A pocket of silence at the point for 6 seconds: enemies inside lose their targets, are weakened and can barely see. | Attuned in the deep dark |
| Sporebloom | Life | II | Spores burst at the point: Poison I and Nausea for 6 seconds to enemies within 3 blocks, and 4 hunger restored to your allies there. | Attuned in mushroom fields |
| Mire | Earth | II | The ground turns to mire under each target for 5 seconds: Slowness IV, no jumping, and soaked. | Attuned in a swamp |
| Rootsnare | Life | II | Mangrove roots burst up around the point: every enemy within 3 blocks is held for 2 seconds and takes 3 damage. | Attuned in a mangrove swamp |
| Stalactite | Earth | II | A stalactite drops on each target from above: 7 damage, 50% more against a bare head. | Attuned in dripstone caves |
| Warp Step | Void | II | Steps you to where the spell landed (up to 24 blocks). 3 seconds later you're pulled back, unless you're sneaking. | Attuned in a warped forest |
| Blood Moss | Blood | II | Crimson moss spreads over each target: 1 damage a second for 6 seconds, and you heal for all of it. | Attuned in a crimson forest |
| Cinderbrand | Fire | II | Brands each target: 3 fire damage, and for 6 seconds your fire spells burn it 50% hotter. | The Ember Sanctum |
| Tidehook | Frost | II | A hook of water snags each target and reels it in to your feet in three tugs (each pulls harder the further out it is, and takes over its drift, so its own steps and the hit's knockback don't undo it): 4 damage, and it's left soaked. A boss is struck and soaked, never moved. It soaks rather than freezes, so it never ices over the water it lands in. | Fishing in open water |
| Current | Frost | II | Only in water or rain (as a trident's riptide): a current holds you at speed the way you look for 6 ticks, then the water or the air carries you on, about 15 blocks in all (Amplify further, by the square root of its power). Fall damage is off until you land. On dry land it fizzles with a hiss and a line above the hotbar, and the mana is spent. | Fishing in open water |
| Manaburn | Arcane | II | 5 damage. A spellcaster (a player wearing a Cord, or a Runebound) also takes 4 more, and a player loses up to 20 mana: 20 times the hit's power (so each of a Barrage's hits takes 7), scaled like PvP damage, and never more than 20 from one player in one cast. | Mana storms (a surge after 10 casts) |
| Resonant Shriek | Void | III | A sculk shriek: 8 damage that ignores armour, and Darkness for 6 seconds. A second later it echoes for half as much. | Ancient cities |
| Tidecall | Frost | III | The tide crashes in at the point: 6 damage to every enemy within 3.5 blocks, dragging them into the middle and leaving them soaked. | Ocean monuments (Elder Guardians) |
| Sandstorm | Earth | III | A sandstorm whirls at the point for 4 seconds: every enemy within 3.5 blocks takes 2 damage a second, can't see and is slowed. | Desert pyramids |
| Shulkershell | Void | III | Shuts the target in a shulker's shell for 4 seconds: 80% less damage and no knockback, but it can't move. When it opens, enemies within 3 blocks float up. | End cities |
| Hoarfrost | Frost | III | Rime creeps over each target for 3 seconds, slowing it more every second; then it freezes solid for 2 seconds and takes 6 damage. | Attuned among ice spikes |
| Sunscorch | Fire | III | 8 fire damage and alight for 5 seconds. Under open sky by day it burns 50% hotter. | Attuned in the badlands |
| Summit Wind | Wind | III | 5 damage and hurls every enemy within 3 blocks up and away. On Self it carries you 12 blocks up and lets you glide down. | Attuned on a mountain peak |
| Soulfire | Fire | III | Blue soul flames: 3 fire damage a second for 5 seconds, and the damage they deal gives you back a little mana: 0.35 per point of damage that really landed (nothing for a burn a Shield blocked or a fire-proof target ignored), at most 5 a cast. | Attuned in a soul sand valley |
| Basalt Surge | Earth | III | Basalt columns burst up in a line from you to the point: 7 damage and a toss into the air for everything along it. | Attuned in the basalt deltas |
| Starlight Tether | Arcane | III | Tethers each target to the point with starlight for 6 seconds: it's dragged back if it strays 3 blocks, taking 2 damage each time. | Attuned on the End's outer islands |
| Ashen Veil | Fire | III | Wreathes the target in ash for 10 seconds: fire can't hurt it, and whatever strikes it up close is set alight. | The Ember Sanctum |
| Eclipse | Void | III | A dark disc eclipses the point for 5 seconds: enemies beneath it are blinded, take 2 damage a second, and your spells hit them 20% harder. | The Astral Observatory |
| Drowning Word | Frost | III | For 5 seconds the target's lungs fill with water (no air, 2 damage a second), and it's soaked. | The Drowned Scriptorium |
| Starshard | Arcane | III | 9 damage, then it splinters into 3 sparks that strike the nearest other enemies within 8 blocks (in sight of the target) for 3. | Fallen Star craters |
| Riftcall | Void | III | Opens a rift at the point for 3 seconds: it drags enemies within 5 blocks toward it for 2 damage a second, then snaps shut on them for 6. | Rift sieges |
| Manatide | Arcane | III | You and your allies hit regain 3 mana a second for 10 seconds (30 at most, however extended). Each player can drink only once a minute, one drink flowing at a time. | Mana storms (a surge after 10 casts) |
| Cinderheart | Fire | IV | Your heart burns for 12 seconds: Strength II, fire can't hurt you, and every enemy within 4 blocks (and in sight) takes 3 fire damage a second. | The Cinder Warden |
| Starmaw | Void | IV | 14 damage, and it swallows each of the target's good effects for 3 more damage apiece. | The Star Eater |
| Tidewrit | Frost | IV | A 7-wide wall of water rolls from you through the point: 10 damage to everything in it, sweeping it 8 blocks on, soaked. | The Tide Scribe |

### Modifiers

| Rune | Tier | Does | Found |
|---|---|---|---|
| Trial Key | II | +60% power against targets at full health: the key to opening a fight. | Trial vaults, ominous vaults |
| Kindled | III | +20% power, and the effect sets what it hits alight for 4 seconds. | The Ember Sanctum |
| Unstable | III | The effect's power swings anywhere from 50% to 200% each time it lands. | Rift sieges, the Riftcaller |

### Links

| Rune | Tier | Does | Found |
|---|---|---|---|
| If Wounded | II | The rest fires only if you're below half health: a last stand in any spell. | Stronghold libraries, Archive libraries |
| If Wet | II | The rest fires only if you're in water or rain. | The Drowned Scriptorium |
| If Outnumbered | III | The rest fires only if 3 or more enemies are within 8 blocks of you: monsters, players you may harm, whatever you're fighting or whatever is hunting you (not cows or villagers). | Woodland mansions |

## Lingering effects

Soulfire, Blood Moss, Infest and Drowning Word linger on their target. Hitting it again with the
same rune refreshes the effect (it starts over) instead of stacking another copy, so a Barrage or a
Zone doesn't pile up a burn per hit. Mire and Shulkershell on a creature that already has one take
over from it, and it lasts until the newest ends. A lingering effect pulses once per second of its
length (Soulfire 5 times in 5 seconds, Cinderheart 12 in 12, Sandstorm 4 in 4), and its later ticks
can be blocked by a Shield but not parried.

## Reactions they set up

Several of them leave the marks the element reactions look for:

- **Soaked** (Tidecall, Undertow, Mire, Drowning Word, Tidewrit, Tidehook): storm on them sets off **Conduct**.
  Try `Beam · Mire · Delay · Beam · Shock`.
- **Frozen** (Hoarfrost's final freeze): fire on them sets off **Shatter**.
- **Thrown by wind** (Summit Wind, Tusk Charge, Basalt Surge, Shulkershell's lift): fire sets off
  **Wildfire**.
- **Pulled** (Vortex, Riftcall): an Explode or Meteor landing on them sets off **Implode**, and Repel
  sets off **Collapse**.
- **Shadowed** (Echolocate's daze, Resonant Shriek, Hush, Eclipse): life damage on them sets off
  **Blight**. Earth damage from Stalactite, Infest, Sandstorm, Basalt Surge or Tusk Charge on a frozen
  target sets off **Fracture**, life damage from Vinelash, Moonpetal or Rootsnare sets off Blight, arcane
  damage from Fangs, Starshard, Starlight Tether or Manaburn on a target with two marks sets off
  **Unweave**, and Summit Wind on a bleeding target sets off **Rupture**.
- **Cinderbrand** makes your own fire spells burn the target 50% hotter, and **Eclipse** makes all of
  your spells hit the targets beneath it 20% harder.

# New runes, batch 2: more ways to build a spell

Eighteen more runes, and these ones you **craft** (a Blank Rune and a couple of items, as every Tier I-III
rune is): three shapes, three modifiers, two links and an effect for every element. Each does something its
family or element couldn't before. Drowse, Prolong and On Reaction also turn up now and then in ancient city,
end city and trial chamber (rare) chests, and every one of them can come from the places that give out crafted
runes at random (the dimension dungeons' chests take the effects of their elements).

## Shapes

| Rune | Tier | Cost | Does | Recipe |
|---|---|---|---|---|
| Imprint | I | 3, effects ×1.3 | Leaves an imprint where you stand; 2 seconds later (1 with Quicken) it erupts on everything within 3 blocks. Charging shows the ring under your feet | Clay Ball, Gunpowder |
| Glaive | II | 5, effects ×1.8 | A spinning glaive flies out up to 12 blocks (turning back at a wall) and curves back to you, striking each creature on the way out and again on the way back. Charging shows its reach | Iron Axe, String |
| Latch | II | 6, effects ×1.9 | A thread latches onto the first creature within 16 blocks of your aim and strikes it 4 times, a second apart, at 70% power, while it stays within 24 blocks and in sight. After a link it tethers the creature that set it off to the spot it was struck. Charging marks the creature it would take | Lead, Amethyst Shard |

## Modifiers

| Rune | Tier | Cost | Does | Recipe |
|---|---|---|---|---|
| Kindred | II | ×1.4 | A helpful effect also lands on you and on the nearest ally within 8 blocks that it missed, at half power. It works on every helpful effect that lands on each creature it touches, but not on summons, Haven, Warcry, Zephyr, the death saves, Soulbond, Transfusion, Cryostasis, Shulkershell, Rewind, Overdrive or innate runes | Cake |
| Thirst | II | ×1.4 | You heal for a quarter of the damage the effect really deals (half with two, three quarters at most) | Spider Eye, Glass Bottle |
| Belated | II | ×1.25 | The effect lands 1.5 seconds late, 40% stronger, on whatever it struck that's still there (three count at most). Lingering landings wait too, and a Belated kill comes too late for On Kill | Clock, Cobweb |

## Links

| Rune | Tier | Cost | Does | Recipe |
|---|---|---|---|---|
| On Weakness | II | 2 | Watches the group before it, as On Hit does, and fires the rest at each creature it struck with an element it's weak to (see [Affinities](affinities.md)). Players have no weaknesses | Fermented Spider Eye, Target |
| On Reaction | III | 2 | Watches the group before it and fires the rest at each creature it set an element reaction off on as it landed (Shatter, Conduct, Overload, Blight...) | Brewing Stand |

What follows either is paid for once, so an Echo or a Pulse after one goes off for the first creature only,
as after On Hit.

## Effects

| Rune | Element | Tier | Cost | Does | Recipe |
|---|---|---|---|---|---|
| Prospect | Earth | I | 3 | The ground rings out: every ore within 12 blocks of where it lands (16 at most, widened) glows through the rock for 20 seconds, in the colour of what it gives. Nothing in the world changes | Stone Pickaxe, Amethyst Shard |
| Galvanize | Storm | I | 3 | A spark of raw power (a redstone block) sits in the air against the block it strikes for 5 seconds: doors open, lamps light, pistons push. Only in empty air nobody stands in, where you may build; it drops nothing, pistons can't move it, and it always goes | Lightning Rod, Redstone Dust |
| Umbra | Void | I | 6 | 4 damage, doubled where the light at the target's eyes is level 7 or less (night, caves), and it leaves the target shadowed for Blight | Ink Sac, Flint |
| Spellbrand | Arcane | II | 8 | Brands each target for 8 seconds; the next time your magic hurts it, the brand bursts for 6 arcane damage (which can Unweave) | Book, Gunpowder |
| Gash | Blood | II | 9 | 3 damage, and for 8 seconds the target can't heal at all (Regeneration, potions, food, healing spells) and is bleeding, for Rupture | Flint, Rotten Flesh |
| Searing Edge | Fire | II | 8 | For 15 seconds each melee hit the target lands sets the foe alight for 4 seconds and deals 2 more fire damage. It can be a passive | Iron Sword, Blaze Powder |
| Flash Freeze | Frost | II | 9 | 4 freeze damage; a wet or soaked target (in rain, in water, after a Bubble) freezes solid for 3 seconds (1.5 on players) and is left frozen for Shatter; a dry one is slowed | Packed Ice, Water Bucket |
| Disarm | Wind | II | 7 | A snatching gust takes the weapon (whatever's in its main hand) from each creature for 5 seconds, then gives it back; windswept. Players and bosses keep hold, and nothing is ever dropped | Wind Charge, Fishing Rod |
| Drowse | Life | III | 14 | Lulls each target to sleep for 6 seconds (2 on players): it can't move or fight back, but any damage wakes it. Bosses are only slowed | Spore Blossom, Honey Bottle |
| Prolong | Time | III | 12 | Every good effect on the target lasts 15 seconds longer, up to 5 minutes | Clock, 2 Redstone Dust |

Tier II recipes also take 2 Lapis Lazuli and a Gold Ingot; Tier III a Mana Crystal and a Diamond.

## How they fit in

- **Reactions.** Umbra leaves foes shadowed and Gash leaves them bleeding; Flash Freeze turns soaked into
  frozen; Disarm leaves them windswept; Searing Edge's blows set them burning; Spellbrand's burst is arcane
  damage. On Reaction lets a spell do something only when one of these pays off.
- **Affinities.** On Weakness fires on a weakness struck: life on the undead, frost on a blaze, wind on a
  spider, storm on an iron golem, earth on a breeze.
- **Monsters, scrolls and imbued items** run all of them as the rest: a monster never places a Galvanize
  spark (monsters never change blocks), and Kindred shares a monster's buffs with its own kind.
- **Passives:** Searing Edge can be sustained on Self, and an Orbit can carry Umbra.
- **In code:** `cast/CraftedRunes` (the effects, Kindred's share and Belated's clock), `cast/CraftedShapes`
  (the three shapes, and the two links' watch over the group they follow), `cast/CraftedVfx`, and
  `mixin/LivingEntityHealMixin` (Gash). The numbers are in `SpellNumbers` ("new runes (batch 2)").
