---
title: Playing Together
parent: Friends and Rivals
nav_order: 5
---

# Playing together

Everything that happens when casters meet: parties, sharing spells, fighting side by side, and fighting each other.

## Parties

### What it is

A **party** is a group of up to **8 players**. Party members can't hurt each other, and your helpful spells (heals,
shields, buffs) land on them.

### How to use it

| Command | What it does |
|---|---|
| `/party` or `/party list` | Shows your party |
| `/party invite <player>` | Invites a player. The invitation lasts 60 seconds. |
| `/party accept <player>` | Joins that player's party |
| `/party decline <player>` | Turns down an invitation |
| `/party leave` | Leaves your party |
| `/party kick <member>` | Removes a member (leader only) |
| `/party disband` | Ends the party (leader only) |

- Only the **leader** can invite, kick and disband. If the leader leaves, someone else becomes leader.
- You can send one invitation a second, with up to 8 waiting at once.
- Your party lasts through death and logging out, but ends when the server restarts.

### What a party does

- **No friendly fire.** Your spells, weapons, arrows and pets can't hurt party members or their pets.
- **Helpful spells reach them.** A Heal aimed at a party member works, just as it does for a teammate.
- **Chorus together.** Party members count as allies for a [chorus]({{ '/social/chorus/' | relative_url }}).
- **Shared hearths.** Party members can charge each other's [Runic Hearths]({{ '/social/runic-hearth/' | relative_url }}).
- **Duels still work.** Two party members can still agree to a [duel]({{ '/social/duels/' | relative_url }}).

### Sword Master trials

Up to **8 challengers** can fight a [Sword Master]({{ '/masters/' | relative_url }}) together. Being in a party doesn't
sign you up: each challenger joins the trial on their own. A party is still worth it, so your spells spare each other
during the fight.

## Sharing spells

