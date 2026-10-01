# Frequently asked questions


---

## Getting going

### How do I start?
Craft **Blank Runes** (4 Cobblestone around 1 Lapis Lazuli, makes 4), then a **Twine Cord** (3 String over a
Blank Rune), and put the Cord in the new slot just above your offhand. You learn Self, Bolt and Push at once,
and spell 1 is threaded for you. Tap `R` to cast it. The whole walk-through is on
[Getting Started](getting-started.md).

### Where's the Cord slot?
In your inventory (`E`), **just above the offhand slot**, with the faint outline of a Cord in it. Shift-click a
Cord and it goes there by itself. In creative it's in the Survival Inventory tab, beside your armour. See
[Cords](spellcraft/cords.md#the-cord-slot).

### Do I have to hold my staff and focus to cast?
No. Your inventory (`E`) has three **gear slots**: Staff, Focus and Tome. A piece in its slot works with nothing in your
hands, and shows on your character. A piece in a slot takes the place of held pieces of its kind; with a slot empty, that
kind still works held as before. See [Casting Gear](gear.md).

### Where are the gear slots?
In your inventory, in a small tray on top of the window, above your armour (in creative, beside the Cord slot in the
Survival Inventory tab). Shift-click a staff, focus or the tome and it goes to its slot by itself.

### Do I lose my gear when I die?
What's in your gear slots drops with the rest of your inventory (and stays with you if the server keeps inventory).

### How do I carry more?
Craft a **Backpack** (a String over a Chest, with 5 Leather round it): 18 more slots. Upgrade it to a **Reinforced
Backpack** (27) and then a **Runewoven Backpack** (36); each upgrade keeps everything inside. Use it to open it, or
wear it in the **Backpack slot** (the fourth slot on the tray on top of your inventory) and press `B`. See
[Backpacks](items/backpacks.md).

### Do I lose what's in my backpack when I die?
Not what's inside it: the backpack keeps its contents, so it drops with everything still in it, as the rest of your
inventory drops (and stays with you if the server keeps inventory). Go back for it like any other drop. The same goes
for a backpack you throw, put in a chest or send through a hopper.

### Why won't my shulker box go in my backpack?
Nothing that holds items goes in a backpack: no other backpack, no shulker box, no bundle, no chest still holding
its contents. That keeps storage from being packed inside storage. See
[Backpacks](items/backpacks.md#what-cant-go-inside).

### Does the mod need to be on the server too?
Yes. Wildercord has to be installed on the server and on every player's game, with Fabric API.

### What's a Silent Rune?

A rune your game doesn't know. Every rune has a name the game looks up; a **Silent Rune** is one whose name
isn't in the Wildercord you have installed, so it can't show what it is or do anything. It almost always means
**your Wildercord is a different version from the server's** (the server has runes your version doesn't, or the
other way round), or the rune came from an add-on mod that isn't installed. Nothing is lost: a Silent Rune keeps
its rune safe, threaded runes stay in their sockets, and they all wake up as soon as you install the matching
version. When you join a server whose Wildercord differs from yours, a message in chat says which version to
install. A server running a Wildercord older than the version check itself can't say which version it runs: a few
seconds after you join, a message says the server's Wildercord is older than yours, so either the server needs
updating or you install the version it runs.

### I pressed `R` and nothing happened.
Look just above your hotbar: the game always says why.

- "Wear a Cord first" means the Cord slot is empty.
- "Spell 1 is empty. Press K to thread runes" means the spell has no runes that can fire (quiet runes don't
  count).
- "Recharging... 0.4s" means it's still on cooldown.
- "Not enough mana (12/29)" means just that. Wait, or meditate.

See [Casting](spellcraft/casting.md#pressing-the-cast-key).

### How do I learn a rune?
Hold the rune and use it (right-click). It's used up and you know it for good: it's in your Codex, and you
can thread it into every spell, as many times as you like. You don't have to wear a Cord to learn one. If you
already know it, nothing is used up.

### Where are the rune recipes?
In your recipe book, as soon as you've held a Blank Rune. Every rune up to Tier III can be crafted from a
Blank Rune and a few items, in any layout. Every rune's tooltip also says how to craft it and where it's found.
The full list is on [Rune Recipes](items/rune-recipes.md).

### Which runes can't be crafted?
Tier IV runes (from bosses, dungeon vaults and rare structures), the 51
[runes of the world](runes/world.md) (each found only in its own places), the 55
[fused runes](runes/fused.md) (made at the Fusion Altar) and the ten
[innate runes](runes/innate.md) (one wakes in your heart).

### Why does a rune I just learned only show a hint?
A rune you've just learned is **unread**: the Codex shows its name, family, tier, element and cost, but only a hint
of what it does. Cast it once and you **glimpse** it (its text, with the numbers veiled); see it at work a few times
and you **understand** it, numbers and all. Runes you already knew, and the three your first Cord teaches you, are
understood. See [Reading runes](spellcraft/harmonies.md#reading-runes).

### What do I do with a rune I already know?
Keep it for the [Fusion Altar](fusion-altar/index.md), where three of a rune make its next
rank; sell it to a [Runesmith](social/runesmith.md), or swap two for one you don't know;
or give it to a friend.

## Building spells

### Does the order of runes matter?
Yes, completely. The Cord reads left to right. A modifier changes the closest rune **on its left** that it can
change, so `Bolt · Fire · Amplify` makes the Fire stronger, while `Bolt · Amplify · Fire` does nothing extra
(a Bolt has no power to amplify). See [How a Spell Is Read](spellcraft/reading-spells.md).

### What do the gold lines under my runes mean?
They show what each modifier is attached to. A modifier with a small red mark instead is attached to nothing,
and the readout says "... does nothing here". Move it to the right of the rune you meant.

### Why is one of my threaded runes marked red?
It's **quiet**: kept, but it won't fire. Hover it to see why: it's too strong for your Cord, it's past your
Cord's last socket, you haven't learned it, or it's from an add-on mod that isn't installed. See
[Quiet runes](spellcraft/cords.md#quiet-runes-and-a-smaller-cord).

### Why are some runes in my Codex dimmed with a lock?
Your Cord can't fire them: their tier is above your Cord's. Hover one to see which Cord it needs. On the
Passives page, runes that can't be part of a passive are dimmed the same way.

### Do I need a shape in every spell?
No. A spell that starts with an effect is cast on you, as if it began with Self: `Heal` alone heals you and
saves a socket.

### Can I put the same rune in a spell twice?
Yes, as often as you like: `Fire · Amplify · Amplify` is +125% power. You only need to learn a rune once.

### What's the difference between `Bolt · Fire` and `Bolt · On Hit · Fire`?
Almost nothing: both set alight the creature the bolt hits. The difference is that after On Hit you can add a
new **shape** that starts where the bolt landed: `Bolt · On Hit · Burst · Fire` bursts into flame around the
impact. See [Links and segments](spellcraft/reading-spells.md#links-and-segments).

### How is a spell's mana cost worked out?
Each shape and effect has a base cost, the shape multiplies its effects, modifiers multiply what they're
attached to, and links add their own cost and everything after them. The readout always shows your exact
price. The full working is on [Cost](spellcraft/reading-spells.md#cost).

### Why does my spell cost less (or more) than my friend's?
The price you see is yours: Thrift on your Cord, the 8th Heart Circle's Archmage perk, a staff or Focus of
Thrift in your hands and a mana storm overhead all make spells cheaper. Your cooldown differs too, with
Celerity and the 5th Circle's Flow. And a [secret spell](spellcraft/secret-spells.md)
costs more, and recharges more slowly, for whoever has found it.

### How long is a cooldown?
About a second for every 20 mana of the spell's plain cost, never less than half a second and never more than
20 seconds. Rapid halves it, Vow makes it four times longer, and every spell has its own cooldown. See
[Cooldown](spellcraft/reading-spells.md#cooldown).

### My spell says it "can't be cast".
It costs more than your whole mana pool. Trim it, grow your mana (see [Mana](progression/mana.md)),
or [overcast](spellcraft/overcasting.md) it, which only works up to twice your pool.

### How do I share a spell with a friend?
Three ways: copy its **spell code** from the Cord screen and paste it in chat (everyone sees a spell card they
can click to copy); **inscribe a scroll** anyone can cast once; or tie it into a **Knot** at the Fusion
Altar, which anyone can learn even without the runes inside. See
[Playing Together](social/playing-together.md) and [Knots](fusion-altar/knots.md).

### I pasted a spell code but some runes were left out.
The Cord screen only loads runes you know and your Cord can fire, up to your Cord's sockets. It tells you how
many it left out. Learn those runes (or upgrade your Cord) and paste again.

## Casting

### Should I tap or hold `R`?
Tap for speed, hold for power. A full charge takes a second and a half and adds 40% power, but you walk slower
while charging. While you charge, a ring or dotted line shows where the spell will go. See
[Tap or charge](spellcraft/casting.md#tap-or-charge).

### What happens if I keep holding `R` after the charge is full?
It **overchannels**: every 1.2 seconds it climbs a stage (up to three, as your Heart Circles allow), each stronger
(+20%, +40%, +60%) and each with a bigger chance the spell surges into wild magic. It drinks a little spare mana as it
goes, but never the mana the spell itself costs. Let go just as the charge fills or a stage lands for 10% more. Hold on
too long past your last stage and it tears loose. See
[Overchannel](spellcraft/casting.md#overchannel-holding-on-past-full).

### My spell tore loose and I couldn't cast for a moment.
You held an overchannel past your last stage. The spell fizzled, the loose magic surged (harmlessly), and you were
dazed for a second and a half and lost 30% of your mana. It never costs health. Watch the ring on your spell badge: when
it turns red, let go. See [Holding too long](spellcraft/casting.md#holding-too-long-the-channel-tears-loose).

### What's the shape round my crosshair while I charge?
Your spell's **glyph**, a few strokes made from its runes. Hold sneak while charging to steady your hands: the camera
holds still and the mouse traces it. A good trace makes an overchannel less likely to surge and adds a little power;
not tracing loses nothing. You can turn it off, or make it more forgiving, in the Magic visual settings screen. See
[Sigil tracing](spellcraft/casting.md#sigil-tracing).

### What are the words rising over a caster's shoulders?
Their spell's **incantation**: one syllable for each rune, spoken as the rune's roundel opens on their circle while they
charge. Learn the syllables and you can read what's coming. A tap is cast without a word. The Magic visual settings
screen can hide incantations: all of them, others' or your own. See
[Incantations](spellcraft/casting.md#incantations).

### My charge won't start.
A charge can't start while the spell is cooling down, with an empty spell, or without a Cord. And a charge
held longer than 12 seconds fizzles.

### How do I switch spells quickly?
Tap `V` for the next spell, or hold it for the spell wheel. Or bind the "Cast spell 1" to "Cast spell 5" keys
in the Controls options to cast any spell directly. See [Switching spells](spellcraft/casting.md#switching-spells).

### How do I get more mana?
Better Cords, Heart Circles, Mana Crystals (+10 max mana each, up to 10), the Reservoir and Wellspring
enchantments, Clarity and Mana potions, meditating (sneak and stand still), ley lines and Wellstones. Hover
the mana badge in the Cord screen for exactly where yours comes from. See [Mana](progression/mana.md).

### Can I cast without enough mana?
Two ways. **Overcast**: press again within two seconds and your outermost Heart Circle cracks to pay (it mends
in 3 minutes). Or thread **Blood Price** and pay in health instead of mana. See
[Overcasting and wild magic](spellcraft/overcasting.md).

### Can my spells hurt my friends or my pets?
No. Harmful effects never touch you, your tamed pets or players on your team, and other players (and their
pets) only when the server allows PvP (and then for 60% damage to a player, unless the server changes it).

### Can I fly?
Yes, with [Soar](runes/effects/wind.md#soar), a Tier III wind rune (an Amethyst Cord or better).
It works as flying does in creative, only a little slower: double-tap jump to take off, then hold jump to rise and
sneak to sink. It lasts 20 seconds (Extend makes it longer), and cast while you're falling it catches you at once.
Three seconds before it ends the wind starts to fade, with a chime and a line over your hotbar, and then it sets you
down gently: you can't be hurt by the landing, however high you were. Cast on a shape that reaches your friends
(a Burst, say) it lifts them too. Being pulled (Pull, Gravity Well) or weighed down (Weigh, Downdraft) tears the
flight away, and nobody flies inside a dungeon's warded arena.

### Why won't my Heal heal my friend?
Helpful spells only reach **you and your allies**, and another player is only your ally when you're on the
**same team** (the game's team command, which an operator can set up). Pets you've tamed always count. See
[friendly fire](spellcraft/reading-spells.md#friendly-fire-who-a-spell-touches).

### Will spells break blocks or grief my base?
Spells only change blocks where you're allowed to build: spawn protection and claim mods are respected, and a
server can turn block-changing spells off entirely. Monsters' spells never change blocks. Fire from a spell
only spreads where fire spreads anyway, frozen water always thaws, and a crust a frost spell lays over lava always
melts back. Two things to mind: a fire spell lights TNT, and a storm spell can charge a creeper. See
[World Magic](world/world-magic.md).

### My spell left ash (or frost, or a strange flower) on the ground.
That's a **residue**: a strong spell (30 mana or more), any overcast, and now and then a reaction leave a mark of
their element where they land. Each fades on its own (a void scar in minutes, everfrost in about a day), and each gives
a **reagent** when you break it (bottle an eddy instead). Reagents have small uses, and at the Fusion Altar they steady
or strengthen a fusion. See [Residues and Reagents](world/residues.md).

### Will residues mess up my base?
No. They only ever take natural ground (grass, dirt, sand, stone, snow...) or the air just above it, never anything
built or placed, never a chest, never inside a claim you can't build in or a dungeon's ward, and a residue that took
the ground's place gives it back when it fades or when you harvest it. There are only a few in any one place, and
other spells and pistons leave them alone.

## Cords and progress

### Do I lose anything when I die?
Your Cord, your learned runes, your spells, your spells' mastery (their ranks and traits), your Heart Circles and
your cooldowns are all kept; the Cord is never dropped. You come back with an empty mana pool, which refills as usual.

### Do I lose my spells when I change Cords?
No. Spells and runes are saved on you, not on the Cord. A better Cord just opens more sockets and spell rows,
and one crafted from your old Cord keeps its enchantments and name. A smaller Cord deletes nothing either: runes past its
sockets, rows it doesn't have and runes too strong for it stay threaded but quiet, and wake again when you put
the bigger Cord back on. See [Upgrading](spellcraft/cords.md#upgrading).

### What's the numeral at the end of each spell row?
Its **rank**. Your spells grow with you: from Kindled (I) to Mythic (V), through casts that matter, and at each rank
from Practised on you choose one of three traits for it. Click the numeral to see the spell's traits, your sigil and
how far it has to go. See [Spell Mastery](spellcraft/mastery.md).

### My spell lost its rank when I changed it.
A spell is its exact runes in order, so a changed spell is a new one and starts from Kindled. The old one is
remembered: change it back and its rank and traits are there again. See
[One spell, its exact runes](spellcraft/mastery.md#one-spell-its-exact-runes).

### How do I rank a spell up faster?
Use it where it matters: on real foes (Runebound and bosses count for more), to heal or ward allies who need it, and
in danger (low health, a crowd, a boss, a dungeon). A kind of foe it hasn't struck lately is worth more. Casting at
the same thing in the same place earns less and less, and training dummies only teach a little. See
[How a spell grows](spellcraft/mastery.md#how-a-spell-grows).

### Can I give a friend my mastered spell?
Once it's Adept, inscribe it onto a scroll from the Cord screen: the scroll carries its traits and your sigil. Your
friend can read it to cast it once, or sneak and use it to learn the spell at Kindled with your traits borrowed. See
[Inscribing a mastered spell](spellcraft/mastery.md#inscribing-a-mastered-spell).

### Can I hide the spell names other players' casts show?
Yes: switch **Spell titles** off in the magic visual settings. It only changes what you see. See
[Spoken names](spellcraft/mastery.md#spoken-names).

### How do I get passive spells?
Form your 1st Heart Circle for the first passive slot, and your 5th for the second. Thread them on the Cord
screen's Passives page. See [Passive Spells](spellcraft/passives.md).

### Someone found a harmony. What's that?
Every world has about a dozen **harmonies** of its own: exact rune sequences that only your world answers, each
with a twist (rain that lands as glass, a pale horse, birds of light...). Casting one finds it, and the server says
so by name, but never which runes it was. Torn Pages can carry their riddles. See
[Harmonies and Reading Runes](spellcraft/harmonies.md).

### Why does Shock behave differently here than in my other world?
Your world has a few **quirks**: small tweaks to particular runes in particular conditions, such as at night, in the
rain or deep underground. They differ from world to world, and each goes into your Grimoire the first time it matters
to one of your spells. See [Quirks](spellcraft/harmonies.md#quirks).

### What's my innate rune, and how do I get it?
One of ten runes that wakes in your heart, chosen at random, when you form your 1st Heart Circle. It can't be
crafted, found or learned from an item, it's Tier I so any Cord holds it, and it grows stronger with every
circle. See [Innate Runes](runes/innate.md).

### How do I find secret spells?
Ten exact rune sequences become something grander. Nothing lists them: experiment, or read the riddles on
**Torn Pages** (found in old chests and dropped by Runebound). Once you've cast one, it's in your Grimoire.
See [Secret Spells](spellcraft/secret-spells.md).

## Aura

### What's aura? Do I need a Cord for it?
Aura is the swordsman's path: mana drawn into your body and out along a blade. You don't need a Cord: read a **Breathing
Manual** to learn a method, then fight with a sword, axe, spear, trident or mace, or sneak and stand still with one in
hand to breathe aura in. It works alongside your spells. See [Aura](progression/aura.md).

### Where do I find a Breathing Manual?
In old battlegrounds: the Archive's library and vault, expedition vaults, trial chamber vaults, stronghold libraries and
ancient cities. Master weaponsmiths and clerics sell one for emeralds and a book. See
[Where to find manuals](progression/aura.md#where-to-find-manuals).

### My aura bar isn't filling when I hit things.
Only **full swings** on real foes count: wait for your attack to recharge between swings. Hitting the same kind of foe in
the same spot for a long time gives less and less, and a Training Dummy only gives a quarter. Or stand in the breathing
stance (sneak and stand still with your blade in hand).

### Why can't I break through to the next stage?
When your aura reaches a stage's limit, a breakthrough waits for a trial: hold the breathing stance for half a minute where
ley lines cross, or fell a foe stronger than you (a boss, a Runebound or anything with twice your health) with your blade
alone, no spells. The Aura page (the Aura badge in the Cord screen) shows how far you've got.

### I pressed the Aura key and got slowed.
That's backlash: you used a technique without enough aura. It wears off in a few seconds and never hurts.

### Can I change my breathing method?
Yes: read another method's manual twice. You keep your stage, but lose the road to your next breakthrough and the aura
you hold.

## The world

### Some monsters are glowing and casting spells.
They're **Runebound**: monsters that carry a Cord. Their nameplate is their spell, and before every cast they
hold out its magic circle for about a second. They drop runes and Torn Pages. See
[Runebound](world/runebound.md).

### How can I tell what a monster is about to cast?
Read its circle: the colour is the element, the seal in the middle is the first rune (usually the shape), and
each little roundel on the star is one rune. Area spells also mark the ground where they'll land. See
[Magic Circles](spellcraft/magic-circles.md#telegraphs).

### Spells keep killing me in one hit. How do I survive them?
Wear armour: it now counts against every spell, for a little over half its worth against pure magic, frost and
sonic booms (full netherite takes 35 to 40% off an everyday spell). Put **Warding** on it, an armour enchantment that
takes 8% off spells per level, up to 80% (it can't share a piece with Protection), and drink a **Potion of Warding**
(Awkward Potion + Tinted Glass) before a boss for 20% more. And you always have a **spellguard**: one spell can't take
you from 80% of your health or more straight to dead, it leaves you on one heart instead, then takes a minute to come
back. See [Defending Against Magic](progression/defence.md).

### I can't see ley lines.
You need to be wearing a Cord, and to be in the Overworld. They're thin ribbons of violet light running along
the ground. See [Ley Lines](progression/ley-lines.md).

### Why do my spells hit harder some nights?
The sky favours some elements: a clear full moon strengthens arcane and void, a new moon blood, the noon sun fire,
dawn and dusk time, rain frost and a thunderstorm storm. Lines over your spell panel say why for a few seconds after
something comes into force, and the Grimoire page keeps them. See
[Places and Times of Power](world/places-of-power.md).

### What's a ley crossing, and where do I find one?
Where two ley lines cross: every spell is 10% stronger and 10% cheaper there. Wearing a Cord you'll see it shimmer, with
rings of light on the ground and a faint column over it, from about 50 blocks. Follow a ley line until another crosses
it; they're a few hundred blocks apart. See [Places and Times of Power](world/places-of-power.md).

### Where are the dungeons?
The Archive is buried under the Overworld (Torn Pages sketch the way to the nearest one); the Ember Sanctum is
in the Nether, the Astral Observatory in the End, and the Drowned Scriptorium on the deep sea floor. See
[The World](world/index.md).

### Does the Cord screen pause the game?
No, not even in single player, so find a safe spot before you rebuild your spells mid-fight. (Every key can be
changed in Options, Controls, Key Binds, under **Wildercord**: see [Controls](controls.md).)

### Does Wildercord work with shaders?
Yes, with Iris (and Sodium). While a shader pack is on, magic is drawn the way the game draws its own glowing
particles, so the pack lights it properly: no squares, smears or oddly lit patches where a spell was. Light looks a
little softer than without a pack, void magic is a deep shade of its colour instead of a hole in the light, and
glows (spell light, a wisp's halo, your Cord's beads) cast no shadows. Nothing needs switching on; without a pack
everything looks as it always has.

## For server owners

### What can a server change?
Wildercord's server settings live in `config/wildercord.json` in the server's folder (in single player, the game's
own `config` folder), written with every default the first time the server starts. Change a value, then have an operator run `/wildercord reload`: it reads the file again
and says what, if anything, was wrong with it. A missing or broken value falls back to its default, so a bad edit
never stops the server.

| Section | What it changes |
|---|---|
| `casting` | How many creatures and blocks one cast may touch, whether spells may change blocks at all, and how hard spells hit other players (60% at first) |
| `mana` | How fast mana comes back, and what every spell and passive costs |
| `world` | How often monsters spawn as Runebound |
| `loot` | How often runes, Mana Crystals, Torn Pages and casting gear turn up in chests (these apply the next time the world loads, or after `/reload`) |
| `imbuing` | How many imbued items and glyphs one caster keeps |
| `features` | Switches for whole features: world events, duels, wild magic, magic that changes the world, creature affinities and elemental climate (all on at first) |
| `travel` | The travel commands: whether they exist at all, how many homes each player may have, the warmup, the cooldowns, how far `/rtp` goes and how long a teleport request waits. See [Getting Around](social/travel.md#for-server-owners) |
| `defence` | How players stand up to spells: whether the spellguard is on, how much health it needs (80% at first) and how long it takes to come back (a minute), how far a spell's bonuses may multiply it against a player (two and a half times), and how much armour counts against magic (a little over half). See [Defending Against Magic](progression/defence.md) |
| `mastery` | Spell mastery: whether spells grow at all and how fast, whether the traits players chose take effect, whether a named spell's name is spoken to players nearby, and whether spells can be inscribed onto scrolls. See [Spell Mastery](spellcraft/mastery.md) |
| `channeling` | Casting as a performance: whether a charge can overchannel at all, how much power and surge chance each stage adds and how much spare mana it drains, the bonus for letting go on the beat, how long tearing loose dazes a caster and how much mana it burns, and whether sigil tracing counts and how much power it adds. Against players, whatever a performance adds counts inside the defence bonus limit. See [Overchannel](spellcraft/casting.md#overchannel-holding-on-past-full) |
| `residues` | Whether big magic leaves residues at all, how much a spell must cost to leave one (30 mana at first), how long they last, and how many a chunk and a dimension hold. See [Residues and Reagents](world/residues.md) |
| `places_of_power` | Whether ley crossings make spells stronger and cheaper and by how much (10% at first), and whether the moon, the hour and the rain favour elements and how strongly. See [Places and Times of Power](world/places-of-power.md) |

A world's own magic has settings of its own as well: a server can switch its harmonies and quirks off, choose
how many harmonies a world holds, draw the world a fresh set (forgetting the old ones and who found them), keep finds
out of chat, and switch reading runes off so every rune is understood the moment it's learned. See
[Harmonies and Reading Runes](spellcraft/harmonies.md#for-server-owners).


### Will players flying with Soar be kicked if flying is turned off?
No. A player soaring on [Soar](runes/effects/wind.md#soar) is allowed to fly, as a creative
player is, so the server's check for players floating in the air leaves them alone, and the gentle fall at the end is
quick enough not to count. Nothing about Soar lingers either: a player who logs out mid-flight is saved unable to fly
(what was left is given back when they return), and a death, a crash or a trip to another dimension never leaves
anyone able to fly.

### I updated Wildercord. Do I need a new settings file?
No. When the server starts (or an operator runs `/wildercord reload`), any setting the file lacks is added to it at
its default, and everything you've already set is kept. So an older file gains the newer settings, such as the
`travel`, `defence` and `channeling` sections and the creature affinity and climate switches, by itself. (A file you've written comments in is left
as it is, so the comments aren't lost; the settings it lacks still run at their defaults.)
