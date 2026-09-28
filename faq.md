---
title: FAQ
nav_order: 13
description: "Answers to the questions new and experienced Wildercord players ask most."
---

# Frequently asked questions
{: .no_toc }

1. TOC
{:toc}

---

## Getting going

### How do I start?
Craft **Blank Runes** (4 Cobblestone around 1 Lapis Lazuli, makes 4), then a **Twine Cord** (3 String over a
Blank Rune), and put the Cord in the new slot just above your offhand. You learn Self, Bolt and Push at once,
and spell 1 is threaded for you. Tap `R` to cast it. The whole walk-through is on
[Getting Started]({{ '/getting-started/' | relative_url }}).

### Where's the Cord slot?
In your inventory (`E`), **just above the offhand slot**, with the faint outline of a Cord in it. Shift-click a
Cord and it goes there by itself. In creative it's in the Survival Inventory tab, beside your armour. See
[Cords]({{ '/spellcraft/cords/' | relative_url }}#the-cord-slot).

### Does the mod need to be on the server too?
Yes. Wildercord has to be installed on the server and on every player's game, with Fabric API.

### I pressed `R` and nothing happened.
Look just above your hotbar: the game always says why.

- "Wear a Cord first" means the Cord slot is empty.
- "Spell 1 is empty. Press K to thread runes" means the spell has no runes that can fire (quiet runes don't
  count).
- "Recharging... 0.4s" means it's still on cooldown.
- "Not enough mana (12/29)" means just that. Wait, or meditate.

See [Casting]({{ '/spellcraft/casting/' | relative_url }}#pressing-the-cast-key).

### How do I learn a rune?
Hold the rune and use it (right-click). It's used up and you know it for good: it's in your Codex, and you
can thread it into every spell, as many times as you like. You don't have to wear a Cord to learn one. If you
already know it, nothing is used up.

### Where are the rune recipes?
In your recipe book, as soon as you've held a Blank Rune. Every rune up to Tier III can be crafted from a
Blank Rune and a few items, in any layout. Every rune's tooltip also says how to craft it and where it's found.
The full list is on [Rune Recipes]({{ '/items/rune-recipes/' | relative_url }}).

### Which runes can't be crafted?
Tier IV runes (from bosses, dungeon vaults and rare structures), the 51
[runes of the world]({{ '/runes/world/' | relative_url }}) (each found only in its own places), the 55
[fused runes]({{ '/runes/fused/' | relative_url }}) (made at the Fusion Altar) and the ten
[innate runes]({{ '/runes/innate/' | relative_url }}) (one wakes in your heart).

### What do I do with a rune I already know?
Keep it for the [Fusion Altar]({{ '/fusion-altar/' | relative_url }}), where three of a rune make its next
rank; sell it to a [Runesmith]({{ '/social/runesmith/' | relative_url }}), or swap two for one you don't know;
or give it to a friend.

## Building spells

### Does the order of runes matter?
Yes, completely. The Cord reads left to right. A modifier changes the closest rune **on its left** that it can
change, so `Bolt · Fire · Amplify` makes the Fire stronger, while `Bolt · Amplify · Fire` does nothing extra
(a Bolt has no power to amplify). See [How a Spell Is Read]({{ '/spellcraft/reading-spells/' | relative_url }}).

### What do the gold lines under my runes mean?
They show what each modifier is attached to. A modifier with a small red mark instead is attached to nothing,
and the readout says "... does nothing here". Move it to the right of the rune you meant.

### Why is one of my threaded runes marked red?
It's **quiet**: kept, but it won't fire. Hover it to see why: it's too strong for your Cord, it's past your
Cord's last socket, you haven't learned it, or it's from an add-on mod that isn't installed. See
[Quiet runes]({{ '/spellcraft/cords/' | relative_url }}#quiet-runes-and-a-smaller-cord).

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
impact. See [Links and segments]({{ '/spellcraft/reading-spells/' | relative_url }}#links-and-segments).

### How is a spell's mana cost worked out?
Each shape and effect has a base cost, the shape multiplies its effects, modifiers multiply what they're
attached to, and links add their own cost and everything after them. The readout always shows your exact
price. The full working is on [Cost]({{ '/spellcraft/reading-spells/' | relative_url }}#cost).

### Why does my spell cost less (or more) than my friend's?
The price you see is yours: Thrift on your Cord, the 8th Heart Circle's Archmage perk, a staff or Focus of
Thrift in your hands and a mana storm overhead all make spells cheaper. Your cooldown differs too, with
Celerity and the 5th Circle's Flow.

### How long is a cooldown?
About a second for every 20 mana of the spell's plain cost, never less than half a second and never more than
20 seconds. Rapid halves it, Vow makes it four times longer, and every spell has its own cooldown. See
[Cooldown]({{ '/spellcraft/reading-spells/' | relative_url }}#cooldown).

### My spell says it "can't be cast".
It costs more than your whole mana pool. Trim it, grow your mana (see [Mana]({{ '/progression/mana/' | relative_url }})),
or [overcast]({{ '/spellcraft/overcasting/' | relative_url }}) it, which only works up to twice your pool.

### How do I share a spell with a friend?
Three ways: copy its **spell code** from the Cord screen and paste it in chat (everyone sees a spell card they
can click to copy); **inscribe a scroll** anyone can cast once; or tie it into a **Knot** at the Fusion
Altar, which anyone can learn even without the runes inside. See
[Playing Together]({{ '/social/playing-together/' | relative_url }}) and [Knots]({{ '/fusion-altar/knots/' | relative_url }}).

### I pasted a spell code but some runes were left out.
The Cord screen only loads runes you know and your Cord can fire, up to your Cord's sockets. It tells you how
many it left out. Learn those runes (or upgrade your Cord) and paste again.

## Casting

### Should I tap or hold `R`?
Tap for speed, hold for power. A full charge takes a second and a half and adds 40% power, but you walk slower
while charging. While you charge, a ring or dotted line shows where the spell will go. See
[Tap or charge]({{ '/spellcraft/casting/' | relative_url }}#tap-or-charge).

### My charge won't start.
A charge can't start while the spell is cooling down, with an empty spell, or without a Cord. And a charge
held longer than 12 seconds fizzles.

### How do I switch spells quickly?
Tap `V` for the next spell, or hold it for the spell wheel. Or bind the "Cast spell 1" to "Cast spell 5" keys
in the Controls options to cast any spell directly. See [Switching spells]({{ '/spellcraft/casting/' | relative_url }}#switching-spells).

### How do I get more mana?
Better Cords, Heart Circles, Mana Crystals (+10 max mana each, up to 10), the Reservoir and Wellspring
enchantments, Clarity and Mana potions, meditating (sneak and stand still), ley lines and Wellstones. Hover
the mana badge in the Cord screen for exactly where yours comes from. See [Mana]({{ '/progression/mana/' | relative_url }}).

### Can I cast without enough mana?
Two ways. **Overcast**: press again within two seconds and your outermost Heart Circle cracks to pay (it mends
in 3 minutes). Or thread **Blood Price** and pay in health instead of mana. See
[Overcasting and wild magic]({{ '/spellcraft/overcasting/' | relative_url }}).

### Can my spells hurt my friends or my pets?
No. Harmful effects never touch you, your tamed pets or players on your team, and other players only when
the server allows PvP (and then for 60% damage, unless the server changes it).

### Why won't my Heal heal my friend?
Helpful spells only reach **you and your allies**, and another player is only your ally when you're on the
**same team** (the game's team command, which an operator can set up). Pets you've tamed always count. See
[friendly fire]({{ '/spellcraft/reading-spells/' | relative_url }}#friendly-fire-who-a-spell-touches).

### Will spells break blocks or grief my base?
Spells only change blocks where you're allowed to build: spawn protection and claim mods are respected, and a
server can turn block-changing spells off entirely. Monsters' spells never change blocks. Fire from a spell
only spreads where fire spreads anyway, and frozen water always thaws. See
[World Magic]({{ '/world/world-magic/' | relative_url }}).

## Cords and progress

### Do I lose anything when I die?
Your Cord, your learned runes, your spells, your Heart Circles and your cooldowns are all kept; the Cord is
never dropped. You come back with an empty mana pool, which refills as usual.

### Do I lose my spells when I change Cords?
No. Spells and runes are saved on you, not on the Cord. A better Cord just opens more sockets and spell rows
(enchantments on the old Cord don't carry over, though). A smaller Cord deletes nothing either: runes past its
sockets, rows it doesn't have and runes too strong for it stay threaded but quiet, and wake again when you put
the bigger Cord back on. See [Upgrading]({{ '/spellcraft/cords/' | relative_url }}#upgrading).

### How do I get passive spells?
Form your 1st Heart Circle for the first passive slot, and your 5th for the second. Thread them on the Cord
screen's Passives page. See [Passive Spells]({{ '/spellcraft/passives/' | relative_url }}).

### What's my innate rune, and how do I get it?
One of ten runes that wakes in your heart, chosen at random, when you form your 1st Heart Circle. It can't be
crafted, found or learned from an item, it's Tier I so any Cord holds it, and it grows stronger with every
circle. See [Innate Runes]({{ '/runes/innate/' | relative_url }}).

### How do I find secret spells?
Ten exact rune sequences become something grander. Nothing lists them: experiment, or read the riddles on
**Torn Pages** (found in old chests and dropped by Runebound). Once you've cast one, it's in your Grimoire.
See [Secret Spells]({{ '/spellcraft/secret-spells/' | relative_url }}).

## The world

### Some monsters are glowing and casting spells.
They're **Runebound**: monsters that carry a Cord. Their nameplate is their spell, and before every cast they
hold out its magic circle for about a second. They drop runes and Torn Pages. See
[Runebound]({{ '/world/runebound/' | relative_url }}).

### How can I tell what a monster is about to cast?
Read its circle: the colour is the element, the seal in the middle is the shape, and each little roundel on the
star is one rune. Area spells also mark the ground where they'll land. See
[Magic Circles]({{ '/spellcraft/magic-circles/' | relative_url }}#telegraphs).

### I can't see ley lines.
You need to be wearing a Cord, and to be in the Overworld. They're thin ribbons of violet light running along
the ground. See [Ley Lines]({{ '/progression/ley-lines/' | relative_url }}).

### Where are the dungeons?
The Archive is buried under the Overworld (Torn Pages sketch the way to the nearest one); the Ember Sanctum is
in the Nether, the Astral Observatory in the End, and the Drowned Scriptorium on the deep sea floor. See
[The World]({{ '/world/' | relative_url }}).

### Does the Cord screen pause the game?
No, not even in single player, so find a safe spot before you rebuild your spells mid-fight. (Every key can be
changed in Options, Controls, Key Binds, under **Wildercord**: see [Controls]({{ '/controls/' | relative_url }}).)
