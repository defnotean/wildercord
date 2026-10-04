**New in 0.10.0-alpha.**

# Root Carry

![A real paid Root Carry spell lifts a soil clod and fine roots between two planting patches](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/screenshots/root-carry/lift.png)

A young Cinder Fern can become the beginning of a garden. Root Carry moves an existing young root between nearby soil patches without turning it into another seed or resetting its growth.

## Craft and learn

At a crafting table, combine one Blank Rune, Rooted Dirt, Bone Meal, String, two Lapis Lazuli and one Gold Ingot. Use the resulting rune to learn Root Carry. Add it to a direct spell such as Touch. Root Carry is a rank-II Life world effect with base mana cost 8; the chosen shape, cord, mastery and admitted modifiers determine each complete spell's actual price.

## Two casts, one root

1. Cast at an actual young Cinder Fern. The plant stays in the ground while the spell remembers its exact state for fifteen seconds.
2. Cast again at the upper face of suitable empty soil. The same root moves there, then Root Carry rests for twenty seconds.

Each cast pays its ordinary spell price. Echo cannot turn the first payment into a free move or keep a selection alive. Both planting cells must be visible, loaded, permitted and within six blocks of your eye. The new cell can be at most eight blocks from the original. Dirt, Grass Block, Coarse Dirt, Rooted Dirt and Podzol support the fern; water and occupied planting cells refuse the move.

## Let living plants keep their history

Only young ferns can be selected. A fern that grows, is cooled, or otherwise changes after selection invalidates that selection. Its new state remains intact. An unchanged hot young fern stays hot after relocation; an unchanged cooled one stays cooled. Mature ferns, crops and other plant species are outside this first implementation.

Walking out of reach, changing dimension, dying, disconnecting or waiting too long clears the selection. Successful rest survives a saved-world restart. Ordinary protection and server spell-editing settings still apply. A failed attempt does not create another root or produce fern drops.

## A root, carried through the air

Fine roots cup a small soil clod, with two folded fronds gathering around it. On a successful move, that carried root appears at the actual source, lifts through a short midpoint, and settles at its destination. Minimal quality retains the clod and one moving root. The lift uses three short beats: source, raised midpoint and destination. It keeps the same physical root and soil identity in both Full and Minimal quality.

![Root Carry travels as folded fronds, a soil clod and carrying roots](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/screenshots/root-carry/flight.png)

These captures come from actual mana-paid spells in a supplied test scene.
