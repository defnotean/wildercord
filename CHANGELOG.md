# Changelog

All notable changes to Wildercord. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed
- **Lightning no longer multiplies on a crowd.** Every strike hurt everything near it, so enemies packed together took a
  strike for each neighbour (three took 108 between them, five 300). Now each enemy takes the strongest strike of a cast
  once: its own 12, or half of that from a strike aimed at a neighbour. Its text also says "slows", which is what it does.
- **Stoneform answers blows, not fire.** Its aftershock fired on any damage, so burning, poison or a fall set it off
  every half second; now only a blow from an attacker does, at most once a second.
- **Ripple heals what it really took**, not what it computed: a target that was protected or invulnerable gave you the
  health anyway.
- **Earth's ground-heave no longer lifts what should sink.** Mire, Sinkhole and Fossilize hopped their targets up as they
  cast, and Tremor, Monolith, Thunderquake, Magma, Sandstorm, Basalt Surge and Pelt each drew a second crack under their
  own; they keep only their own show now.
- **Excavate no longer shakes the screen** (it played Tremor's), and shows nothing when it mines nothing. **Thunderclap's
  shake** is gentler and only comes when it hits something.
- **Shock's second victim** takes part in Conduct and Overload like the first.

### Added
- **Every spell reads from how it's built.** Each shape leaves your hands with its own gesture and sound (a flick, a
  throw, a line of light, a slash, a blast, a seal, a call, an aura), and you hold each kind its own way while
  charging. The circle under your feet grows with the spell's cost, charge and tier, and the biggest spells leave
  with a shove. See [How a spell sounds and looks](https://defnotean.github.io/wildercord/spellcraft/casting/#how-a-spell-sounds-and-looks).
- **Spells have melodies.** While you charge, each rune plays its own note as its roundel opens (a knock for a shape,
  glass for an effect, a bell for a modifier, a clink for a link), and the hum is in your first element's key.
- **Modifiers show in gold, links in violet.** Amplify, Overcharge, Focus, Widen, Extend, Quicken, Split, Pierce, Vow,
  Blood Price and the rest each have their own cue as the spell leaves; bolts show Amplify, Overcharge, Frugal, Pierce and
  Homing in flight; Volley shots and Chain jumps climb the scale. Delay ticks down, Pulse keeps time, Echo ripples, On Hit
  rings at each creature it hands on to, On Kill tolls, conditions click open or shut, and reactive links show a seal at
  your feet while armed.
- **Reaction marks are visible.** Frozen, Windswept, Pulled, Wet, Soaked, Resonant, Cracked, Shadowed, Bleeding and Ionised
  creatures wear a small halo in the mark's colour, and a tick sounds when a mark is set.
- Fields keep time (a Zone beats, a Totem's bell climbs with each strike, a Domain tolls, each Orbit orb chimes its note),
  impacts leave a moment's trace of their element, a failed cast fizzles, and the HUD chimes and flashes when a long
  cooldown ends.
- **Casting gear slots.** Your inventory has three new slots for casting gear: **Staff** (any staff, a greater one
  too), **Focus** (any focus) and **Tome** (the Tome of the Fifth Page). A piece in its slot works with nothing in
  your hands, so you no longer have to hold your staff or a focus to cast. In the survival inventory the slots sit in
  a small tray on top of the panel, above your armour; in the creative inventory's Survival Inventory tab they are
  beside the Cord slot. Shift-click sends a piece to its slot and back, and a slot only takes what it is for, one
  piece each. Hover an empty slot to see what it takes.
- **Gear shows on your character**, to you in third person, to everyone around you and in the inventory's paper doll:
  a staff strapped across your back with its head over your shoulder, a focus hovering off your other shoulder
  (bobbing and turning, with a faint glimmer circling it), and the tome hanging from a belt at your hip. Each looks
  like its item, keeps clear of armour, capes and elytra, follows a sneak, and isn't drawn when you're invisible or
  already holding that piece.
- The **Cord screen's mana badge** lists the gear that counts, wherever it is; a gear tooltip now says which slot it
  goes in.
- For add-on makers: `api.gearSlots()`, `equippedGear`, `equipGear`, `unequipGear` and `gearSlotFor` (API 1.1).

### Changed
- **Casting sounds are cleaner.** The element's cast sound used to play twice on a dozen shapes (Bolt, Arc, Cone, Crescent, Spark,
  Comet, Ricochet, Stream, Blitz, Glaive, Imprint, Latch); now once. A sound can play at most three times in a tick, so a blast on a
  crowd no longer roars, and a tap cast has its own little snap of release.
- **Touch is a touch,** not a mini Beam: a thin thread, a glow at your palm and no beam sound.
- **Fields and streams are priced by their strikes.** Fields and streams were far too cheap for the hits they land, and Quicken, Linger and Extend multiplied
  them further (a Zone with Linger and two Quickens dealt 500 damage to one creature for 53 mana). Now: **Quicken** makes the same strikes come
  twice as fast, so a Zone, Wall, Totem, Domain, Latch, Stream or Barrage is over sooner instead of hitting more; **Linger's** later landings
  strike at 60%; and Zone (x2.75), Wall (x2.95), Totem (x3.15), Orbit (x3.05), Trail (x2.85), Vortex (x2.8), Domain (x3.9), Barrage (x1.85),
  Latch (x2.15) and Stream (x1.95) cost more in proportion to how often they strike.
- **Touch** lays the spell on 30% harder; **Cone** hits 35% harder in its near half and 15% softer at its edge; each of
  **Orbit's** orbs strikes a creature once a second on its own, so a Split Orbit's extra orbs count.
- **On Hit** fires its payload a little weaker at each further creature of one landing (85% each), still paid once.
- **Vow's** cooldown is 5x (was 4x); **Blood Price** costs 1 health per 4 mana (was 5); **Belated** is 25% stronger per rune (was 40%).
- **Weak runes lifted:** Amplify x1.5 cost (was x1.6), Overcharge x2.6 (was x3.0), Kindled +30% power (was +20%), **Rain**'s strikes
  seek enemies in the area and strike 2 blocks wide, **Orb** is steered by your aim and drifts 30 blocks (x2.0), **Cluster** costs x1.5.
- **Near clones set apart:** **Ring** is hollow (it spares the ground around you) and costs x1.7; **Arc** x1.25; **Ray** x1.15; **Sweep** x1.8;
  **Beam** costs 3; **Wall** is a real barrier (it turns projectiles aside and slows what touches it); **Snare** trips what it catches (Slowness II
  for a second).
- **Conditions give back what they didn't use:** Combo on its first two casts, and If Sneaking, If Airborne, If Wounded, If Outnumbered and If
  Wet when they don't hold, return the mana their branch cost.
- Casting poses now follow a spell's first *shape* (a spell that began with an effect always thrust before).

- **Passives hold two runes.** A passive used to hold up to five, so two of them could keep ten runes running at once,
  which was a lot of free power. Each passive now holds **two runes**, whatever your Cord, and you still have the two
  slots (1st and 5th Heart Circle): at most four runes are ever active. An effect before any shape is on Self already,
  so `Stoneskin · Empower` needs no Self rune, and a modifier takes one of the two sockets (`Swift · Amplify`). A
  passive or loadout saved with more keeps its first two runes (nothing else is lost); threading a third is refused
  ("Passives hold 2 runes"). See [Passive Spells](https://defnotean.github.io/wildercord/spellcraft/passives/).
- **A piece in its slot takes the place of held pieces of its kind:** a staff in the Staff slot and staffs in your
  hands, only the slotted one counts; the same for a focus and for the tome. A slot that is empty lets held gear work
  exactly as before (staffs in either hand, foci and the tome in the off-hand), so nothing stops working. The slots
  are separate, so the tome and a focus can now work together.
- Gear in a slot **drops when you die**, like the rest of your inventory (Curse of Vanishing destroys its own), and
  stays with you when `keepInventory` is on. It never duplicates through death, respawning or changing dimension.
- The tooltips and Cord screen texts about holding the tome or a Focus of the Deep Well now mention the slot.
- **Frost and wind runes each do their own thing.** Chill deepens if you Chill again (Slowness II, III, IV). Frost
  leaves a target brittle for Shatter. Icicle hits harder on a slowed target and melts after 2 seconds. Hail is five
  staggering stones. Hoarfrost creeps for 3 seconds, freezes solid, and blooms onto neighbours. Bubble lifts its
  target and holds by size (small 2.5 s, large 1.5 s). Absolute Zero counts a target's signs of cold: the more, the
  harder it hits and the longer it holds. Glacier's ice spreads to a few neighbours and cracks for damage. Blizzard
  walks the way you faced. Frostbloom's ward freezes attackers and heals you. Black Ice shatters what dies while
  it holds. Cryostasis bursts when it opens. Coldsnap is a wider snap that leaves everything brittle. Freeze costs
  14 mana, Bubble 9 and Absolute Zero 18.
- **Wind now lifts.** Launch, Levitate, Updraft and Summit Wind mark targets *airborne*: every spell hits them 20%
  harder while they are off the ground. Light creatures fly farther in wind and heavy ones barely budge. Windcut
  breaks what a target is winding up (a charge, a bow's draw, a creeper's fuse, a spell). Push slams into walls for
  damage. Repel deals 4 damage. Downdraft pins whoever stands under it when nothing is in the air. Recoil deals 5.
  Cyclone and Gale Mantle follow where you steer. Feather Fall drifts the way you look. Zephyr lasts 10 seconds and
  clears blindness, nausea and slowness. Mirrorfrost casts back at 70% power and can't be cast back again.
- **Frost and wind look and sound like themselves.** Every frost, water and wind rune has its own sound now (thirty-four
  new ones: a needle for Icicle, a creeping crystal for Hoarfrost, a deep lock for Freeze, a wet splat for Flash
  Freeze, a rising wave for Tidecall, a snatch for Disarm, a rewinding whoosh for Recoil and so on) in place of the
  handful of shared glass, powder-snow and wind pops. Runes read by silhouette: Push is one broad slab, Windcut one long
  cut, Launch a pillar of rings, Repel a dome, Icicle a needle falling from above, Chill three rings climbing the body,
  Tidewrit a wall, Undertow a corkscrew of bubbles, Flash Freeze a crown of ice spikes. Each element family has its own
  colour (rime, steel and deep cyan for frost, aqua for water, mint, sky and grey-green for wind), and a rune's size on
  screen follows its power. Timed effects now tell you when they end: ice shows hairline cracks in its last half second,
  a Bubble strains before it pops, Hoarfrost flashes white before it closes, and Frostward and Cushion show a faint ring
  while they last. Silenced creatures show a muted ring over their head, and a creature lifted by wind shows pale rings
  and lights while a spell hits it harder. Fewer particles for the same rune (Push, Prune, Repel, Tidewrit, Undertow).
- **New status: silenced.** A silenced creature can't cast; Drowning Word now silences its target. A Runebound
  telegraphing a spell has it broken by silence or a Windcut.
- Tidecall, Undertow, Tidehook, Tidewrit, Current, Disarm and Tidebreath got small mechanic tweaks (Tidecall pulls and
  soaks, Undertow drags toward the nearest water, Tidehook leaves a gasp, Current carries allies, Disarm's player
  version stops them using what they hold for 3 seconds).

### Fixed
- Shatter and Fracture now end a freeze hold instead of leaving the target frozen.
- Repel was drawn in red; it now uses the wind palette. Glacier's shell no longer lingers after the ice cracks.
- Tidewrit no longer repeats its sound for every target it sweeps.
- A disarmed creature that died lost its weapon; it now drops it.
- Frostward could be beaten by a long frost hold; the hold is now capped.
- Water runes were audible in quiet places where they shouldn't be; Tidecall and Tidewrit are now quiet like the rest.
- Casting Hoarfrost or Bubble repeatedly stacked their damage; each is now once per target at a time.
- Several frost and wind rune descriptions no longer matched what the runes do.

## [0.5.0-alpha] - 2026-09-29

Your own affinities, runes to fish up, thirty-four new runes (eighteen to build with and sixteen signature fusions),
and boss arenas that can only be entered through their dungeons.

### Added
- **Signature fusions: sixteen new fused runes.** At the Fusion Altar, sixteen pairs of *particular* effects now fuse
  into a rune of their own instead of their elements' fusion: Chill with Shock makes Frostwire, while any other Frost
  and Storm effects still make Hail. Same shard, same 3 XP levels, the lower of the two ranks. The altar's panel says
  **Signature fusion**, and it flares with a star as it makes one. See
  [Signature fusions](https://defnotean.github.io/wildercord/fusion-altar/combining/#signature-fusions).
  - **Frostwire** (Chill + Shock, storm): chills each target, then a current races through every chilled or frozen
    enemy within 6 blocks, 4 damage each (6 to one frozen solid).
  - **Seethe** (Bubble + Fire, fire): a bubble of boiling water scalds for 2 seconds, then bursts into steam: 4 damage
    around it, blinded and soaked.
  - **Bloomstep** (Grow + Blink, life, moves you): step up to 32 blocks through a door of blossoms; grass and flowers
    grow at both ends, and Regeneration where you arrive.
  - **Skyburst** (Launch + Explode, fire): flings up to 3 targets high; each explodes at the top of its flight, raining
    fire down for 7 (and fanning Wildfire).
  - **Stitchtime** (Heal + Countdown, life): heals 4, then heals back every wound of the next 4 seconds (up to 12).
  - **Parasite** (Venom + Leech, blood): poisons and drains 1 a second into you for 6 seconds, and leaps to the next
    enemy when its host dies.
  - **Razorgale** (Windcut + Bleed, wind): blades cut everything within 3 blocks and leave it bleeding, then come back
    round and tear the wounds open (Rupture).
  - **Doomclock** (Primer + Stasis, time, Tier IV): a 3-second clock every blow winds 2 tighter; it bursts for 8 and
    all it was wound (up to 20) around its bearer.
  - **Thunderstep** (Shadowstep + Lightning, storm, moves you): come down as lightning behind the first enemy hit, 8
    damage and a stun around you.
  - **Halo** (Smite + Regrowth, arcane): a halo over an ally smites the nearest enemy every 2 seconds for 3 (tripled on
    undead) and heals the ally 1 each time.
  - **Thunderquake** (Thunderclap + Tremor, earth): three shockwaves to 2, 4 and 6 blocks, 4 each: 12 at the heart.
  - **Cometfall** (Starfall + Meteor, arcane, Tier IV): a comet falls a second later for 16 within 4 blocks, setting
    them alight, and five shards strike the enemies around for 4.
  - **Riposte** (Reflect + Foresight, time): the ally's next 2 blows within 10 seconds are sidestepped and answered
    for 6.
  - **Dust Devil** (Summit Wind + Sandstorm, wind, Tier IV): a wandering dust devil catches enemies up, blinds and
    scours them for 3 a second, and flings them high when it blows out.
  - **Malison** (Hex + Resonance, void): 3 damage and a curse (your spells hit 25% harder); when its bearer dies the
    curse passes to up to 3 enemies near it.
  - **Avalanche** (Coldsnap + Stalactite, frost): 6 damage in 3 blocks (9 on a bare head), Slowness III, and drifts of
    snow that melt 10 seconds later.
  - Each wears its two runes' elements in a spell's circle, with a star to tell it from the element fusion, and has a
    hand-drawn icon in both elements' colours.
  - The Grimoire lists them apart, *Signature fusions (n of 16)*, hidden as ??? + ??? with both runes' elements as the
    hint until found. Each condenses as much mana as any fusion; the element fusions are still counted out of 55.
  - Two new advancements: **Signature** (make your first) and **Hallmarks** (find five).
- **Your affinities.** Every player now has an affinity with each of the ten elements, and it grows with what you
  do: casting the element (a point for every 10 mana its effects cost, so cheap spam is worth no more than a real
  spell), setting off reactions it takes part in, finding creatures weak to it, and everyday things that fit it.
  Smelting, killing with fire and the Nether's lava feed fire; fishing, snow and powder snow frost; thunderstorms and
  lightning (a strike you live through is worth a lot) storm; elytra flight, long falls and great heights wind; stone
  and ores mined with a pickaxe, and the deep, earth; ripe harvests, breeding, taming and healing others life; the
  End, endermen, ender pearls and the deep dark void; enchanting, meditating, Torn Pages, new runes and ley lines
  arcane; a whole night watched under the sky, time magic aging the world and a clock in hand time; melee kills, Blood
  Price and heavy blows survived blood. Every way has a daily allowance (an in-game day, which sleeping doesn't
  hurry), so no farm grows an affinity past it; casting slows to a tenth past its own instead of stopping.
- **Five levels** at 100, 600, 1,800, 4,500 and 10,000 points: each gives **+3% power** with that element's effects
  (heals and shields too: up to +15% at V), from **III** others' spells of it land **10%, 15%, 20%** softer (a
  reaction still breaks through, and no callout floats over you), and at **V** its share of a spell's price costs
  **10% less**, shown in the Cord screen and the HUD. It counts on your own spells (from your Cord, imbued, turned
  back by Mirrorfrost), not on scrolls or passives.
- Every new level is announced with a message and a **toast** with the element's mark in its colour; the first level
  of each element is a Grimoire entry worth **100 mana** toward your next Heart Circle.
- The **Grimoire page** has an **Affinities** section under your heart: each element's mark, its level and a bar
  toward the next, with a tooltip of what it gives and everything that raises it. The readout says a spell's affinity
  bonus quietly under its cost ("Fire affinity III: +9% power").
- Server settings: `features.player_affinity` switches affinities off, and `affinity.gain_multiplier` makes them grow
  faster or slower. An older settings file gains both by itself. See
  [Your Affinities](https://defnotean.github.io/wildercord/progression/affinity/).
- **Eighteen new runes to build spells with**, all crafted (a Blank Rune and a couple of items). See
  [Shapes](https://defnotean.github.io/wildercord/runes/shapes/), [Modifiers](https://defnotean.github.io/wildercord/runes/modifiers/),
  [Links](https://defnotean.github.io/wildercord/runes/links/) and each element's [Effects](https://defnotean.github.io/wildercord/runes/effects/) page.
  - **Three shapes.** **Glaive** (Tier II): a spinning glaive flies out 12 blocks and curves back to you,
    striking everything on the way out and again on the way back. **Imprint** (Tier I): an imprint where you
    stand erupts 2 seconds later on everything within 3 blocks: cast it and run. **Latch** (Tier II): a thread
    holds on to the first creature in your aim and strikes it 4 times a second apart at 70% power, until it
    strays past 24 blocks or out of sight. Charging shows where each one goes.
  - **Three modifiers.** **Kindred**: a helpful effect also lands on you and on the nearest ally it missed, at
    half power. **Thirst**: you heal for a quarter of the damage the effect deals. **Belated**: the effect lands
    1.5 seconds late, 40% stronger.
  - **Two links.** **On Reaction** fires the rest of the spell at each creature the shape before it set an
    element reaction off on; **On Weakness** at each one it struck with an element it's weak to.
  - **An effect for every element.** **Spellbrand** (arcane): a brand your next spell damage bursts for 6.
    **Gash** (blood): 3 damage, and no healing of any kind for 8 seconds, bleeding. **Prospect** (earth): every
    ore within 12 blocks glows through the rock for 20 seconds. **Searing Edge** (fire): for 15 seconds, melee
    hits set foes alight for 2 more fire damage. **Flash Freeze** (frost): 4 damage, and a wet or soaked target
    freezes solid. **Drowse** (life): sleep for 6 seconds that any damage breaks. **Galvanize** (storm): a spark
    of redstone power against a block for 5 seconds (doors, lamps, pistons), gone without a trace.
    **Prolong** (time): every good effect lasts 15 seconds longer, up to 5 minutes. **Umbra** (void): 4 damage,
    doubled in the dark, and shadowed. **Disarm** (wind): a creature's weapon is snatched for 5 seconds, then
    given back.
  - Drowse, Prolong and On Reaction also turn up in ancient city, end city and trial chamber chests.
- **Runes from fishing.** A rod in open water (vanilla's rule for treasure) now brings up magic. See
  [Fishing](https://defnotean.github.io/wildercord/world/runes-of-the-world/#fishing).
  - **Treasure catches can be runes.** About 4 treasure catches in 11 are a rune and 1 in 11 a Torn Page, beside the
    name tags, saddles and enchanted books (Luck of the Sea makes treasure likelier: about 1 catch in 55 is a
    rune with a plain rod, 1 in 24 with Luck of the Sea III). The runes are 21 crafted runes of water, frost and storm
    and a few a fisher is glad of (Tidebreath, Chill, Icicle, Icepath, Shock, Feather Fall, Night Eye, Swift, Heal,
    Collect, Leap, Bubble, Frost, Thunderclap, Jolt, Pull, Grapple, Levitate and Wave, and now and then Freeze or
    Lightning), lower tiers more often, and the two runes below.
  - **Magic waters.** Where magic runs strong at the bobber, a rune comes up tangled in the line **on top of** the
    catch: 12% a catch under a mana storm, 5% on or near a ley line, 5% in a thunderstorm with its rain (or snow) on the bobber,
    adding up to 20% at most. *"Something magical was tangled in your line!"*, and a glint on the water.
  - **Two new runes of the world, found only by fishing:**
    - **Tidehook** (Tier II frost, 9 mana): a hook of water snags each target and reels it in to your feet in three
      tugs, 4 damage, and leaves it soaked (storm then sets off Conduct). Amplify and Linger work on it.
    - **Current** (Tier II frost, moves you, 6 mana): only in water or rain, a current sweeps you about 15 blocks the
      way you look, and you land without fall damage. On dry land it fizzles. Amplify carries you further.
  - A new feat and advancement, **Reeled In**: fish a rune out of open water (250 mana toward your next circle).
  - The server's rune and page loot multipliers scale all of it, as they do chests (0 turns it off).
- Craftable runes' wiki entries now say where else they're found, and Lightning's and Shock's tooltips name trail
  ruins (brushing), where they've been found all along.

### Changed
- **Leaning follows your deepest affinity.** Your magic leans toward the element whose affinity is at level I or more
  and a quarter ahead of the rest, and your circles take its colour as before. Leaning's own +10% power is gone: the
  affinity's levels give power now, for every element you grow. The casts leaning counted before give each element a
  start (2 points a cast, never past level II).

### Fixed
- **Sweep** missed creatures at long range, and more with Quicken or Widen; it now traces the whole arc.
- The **aim preview** took its colour from the second rune (a modifier's gold or a shape's teal); it now uses the first effect's element.
- **Wall's** fence blinked off for a third of the time.
- **Dungeon arenas and vaults are warded.** Players could tunnel, blast or spell their way straight into a boss arena
  or vault without going through the dungeon. Their walls, floors and domes now can't be broken by survival players,
  explosions, block-breaking spells or pistons ("These walls are warded: the only way in is through the dungeon").
  The halls on the way can still be dug into, and a block you put down inside an arena can be broken again. Dungeons
  already in a world are warded too.

## [0.4.4-alpha] - 2026-09-29

### Fixed
- **A server reached through its own domain shows in the server list.** Minecraft greets a server with the address as
  typed when it asks for its status, but with the address its DNS (SRV) record points at when joining it. Tunnels that
  sort connections by that greeting (playit.gg's, for one) only know the second, so such a server said "Can't connect
  to server" in the list while joining it worked. The list now greets a redirected server the way joining does;
  addresses no SRV record redirects go as typed, as before.

## [0.4.3-alpha] - 2026-09-29

Loadouts, a second layer of world-changing magic (all ten elements now touch the world), and another bug hunt: the
new travel, reaction and affinity code, every rune, server performance, and the issues earlier audits left open.

### Added
- **Loadouts.** Save your whole Cord (every spell's runes and name, your passives and which are on, and the
  selected spell) under a name, and swap between up to six setups: one for fighting, one for mining, one for
  exploring. Open them from the new list badge at the end of the Cord screen's tabs row (or `Ctrl`+`L`): each
  loadout shows its name and first runes, with **Load**, **Save current here**, **Rename** and **Delete** (saving
  over and deleting ask twice), plus **Save current as new**. The arrow keys, `Enter`, `Ctrl`+`R` and `Delete` work too.
  Loading never teaches a rune: runes you don't know or your Cord can't hold, and sockets and spells it doesn't
  have, stay threaded but quiet, as on a smaller Cord. Every spell that changes starts its cooldown unless it's
  cooling already, so swapping mid-fight is no cooldown reset, and loading is refused while you charge a spell,
  duel or are sealed in a Cryostasis. See [Loadouts](https://defnotean.github.io/wildercord/spellcraft/loadouts/).
- A **Next loadout** key (unbound at first, under Wildercord in the controls) loads your next loadout and names
  it above the hotbar.
- `/loadout save <name>`, `/loadout load <name>`, `/loadout delete <name>` and `/loadout list` for every player.
- **More of the world answers your magic.** A second layer of world-changing magic, and all ten elements now take part
  (see [Magic that Changes the World](https://defnotean.github.io/wildercord/world/world-magic/)):
  - **Frost cools lava into a crust** of basalt you can walk across (up to 12 blocks). It holds about 25 seconds; for
    its last 5 it turns to glowing, cracking magma, then melts back into lava. It always melts back, even after a
    crash or with nobody near, breaking it gives nothing, and it never forms round a creature in the lava.
  - **Storm scrapes copper** a stage of oxidation clean (up to 4 blocks) and **pulses lightning rods** like a real
    strike. Each creeper it strikes has a **1 in 4 chance of being charged**: careful.
  - **Fire lights candles, candle cakes and campfires, and primes TNT** (up to 4 within 2 blocks): stand clear.
    Frost and wind now snuff candles too.
  - **Time ages the world**: crops and saplings grow a stage and copper weathers one (up to 3 blocks), baby animals
    nearby grow up 2 minutes, and a furnace, smoker or blast furnace jumps 5 seconds ahead in its smelting. Its
    helpful spells do it too, as life's do (not Stasis or Rewind).
  - **Void anchors endermen**: one it strikes can't teleport for 5 seconds, a ring of darkness at its feet.
  - **Life starts curing a zombie villager** that has Weakness, as a golden apple would.
  - **Arcane** shows invisible creatures nearby (they glow for 3 seconds) and makes bookshelves and enchanting
    tables shimmer. **Blood** ripens nether wart and grows crimson fungus.
  - Every block change keeps the old rules: only a player's spell, only where they may build, within the cast's
    block budget, and not on a server that turned world-changing magic or spells' block changes off. Charging a
    creeper, curing and growing babies are a player's spell's only, and off with world-changing magic off too.
  - The rune tooltips' dark green *"Where it lands"* lines say all of this, with the warnings.

### Changed
- **Absolute Zero freezes the same creature solid at most once every few seconds.** Its first hit leaves a target
  slowed, so with Linger, a Zone or a second copy every later hit was a certain freeze for 7 damage. Frozen solid, a
  creature can't be again until 3 seconds after it thaws (it's still slowed meanwhile).
- **Magma's pools and Tempest's strikes don't stack on one enemy.** Each lays one under (or on) every target, so an
  enemy bunched with others took every overlapping one: up to four burns a second from Magma, eight strikes from
  Tempest. Now each enemy takes your strongest one once (Magma once a second, Tempest once per strike).
- **A Blizzard or Rime Seal cast again on its own spot keeps it going** (up to three times its length) instead of laying
  a second one on top: a Zone, an Echo or a Split used to stack several storms biting at once, or seals each freezing
  the same enemy, as Hellmouth, Sinkhole and Crimson Mist already didn't.
- Bloom's description says it grows plants around the first 3 allies it touches (it said every ally), and Devour's
  that it feeds you twice a cast at most, as they always did.
- **Manatide gives 30 mana a drink at most.** Extend made it flow longer without limit (four Extends: about 480 mana for
  a 46-mana spell), and a stream that outlasted the minute between drinks ran alongside the next. It now flows 10
  seconds however it's extended, and only your newest drink flows.
- **Cheating death rests.** Once Reversal, Second Wind or a certain secret spell has saved you from a killing blow,
  none of them saves you again for a minute, and the secret spell can't be taken up again for 3 minutes. Each could be
  recast long before it ran out, so keeping one up meant never dying.
- Glacial Lance's Grimoire entry says it flies 32 blocks, as far as it has always reached (it said 40).
- Borrowed Time says its debt is forgiven when you slay a monster, as it always was (it said "something": killing an
  animal never forgave it).
- Vein's description now says what Amplify does to it: like Break, Tunnel and Smelt, an Amplified Vein mines at
  diamond-pickaxe strength.

### Fixed
- **Magma burns what it lands on from its first second.** Magma is earth too, and its own heave threw the creatures
  it landed on into a little hop, so its first burn (which only reached what stood on the ground) missed them.
- **Warp Step, Time Skip and Zipper set you down safely**, as Blink now does: never into lava or fire, and Warp Step and
  Time Skip never over a drop or the void (Time Skip takes the farthest safe spot on its way, and says so when there's none).
- A Runebound monster standing where the world has stopped running (at the edge of what's loaded) no longer winds up
  and casts at you from there.
- **The server config file shows every setting.** A fresh `wildercord.json` now lists the `travel` section (it was read
  but never written), and a file written by an older version gains any settings added since, at their defaults, the
  next time the server loads it. Everything already in the file stays as it was.
- **Bosses can't be lifted or dragged any more.** Levitate floated the Warden, the Wither, the Archivist and the
  dungeon bosses up out of their fights (Extended, high enough for the fall to hurt), Gravity Well held them in
  its pull straight through their knockback resistance, and Shulkershell's opening lifted them too. Bosses are
  only ever slowed: Levitate now gives them Slowness II instead, and Gravity Well still marks them pulled and
  crushes them, but no longer moves them.
- **A Shield now stops Swap and Shadowstep.** A Shield that blocked the spell still let Swap trade places with the
  enemy behind it, and Shadowstep put you at its back, as if the spell had gone through.
- **Icepath no longer freezes a swimmer into the ice.** It froze the water a creature (or a player, or you) was
  swimming in, leaving it stuck in a block of ice to choke. Like a frost spell's freezing, it now leaves the water
  round a swimmer alone.
- **Harvest replants with a seed from the crop it cut.** It dropped everything the crop gave and then replanted it
  for free, a seed out of nothing every time. Now one of the crop's own seeds goes back into the ground (a crop
  that dropped none isn't replanted), and a crop you may not touch (in a claim) no longer stops it harvesting the
  rest of the field.
- **Grow keeps to the spell's block budget.** Every other rune that changes blocks counts each one against the
  blocks a cast may change (32, or the server's `max_blocks_per_cast`), but Grow bone-mealed every block it
  reached, however many times a Zone, a Split or an Echo landed it.
- **Blink never lands you in lava.** Aimed at a lava pool (or a creature dying in one), it put you on the ground at
  the bottom of it, aimed out over the void or a deep drop it left you in mid-air to fall, and it could also put
  you past the world border. It still only lands where there's room to stand, and now only on ground, never in
  lava or fire or outside the border: with nowhere safe, you stay where you are.
- **Banish never drops a creature into the void.** Its target reappears near its own height, but with no ground
  within reach below (the edge of an End island, a deep ravine) it could be left in mid-air to fall. A creature
  standing on the ground now always reappears on ground, or not at all.
- **A Bubble popping, or a Stasis ending, no longer thaws a longer Freeze.** Either one let the creature go the
  moment it ended, even if a Freeze (or another hold) cast on it had seconds still to run.
- Echolocate now lights up every enemy in range (up to 32) in a big crowd too: enemies just outside its range were
  using up the count, leaving some in range dark.
- **Blackflame spreads only from a death.** A burning creature that simply went away (its ground unloaded, it
  despawned, or it went through a portal) passed its black flames on as if it had died. Blackflame, Dismantle's
  later slashes, Aftershock's second impact and a Bubble's pop also no longer follow a player through a portal.
- **`/rtp` typed over and over could stall a server.** Each `/rtp` searched for a new spot, loading (or generating) up
  to a dozen far-off chunks, and a broken warmup starts no cooldown, so it could be repeated as fast as it was typed.
  Now a player searches at most once every 10 seconds: trying again sooner goes to the spot already found (or, if the
  last search found none, says how long to wait). Operators aren't limited.
- **An accepted teleport request could land you in a duel.** If the player you were going to started a duel during
  your warmup, you still arrived beside them. Now the teleport is called off, as it is when they're duelling at the
  moment you accept.
- **Waypoint offers could flood someone's chat.** `/waypoint share` had no limit and can't be switched off like
  requests. Now you can share with the same player once every 10 seconds.
- A teleport still warming up when an operator switches travel off (and reloads the settings) no longer goes ahead.
- **Fracture, Blight, Unweave, Rupture and Elapse didn't break through a resistance.** A reaction is meant to ignore
  what a creature resists, and Shatter did, but the five set off by any damage of their element still let the
  resistance halve the hit that set them off (a Fracture on an iron golem, an Unweave on the Archivist). Now they land
  in full like the rest.
- **One Lightning cast could set off Overload again and again.** Each strike set its targets alight at once, so on a
  crowd every later strike found them burning and blew them apart again, up to seven times over in one cast. Lightning
  now sets them alight after its last strike.
- **Overload in a crowd could launch creatures sky-high.** Every burning enemy that blew apart threw all its neighbours,
  so in a packed crowd each was thrown once for every neighbour, and the throws added up (players too, in PvP). Now an
  Overload throws each creature at most once at a time; the damage is unchanged.
- **Stoneform's aftershock took on the element of the blow it answered.** Hit by a wind spell, its aftershock counted as
  wind (and could set off Rupture on a bleeding creature beside you, writing it in your Grimoire). It's always earth now.
- **A spell could set the Cinder Warden alight for a moment**, long enough for a storm effect in the same spell to set
  off Overload and crack its armour. Nothing sets it alight now, as the Ember Sanctum page says.
- **A settings file that couldn't be written lost its settings.** When the server couldn't add the newer settings to an
  older `wildercord.json` (a read-only file, say), it ran on the defaults instead of the owner's settings. The file's
  settings now hold either way; only the new ones run at their defaults.
- Adding the newer settings to an older `wildercord.json` no longer strips out comments an owner wrote in it: a file
  with comments is left as it is (its settings are read as before, and any it lacks run at their defaults).
- **Duels gave back more health than the opponent took.** A blow was counted at its damage before armour, Resistance and
  absorption hearts, so an armoured duellist got back far more than they lost, healing damage from falls and monsters
  too (and a knockout gave back the whole killing blow). Now what each blow really took from your health is given back.
- **Duels gave back used-up effects.** Every effect a duellist had at the start came back if it was gone at the end, so a
  Bad Omen a raid had used, or Absorption a monster had knocked away, returned after every duel. Only helpful effects
  come back now, and never Absorption.
- **A creature immune to an element counted toward contracts.** Frost cast at a snow golem, which frost can't hurt,
  counted as a frost cast landing on a real creature, making it a target for the Runesmith's contracts that never ran
  out. A spell has to hurt what it lands on to count now.
- Elapse no longer counts fire that couldn't hurt its target: a burning creature with Fire Resistance had at least 3
  damage land on it, and its flames put out, for a burn that was dealing nothing.
- **Less work for the server each tick, and for your game each frame.** A spell's particles are packed once for
  everyone who sees them instead of once per player (and not at all when nobody is near), lasting spells (Orbits,
  Mines, Totems, Domains...) no longer make the server look through every spell part still waiting each tick, and an
  Orbit looks for creatures once a tick instead of once per orb. On your side, the spell panel no longer reads your
  whole spell again every frame just to write its name, and ley lines, beams, motes, glows and magic circles are drawn
  without making new objects for every little piece. Nothing looks, sounds or plays any differently.
- **A passive can't stack its Orbits.** A passive that started with an effect and then held an Orbit (`Swift · Orbit ·
  Shock`) renewed every two seconds like a Self passive, so up to four rings of orbs (eight with Extend) struck at
  once for one upkeep. The effect before the Orbit counts as Self, so that passive now has two shapes and is refused,
  as the Passives page says; keep the buff and the aura in two passives.
- **Switching an Orbit passive off and on no longer doubles its orbs.** The old ring came back beside the fresh one it
  started, so every switch within its 8 seconds added another ring for one second's upkeep. Only the latest ring flies.
- **Long-lasting shapes no longer pile up waiting steps.** An Orbit, Zone, Trail, Wall, Totem, Domain, Vortex, Mine or
  Snare booked every one of its steps up front: an Orbit that Extend made last hours held hundreds of thousands of
  them, kept even after its caster had left. Each now keeps one step waiting at a time and stops once its caster is
  gone.
- **Quicken makes a Wall strike twice as often.** A Wall struck once a second, and one Quicken left it at that (two
  jumped it to five times a second). It still strikes once a second, now twice with one Quicken and four times with
  two, and the readout says how often: "A 7-block wall (5s, every 1s)".
- **A cast really goes eight links deep.** A bolt in flight, each strike of a Zone, Orbit or Domain, a Lance's or a
  Ring's hits and a Linger's second landing all used up one of the cast's eight links, so `Bolt · On Hit` four times
  over and then `Bolt · Fire` never threw its last bolt, and a Linger deep in a chain never landed again. Only links
  count now.
- **Quicken can't fling a spell across the world.** Quicken stacked without limit on flying shapes, so a spark or bolt
  with a pile of Quickens crossed thousands of blocks in one step and loaded (even generated) the world along its
  way, a cheap way to lag a server. A flying shape is now at most 8 times as fast (three Quickens on a bolt), a Wisp
  or a Ricochet stops where the loaded world ends, and a bolt flies its 48 blocks and no further (its spare ticks
  used to carry a quickened bolt 70 blocks and more), a wave its 14.
- **An Arc bursts where it lands, even on a creature.** An Arc that came down on the ground splashed everything within
  2 blocks, but one that landed on a creature struck only that creature. It bursts there too now.
- **An Echo in an imbued spell repeats it at its target.** The Echo went off from the caster instead, so a sword or a
  glyph holding `Fire · Echo` burned what it struck once and then "burned" its own maker, which did nothing. It now
  repeats what was stored where the release was set off, as the Imbuing page says.
- **A secret spell read from a scroll weighs its full price against a Shield**, as it does cast from a Cord (it counted
  only its runes' mana).
- **Singularity no longer holds a boss at its black star.** It still strikes them, but bosses are only ever slowed.
- **Stormheart's lightning is spell damage.** It struck whoever hurt you in full, even a player (spell damage to players
  is scaled down by the server's PvP setting) and even a friend whose blow landed; it now spares friends, counts as
  storm damage (a creature weak to storm feels it), a Shield meets it, and players take it at the PvP scale.
- **A widened Rain or Sweep no longer loads the world far away.** Shapes keep growing with every Widen, and a Rain's
  strikes or a Sweep's beam could reach hundreds of blocks past what was loaded, loading (even generating) the land as
  they felt for the ground or a wall. They now stop at the edge of the loaded world.

- **A passive's buffs no longer outlast it.** Whatever a passive gives lasts 15 seconds at most (it's renewed every
  2), so switching `Self · Night Eye` on for a second no longer leaves a minute of night vision for a second's
  upkeep. The same effect from a potion keeps its full length.
- **A spell's blocks never drop anything.** A Rampart blown up by an explosion dropped its packed mud (a free packed
  mud farm); a Rampart's wall, a Span's glass, Light's light and frost's crust on lava now drop nothing however
  they're broken, and a crust blown up (or taken any way but melting) gives its lava back at once.
- **Servers ignore a flood of cast requests**, as they already did for spell edits: a modified client can send at
  most a burst of 20 casts (or charges), then 20 a second, far more than any hand on the cast keys.
- **Fire spells no longer light campfires where they may not change blocks.** Lighting a campfire (as the Archive's
  braziers are lit) skipped the rules the rest of world magic keeps: now it doesn't happen in a claim or spawn
  protection, for a player who can't build there, or on a server that keeps spells off its blocks.
- **Spells that grow something ask a claim first.** Protection and claim mods were only asked when a spell took a block
  away, so Glimmer's lichen, Glowvine's vines and an Ancient Seed's flower could be put into the air inside someone
  else's claim for good. Putting a block into the air now asks them too.
- **Fangs never bite your friends.** Its evoker fangs bit anything that walked onto them, your own pets included, and
  their bites went past Shields and the server's PvP damage scale. They now bite as the spell does (6, once for each
  creature however many fangs it stands on), only what your spells may harm, and nothing once the spell has ended.
- **A creature held in Stasis (or an arrow held by Infinity) that's carried through a portal falls again** as soon as
  it arrives, instead of floating until its ground was next loaded.
- **The glyph limit counts all your glyphs**, in every dimension together (12 by default), rather than 12 in each:
  writing one more lets your oldest fade wherever it is.
- **A parried spell doesn't earn Siphon mana afresh.** Turned back, it's still the one spell paid for once, so it
  shares the original's Siphon cap however many times it's parried back and forth.
- **A duel's end only undoes what your opponent did.** It took away every harmful effect and fire gained during the
  duel, from anything: a monster's poison or lava's fire now stay, and only your opponent's (their spells, a tipped
  arrow, a flaming blade) are taken off.
- **The Tide Scribe remembers its tide.** Reloaded (a restart, or its dungeon unloading and loading again), it
  turned the tide at once; it now keeps to when the tide was due to turn.
- **Archives woken before lecterns remembered their Archivist can re-arm.** Such a lectern takes the Archivist it finds
  about the Archive as its own, and if none is there for 3 minutes while players are, it wakes a new one for whoever
  comes near, like any other.
- **Subtitles no longer cover the spell panel's name**: where the two meet in the bottom-right corner, the subtitles
  move up past it. On a narrow screen, the tracked waypoint's line moves below any boss bars instead of running under
  them.
- **A possible crash opening creative search**: the Blank Rune's and Wisp Lantern's tooltips read your own data while
  the search index was built in the background; they now read it only on the game's own thread.
- **Resizing the window no longer undoes a spell edit** made a moment before (the Cord screen copied your spells from
  the server again, before your edit had come back).

## [0.4.2-alpha] - 2026-09-28

Travel commands for servers, six new element reactions, creature affinities and elemental climate, and a bug
hunt through every part of the mod (more than 70 fixes).

### Added
- **Travel commands for servers**: `/sethome`, `/home`, `/delhome` and `/homes` (3 homes each); public warps
  (`/warp`, `/warps`, and `/setwarp` and `/delwarp` for operators); personal waypoints (`/waypoint add`, `remove`,
  `list`, `track`, `untrack` and `share`), with an arrow, name and distance in the top-left corner and a faint beam
  only you can see; teleport requests (`/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tptoggle`) with
  clickable answers; `/back` (to before your last teleport, or where you died), `/spawn` and `/rtp`. Every teleport
  forms a magic circle at your feet for a 3-second warmup (moving or being hurt cancels it), has a cooldown, lands
  on safe ground and works across worlds, and none can be used in a duel. Operators skip the warmup and
  cooldowns. Server owners can change the limits and times, or switch it all off, in the new `travel` section of
  the config. See [Getting Around](https://defnotean.github.io/wildercord/social/travel/).
- **Six new element reactions, so every element takes part.** Each has its own burst, its name in
  bold, a place in the Grimoire (150 mana the first time), an advancement and Runesmith contracts:
  - **Overload**: storm on a burning foe blows the flames apart: +30%, and 4 damage to every other
    enemy within 3 blocks, thrown back. The fire goes out.
  - **Fracture**: earth on a frozen foe cracks the ice: +40%, and for 5 seconds it's *cracked*: every
    spell hits it 20% harder, a friend's included.
  - **Blight**: life on a *shadowed* foe (hexed, blinded, withered, or in Hush, Eclipse, Echolocate,
    Resonant Shriek, Blackflame or Entropy) turns the darkness to rot: 3 damage and poison to it and
    up to 5 foes around it, and you heal 1 for each (once a second at most).
  - **Unweave**: arcane on a foe carrying two marks or more undoes them all, +30% for each (up to
    +120%).
  - **Rupture**: wind on a *bleeding* foe (Bleed, Rend, Cleave, Dismantle, Crimson Mist, Bonespur)
    tears the wound open: +50%, 4 more through armour, and you heal 2 (once a second at most).
  - **Elapse**: time on a burning, poisoned or withering foe lands everything they still had to deal
    at once, half again as hard (3 to 16).
- Rune tooltips in the Cord screen now say which of these a rune plays a part in: *"Leaves foes
  bleeding: wind damage on them sets off Rupture"*, *"On a frozen foe it sets off Fracture"*.
- Bolts of the right elements colliding in the air set off the new reactions too (fire and storm,
  earth and frost, life and void, blood and wind, fire and time).
- **Chain Reaction**, an advancement for setting off every reaction.
- **Creature affinities.** Creatures can be **weak** to an element (it hits them 50% harder), **resist** one
  (half) or, now and then, be **immune**. The Nether's creatures resist fire and fear frost, the cold's the
  reverse, the undead burn under life magic, golems conduct storm, arthropods are blown about by wind, the End's
  creatures resist void and fear time, and each boss has its own (the Cinder Warden is weak to frost, the
  Star-Eater and the Tide Scribe to life, the Archivist to void). A Runebound resists the element on its Cord.
  **A reaction breaks through a resistance**, so a Shatter still lands in full. A weakness struck floats
  **Weak!** over the creature in the element's colour, a resistance **Resisted**. The table is entity type tags,
  so a datapack can change it: see the [wiki](https://defnotean.github.io/wildercord/spellcraft/affinities/) and
  [docs/features/affinities.md](docs/features/affinities.md).
- **The Bestiary**, a new part of the Grimoire: every kind of creature your spells have struck, with the
  weaknesses and resistances you've found and a **?** for each still to find. Each weakness found condenses 25
  mana toward your next Heart Circle.
- **Elemental climate.** Where you fight nudges the elements: fire +20% and frost -25% in the Nether, void +20% in
  the End, storm +25% under a thunderstorm, frost in the snow, fire in hot dry lands (and less in the rain), void
  at night, life in sunlight, earth deep underground and arcane on a ley line or under a mana storm. A small mark
  after your spell's name on the HUD shows each element favoured (▲) or hindered (▼) where you stand, and the
  Grimoire lists it under *Where you stand*.
- Server config switches `features.creature_affinities` and `features.elemental_climate` (both on).

### Changed
- Joining a server whose Wildercord is **older** than yours (from before the version check) now says so in chat too,
  a few seconds after you join.
- **Cryostasis holds for 4 seconds at most**, however far Extend stretches it (three Extends made it 16 seconds
  of being untouchable), and nobody can cast from inside the ice: it's a moment's shelter, not a fortress.
- The 6th Heart Circle asks for **five different reactions** (any five of the eleven) instead of all
  of them, so it's no harder to reach than before.
- **Prismatic Burst** also uses up the new marks (cracked, shadowed, bleeding); it still deals at most 22.
- Frost no longer hits blazes, striders and magma cubes five times as hard (a vanilla rule meant for powder
  snow). They're weak to frost instead, +50% like every weakness.

### Fixed
- Other players no longer see your Cord vanish from your wrist for a moment after you respawn.
- **Fused runes skipped part of a crowd.** Cast into more than eight enemies at once (a Domain or a big
  Burst), Firestorm, Absolute Zero, Frostbite, Riftbolt, Heartstopper, Stormclock, Sanguine Rite and
  Timesteal did nothing at all to every enemy past the eighth, and Lifebloom didn't heal allies past the
  eighth. Every target now takes the hit; only the lasting and spreading parts (the fire leaping on, the
  cold setting in, the rift, the skipping heart, the bloom) are still kept to the first eight.
- **A teammate sealed in Cryostasis could be pulled into a wall.** If they went through a portal while
  sealed, the ice kept pulling them back to the spot where they were sealed, but in the new world, so they
  could land inside rock or over lava. The seal now breaks when they leave the world it was cast in, and
  Frostbloom and Geode stop answering blows there too.
- **Magma's burning ground could be parried.** Raising a Shield while standing in it parried its next second
  of burning (a counter-burst at its caster, and the Parry feat) as if a spell had just arrived. Like every
  other burning or freezing ground, its later seconds now only meet a Shield as a block. Its cracked, glowing
  ground also now lasts as long as the magma does, instead of vanishing after 4 seconds when Extend makes it
  burn longer.
- A Knot put on the Fusion Altar was called "a silent rune (its add-on is missing)". The altar now says what's
  really wrong (only effects with an element fuse, and a Knot has no power to rank up).
- **Passives couldn't be kept running for free any more.** Switching a passive on now pays its first second of
  upkeep straight away: switched on and off between two seconds, it cast its buffs at full length without ever
  paying, even with no mana at all.
- **Borrowed Time's debt can't be dodged.** Casting it again adds to what you still owe (it used to wipe most of
  it), logging out no longer forgives it, and dying settles it instead of carrying it on after you respawn. Paying
  it back could also, rarely, crash the server.
- Pets covered by your Shield no longer parry. A spell a pet turned back spared the monster that cast it and hit
  players instead; their Shields still block and shatter as before.
- A Grow cast beside a Rampart no longer crumbles the wall, and a Grow or Collect over a Span no longer takes the
  bridge away under you.
- Rewind can't take you back to where you were before you died.
- A lingering effect's later hits no longer follow a player through a portal, and On Land, On Hurt and On Low
  Health no longer play their shockwave in the world you left.
- Stasis ends when its creature is carried to another world, instead of pinning it to the old coordinates there.
- Relogging no longer resets your imbued items' shared cooldown.
- Cleave and Aftershock no longer release the imbued weapon in your hand, or roll Fortune twice.
- Soulfire's mana refund, and Siphon and Imbue in a chorus spell, are capped once per payment: a mana storm's echo or
  Twin Star no longer gets them a second time.
- A Spell Scroll costs twice what its spell costs you to cast, as the Cord screen shows it: the server's cost
  multiplier, your discounts and a found secret's price now count.
- Holding the cast key with the Tome of the Fifth Page's spell selected but the tome put away now charges your next
  spell, as a tap casts it.
- A cracked Heart Circle no longer adds to your innate rune's power.
- Rebirth no longer saves you from /kill or the void.
- In singleplayer, chest loot in a world opened after editing `wildercord.json` uses the new settings (it kept the
  old ones until /reload). A dedicated server no longer logs every config warning twice.
- Shift-clicking a Cord into its slot works alongside mods that add their own inventory slots.
- **Tooltips ran off the side of the screen.** A tooltip line was never wrapped, so most rune descriptions (and
  Imbue's, several hundred pixels long, on any screen) were cut off unless the window was very wide. Tooltips in
  the Cord screen, the Cosmetics page and the Fusion Altar, and those of the mod's items everywhere, now wrap to
  fit. One still taller than the window (the mana badge's, on a short window) is cut short at the bottom instead
  of losing its title and numbers off the top.
- An add-on's rune drew black-and-magenta squares in the magic circle beside the Cord screen and on the Fusion
  Altar. It now wears its family's art there, as its circle in the world always did.
- When the spell HUD had too little room beside the hotbar (a small window with the attack indicator or the
  off-hand slot on its side), it hid all but one of the spell's runes behind a "+2" even though they fitted.
- On a window a little too narrow for the spell HUD beside the hotbar's attack indicator (or a left-handed
  player's off-hand slot), such as 1280x720 or 2560x1440 at the automatic GUI scale, the HUD was drawn over the
  indicator or slot. It now sits on top of them when there's room beside the hotbar.
- The spell HUD's hint for an empty spell always said "K", even with the Open Cord key bound to another key.
- A Cord's beads lit up for three seconds after every respawn, as if it had just been put on (and for a player
  whose Cord showed up a moment after they came into view).
- Renaming a spell, then picking another spell before pressing Enter, gave the name to the other spell. Picking
  another spell now drops the name being typed.
- After leaving a world, the next one could show the last world's ley lines until it sent its own (on a server
  that never does, for good).
- **A finished contract you hadn't handed in yet vanished at dawn**, reward and all. It now stays on the
  Scribing Desk's board, in place of one of the new day's contracts, until you hand it in.
- **Bosses could be led out of their arenas**: any of them through a Nether portal (its altar or lectern then
  woke another, loot and all), and the Archivist in a boat or minecart. They stay where they belong now.
- A boss's bar could stay stuck on screen after its chunk unloaded, and the Archivist's showed to anyone who
  could see it from far off. Like the dungeon bosses', the Archivist's bar now shows only in its arena.
- `/kill` and the void couldn't kill a boss while it gathered itself between phases.
- **An Archivist lost without being killed never came back**: its lectern stayed dark for good. Like a dungeon's
  altar, the lectern now wakes a new one if its Archivist has been missing for a few minutes while players are
  there, and stays quiet only once it has truly fallen.
- The allies the Archivist calls up as it rewrites its Cord now go when it falls, as the dungeon bosses' do.
- Spectators watching a boss fall no longer earn its feat.
- A storm spell shocking the Tide Scribe's flooded arena also shocked your friends, your pets and any villager
  wading in it. It now spares everyone your spells can't harm (you still get a jolt if you're in the water).
- **A duel was a free heal**: when it ended, both duellists got back all the health and mana they'd had, whatever
  had taken it (a fall, a monster, spells cast at anything). Now only the harm your opponent did you is undone,
  and mana you spent stays spent.
- A duellist who logged off mid-duel was saved as the duel left them, and a server shutting down during a duel
  gave one of the two a loss. The one logging off is now put back as they began, and a shutdown ends the duel
  for nobody.
- Accepting a duel that couldn't start yet (too far apart, hurt too recently) used up the challenge, and the
  reason given when one of you was dead was that you were too far apart. The challenge now stands until it
  runs out, and the message says who can't duel.
- Turning the time back with `/time set` gave you a fresh day of rune buybacks at the Runesmith. It now counts
  as the same day, as the contract board already did.
- Any villager's trade asking for a rune (from a data pack, say) was taken for one of the Runesmith's swaps: it
  gave no experience and refused ranked runes. Only the Runesmith's own swaps are treated that way now.
- Casting at a wild wisp (which nothing can hurt) counted toward the casting and reaction contracts, as if it
  were a real foe. Like a Training Dummy, it no longer does.
- New Ember Sanctums are no longer overgrown by basalt columns and lava sheets in basalt deltas, and no longer
  generate through fortresses and bastions.
- **A server that crashed or was killed could leave a Rampart's packed mud, a Span's magenta glass or a Light
  spell's invisible light in the world for good.** They're now written down with the world and taken down as
  soon as their ground loads again. Taking them down no longer loads far-off chunks either.
- A Rampart or Span raised over a Light spell's light put the light back when it came down, leaving it lit
  forever. They now leave such a light alone.
- **Stars could stop falling in a world until the server restarted**: one that landed where nobody went
  afterwards (so it never faded) kept counting as the world's star. Stars now also land only where the world is
  running, and one landing on something built while it fell burns up instead of crushing it.
- When a Fallen Star went, its crater filled back in over anyone standing in it, and guards that had wandered
  out of loaded ground stayed in the world for good. Anyone in the bowl is now lifted clear, and every guard
  goes. A Wither could also break a star, leaving its crater open and its guards behind; stars are now safe
  from the Wither, and one taken away any other way (by a command, say) still cleans up after itself.
- **A rift's or star's monsters could pick up a fallen player's gear and take it with them when they went.**
  They no longer pick anything up, and one sent away drops whatever it was carrying.
- A rift's monster that turned into another (a zombie drowning, a skeleton freezing in powder snow) vanished on
  the spot, and its wave could then never be beaten. It now stays, still one of the rift's (or the star's) own.
- Two mana storms could roll in on top of each other from neighbouring regions. A storm no longer starts where
  it would overlap one already raging.
- `/wildercord event` worked in any dimension (a star would land on the Nether's roof). Like the events the
  server rolls, it now works only in the Overworld.
- Icepath's frosted ice never melted in the dark (in caves, or at night). Like frost's, it now thaws back into
  water after half a minute wherever it is.

## [0.4.1-alpha] - 2026-09-28

A player wiki, and the bugs found while writing it.

### Added
- **The Wildercord Wiki**, at [defnotean.github.io/wildercord](https://defnotean.github.io/wildercord/). It covers
  every rune (its icon, what it does, how to get it and which modifiers work on it), every recipe, every dungeon
  and boss with strategy, and every mechanic from your first Cord to the 8th Heart Circle.
- Mana storms now give their runes: from your 10th cast under a storm, a surge can crystallise **Manaburn** or
  **Manatide** straight into your pack, one per storm.

### Changed
- **Rapid pays for the whole spell** wherever it sits, so putting it on an empty shape no longer halves a
  spell's cooldown for free.
- An **Echo or Pulse after On Hit or On Kill** repeats for the first hit or kill only (it was charged once but
  fired for every hit). The readout says so.
- **Secret spells** show their real price and longer cooldown once you've found them. Until then, the HUD,
  spell wheel and scrolls keep their names hidden, as the Cord screen always did.
- The **spell wheel** shows the Tome of the Fifth Page's spell while the tome is held.
- **The Cord's beads glow only when there's a reason:** for a couple of seconds after you put the Cord on,
  while you charge, and in a flare after you cast. The rest of the time they show their material, instead of
  glowing all the time.
- **Joining a server with a different Wildercord version** now tells you in chat which version to install.
  Runes one version knows and the other doesn't show as Silent Runes, and their tooltip now says why.

### Fixed
- **Runes from chests and bosses came out blank.** Minecraft 26.3 reads loot tables in a new format and
  silently skipped the part that sets which rune an item is. So the Archive's chests, every dungeon chest and
  every boss drop handed out Silent Runes, and advancements gave 1 Blank Rune or Mana Crystal instead of the
  full amount. All loot now uses the new format. Any blank rune you already have turns into a random rune
  (Tier I to IV) the moment it's in your inventory.
- Manaburn and Manatide couldn't be obtained at all.
- Upgrading a Cord lost its enchantments and name. It now keeps them.
- With PvP off, your spells could hurt other players' pets and familiars. They're as safe as their owners now.
- One huge hit could skip a phase of the Archivist, unlike the other bosses.
- The server config's `world_events`, `world_changing_magic`, `duels` and `wild_magic` switches did nothing.
- The Heart Circle boss breakthrough left out the Cinder Warden, the Star-Eater and the Tide Scribe.
- Effects inside a Knot didn't count toward element contracts.
- The "Where it lands" lines were missing from rune tooltips.
- Runes of the world listed dungeon vaults and bosses among their sources, though those never give them.

## [0.4.0-alpha] - 2026-09-28

### Added
- **A fused rune for every pair of elements.** The Fusion Altar now fuses any two effects of the ten
  elements, and two of the same element too: 55 fused runes in all, 43 of them new. See the grid in
  the [Fusion Altar guide](docs/features/fusion-altar.md). A few of the new ones:
  - **Phoenix Pyre** wreathes allies in healing flame.
  - **Singularity** opens a black hole that bursts.
  - **Second Wind** saves an ally from a killing blow.
  - **Reckoning** makes every wound come due twice.
  - **Chronoshift** turns an ally's cooldowns forward.

### Changed
- **Fused runes wear both of their elements.** In a spell's magic circle, a fused rune's ring is a
  braid of two strands, each element's motif in its own colour. Its emblem is split down the middle
  between the two elements' glyphs, and a fusion of one element with itself shows that element's
  glyph widened.

### Fixed
- The Astral Observatory's altar drew as a magenta-and-black missing texture. A test now checks that
  every texture the mod's models use exists.

## [0.3.0-alpha] - 2026-09-28

The big one: three new dungeons with bosses, 51 runes you can only find out in the world, a Fusion
Altar, familiars, world events, a Runesmith villager, duels, casting gear, parries, wild magic and
an advancement tab. Player guides for each are in [`docs/features/`](docs/features/).

### Added
- **Dimension dungeons.** Each dimension has a dungeon and a keeper that fights like a caster, with
  one trick to learn:
  - the **Ember Sanctum** in the Nether: the Cinder Warden's armour turns everything except a
    reaction (Shatter, Conduct, Wildfire...)
  - the **Astral Observatory** in the End: the Star-Eater turns spells back until a heavy enough
    spell, or its own parried shard, breaks its shield
  - the **Drowned Scriptorium** on the deep sea floor: the Tide Scribe floods and drains its pit,
    and storm magic in the flood shocks everyone wading in it

  Each has Rune Seal doors, Runebound guards, a domed arena and a vault. Boss fights run in three
  phases, with a boss bar that names the spell being cast. Everyone who hurt the boss gets a Tier IV
  rune they don't know yet.
- **51 runes of the world** that can't be crafted:
  - found in vanilla structures' chests (Elder Guardians carry the ocean monument's)
  - found in Wildercord's dungeons and on their bosses
  - found in fallen-star craters, rift sieges and mana storms
  - found by **Attunement**: hold a Blank Rune and meditate in the right land at the right moment,
    and the land's rune slowly fills it

  The Grimoire lists every one by where it's found, with a riddle for the ones you haven't learned.
- **The Fusion Altar.**
  - Three of a rune make its next rank (up to III, +50% power).
  - Pairs fuse into new effects.
  - A Blank Rune and string tie a whole spell into one **Knot** that takes a single socket and costs
    10% less.
- **Familiars.** Wild **wisps** rise from ley lines at night, one per element. Tame one with its own
  element's magic or its favourite food. Your familiar floats at your shoulder, quickens your mana,
  casts a little spell of its own every few seconds and levels up as you fight together.
- **Cord cosmetics:** a Cosmetics page for your Cord's beads and glow colour. Some are bought with
  materials, some are earned (the 4th Heart Circle, slaying a Runebound), and everyone near you sees
  them.
- **World events** in the Overworld:
  - **Mana storms** over ley lines: mana flows twice as fast, spells cost 25% less and casts
    sometimes surge.
  - **Fallen stars** leave a guarded crater with a star to crack open.
  - **Rift sieges** send waves of monsters and end with the Riftcaller.
- **Magic that changes the world.**
  - Fire lights grass and boils water into blinding steam.
  - Frost freezes water you can walk on (it always thaws).
  - Storm arcs through water to everything standing in it.
  - Life makes flowers spring up and crops grow.
  - Wind knocks arrows back and blows fires out.
  - Earth heaves the ground and throws foes up.
  - Void draws loose items in.
- **Parrying.** A Shield raised at the last moment turns a flying spell back at its caster, or
  answers anything else with a counter-burst.
- **Wild magic.** Overcasting may surge into something unexpected.
- **The Runesmith**, a villager who works at the new Scribing Desk:
  - sells runes, Torn Pages and Mana Crystals
  - buys duplicate runes, or swaps two known runes for one you haven't learned
  - posts **daily contracts** on a contract board
- **Duels:** a formal one-on-one fight that puts everyone's health, mana and effects back afterwards.
  Your record shows in the Grimoire.
- **Chorus casting:** two or three allies casting the same shape at the same target within a second
  sing a chorus: the last spell gains +50% power per extra voice, and grows wider.
- **Casting gear:** held items that shape your spells.
  - Elemental and greater **staffs** (+20% / +35% power for their element, and cheaper spells).
  - The **Tome of the Fifth Page**, which gives you a fifth spell.
  - Four off-hand **foci**: Haste, Thrift, the Deep Well and Echoes.
- **A Wildercord advancement tab**, from your first Blank Rune to the 8th Heart Circle.
- **A server config** (`config/wildercord.json`) for cast budgets, loot, PvP scaling and events,
  reloadable in game.
- **An add-on API** for other mods: custom runes and effects, plus events before a cast, after a
  cast and when a spell is blocked.

### Changed
- **Crisper magic circles.** Big ground seals (Sandstorm, Vortex, Riftcall, Moonpetal, Hush, Eclipse,
  Magma, reticles and the rest) are drawn in thin lines of light with little glyphs, like a spell's
  own circle, instead of a stretched low-resolution texture with fat pixel strokes. They draw
  themselves in as they open, and the zone seals now sit on the edge of the area they cover.
- **Attunement is a ritual.** The land's own rune circle opens under you and slowly turns, motes of
  its colour rise off the ground and spiral into the Blank Rune, and it builds through the four
  stages (a rune band, then rays, then an outer ring). It finishes in a flash, rings racing out and
  the rune's emblem shining over your hand. Meditation's ring is soft light instead of squares.
- **Storm through water** now shows: branching arcs skitter over the surface from the strike to
  every creature shocked, the water flashes and sparks burst off each one.
- **Steam** is a soft white cloud that swells, rises, drifts off and lingers instead of vanilla
  puffs; a Steam fusion and a Shatter boil off the same way.
- **Wild magic**: the butterflies are bigger, brighter, many-coloured and actually flutter, with
  little fireworks overhead; every surge's swirl is broader, with motes spiralling up into a flash.
- The mod's own smoke (explosions, burning, snuffed fires, bosses, fallen stars) is soft grey
  billows instead of big black squares.

### Fixed
- **Multi-hit spells now land every hit.** On Minecraft 26.3, a creature hit twice in the same moment
  shrugged off the second hit, so Inferno, Wards, innate runes and chained effects did far less
  than intended. Every hit counts now.
- **The game no longer crashes with Improved Transparency (Fabulous-style) graphics** turned on: the
  glowing spell particles now have the rendering pass it needs.
- A discount too small to survive rounding (a staff's 10% on a cheap spell) now still takes at least
  1 mana off.
- Very long Knot names no longer disconnect you. An add-on that throws an error can't crash the
  server. Innate runes can't be tied into Knots. The Surge fusion's Strength stops at II, and fused buffs
  get stronger only from Amplify and rank, not from charge or gear.
- Combining ranked runes keeps the lower rank, not the higher. A quiet selected spell 5 falls back
  to the next spell instead of doing nothing.
- The Runesmith's buyback and swaps can't be farmed:
  - only plain rank I runes it sells
  - 8 buybacks a day
  - no XP from them
- Duels:
  - they restore a snapshot rather than healing anyone
  - they can't be started mid-fight
  - an outsider's blow calls the duel off
- Chorus only joins allies' voices, and never cancels the earlier cast.
- Contracts only count spells that land on a real creature (not Training Dummies).
- Familiars:
  - only their owner can name them
  - projectiles pass through them
  - the Void wisp only pulls drops nobody owns
- Cosmetics cost materials in creative too.
- Dungeon bosses:
  - no single blow can skip a phase
  - loot goes to the fighters
  - boss bars show to anyone near the altar
  - a boss lost without falling re-arms its altar
- The Tide Scribe's pit starts dry and drains completely after every ebb.
- Fallen-star guards stay guarded. A rift can only be sealed once you've beaten its waves, and
  rewards scale with the waves beaten. Event cooldowns survive a restart.
- Soulfire refunds mana only for damage that landed. Lingering burns refresh instead of stacking.
- Manaburn is capped. Constellation, Starshard and Cinderheart need line of sight.
- Attunement gives each land's rune once a day.
- Frosted ice always thaws, even after a restart.
- Inferno and Dragon Breath's later pulses can be blocked by a Shield but not parried.
- Familiars no longer trail far behind you in the air.
- The HUD's Shield readout no longer writes over the spell's name.
- Many flashes came out as darkness instead of light (a parry, a Shield blocking, a bolt's hit, a
  Heart Circle cracking, Chorus and Unison, Fortune, Phantom, several secret spells and the wild
  butterflies' fireworks): a colour written with a full alpha byte carried the darkness flag. They
  shine again. Void's own dark seals (Hush, Eclipse, Riftcall, Portalfall) are darkness again too,
  with a violet rim, where they had come out as faint light.
- A storm's echo, Twin Star, a Focus of Echoes or a wild surge's second go could Imbue again (a
  second imbued item or glyph for one payment) and Siphon past the per-cast cap. Every copy of one
  cast now shares its payment's limits, and keeps its casting gear (the storm's echo lost it, and
  so did a wild surge's copies and a parried spell).
- Glyphs now share their maker's imbued cooldown: a dozen on one redstone line no longer go off
  together. Breaking your own glyph and putting it down again no longer resets its re-arm.
- A glyph on a block from a mod that's since been removed could lose every glyph in that dimension;
  now only that glyph is dropped.
- Add-on effects named like a built-in rune (`example:bleed`) ran the built-in one as well.
- `BEFORE_CAST` fired on both presses of an overcast (and a circle cracked even if an add-on then
  stopped it), and asked a non-zero cost for a wild surge's free recast.
- A Blood Price spell ignored a mana storm's discount, and a secret spell's price was rounded twice.
- A parry now fires `SPELL_BLOCKED`, like any other block.
- Advancements: "know N runes" counted Knots and missing add-ons' runes (the Heart Circles don't);
  Every Word asked for runes nobody can get, and Every Page Filled for Mirrorfrost's feat (only that
  innate rune earns it) and feats that need other players (Unison, Domain Clash, Chorus).
- The server now drops floods of spellbook packets past a generous allowance, skips saving a
  spellbook that didn't change, checks a spell's cooldown before compiling it, and only rechecks
  the runes-known advancements when the runes known change.

## [0.2.1-alpha] - 2026-09-28

### Changed
- **Imbue balance.** Releasing still costs no mana, but it's no longer a way around cooldowns or a
  mana bank:
  - Everything you've imbued shares one cooldown, as long as the stored spell's own (Rapid, Vow and
    heart perks counted, at least half a second). Swapping between imbued swords, bows and armour
    can't release a spell faster than the Cord could cast it, and Vow in a stored spell (twice the
    power) now waits out its four-times cooldown. Bows follow it too: a shot while it cools is a
    plain arrow and keeps its charge.
  - You keep at most **6 imbued items**: imbuing a seventh lets the oldest one's magic fade.
  - Releases no longer Siphon mana back (they were paid for when imbued).
  - A glyph re-arms no faster than its spell's cooldown (at least a second), and catches a creature
    standing on it once, not every second until its charges are gone.

### Fixed
- A stored spell that only helps (a Heal in a sword, a Shield in a chestplate) went off at the foe
  you struck or the block you broke, and did nothing. It now goes to whoever holds the item.
- An Echo inside a stored spell repeated the whole spell from you (and was priced that way); it now
  repeats only what was stored, at the same target. Combo in a stored spell never fires, and the
  Cord screen now says so.

## [0.2.0-alpha] - 2026-09-28

### Added
- **Shield is a spell shield now**: a one-time spell block, invisible until a harmful spell comes
  at you. Then its magic circles spawn in front of the spell, stacked one behind another (one for
  every 8 mana of the spell that raised it, up to 7). A spell that cost as much or less punches
  through as many circles as its mana pays for, and the next one stops it in a flare of light and
  ripples; one that cost more cracks every circle and shatters them like glass, front to back (the
  rims into curved slivers, the rest into tumbling, glinting shards), and goes through. Flying
  spells make the circles appear a moment before they arrive, and strike the front circle. The HUD
  shows your Shield's strength and time left. (It used to give absorption hearts; Barrier and
  Stoneskin still do.)
- **Imbue**, a new Tier II link: the rest of the spell is stored, with 3 charges, instead of cast.
  With Self it goes into the item in your hand: weapons release it at what they strike, tools at
  the blocks they break, bows with their arrows, armour and shields at whatever hurts you, and
  anything else when used. **Any block** can hold magic too: aimed at a block, cast with empty hands
  at the block you're looking at, or placed from an imbued block item, it becomes a glyph that goes
  off at whoever steps on, uses (doors, chests, buttons), shoots or breaks it, or when it's powered
  by redstone. Break your own glyph and the block comes back still imbued. Imbued items glint and
  say what they hold.
- Three feats: Spellguard, Shieldbreaker and Imbuer. New sounds for a spell ringing off a shield
  and for imbuing; the shield's shatter now ends with shards pattering down.
- Monsters can cast spells: the engine now takes any living caster. **Runebound** zombies,
  skeletons, witches and illagers carry Cords, telegraph every cast with a magic circle and show
  their spell as their nameplate.
- **The Archive**, a buried library with Rune Seal doors (opened by spells of the right elements),
  Runebound guards, a domed arena and a vault of Tier IV runes; and **the Archivist**, a
  three-phase boss that rewrites its Cord and casts a secret spell.
- **Charged casting** (hold the cast key) with a growing magic circle and an aim preview; the new
  `wildercord:sigil` particle draws magic circles everywhere.
- Magic circles you can read: every spell writes its own circle (a frame, a band of script made
  of its runes, its element's pattern, a star with a point per rune and on each a roundel with that
  rune's emblem, and its shape's seal in the middle). Every rune has an emblem and ring pattern no
  other shares (generated by `tools/circle_art.py`). The same circle opens in front of a charging
  caster's raised hands, lies under every cast, is a monster's telegraph and a Domain's floor, and
  stays small: a longer spell gets more points, not a bigger circle.
- The **spell wheel** (hold the switch key; let go without pointing and it stays open until you
  pick a spell, by click or number, or press Esc), **spell names** (automatic, and renamable), and
  **spell codes** you can copy and paste in the Cord screen and that turn into readable cards in
  chat.
- Ten **secret spells** found by exact rune sequences, and **Torn Pages** with their riddles (and
  the way to the nearest Archive), found in old libraries, ruins and the Archive, and dropped by
  Runebound and the Archivist.
- The **Grimoire** page: reactions, secrets, feats and riddles discovered, with toasts; each new
  reaction, secret and feat is worth condensed mana.
- Ten **innate runes**, one awakened per caster at the 1st Circle.
- **Rhythm** casting, **elemental leaning**, and **overcasting** (crack a Heart Circle to cast
  beyond your mana).
- **Spell collisions**, **domain clashes** and **Unison** strikes between casters.
- **Ley lines** (visible to Cord-wearers; mana regenerates and Heart Circles form twice as fast on
  them) and the **Wellstone**.
- **Spell Scrolls** anyone can cast once, and the **Training Dummy** with floating damage
  numbers and DPS.
- Operator commands `/wildercord innate <rune>` (choose your innate rune) and
  `/wildercord runebound` (bind the nearest monster to a Cord).
- **41 new Tier I and II runes**, all craftable:
  - Energy balls and beams: **Spark**, **Ray** and **Nova** (Tier I); **Wisp**, **Comet**,
    **Ricochet**, **Cluster**, **Lance**, **Sweep**, **Prism** and **Stream** (Tier II), drawn as
    glowing orbs and beams of light.
  - Protection: **Barrier**, **Brace**, **Anchor**, **Bramble**, **Frostward** and **Cushion**
    (Tier I); **Deflect** and **Haven** (Tier II). Anchor, Frostward and Cushion can be passives.
  - Mining and building: **Chisel**, **Glimmer** and **Prune** (Tier I); **Tunnel**, **Vein**,
    **Smelt**, **Fell** and **Span** (Tier II). They break blocks as a pickaxe of their strength
    would, and only where you may build.
  - A simple spell for every element: **Ember**, **Icicle**, **Pelt**, **Windcut**, **Leech**,
    **Hex**, **Rend** and **Countdown** (Tier I); **Jolt**, **Bleed**, **Coldsnap**,
    **Flashfire**, **Banish** and **Cyclone** (Tier II). An Orbit passive can carry Ember,
    Icicle, Pelt and Windcut.
- **Wildercord's own sounds**, synthesised by `tools/sound_art.py` and all in one key so they
  harmonise: a cast and an impact for every element, magic circles, beams, orbs, shields, domains,
  blinks, the Cord screen, the spell wheel, the Grimoire and Heart Circles. Charging now hums,
  rising in pitch as the charge builds, chimes when it's full and rushes out on release.

- **Magic glows.** Every circle, beam and effect is drawn with additive light, so overlapping
  light burns brighter, and void magic is drawn as darkness with a thin violet rim.
- Bolts glide as **comets** with tapering trails, drawn by each player's game from the bolt's
  smoothed position.
- **Secret spell circles:** each secret spell's circle has a centrepiece of its own (a sun, a
  snowflake, a horizon, a flower, lightning, a black star, a clock, wings, nested stones, a
  constellation).
- **Casting poses:** you raise both hands while charging and move with each shape when it goes off
  (a thrust, a sweep, alternating blows, arms flung up for the great circles).
- **Screen effects:** big impacts shake the camera, a charged release kicks the view, heavy hits
  land with a punch, and a Domain tints the edges of the screen of everyone inside it. They follow
  vanilla's Screen Effect Scale.
- **The worn Cord** shows on every player's wrist: a band in its tier's material and a glowing
  bead for each rune of the spell they have ready, brighter while charging.
- The Cord screen shows the edited spell's **magic circle** beside the window, opening as you build
  it; on the Grimoire page it shows the secret spells you've found, like plates in a book.

### Changed
- Heart Circle breakthroughs ask for feats (reactions, secrets, Runebound, the Archivist) instead
  of ever-bigger kill counts.
- The HUD shows the spell's name, the rhythm beat, cracked circles and the charge.
- Impacts and bursts flash with a small glow of their own instead of the vanilla firework flash,
  which up close showed as a pale square several blocks wide.
- Casting no longer fills your own first-person view: the spin of your Heart Circles on each cast
  is shown only to the people around you, the circle under every cast is a flat magic circle
  instead of a dust ring, the cast no longer throws potion sparkles, and bolt trails start a
  little way out. (You still see your rings while meditating and when a circle forms.)
- Every shape is drawn in light (a new `wildercord:light` particle: shockwaves, beams with a hot
  core, sweeping crescent slashes and orbs): bolts are glowing comets, beams fire through a circle
  with rings of power, Crescents are blades of light, Bursts throw shells of light, Rain opens a
  circle in the sky, Pillars erupt as columns of light, and a Domain's floor is the spell's own
  circle under a dome of light.
- A Blitz on the ground runs level, so looking slightly down no longer stops it dead.
- Bolt hits are softer: finer, fading trail dust, fewer sparkles and a finer ring on impact, so the
  burst no longer hides what it hit. Big shockwaves (explosions, mines) keep their weight.
- Casting monsters hold their telegraph circle out in the right hand, so their face stays in view.
- Tectonic Rise raises real stone spires and Glacial Lance closes its targets in ice (block
  displays, never real blocks). Starfall and Starlight Cascade mark where each star will land.
- Creatures and places look the part: Runebound glow with rune marks in their spell's colour (and
  a faint aura) that flare as they telegraph; the Archivist is a hooded, hovering figure with a
  floating tome and circling pages that tear loose when it rewrites its Cord; ley lines flow as
  ribbons of pale violet light; the Archive has shafts of light and dust, flickering braziers, a
  glowing inlaid circle and quiet whispers; an awake Wellstone wears a turning halo.
- Every effect is drawn in its element's own visual language instead of dust puffs: fire licks and
  flares, frost shatters and creeps, storm forks into branching lightning, wind swirls, earth
  cracks, life blooms, void is darkness imploding round a black core, arcane draws star seals and
  orbiting comets, time turns clock hands and blood cuts and pulses. Reactions, Heart Circles,
  innate runes, Unison, domain clashes, overcasting and the secret spells got the same treatment.

- Circles and shaped light never open right in front of a player's own eyes (a beam's hand circle
  used to fill its caster's screen), and stay silent while a passive renews itself quietly.
- Crescent's recipe gains a feather, so it no longer clashes with Empower's.
- Rune emblems are handed out in the order runes are defined, so adding runes no longer changes
  existing emblems. (This reshuffled some emblems once.)

### Fixed
- Two same-element bolts colliding no longer throws an error.
- Spells no longer mine a Rampart's temporary wall (which dropped packed mud).
- Span's rules (its glass drops nothing, bridges come down when the server stops) are in force from
  the start, not only after the first Span is cast.
- The HUD's charge readout no longer overlaps the mana count.
- A spell of linked repeating shapes (Zone On Hit Zone On Hit ...) could multiply into hundreds of
  thousands of casts and stop the server; a whole cast now runs at most 128 parts.
- Overcasting can't pay for a spell costing more than twice your full mana (Overcharge stacks
  could one-shot bosses or freeze the server for the price of one cracked circle), and a prompt
  left over from another world no longer overcasts on the first press.
- Widen is capped at eight times the radius, so a heavily widened Prune, Icepath or Collect can't
  scan hundreds of blocks and load chunks; Icepath counts against the block budget, and Collect
  reaches at most 24 blocks and never out of land you can't build on.
- Spells that change blocks respect claim and protection mods (a spell's break is offered to them
  as the caster's own), as the Break rune always promised.
- Something held by Stasis or Infinity no longer floats forever when it's saved held (a player
  logging out in Stasis, arrows frozen when the server stops).
- Spectators and dead players can't cast, run passives or share a boss kill; Rebirth cast from the
  death screen no longer arms a free death save.
- A charged spell no longer fires when you open a screen (the spell wheel, chat, the pause menu):
  it waits, like a drawn bow, and dying drops it. A charge that fizzled no longer casts when you let
  go.
- Mashing the cast key no longer keeps the rhythm chain at full: a press before the beat starts it
  over, as it should.
- Spell cooldowns are saved, so logging out and back in doesn't reset them.
- Monsters' spells hit players at full power; only damage between players is scaled down.
- Execute and Unison apply to damage that lands later (Meteor, Countdown, Starfall, the second and
  third Dismantle slashes, Aftershock and Bleed after the first hit, and more).
- Zero Hour follows the PvP rule and leaves creative and spectator players alone.
- Shackle only slows bosses instead of chaining them in place.
- Stoneform on a pet no longer hurts its owner, and its knockback resistance comes off.
- Phantom's afterimages, Light's light blocks and wards no longer outlive a server stop, and state
  from one singleplayer world (wards, reaction marks, bolts in flight, overcast prompts) no longer
  leaks into the next.
- Pistons can't move a Span's glass or a Rampart's wall somewhere it would never be taken down.
- An Orbit passive is cast again straight away after a dimension change, instead of charging upkeep
  with nothing orbiting.
- The 1st Circle's title is no longer cut off by the innate rune waking in the same moment.
- The server forgets a player's charge, passives, prompts and meditation when they leave.
- `/wildercord learn` and `/wildercord innate` accept namespaced ids (`wildercord:fire`, an
  add-on's runes).
- A Spell Scroll made by command with more than 16 runes no longer disconnects everyone it's sent to.
- A charge whose Cord was taken off now fizzles.
- **The Archive:** its first two seal doors (Frost and Storm, Fire and Wind) are generated instead of
  being walled over, its stairway steps face the right way, and the vault chests turn with the
  structure.
- Runebound keep their extra health through a reload (Archive guards never had it), don't take over
  a named mob's name, keep their Cord when they turn into another monster (and lose the nameplate
  when they turn into something that can't carry one), and the Archivist's guards are bound once.
- The Wither can't destroy Rune Seals or the Archive Lectern.
- A sneaking caster's spell hits the Training Dummy instead of picking it up; its damage numbers and
  DPS nameplate no longer stay behind after a save.
- Brushing trail ruins can actually find Lightning and Shock runes.
- The Ender Dragon's runes land at the feet of whoever killed it, not over the void.
- The previous world is let go when you quit to the title screen (some effects held it in memory),
  and screen shake, comets and the charge hum reset properly between worlds and dimensions; the
  camera no longer shakes behind the pause menu.
- Switching page in the Cord screen mid-drag no longer crashes it, and category chips that don't
  fit are no longer clickable. Right-clicking a rune (to take it off a spell, or thread it from the
  Codex) works again, and a click the Cord refuses now says why under the spell's name (a spell the
  Cord doesn't hold yet, a rune too strong for it, a full spell, a rune that can't be a passive)
  instead of only playing a low note.
- The worn Cord fits slim arms and shows over sleeves.
- Orb rings keep spinning on very old worlds, the Wellstone halo no longer jumps once a day, and
  spell wheel labels stay on screen at small GUI sizes.
- The Cord enchantment tag has a name in recipe viewers.

### Removed
- The Stand shape. Cords that still hold a Stand rune keep it threaded, but it stays quiet.

## [0.1.0-alpha] - 2026-09-27

The first public version.

### Spells
- The Cord slot (above the offhand) and four Cords: Twine, Copper, Amethyst and Echo, with more
  sockets, spell slots, rune tiers and mana at each step.
- The rune sequencing engine: shapes, effects, modifiers and links, read left to right, with a
  plain-English readout, cost and cooldown for every spell.
- 135 runes across four tiers and ten elements (Fire, Frost, Storm, Wind, Earth, Life, Void,
  Arcane, Time, Blood), each with a hand-drawn icon.
- Element reactions: Shatter, Conduct, Wildfire, Implode and Collapse.
- Stasis holds every hit on a stopped target and lands it all when time moves again.

### Progression
- Mana: Mana Crystals, Clarity and Mana potions, meditation, and the Reservoir, Wellspring and
  Siphon Cord enchantments.
- Spell enchantments for Cords: Potency, Celerity, Thrift and Persistence.
- Heart Circles: condense mana by casting, earn breakthroughs, and meditate to form up to eight
  circles, each adding mana, regeneration and power, with perks at the 3rd, 5th, 7th and 8th.
- Passive spells: up to two always-on spells that cost mana every second, from a restricted set of
  sustainable runes.
- Every Tier I-III rune is craftable (costlier by tier) and appears in the recipe book; Tier IV
  runes drop from bosses and rare structures. Chest loot favours low tiers.

### Interface
- The Cord screen: drag and drop, type-to-search Codex, family tabs, category chips that appear as
  you learn runes, a Spells/Passives switch, and heart, mana and help badges.
- The spell HUD beside the hotbar, readable at every GUI scale.

### Technical
- Server-authoritative casting with per-cast budgets, friendly fire off, and PvP damage scaling.
- A generated asset pipeline (`tools/`) driven by the rune roster.
- Unit tests for the spell engine and client game tests for mechanics and layout.
