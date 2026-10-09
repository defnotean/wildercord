# Circle Disciplines

## What it is

![Bloom Circle opening six petals around the caster's seal](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/circle-bloom.jpg)

Circle Disciplines are twelve modifier runes. Each one changes the magic circle of one group of your spell and
gives it a tradeoff: more power for less reach, wider coverage for less power, and so on.

| Circle | What it does | Group mana |
|---|---|---:|
| Needle | 35% smaller radius, 20% more power | ×1.20 |
| Bloom | 35% larger radius, 20% less power | ×1.15 |
| Gyre | Flying shapes 30% faster, 15% less power | ×1.15 |
| Anchor | Effects last 40% longer, 15% less power | ×1.25 |
| Reservoir | 20% less power, effects 15% shorter | ×0.75 |
| Crucible | 15% more power, effects 25% shorter | ×1.20 |
| Confluence | Power starts at 90% and gains 8% per different element in the group, up to 130% | ×1.20 |
| Pilgrim | 15% more power if you're moving when you let go, otherwise 10% less; effects 10% shorter | ×1.10 |
| Vigil | 20% more power if you're crouching when you let go, otherwise 10% less; flying shapes 15% slower | ×1.15 |
| Mercy | Helpful effects 20% stronger, all other effects 25% weaker | ×1.15 |
| Tempest | 20% more power if you're wet or in rain when you let go, otherwise 10% less; flying shapes 10% faster | ×1.20 |
| Eclipse | 20% more power at night, otherwise 10% less | ×1.15 |

## How to get it

Circle runes are crafted like other runes. See the
[rune recipes](../items/rune-recipes.md). Some also turn up in the Archive's library.
In the Cord screen they sit under **Modifiers → Circle disciplines**.

## How to use it

![The Cord screen previewing a spell's circle discipline](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/circle-editor.jpg)

Put the circle rune after a shape, for example `Beam · Confluence Circle · Fire · Shock`. The readout shows the
discipline and its full tradeoff.

- **One per group.** Only the first circle in a group counts. Extras are ignored, cost nothing and show a
  warning.
- **Each group is separate.** A new shape starts a new group that can have its own circle. A group after a link
  doesn't keep the earlier group's bonus.
- **Checked when you let go.** Pilgrim, Vigil, Tempest and Eclipse look at you once, when the group fires.
  Changing stance or weather later doesn't help a lingering spell. Eclipse gets no night bonus where time
  doesn't pass.
- **Radius** only matters for shapes that have one, and **speed** only for flying shapes. Beams stay instant.
- **Fusing:** circles are modifiers, so tie them into a [Knot](../fusion-altar/knots.md) to
  save sockets.

Spells without a circle rune still show one of these twelve designs, picked by their shape. That's only a look and gives
no bonus.

## Tips and counterplay

- `Burst · Bloom Circle · Heal` heals a wider area, but less per ally.
- `Bolt · Gyre Circle · Frost` gives a faster frost bolt.
- `Self · Anchor Circle · Haste` makes Haste last longer.
- `Beam · Confluence Circle · Fire · Shock` has two elements, so 106% power. A second Fire doesn't count.
- `Self · Mercy Circle · Heal` heals more. Add Harm and the Harm gets weaker.
- **Read the circle.** Each discipline has its own moving design, so you can spot a Needle shot or a Bloom heal
  before it lands. See [Magic Circles](magic-circles.md).
