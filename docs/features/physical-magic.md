# Physical magic and expanded weaving

These spells create temporary blocks with server collision, not just pictures of terrain. Their item icons, circle emblems, release scripts and impact scripts are individually authored.

## Three new craftable runes

| Rune | Tier / base mana | What it does |
|---|---|---|
| Strata Rise | II / 9 | Raises a five-block-wide, three-block-high wall in three stages. |
| Tidal Lift | II / 12 | Borrows up to three nearby water sources and carries them along an arcing attack, then returns the source water. Each enemy takes at most one hit from that cast and gains Soaked. |
| Wind Steps | II / 8 | Assembles five thin collision platforms ahead of the caster, rising as they extend. |

Crafting uses a Blank Rune and the normal Tier II cost (two lapis and one gold ingot), plus stone/packed mud/flint for Strata Rise; prismarine shard/kelp/clay ball for Tidal Lift; feather/breeze rod/string for Wind Steps. The recipe book and [complete recipes](../RECIPES.md) show exact entries.

Walls and steps normally last eight seconds, with duration scaling capped at twenty seconds. Steps grant nearby allies a short slow fall as they approach expiry. Each cast has a block budget; active physical terrain is additionally limited to 64 cells per owner and 512 globally. This bounds how much terrain a group can maintain.

Water needs source blocks in the surrounding four-block horizontal search area, within two blocks vertically. It travels at most ten blocks and stops at an obstruction. The visible traveling water uses a custom translucent block model; it is not a spreading fluid flood. The original vanilla source water is reserved while borrowed and restored afterward, preventing infinite-water refill from duplicating it.

## Six new signature fusions

Put the specified two runes and an amethyst shard into the Fusion Altar. Each costs three XP levels to fuse.

| Pair | Result | Gameplay |
|---|---|---|
| Strata Rise + Fire | Cinder Bulwark | Glowing wall; a bounded contact hit against enemies. |
| Strata Rise + Grow | Root Bulwark | Living wall that gives nearby allies regeneration. |
| Tidal Lift + Fire | Boiling Surge | Hot water attack with custom rising vapour and brief Weakness. |
| Tidal Lift + Shock | Thunder Tide | Conductive water attack, electric rings and brief Slowness. |
| Wind Steps + Frost | Rime Causeway | A wider, three-block-wide ice platform route. |
| Wind Steps + Shock | Thunder Walk | Charged platforms with a bounded enemy contact hit. |

All six are Tier III. Their base mana costs are 16, 16, 19, 19, 16 and 16 respectively. Walls and platforms never overwrite valuable terrain, block entities or occupied living-entity spaces. Placement respects build permissions, the world border, build height and loaded chunks. Cancelling the cast restores its terrain. Saved undo records cover expiry after unload or a restart; cleanup does not force chunks to load.

## Change terrain with another spell

Allied magic can frost a Wind Step into rime, charge it with storm, or solidify it with earth. Fire turns a strata wall into cinder; life turns it into a root wall. Frost condenses traveling water into a temporary rime platform. These changes keep the original expiry instead of renewing it indefinitely.

Hostile wind can dispel steps, and hostile fire can melt rime. Fire evaporates traveling water. Ordinary targets, shields, team rules and defensive damage handling still apply to attacks.

## Exact weaves of two to eight effects

An **amethyst block** combines exact effects rather than the named element-grid fusion. An existing weave can be combined with another effect or another weave, up to eight total registered effect leaves. Input order and grouping produce the same canonical result; duplicates remain duplicates, and no component is silently discarded.

- Two effects need at least the highest component tier.
- Three or four need at least Tier III; five to eight need Tier IV.
- The fusion fee is three XP levels per extra leaf: 3, 6, 9, 12, 15, 18 or 21 levels.
- Mana is the full sum of the component costs; socket compression does not grant free effects.
- An amethyst shard cannot collapse an existing exact weave into an element-grid result.
- Shapes, modifiers and links keep their normal sequence roles. Use **Knots** to store a valid sequence in one socket.

Every elemental effect has a weaving route, including the ten innate effects. A soul weave may contain only one distinct innate identity and always requires Tier IV. Innate growth follows the caster's Heart Circles, not rune ranks. Survival players can learn and cast only soul weaves containing their own awakened innate; refusal occurs before mana or cooldown is paid.

**Imprint your innate:** crouch and use a Blank Rune on a Fusion Altar. It consumes one blank and three XP levels and gives a physical copy of your own awakened innate. Then weave that item with an elemental effect and an amethyst block. Insufficient XP consumes nothing. Creative testing bypasses ownership and payment restrictions.

## Original material effects

Spell decoration now uses Wildercord's own sprites and motion for ember, frost, storm, wind, stone, petal, void, arcane, time, blood, water and vapour. These retain the authored per-rune geometry and timing. Fused ingredients contribute their material motion as well as colour. Normal Minecraft world feedback, such as a burning creature or flowing source water, remains native world behaviour.

Caster release strokes follow the live rear pose for their short animation beats, avoiding a fixed old circle appearing in front of the player after a rapid turn. The shape still determines the outgoing projectile, beam, ring, rain or self effect.

Spellguard also snapshots a target's starting health for hits from the same cast in one server tick. A multi-effect burst can no longer bypass the full-health survival threshold merely by splitting its lethal damage into smaller hits. Its existing recharge and short protection against the remainder of that cast still apply.
