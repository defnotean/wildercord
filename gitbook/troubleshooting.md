# Troubleshooting

Start with the symptom below. These instructions describe 0.9.0-alpha; jars older than 0.7.0 do not include the mouse, ownership and casting fixes.

## The Fusion Altar opens, but Fuse does nothing

1. Empty the **result socket** first.
2. Read the preview panel: it names the output, catalyst and XP requirement.
3. For a named fusion, use two elemental **effects** and an **Amethyst Shard**. For exact weaving, use a **Block of Amethyst**.
4. Keep the third socket empty for a two-input combine. Three identical, equally ranked effects with no catalyst are a rank upgrade.
5. Have the displayed **XP levels**, not just XP points. Creative mode waives the cost.
6. Update both client and server to this release. The Fuse button's native mouse handling was corrected for Minecraft 26.3.

Weaves have at most eight effect leaves. A shard cannot flatten an existing exact weave into a named fusion. Shapes, modifiers and links use [Knots](fusion-altar/knots.md), rather than elemental weaving. An imprinted innate in Survival requires its owner and the appropriate Heart Circle access.

## Cinnamon spawns, but I cannot make her sit

Her owner must be set in `config/wildercord-cinnamon.json`. A blank owner leaves her unassigned; a summon does not make the summoner her owner. Set the exact username or UUID of her intended owner, then have that player join her dimension. The file reloads about every five seconds.

Only her owner toggles sitting with an ordinary click. Sneak-click pets her instead. The Red Bone starts play without changing her saved sit preference. Read [Cinnamon](companions/cinnamon.md) for setup and rescue behaviour.

## I cannot see the large casting circle in first person

The large caster circle sits **behind your shoulders** and follows your turns. Use third person or ask another player to look at it. A smaller shape-specific focus gathers ahead of you; a Self cast affects you, while rain, beams and projectiles keep their own delivery. Shield interception and linked impact circles appear where their actual interaction occurs.

## A circle bonus seems absent

Only one discipline per spell group is active. Put it after an explicit shape and read the compiler explanation. Gyre changes flying speed, not an instant Beam. Bloom only expands shapes that use radius. Pilgrim, Vigil, Tempest and Eclipse check conditions **at release**; changing conditions after casting cannot improve a lingering spell. Fixed-time dimensions do not receive Eclipse's night bonus.

## My new dungeon or practice arena is missing

Restart after installing the new version to register the practice dimension. New dungeons generate in new terrain; structures already present are not rebuilt. Practice entry is `/runelab practice enter`; leaving is `/runelab practice leave`.

## Magic is overwhelming or slow

Assign **Magic visual settings** in Minecraft Controls. Choose Balanced or Minimal for other casters, enable reduced flash, and reduce camera motion. Keep warning circles visible. For diagnostic commands and what their numbers mean, see [Performance](performance.md).

## What to include in a bug report

Give the exact spell sequence, Cord tier, rune ranks, worn and held gear, game mode, dimension and steps to repeat it. For altar problems include inputs, catalyst, XP level and preview text. Attach `logs/latest.log` and a screenshot where useful. [Open an issue](https://github.com/defnotean/wildercord/issues/new/choose).
