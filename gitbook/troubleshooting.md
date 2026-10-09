# Troubleshooting

Find your problem below. For startup and version errors, see [Installation](installing.md#if-it-wont-start).

## The Fusion Altar opens, but Fuse does nothing

1. Empty the **result socket**.
2. Read the preview panel. It names the output, the catalyst and the XP cost.
3. For a named fusion, use two elemental **effects** and an **Amethyst Shard**. For an exact weave, use a **Block of Amethyst**.
4. For a two-input fusion, leave the third socket empty. Three identical runes of the same rank and no catalyst make a rank upgrade.
5. You need the shown number of **XP levels**. Creative skips the cost.
6. Make sure your client and the server run the same Wildercord version.

A weave holds at most eight effects. Shapes, modifiers and links use [Knots](fusion-altar/knots.md) instead.

## Cinnamon spawns, but I can't make her sit

Her owner must be set in `config/wildercord-cinnamon.json`. Put the owner's exact username or UUID there, then have that player join her dimension. The file reloads about every five seconds. Summoning her doesn't make you her owner.

Only her owner makes her sit with a normal click. Sneak-click pets her. See [Cinnamon](companions/cinnamon.md).

## I can't see my casting circle in first person

The large circle sits **behind your shoulders**. Switch to third person to see it. A smaller focus forms ahead of you while you charge.

## A circle bonus seems missing

Only one discipline per spell group works. Put it after a shape and read the explanation in the Cord screen. Pilgrim, Vigil, Tempest and Eclipse check conditions **when you release**, so changes afterwards don't help. See [Circle Disciplines](spellcraft/circle-disciplines.md).

## A Sword Master won't come

- You need **Aura Form** or **Heart Circle VIII** first.
- Masters don't come on Peaceful, in Creative, or while you're in a duel or spar.
- Only one Master at a time can be near you. Finish or leave the other trial first, or move 64 blocks away from another Master.
- With a Wandering Duelist, sneak and use it once to hear its offer, then again within 10 seconds.
- Or use `/master challenge <name>`, such as `ember`, `tide` or `venom`. The [Sword Masters](masters/index.md) page lists all 16 names.

See [Sword Masters](masters/index.md).

## The Master form key does nothing

The Master form key (`C`) does nothing in Creative, so the game's Save Toolbar still works there. You also need a [Master form](masters/master-forms.md) equipped. Check the key isn't clashing with another in **Options > Controls > Key Binds**.

## A new dungeon or the practice arena is missing

Restart after you update. New dungeons only generate in new terrain. Enter the practice arena with `/runelab practice enter` and leave with `/runelab practice leave`.

## Magic is overwhelming or slow

Open **Magic visual settings** (bind a key in Controls first). Set other spells to minimal, turn on reduced flash, and turn off camera motion. See [Performance and Visual Settings](performance.md).

## Reporting a bug

Give the exact spell, your Cord, rune ranks, the gear you wore, game mode, dimension and the steps to repeat it. For altar problems, add the inputs, catalyst, XP level and preview text. Attach `logs/latest.log` and a screenshot if you can, then [open an issue](https://github.com/defnotean/wildercord/issues/new/choose).
