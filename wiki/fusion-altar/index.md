---
title: The Fusion Altar
nav_order: 6
has_children: true
permalink: /fusion-altar/
---

# The Fusion Altar

Once you've learned a rune, every spare copy you find is just a tablet. The **Fusion Altar** turns those
spares into something: a stronger rank of a rune you know, a brand new fused rune, or a whole spell tied into a
single rune.

<img src="{{ '/assets/images/fusion-altar.jpg' | relative_url }}" alt="A purple and black altar on the grass, wrapped in rings of golden light as a fusion finishes" class="shot">
<span class="caption">The altar's flourish as a fusion finishes</span>

## Making the altar

The Fusion Altar is crafted, not found. Lay it out like this in a crafting table:

{% include recipe.html id="fusion_altar" alt="Crafting grid: top row Deepslate Tiles · Block of Amethyst · Deepslate Tiles; middle row Block of Amethyst · Lodestone · Block of Amethyst; bottom row Deepslate Tiles · Block of Amethyst · Deepslate Tiles" %}

That's 4 Deepslate Tiles in the corners, 4 Blocks of Amethyst on the sides and a Lodestone in the middle. It makes one
altar.

- Place it anywhere. It glows softly (light level 7) and hums when you open it.
- Mine it with a pickaxe to take it with you. Broken without a pickaxe, it drops nothing.
- The altar keeps nothing inside it, so any altar works for anyone, and you can move it freely.

## The three fusions

The altar works out what you mean from what you put on it. There's no mode to pick.

| Put in the three rune sockets | Put in the middle socket | You get | It costs | Read more |
|---|---|---|---|---|
| Three copies of the same rune, all at the same rank | nothing | That rune, one rank higher | 2 XP levels for rank II, 5 for rank III | [Ranks]({{ '/fusion-altar/ranks/' | relative_url }}) |
| Two effects (the third socket empty) | an Amethyst Shard | A fused rune, chosen by the two effects' elements; or, for sixteen particular pairs of runes, their own **signature** rune | 3 XP levels | [Combining]({{ '/fusion-altar/combining/' | relative_url }}) |
| One Blank Rune, and nothing else | String | A Knot: one of your spells tied into a single rune | 1 XP level per rune inside, at least 2 | [Knots]({{ '/fusion-altar/knots/' | relative_url }}) |

