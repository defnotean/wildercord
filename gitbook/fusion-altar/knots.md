# Knots

A **Knot** is a whole spell tied into a single rune. It takes **one socket**, costs **10% less mana** than the runes
inside it, and **anyone can learn it**, even someone who doesn't know a single rune inside. Knots are how you hand a
friend your signature spell, and how you fit a long spell into a short Cord row.

![The tooltip of a Knot called Ember Fan: Knot, Tier III, a whole spell tied into one rune (4 runes): Bolt, Fire, Amplify, Split; takes one socket and costs 10% less mana than the runes inside; needs an Amethyst Cord or better; right-click to learn it](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/d-tooltip-knot.jpg)
<span>A Knot's tooltip shows the spell inside, rune by rune</span>

## Tying a Knot

You need a [Fusion Altar](index.md), a **Blank Rune**, a piece of **String** and the
spell already threaded on your Cord.

1. Put the **Blank Rune** in any one of the three rune sockets, and nothing else in the other two.
2. Put the **String** in the middle socket.
3. The panel reads **Tie a Knot: pick a spell** and lists your spells 1 to 4 by name. Spells that can't be tied are
   greyed out. Hover a spell to see its runes and what it would cost, or why it can't be tied.
4. Click the spell you want, then press **Tie Knot**.

![The Fusion Altar screen with a Blank Rune and string on it; the panel lists spells 1 Ember Fan, 2 Chained Lightning, 3 Heal and Swift, 4 empty, and says costs 4 XP levels](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/d-altar-knot.jpg)
<span>Picking a spell to tie</span>

The Blank Rune and the string are used up, and the Knot waits in the result socket.

- **What goes in** is the spell as your Cord casts it right now. Runes that sit past your Cord's last socket, or that
  are too strong for your Cord, are quiet on the Cord screen, and they're left out of the Knot too.
- **Its name** is the spell's name. If you renamed the spell on the Cord screen, the Knot keeps your name (the item is
  called *Knot: Ember Fan*); otherwise it takes the spell's automatic name.
- **Only spells 1 to 4** can be tied. The fifth spell from the [Tome of the Fifth Page](../gear.md#the-tome-of-the-fifth-page)
  isn't on the list.
- **Tying a Knot doesn't teach it to you.** Right-click it to learn it, like anyone else.

## What it costs

Tying a Knot costs **1 XP level for every rune inside**, counting the runes inside any Knots it holds, and never less
than **2 levels**. Creative mode is free.

| The spell | Runes counted | XP levels |
|---|---|---|
| Self · Heal | 2 | 2 |
| Bolt · Fire · Amplify · Split | 4 | 4 |
| An Echo Cord's full row of 12 runes | 12 | 12 |
| Bolt · Fire · and a Knot holding 5 runes | 7 | 7 |

## What a Knot does

- **One socket.** However many runes are inside, the Knot fills one socket. On an Echo Cord you could thread a Knot of
  twelve runes and still have eleven sockets left.
- **10% cheaper.** Every rune inside costs 90% of its usual mana. A spell's cooldown follows its cost, so a Knot
  usually recharges a little sooner too.
- **It reads as if it were written out.** Apart from modifiers (below), a Knot's runes work exactly as if you had
  threaded them one by one in its place. The Cord screen's readout explains the spell in full and adds a line such as
  *Ember Fan is a Knot: 4 runes in one socket, 10% less mana.*
- **It's sealed.** A modifier inside a Knot only changes the Knot's own runes, and a modifier outside can't reach in.
  An Amplify after a Knot won't boost the Fire inside it.
- **Its tier is the highest tier inside.** A Knot holding one Tier III rune is a Tier III Knot and needs an Amethyst
  Cord or better, just as the runes inside would. Its tooltip says which Cord it needs.
- **Ranks come from the caster.** A Knot holds runes, not ranks. Whoever casts it uses their own
  [ranks](ranks.md) for the runes inside.
- **Its elements count for affinities, staffs and contracts.** The effects inside count toward your affinities
  and the element [contracts](../social/contracts.md), and a [staff](../gear.md)
  of their element boosts them, just as if they were threaded one by one.

## Knots inside Knots

A Knot can hold other Knots, **2 deep**: a Knot inside a Knot is fine, but not a Knot inside a Knot inside a Knot.

Each Knot around a rune takes another 10% off, so the runes of a Knot inside a Knot cost 81% of their usual mana
(**19% off** in all).

For example, name a spell *Self · Heal* "Healing" and tie it into a Knot. Learn it, thread it after *Bolt · Fire* in
another spell, and tie that spell too. In the Codex the new Knot reads *Holds: Bolt · Fire · (Healing: Self · Heal)*,
and the item's tooltip lists Bolt, Fire and Healing one per line, with Self and Heal indented under Healing. It holds
4 runes in all, so it cost 4 XP levels to tie.

## Learning and sharing Knots

- **Right-click a Knot to learn it.** Anyone can, even without knowing any of the runes inside. Once learned it's in
  your Codex under **All**, and you thread it like any rune.
- **The tooltip shows the whole spell** rune by rune, how many runes it holds, the socket and mana rule, the Cord it
  needs, and *Right-click to learn it: anyone can, even without knowing the runes inside*.
- **Knots stack** up to 16. Tie as many copies as you like (each costs a Blank Rune, a string and the XP) to hand out
  to a group.
- **You can't learn the same Knot twice.** The game tells you that you already know it, and the Knot isn't used up.
- **Knots aren't runes** for counting purposes: learning one doesn't count toward the number of runes you know for
  Heart Circle breakthroughs or advancements.

## Limits

| You can't | Why |
|---|---|
| Tie an empty spell, or one that doesn't do anything | *There's nothing in that spell to tie*, or *That spell doesn't do anything yet* |
| Tie more than 12 runes directly into one Knot | *A Knot holds 12 runes at most* (a full Echo Cord row) |
| Tie a spell with Imbue in it | A stored spell can't be tied up again. See [Imbuing](../spellcraft/imbuing.md). |
| Tie a spell with your innate rune in it, even inside a Knot it holds | Your [innate rune](../runes/innate.md) is yours alone, and a Knot would hand it to whoever learns it |
| Nest Knots more than 2 deep | *Knots can only hold Knots 2 deep* |
| Use a Knot in a passive | [Passives](../spellcraft/passives.md) take no Knots |
| Put a Knot on the Fusion Altar | Knots can't be ranked up or combined |
| Sell or swap a Knot with a Runesmith | The [Runesmith](../social/runesmith.md) only deals in plain runes |
| Tie the tome's fifth spell | Only spells 1 to 4 are listed |

## Ideas

- **A signature spell for a friend.** Tie your best spell, hand over the Knot, and they can cast it tomorrow without
  learning a single rune in it (as long as their Cord can hold its tier).
- **Room on a small Cord.** A Copper Cord has five sockets per spell. Tie a four-rune combo and thread it with four
  more runes around it.
- **A cheaper favourite.** Even alone in a spell, a Knot costs 10% less than the same runes threaded loose.
