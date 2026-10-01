# Casting circle disciplines

Twelve craftable modifier runes change a spell group's magic circle and its behaviour. Place one after an explicit shape, for example **Beam → Confluence Circle → Fire → Shock**. Find them under **Modifiers → Circle disciplines** in the Cord editor. The compiler readout shows the discipline and its complete tradeoff.

Only the first discipline attached to a group counts. Additional disciplines in the same group are ignored with a warning and add no mana cost. A new shape starts another group and can have its own discipline. A linked group does not inherit the previous group's bonuses.

| Circle | Animated mechanism | Gameplay tradeoff | Group mana |
|---|---|---|---:|
| Needle | Eight iris blades close inward | Shape radius ×0.65; power ×1.20 | ×1.20 |
| Bloom | Six petals unfold around the seal | Shape radius ×1.35; power ×0.80 | ×1.15 |
| Gyre | Three spiral turbine arms counter-turn against the script | Flying shape speed ×1.30; power ×0.85 | ×1.15 |
| Anchor | Locked square lattice and crossbars | Effect duration ×1.40; power ×0.85 | ×1.25 |
| Reservoir | Three basins fill in opposite directions | Power ×0.80; effect duration ×0.85 | ×0.75 |
| Crucible | Breathing hexagon and opposed furnace triangles | Power ×1.15; effect duration ×0.75 | ×1.20 |
| Confluence | Material-coloured satellites braid into the centre | Power starts at 0.90, adds 0.08 per distinct element in the group, caps at 1.30 | ×1.20 |
| Pilgrim | Rolling compass diamond and cardinal arrows | Power ×1.15 while moving horizontally on release, otherwise ×0.90; effect duration ×0.90 | ×1.10 |
| Vigil | Eye opens and its pupil scans | Power ×1.20 when crouching on release, otherwise ×0.90; flying speed ×0.85 | ×1.15 |
| Mercy | Paired sheltering crescents and a breathing cross | Helpful effect power ×1.20; all other effect power ×0.75 | ×1.15 |
| Tempest | Six forked storm conductors | Power ×1.20 when in water or exposed to rain on release, otherwise ×0.90; flying speed ×1.10 | ×1.20 |
| Eclipse | Moving moon arc and counter-turning sun rays | Power ×1.20 at night, otherwise ×0.90 | ×1.15 |

Radius changes apply where a shape uses its radius; flight speed changes apply to flying shapes, without increasing their range. Instant beams remain instant. Duration changes apply to delivered effects, not to an instant spell's animation or its number of strikes. A stronger helpful effect only changes values that that effect supports. Terrain lifetime caps and existing speed, damage, healing and cast budgets still apply.

Pilgrim requires horizontal velocity of at least 0.05 blocks per tick. Vigil, Pilgrim, Tempest and Eclipse are sampled once when each group releases: changing stance, weather or time afterwards does not improve an existing lingering hit. Eclipse's night window is 13000–22999 on a sky-lit dimension with a running day cycle; fixed-time dimensions do not receive the night bonus.

## Fusion and examples

Circles are modifiers, so fuse them through the altar's **Knot** route. A Knot preserves the discipline and the effects it surrounds; an amethyst elemental weave still combines effects rather than modifiers.

- **Burst → Bloom Circle → Heal:** wider supportive coverage at lower healing per target.
- **Bolt → Gyre Circle → Frost:** faster frost projectile at reduced power.
- **Self → Anchor Circle → Haste:** longer mining support at a higher price.
- **Beam → Confluence Circle → Fire → Shock:** two actual elements give 1.06× power; duplicates of Fire do not count as extra elements.
- **Bolt → Needle Circle → Fire → On Hit → Burst → Bloom Circle → Frost:** focused opening shot, followed by a wider but softer frost burst. Each group keeps its own circle.
- **Self → Mercy Circle → Heal:** stronger healing; adding Harm still gives Harm the offensive penalty.

## Circle presentation

Plain spells also use the twelve mechanisms according to their shape. This is visual only and gives no hidden bonus. A selected discipline overrides that layout. The circle retains the spell's rune emblems and individual material colours, and its moving outer circuit displays the ingredients rather than using only one colour.

Both charged circles and release circles use these mechanisms. Caster release circles stay behind the shoulders and follow turning; shape-specific assembly and material effects still gather at the smaller forward focus. Secret spells retain their authored special centrepieces.

A circle in a linked group also opens at that group's actual trigger point, making the follow-up discipline visible where the linked spell lands. The opening caster circle uses the opening group; a later group's discipline does not replace it. The Cord preview and world circle share the same authored stroke geometry.

All twelve circle runes have explicitly drawn item glyphs, emblems and repeating circuit bands. Their recipes are listed in [the recipe catalogue](../items/rune-recipes.md).

## In game

![Bloom opens six petals around the caster seal.](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/circle-bloom.jpg)

Bloom opens six petals around the caster seal.

## In game

![The editor previews the same authored circle geometry used in the world.](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/circle-editor.jpg)

The editor previews the same authored circle geometry used in the world.
