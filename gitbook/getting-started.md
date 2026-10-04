# Getting Started


This page takes you from nothing to your first spell in a few minutes, then shows you where the rest of the
game is. Everything here works in survival from the very first day: you need cobblestone, a little lapis
lazuli and some string.


---

## The idea in one minute

You wear a **Cord** in a new slot of your inventory. A Cord has **sockets**, and you thread **runes** into
them. Each rune does one small thing, and the Cord reads them from left to right as a single spell:

- a **shape** says *where* the spell goes (a bolt, a burst around you, yourself),
- an **effect** says *what happens* there (fire, healing, a push),
- a **modifier** changes the rune on its left (stronger, wider, three copies),
- a **link** makes the rest of the spell happen *later* (when the bolt hits, a second later).

`Bolt · Fire` is a fire bolt. `Bolt · Fire · Split` is three of them. `Bolt · Fire · Split · On Hit · Burst · Explode`
is three fire bolts that each explode where they land. You learn a rune once and can thread it into as
many spells as you like. The full rules are on [How a Spell Is Read](spellcraft/reading-spells.md).

## Step 1: make Blank Runes

Every rune starts as a **Blank Rune**. Put **4 Cobblestone** around **1 Lapis Lazuli** in a plus shape,
in any crafting table:

![Crafting grid: top row empty · Cobblestone · empty; middle row Cobblestone · Lapis Lazuli · Cobblestone; bottom row empty · Cobblestone · empty](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/blank_rune.png)

It makes **4 Blank Runes**. The recipe appears in your recipe book as soon as you pick up lapis lazuli.
Holding a Blank Rune then unlocks the recipe for the Twine Cord and for every rune you can craft.

Keep a few spare: Blank Runes are the base of every rune recipe, the Fusion Altar uses them to tie
[Knots](fusion-altar/knots.md), and some lands will fill one with a rune of their own
(see [Runes of the World](runes/world.md)).

## Step 2: make a Twine Cord

The **Twine Cord** is the first Cord: **3 String** over a **Blank Rune**.

![Crafting grid: top row String · empty · String; middle row empty · String · empty; bottom row empty · Blank Rune · empty](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/recipes/twine_cord.png)

