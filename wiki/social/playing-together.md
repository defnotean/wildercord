---
title: Playing Together
parent: Friends and Rivals
nav_order: 5
---

# Playing together

Everything that happens when casters meet: sharing spells, fighting side by side, and fighting each other.

## Sharing spells

There are four ways to give someone a spell:

| Way | What they get | Do they need the runes? | Do they need a Cord? |
|---|---|---|---|
| [A spell code](#spell-codes) | The recipe, to thread themselves | Yes: only runes they know load | Yes |
| [A Spell Scroll](#spell-scrolls) | One cast of your spell | No | No |
| [A Knot]({{ '/fusion-altar/knots/' | relative_url }}) | Your whole spell as one rune, for good | No | Yes, one that holds the Knot's tier |
| The runes themselves | Runes to learn | They learn them from the items | Yes |

### Spell codes

Every spell can be written as a short **spell code** that starts with `wc:`, such as `wc:bolt.frost.split` (Bolt,
Frost, Split). Codes hold up to 12 runes.

**To share a spell:**

1. Open the Cord screen, pick the spell, and press **Copy spell code** (one of the small buttons by the readout). The
   code goes to your clipboard.
2. Paste it into chat. Everyone sees it turn into a **spell card**: *[✦ Splitting Frost Bolt]* in the spell's colour.
3. Anyone can **hover** the card to read the spell: its name, its runes in order, its mana cost and cooldown, and the
   readout explaining what it does (as if every rune were rank I).
4. **Click** the card to copy the code.

**To load a spell from a code:** copy the code (from chat, a website, anywhere), open the Cord screen, pick the spell
row you want to replace, and press **Paste spell code**. The row is replaced with the code's runes, keeping only the
runes **you know** that **your Cord can hold**, as many as your Cord has sockets for. If anything was left out, you're
told how many: *Loaded, but 2 rune(s) left out: not known, too strong, or no socket free*.

- A code carries runes, not ranks, names or Knots. A [Knot]({{ '/fusion-altar/knots/' | relative_url }}) doesn't
  survive in a code: give the Knot itself instead.
- More on the Cord screen's tools: [The Cord Screen]({{ '/spellcraft/cord-screen/' | relative_url }}).

### Spell Scrolls

A **Spell Scroll** is one of your spells written on paper. **Anyone** can cast it **once**, with or without a Cord,
whether or not they know its runes.

**Inscribing a scroll:**

1. Open the Cord screen, pick the spell, and press **Inscribe a scroll**.
2. It takes **a sheet of Paper** and **an Ink Sac** (a Glow Ink Sac works too) from your inventory, and **twice what
   the spell costs you to cast** (at least 2), as the Cord screen shows it: your discounts, the server's own cost
   setting and a found secret spell's higher price all count. Creative mode needs nothing.
3. The scroll appears in your inventory (or at your feet if it's full), named after the spell: *Scroll of Splitting
   Frost Bolt*.

The scroll holds the spell as your Cord casts it right now, leaving out any quiet runes. An empty spell, or one that
doesn't do anything, can't be inscribed. Your first scroll earns the **Scribe** feat and advancement.

**Reading a scroll:** hold it and right-click. The spell's magic circle opens and the spell goes off from you, and
the scroll is used up.

- **No mana, no Cord, no runes needed.** The scroll carries everything.
- **Base strength.** A scroll's spell goes off at its plain strength: the reader's Heart Circles, affinities and casting
  gear don't add to it.
- **One at a time.** After reading a scroll you wait a second before you can read another.
- **Its tooltip** shows the spell's readout, *Inscribed by (player)*, and *Right-click to cast it once. No Cord
  needed*. Inscribed scrolls shimmer. Scrolls stack to 16.
- A scroll that holds a [secret spell]({{ '/spellcraft/secret-spells/' | relative_url }}) casts the secret spell.
- The reader is the caster: the friendly fire rules below are the reader's.

A [Runesmith]({{ '/social/runesmith/' | relative_url }}) sells paper (Apprentice) and ink sacs (Journeyman).

## Fighting side by side

### Unison

Strike a foe with **a different element** from another player within **a second** of them, and your hit lands a
**Unison**:

- that hit deals **50% more damage**;
- both elements' colours burst from the target together: two shells of light on crossed tilts, crescents of each
  colour and two circles turning against each other underneath;
- **both** of you see *Unison!*, and both earn the **Unison** feat (and advancement).

The rules:

- Two **different players**, each casting a spell with an element.
- **Different elements**: Fire then Frost is a Unison; Fire then Fire isn't.
- **The same foe**, within **one second** of the other's hit.
- **Not on players.** A Unison never lands on a player.
- You don't need to be on the same team.
- After a Unison, the next one needs a fresh pair of hits.

Unison is about elements; a [chorus]({{ '/social/chorus/' | relative_url }}) is about casting the same shape at the
same moment. They're separate bonuses.

### Chorus casting

Cast the same shape at the same target as your allies, each within a second of the one before, and the last spell
sings for all of you: up to twice as strong. See [Chorus Casting]({{ '/social/chorus/' | relative_url }}).

## Casters against casters

### Domain clashes

<img src="{{ '/assets/images/domain-clash.jpg' | relative_url }}" alt="A caster stands inside a dome of white light where two Domains meet, the words Domain clash! on screen and magic circles across the ground" class="shot">
<span class="caption">Two Domains meet: *Domain clash!*</span>

Two casters' **Domains** can't overlap. When a Domain opens into another caster's Domain, friend or foe:

1. Both casters see **Domain clash!** Where the two shells meet, each Domain's circle stands against the other's and
   lightning of both colours crackles up and down the front.
2. For about a second and a half the two push against each other.
3. The **weaker Domain shatters like glass**: its dome falls in on itself in light, shards rain down, and its spell
   ends. Its caster reads *Your domain shatters*; the winner reads *Your domain holds* and earns the **Domain Clash**
   feat (and advancement).

**Which Domain is stronger:**

- **Its power:** everything that strengthens the cast (Heart Circles, enchantments, charge) and the Domain's own
  modifiers (each Focus adds half again, each Vow doubles it).
- **Your working Heart Circles:** each circle that isn't cracked adds 10%. A monster's Domain counts as if it had four.
- **Its size:** a bigger Domain is stronger, though not in proportion (four times the radius is twice the strength).
- **A tie goes to the Domain that was there first.**

See the Domain rune on [Shapes]({{ '/runes/shapes/' | relative_url }}), and [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}).

### Shooting spells out of the air

A **Bolt** or an **Arc** that meets an **enemy** caster's Bolt or Arc in flight collides with it, and **both** burst:

| The two bolts | What happens where they meet |
|---|---|
| The same element | Both burst, harmlessly |
| Different elements | A blast of **5 damage** to enemies within 2.5 blocks |
| A reacting pair | A named reaction: **8 damage** to enemies within 4 blocks |

The reacting pairs are **Fire and Frost** (Shatter), **Frost and Storm** (Conduct), **Fire and Wind** (Wildfire),
**Void and Arcane** (Implode), **Fire and Storm** (Overload), **Earth and Frost** (Fracture), **Life and Void** (Blight),
**Blood and Wind** (Rupture) and **Fire and Time** (Elapse). The damage is the player's (it grows with the power of their spell) and only hurts what
they could harm anyway.

- "Enemy" means a caster your spell could harm: a spellcasting monster such as a
  [Runebound]({{ '/world/runebound/' | relative_url }}), or another player when PvP is on or you're duelling. Allies'
  bolts pass through each other.
- You see *Collision!* above your hotbar (or the reaction's name, for a reacting pair), and earn the
  **Spell Collision** feat (and advancement). A reacting collision also counts as setting off that reaction, for your
  Grimoire's list of reactions.

For stopping spells aimed at you with a Shield, see [Shields and Parrying]({{ '/spellcraft/shields/' | relative_url }}).

## Friendly fire and PvP

Wildercord's friendly fire is off by design. Every spell checks who it's allowed to touch.

### Who your spells can harm

Harmful effects (damage, slowing, pushing and so on) **never** touch:

- **you** (except movement effects on a Self spell, which move you on purpose),
- **your own tamed pets**,
- **anyone on your scoreboard team**, and their pets,
- **players in creative or spectator mode**,
- **armour stands**,
- **other players at all while PvP is off** on the server, **nor their pets**.

Everything else can be harmed: monsters, animals, and other players and their pets when PvP is on. Another player's
pets are as safe as the player: with PvP off your spells pass by their wolves, cats, parrots, horses and summoned
spirit wolves, even while their owner is away.

### Who your spells can help

Helpful effects (healing, shields, buffs) only land on **you**, **your own pets** and **your teammates** (and their
pets).

{: .note }
To heal, shield or buff a friend with your spells, you need to be on the **same team**. On most servers an operator
sets teams up with Minecraft's own team command. Without a team, a Heal aimed at a friend does nothing to them.

### Player against player

- **PvP on:** your harmful spells can hit other players, and theirs can hit you.
- **Spell damage between players is scaled down:** by default a spell hits a player for 60% of what it would do to a
  monster. Some effects also hold players for less time (the rune descriptions say so, for example *1 second on
  players*).
- **Duels** override all of this for the two duellists: they can hurt each other even with PvP off, and can't harm
  anyone else while it lasts. See [Duels]({{ '/social/duels/' | relative_url }}).

### Monsters' spells

A spellcasting monster's spells hit players, their pets, golems and whatever it's hunting, but never other monsters.
Players can't be hurt by them in creative or spectator mode.

### Familiars

Your [familiar]({{ '/companions/familiars/' | relative_url }}) follows the same rules: it only harms what you're
fighting and what you'd be allowed to harm, so it never attacks another player unless the two of you are fighting and
PvP is on (or you're duelling).

### Who counts as allied for what

| | Needs |
|---|---|
| Helpful spells land on them | Your team (and their pets), or your own pet |
| Harmful spells avoid them | Your team (and their pets), your own pet, or PvP off (for other players and their pets) |
| [Chorus]({{ '/social/chorus/' | relative_url }}) together | Your team, or neither of you able to harm the other (PvP off), and not duelling |
| [Unison](#unison) together | Any two players |
| [Domain clash](#domain-clashes) | Any two casters, friend or foe |
| [Spell collision](#shooting-spells-out-of-the-air) | Casters who could harm each other |
