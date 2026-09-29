# Changelog

All notable changes to Wildercord. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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

### Changed
- **Cryostasis holds for 4 seconds at most**, however far Extend stretches it (three Extends made it 16 seconds
  of being untouchable), and nobody can cast from inside the ice: it's a moment's shelter, not a fortress.

### Fixed
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