A Twine Cord holds **one spell** of **3 runes**, only **Tier I** runes, and gives you **100 mana** that
refills at **5 mana a second**. That's plenty to start. Better Cords come later (see
[The next Cords](#the-next-cords) below).

## Step 3: wear it

Open your inventory (`E`). There's a new slot **just above your offhand slot**, with the faint outline of a
Cord in it. Put the Twine Cord there, or shift-click the Cord and it jumps into the slot by itself. In the creative
inventory the slot sits in the Survival Inventory tab, beside your armour.

![The inventory with an Echo Cord in the Cord slot above the offhand slot, and a Fire rune's tooltip showing its family, tier, element, description, recipe and where it's found](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/a-cord-slot.jpg)
<span>The Cord slot, just above the offhand. Hover any rune for everything about it: what it does, which Cord it needs, how to craft it and where it's found.</span>

The first time you put on a Cord (any Cord), it hums and you're told:

> Your Cord hums. You learned Self, Bolt and Push, and spell 1 is ready: R casts, V switches spell, K opens your Cord.

You now know three runes, **Self**, **Bolt** and **Push**, your mana is full, and your first spell is
already threaded for you: `Bolt · Push`, a bolt that knocks back whatever it hits.

Your Cord also shows on your right wrist for everyone to see, with a bead for each rune of the spell you have
ready. The beads glow for a couple of seconds when you put the Cord on, and light up again whenever you charge
or cast. The Cord stays with you when you die: it's never dropped, and neither is anything you've
learned or threaded.

## Step 4: cast your first spell

Look at a creature and **tap `R`**. A casting circle opens behind you and a smaller focus forms ahead, a bolt flies out, and whatever
it hits is thrown back.

![The spell panel to the right of the hotbar: a badge with the spell's number, its rune icons and cost, a mana bar and the mana count](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/hud.png)
<span>The spell panel beside your hotbar: the spell's number, its runes, its mana cost, your mana bar (the gold mark shows what the spell will cost) and your mana.</span>

The panel to the right of your hotbar shows the selected spell: its number, its runes and its cost, your
mana bar and your mana. After a cast the badge darkens and fills back up as the spell recharges. Every spell
has a short **cooldown** that depends on its cost; `Bolt · Push` costs 8 mana and is ready again after half
a second. The whole panel is explained on [Casting](spellcraft/casting.md#the-hud).

If nothing happens, a line above your hotbar says why:

| Message | What it means |
|---|---|
| Wear a Cord first: the slot above your offhand (E) | No Cord in the Cord slot. |
| Spell 1 is empty. Press K to thread runes | The spell has no runes that can fire. |
| Recharging... 0.3s | It's still cooling down. |
| Not enough mana (5/8) | You're short of mana. Wait a moment, or meditate (below). |

## Step 5: hold to charge

Tap `R` and the spell goes off at once. **Hold `R`** instead and you raise both hands: the spell's magic
circle opens behind their shoulders, a ring at a time, a roundel for each rune. Let go to cast. A full charge
takes a second and a half, flares and chimes when it's ready, and makes the spell **40% stronger**. You walk
slower while you charge, and while you do, a faint line or ring shows you where the spell will go.

![A player with raised hands charging a spell, its authored magic circle behind the shoulders](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/circle-bloom.jpg)

Charging is never required. Tap for speed, charge for power. Details are on
[Casting](spellcraft/casting.md#tap-or-charge).

## Step 6: open the Cord screen

Press **`K`** to open the **Cord screen**. It's where you build spells:

- the **rows** at the top are your spells, one row of sockets each (a Twine Cord has one row of 3),
- the **Codex** below lists every rune you know, sorted by family and category,
- the **readout** at the bottom explains the selected spell in plain English, with its mana and cooldown,
- and beside the window, when there's room, the spell's **magic circle** opens again every time you change it.

To thread a rune, click it in the Codex (it goes on the end of the spell) or drag it onto a socket. Click a
threaded rune to take it off, or drag it along the row to move it. Just start typing to search the Codex.
Everything you change is saved at once.

The screen doesn't pause the game, even in single player. Press `Esc` to close it. Every part of it is
explained on [The Cord Screen](spellcraft/cord-screen.md).

## Step 7: craft and learn more runes

Three runes don't go far. Craftable runes up to Tier III are made from a **Blank Rune** and a few items
that suit it, in any layout (the recipes are shapeless). Once you've held a Blank Rune they're all in your
recipe book, and every rune's tooltip says how to craft it and where it's found.

To **learn** a rune, hold it and use it (right-click). The rune is used up and is yours for good: it goes
into your Codex and you can thread it into every spell at once, as many times as you like. You don't need to
be wearing a Cord to learn one. If you already know it, nothing is used up; keep the spare to trade to a
[Runesmith](social/runesmith.md) or to rank it up at the
[Fusion Altar](fusion-altar/index.md).

Good first runes, all Tier I (a Twine Cord can hold them):

| Rune | What it does | Craft it with a Blank Rune and |
|---|---|---|
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/ember.png) Ember | 3 fire damage and sets the target alight for 3 seconds | Coal, Flint |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/shock.png) Shock | 4 lightning damage that arcs to one more enemy nearby | Lightning Rod |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/icicle.png) Icicle | 4 freeze damage, 6 against a slowed target | Ice, Pointed Dripstone |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/heal.png) Heal | Restores 8 health (4 hearts) | Glistering Melon Slice |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/swift.png) Swift | Speed III for 10 seconds | 2 Sugar |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/feather_fall.png) Feather Fall | Slow falling and no fall damage for 12 seconds | 2 Feathers |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/barrier.png) Barrier | 2 absorption hearts for 20 seconds | Glass, Amethyst Shard |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/amplify.png) Amplify | +50% power to the effect on its left | Gold Ingot |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/extend.png) Extend | Doubles how long the effect on its left lasts | 2 Redstone |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/nova.png) Nova | Hits everything within 2.5 blocks of you | Gunpowder, Glowstone Dust |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/spark.png) Spark | A quick, cheap spark (its effects land at 75% power) | Flint, Glowstone Dust |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/touch.png) Touch | Whatever you're looking at, within reach | Leather |
| ![](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/runes/chisel.png) Chisel | Mines the block hit, like a stone pickaxe | Stone Pickaxe |

Every recipe is on [Rune Recipes](items/rune-recipes.md), and every rune has its own
entry under [Runes](runes/index.md).

## Step 8: your first real spells

With a Twine Cord you have three sockets. Here are some spells that fit, with their price:

| Spell | What it does | Mana | Cooldown |
|---|---|---|---|
| `Bolt · Ember` | A burning bolt | 10 | 0.5 s |
| `Bolt · Ember · Amplify` | The same, 50% harder | 14 | 0.7 s |
| `Heal` | Heals you (a spell that starts with an effect is cast on you) | 12 | 0.6 s |
| `Self · Swift · Extend` | Speed III for 20 seconds | 9 | 0.5 s |
| `Nova · Push` | Throws back everything within 2.5 blocks of you | 9 | 0.5 s |
| `Spark · Ember` | A cheap, quick ember | 7 | 0.5 s |
| `Touch · Chisel` | Mines the block you're looking at | 3 | 0.5 s |
| `Self · Feather Fall` | No fall damage for 12 seconds | 6 | 0.5 s |

Two things to notice:

- **Order matters.** `Bolt · Ember · Amplify` makes the Ember stronger, because Amplify changes the closest
  rune on its left that has power. `Bolt · Amplify · Ember` does nothing extra: a Bolt has no power to
  amplify, and the readout warns you with "Amplify does nothing here". Gold lines under the sockets show
  what every modifier is attached to.