| Way | What they get | Need the runes? | Need a Cord? |
|---|---|---|---|
| [A spell code](#spell-codes) | The recipe, to thread themselves | Yes: only runes they know load | Yes |
| [A Spell Scroll](#spell-scrolls) | One cast of your spell | No | No |
| [A Knot]({{ '/fusion-altar/knots/' | relative_url }}) | Your whole spell as one rune, for good | No | Yes, one that holds the Knot's tier |
| The runes themselves | Runes to learn | They learn them from the items | Yes |

### Spell codes

Every spell can be written as a short code that starts with `wc:`, such as `wc:bolt.frost.split`. A code holds up to
12 runes.

1. Open the Cord screen (**K**), pick the spell and press **Copy spell code**.
2. Paste it into chat. Everyone sees a **spell card** in the spell's colour.
3. Hover the card to read the spell. Click it to copy the code.

To load a code, copy it, open the Cord screen, pick the spell row to replace and press **Paste spell code**. Only runes
you know and your Cord can hold are loaded; you're told how many were left out.

A code carries runes only: no ranks, names or Knots. To share a Knot, give the Knot itself. See
[The Cord Screen]({{ '/spellcraft/cord-screen/' | relative_url }}).

### Spell Scrolls

A **Spell Scroll** is one of your spells on paper. **Anyone** can cast it **once**, without a Cord or the runes.

**To inscribe one:** open the Cord screen, pick the spell and press **Inscribe a scroll**. It uses a sheet of
**Paper**, an **Ink Sac** (or Glow Ink Sac) and **twice the spell's mana cost**. Creative mode needs nothing.

- **To read it,** hold it and right-click. The spell goes off from you and the scroll is used up.
- A scroll casts at plain strength. The reader's Heart Circles and gear don't add to it.
- After reading a scroll, wait 1 second before the next.
- Scrolls stack to 16. Your first scroll earns the **Scribe** feat.
- Empty spells, and spells holding Reweave, Excise or Lesson Pack runes, can't be inscribed.
- The reader is the caster, so the friendly fire rules are the reader's.

A [Runesmith]({{ '/social/runesmith/' | relative_url }}) sells paper and ink sacs.

## Fighting side by side

### Unison

Hit a foe with **a different element** within **1 second** of another player's hit, and your hit lands a **Unison**:
it deals **50% more damage**, and both of you earn the **Unison** feat.

- Two different players, two different elements, the same foe.
- A Unison never lands on a player.
- You don't need to be allies.

### Chorus casting

Cast the same shape at the same target as your allies, each within a second of the one before, and the last spell
goes off up to twice as strong. See [Chorus Casting]({{ '/social/chorus/' | relative_url }}).

## Casters against casters

### Domain clashes

<img src="{{ '/assets/images/domain-clash.jpg' | relative_url }}" alt="A caster stands inside a dome of white light where two Domains meet, the words Domain clash! on screen and magic circles across the ground" class="shot">
<span class="caption">Two Domains meet: *Domain clash!*</span>

Two **Domains** can't overlap. When one opens into another, friend or foe, they push against each other for a moment
and the **weaker one shatters**. Its spell ends. The winner earns the **Domain Clash** feat.

What makes a Domain stronger:

- **Its power:** everything that strengthens the cast, plus its own Focus and Vow runes.
- **Your Heart Circles:** each uncracked circle adds 10%.
- **Its size:** bigger is stronger, but not in proportion.
- **A tie** goes to the Domain that was there first.

See [Shapes]({{ '/runes/shapes/' | relative_url }}) and [Heart Circles]({{ '/progression/heart-circles/' | relative_url }}).

### Shooting spells out of the air

When your **Bolt** or **Arc** meets an **enemy** caster's Bolt or Arc in flight, both burst:

| The two bolts | What happens |
|---|---|
| Same element | Both burst, harmlessly |
| Different elements | **5 damage** to enemies within 2.5 blocks |
| A reacting pair | A named reaction: **8 damage** to enemies within 4 blocks |

The reacting pairs: Fire and Frost (Shatter), Frost and Storm (Conduct), Fire and Wind (Wildfire), Void and Arcane
(Implode), Fire and Storm (Overload), Earth and Frost (Fracture), Life and Void (Blight), Blood and Wind (Rupture),
Fire and Time (Elapse).

"Enemy" means a caster you could harm, like a [Runebound]({{ '/world/runebound/' | relative_url }}), or a player when
PvP is on or you're duelling. Allies' bolts pass through each other. You earn the **Spell Collision** feat.

To stop spells aimed at you, see [Shields and Parrying]({{ '/spellcraft/shields/' | relative_url }}).

## Friendly fire and PvP

### Who your spells can harm

Harmful effects **never** touch:

- **you** (except movement on a Self spell),
- **your own pets**,
- **your party and your scoreboard team**, and their pets,
- **players in Creative or Spectator mode**, and armour stands,
- **other players and their pets while PvP is off**.

Everything else can be harmed.

### Who your spells can help

Helpful effects only land on **you**, **your own pets**, **your party** and **your team** (and their pets). Without a
party or team, a Heal aimed at a friend does nothing.

### Player against player

- **PvP on:** your harmful spells can hit other players.
- **Damage is scaled down:** by default a spell hits a player for 60% of what it does to a monster.
- **Duels** override this for the two duellists, even inside a party. See [Duels]({{ '/social/duels/' | relative_url }}).

### Monsters and familiars

A spellcasting monster's spells hit players, their pets, golems and whatever it's hunting, never other monsters. Your
[familiar]({{ '/companions/familiars/' | relative_url }}) follows your rules: it only harms what you could harm.

### Who counts as allied

| | Needs |
|---|---|
| Helpful spells land on them | Your party or team (and their pets), or your own pet |
| Harmful spells avoid them | Your party or team (and their pets), your own pet, or PvP off |
| [Chorus]({{ '/social/chorus/' | relative_url }}) together | Party, team, or neither able to harm the other; not duelling |
| [Unison](#unison) | Any two players |
| [Domain clash](#domain-clashes) | Any two casters |
| [Spell collision](#shooting-spells-out-of-the-air) | Casters who could harm each other |