**Signature fusions** are combines too: two particular effects that make a rune of their own instead of their elements'
fusion (Chill and Shock make Frostwire, where any other Frost and Storm effects make Hail). The panel says **Signature
fusion** when the two on the altar are such a pair. See
[Signature fusions]({{ '/fusion-altar/combining/' | relative_url }}#signature-fusions).

## Using the altar

Right-click the altar to open it. On the left, three **rune sockets** sit on the points of a triangle inside a
magic circle, with the **middle socket** (the catalyst) at its centre. The **result socket** is at the top, with the
button beside it, and the panel under the button explains what's going on. Your XP level is shown in the top right
corner.

<img src="{{ '/assets/images/d-altar-upgrade.jpg' | relative_url }}" alt="The Fusion Altar screen: three Fire runes on the circle, and the panel reading Upgrade, Fire II, +25% power at the same mana, costs 2 XP levels" class="shot">
<span class="caption">Three Fire runes on the altar. The panel says what they'll make and what it costs.</span>

- **The rune sockets** take runes and Blank Runes only. A Knot can't go on the altar at all.
- **The middle socket** takes an Amethyst Shard or String, and nothing else.
- **Shift-click** a stack of runes in your inventory and one rune drops into each empty rune socket, so three copies
  spread themselves out ready to rank up. Shift-click a shard or string and it goes into the middle socket.
- **The panel** always tells you what the altar would do: which fusion, what it makes, and what it costs in XP levels.
  If something is wrong, it says what (see below). With nothing on the altar, it lists the three fusions.
- **The button** says **Fuse** (for a rank-up or a combine) or **Tie Knot**. Nothing is used up until you press it.
  Then one of each rune in the sockets goes, and so does the shard or string, if the fusion needed one.

When a fusion finishes, the altar flares with rings of light and a burst in the new rune's colour.

### Paying for it

Every fusion is paid in **XP levels**, taken when you press the button. If you don't have enough, the button won't
work and the panel says how many you need. In creative mode, fusions are free.

### The result socket

The new rune waits in the result socket. Take it out before the next fusion: while anything is sitting there, the
altar won't fuse again and the panel says **Take the last result out first**. Nothing already in the result socket
is ever lost.

Closing the altar gives you back everything still on it (runes, the shard or string, and the result). If your
inventory is full, it drops at your feet.

### When the altar says no

| The panel says | What to do |
|---|---|
| Ranking up takes three of the same rune, at the same rank. | Use three copies of one rune, all rank I, or all rank II. |
| Ranking up takes only the three runes: take the catalyst out. | Empty the middle socket. |
| (Rune) has no power to rank up: only effects with power have ranks. | That rune can't be ranked. See [which runes can rank]({{ '/fusion-altar/ranks/' | relative_url }}#which-runes-can-rank). |
| (Rune) is already at its highest rank. | Rank III is the top. |
| Combining two effects takes an amethyst shard. | Put an Amethyst Shard in the middle socket. |
| Only effects with an element fuse. | Shapes, modifiers, links and innate runes can't be combined. |
| A Knot takes only the Blank Rune: take the other runes out. | Leave just the Blank Rune in the rune sockets. |
| Tying a Knot takes string too. | Put String in the middle socket. |
| None of your spells can be tied yet | Each of your spells is empty or breaks one of the [Knot rules]({{ '/fusion-altar/knots/' | relative_url }}#limits). |
| Add two more to rank it up, or a second effect and an amethyst shard to combine them. | You've only put one rune on. |

## Learning what you make

Whatever the altar makes is an ordinary item until you learn it. Hold it and right-click:

- A **ranked rune** raises that rune to its new rank in every spell it's threaded in.
- A **fused rune** goes into your Codex like any rune you learn.
- A **Knot** goes into your Codex under **All**, ready to thread. Anyone can learn a Knot, even someone who doesn't
  know the runes inside it.

Because they're items, you can give any of them to a friend, and it works for them just the same.

## Feats and advancements

Each kind of fusion has a feat in your [Grimoire]({{ '/progression/grimoire/' | relative_url }}) and an advancement
of the same name. A new feat condenses a little mana toward your next
[Heart Circle]({{ '/progression/heart-circles/' | relative_url }}).

| Feat | How |
|---|---|
| **Honed** | Rank up a rune at the Fusion Altar |
| **Fusion** | Fuse two effects into a new one |
| **Knotted** | Tie a whole spell into one rune |

Every fused rune you make for the first time is also written into the Grimoire's list of fusions, and condenses a
little more mana. See [Combining]({{ '/fusion-altar/combining/' | relative_url }}#the-grimoire).

Signature fusions have no feat of their own, but two advancements: **Signature** (make your first) and **Hallmarks**
(find five).

## Tips

- **Rank up before you combine.** A fused rune keeps the lower of the two ranks you put in, so two rank III effects
  make a rank III fused rune.
- **Use your cheapest spares for fusions.** Only the elements count, so a Tier I Ember does the same job as a Tier II
  Fire.
- **Try the pairs that belong together.** Runes that seem made for each other (a bubble and a flame, a heal and a
  countdown) may be a signature pair. Your Grimoire hints at each one's two elements.
- **Spare runes have other uses too.** A [Runesmith]({{ '/social/runesmith/' | relative_url }}) buys back plain runes
  you already know, or swaps two of them for one you don't.
- **Share signature spells as Knots.** One rune, one socket, 10% cheaper, and your friend doesn't need to know any
  of the runes inside.