- **You don't always need a shape.** A spell that begins with an effect is cast on yourself, as if it
  started with Self, so `Heal` alone heals you and leaves two sockets free.

## Mana and cooldowns

Every spell costs mana, shown in the readout and on your spell panel. The **cooldown** comes from the cost:
about a second for every 20 mana, never less than half a second and never more than twenty. Each spell has
its own cooldown, so a Cord with several spells lets you cast one while another recharges.

Your mana refills on its own. To refill it faster, **meditate**: sneak and stand still for a second while
wearing your Cord, and your mana comes back twice as fast until you move. There are many more ways to grow
your mana (Mana Crystals, Cord enchantments, potions, ley lines): see
[Mana](progression/mana.md).

## The next Cords

Each Cord is crafted from the one before it, so nothing is wasted. Your spells and runes are saved on
**you**, not on the Cord, so changing Cords never loses anything.

| Cord | Sockets per spell | Spells | Rune tiers | Mana | Mana a second | Recipe (shapeless) |
|---|---|---|---|---|---|---|
| Twine Cord | 3 | 1 | I | 100 | 5 | 3 String over a Blank Rune |
| Copper Cord | 5 | 2 | I and II | 150 | 6 | Twine Cord, 4 Copper Ingots, 1 Amethyst Shard |
| Amethyst Cord | 8 | 3 | I to III | 225 | 7 | Copper Cord, 4 Amethyst Shards, 2 Gold Ingots |
| Echo Cord | 12 | 4 | I to IV | 300 | 8 | Amethyst Cord, 2 Echo Shards, 1 Netherite Scrap |

The **Copper Cord** is the big early step: a second spell, five sockets, and Tier II runes such as Fire,
Frost, Burst, Beam, Shield and On Hit. Everything about Cords is on [Cords](spellcraft/cords.md).

## What to do next

There's no fixed path, but this order works well:

1. **Fill your Codex.** Craft the Tier I runes that suit how you play, and try them on a
   [Training Dummy](progression/training-dummy.md): it shows every hit and your damage
   per second.
2. **Form your first Heart Circle.** Every point of mana you spend on spells condenses in your heart. After
   600, meditate for 10 seconds without getting hurt to form the **1st Circle**: more mana, a little more
   power, your first **passive spell** slot and your own **innate rune**, one of ten, chosen at random, that
   can't be crafted, found or learned from an item.
   See [Heart Circles](progression/heart-circles.md),
   [Passive Spells](spellcraft/passives.md) and [Innate Runes](runes/innate.md).
3. **Make a Copper Cord** and learn Tier II runes. Try `Bolt · On Hit · Burst · Fire` (a fire bolt that
   bursts where it lands) and a `Self · Shield` to stop the next spell cast at you
   ([Shields and parrying](spellcraft/shields.md)).
4. **Walk the ley lines.** With a Cord on you can see veins of violet light running over the land. Mana flows
   twice as fast on them, and a Heart Circle forms twice as fast there. A Wellstone set on one helps everyone
   nearby.
   See [Ley Lines](progression/ley-lines.md).
5. **Hunt the Runebound.** Some monsters carry Cords of their own and cast real spells, glowing with rune
   marks. They drop runes and Torn Pages. See [Runebound](world/runebound.md), and learn to
   read what they're about to cast on [Magic Circles](spellcraft/magic-circles.md).
6. **Make an Amethyst Cord** for Tier III: Split, Lightning, Zone, Explode, Homing and the rest. Set off
   [element reactions](spellcraft/reactions.md) (frost then fire shatters), and follow the
   riddles on Torn Pages to [secret spells](spellcraft/secret-spells.md).
7. **Find the Archive**, a buried library in the Overworld, and defeat the **Archivist** for a Tier IV rune.
   See [The Archive](world/archive.md).
8. **Build a Fusion Altar** to rank runes up, fuse two elements into one of 55 fused runes (or a signature rune, for forty particular pairs), and tie whole spells
   into single-socket Knots. See [The Fusion Altar](fusion-altar/index.md).
9. **Make an Echo Cord** for four spells of twelve runes and Tier IV runes, then take on the dimension
   dungeons: the [Ember Sanctum](world/ember-sanctum.md) in the Nether, the
   [Astral Observatory](world/astral-observatory.md) in the End and the
   [Drowned Scriptorium](world/drowned-scriptorium.md) under the sea.

Along the way: tame a wisp as a [familiar](companions/familiars.md), style your Cord on
the [Cosmetics](companions/cosmetics.md) page, trade with the
[Runesmith](social/runesmith.md), pick up [casting gear](gear.md), and
share spells with friends ([Playing Together](social/playing-together.md)). The
[Grimoire](progression/grimoire.md) page of the Cord screen keeps track of everything you
discover, and the [advancements](progression/advancements.md) tab lights the way from your
first Blank Rune to the 8th Heart Circle.


All keys can be changed in the game's Controls options, under **Wildercord**. See [Controls](controls.md).
