# What's new in 0.9.1-alpha

This release brings the expanded Aura path, living highland and wetland interactions, authored fire/frost/storm/wind preparations and projectile bodies, and fixes for extreme spell damage. **Update the server and every client together.** Spell packets and particle registration have changed since 0.9.0.

## Fairer spell damage

Vow, Execute and Trial Key now apply once per target rune; Focus twice; Extend three times. Extra copies warn and add neither cost nor power, including inside Knots. Each paid player cast shares one damage allowance per target across its repeats, linked hits and damage over time: `min(512, 12 + 2 × effective mana price)`, with a maximum 96 raw damage per hit. Armor still applies. See [casting rules](spellcraft/casting.md).

## Authored elemental motion

Preparations and moving projectile bodies now cover 28 fire, 30 frost/water, 20 storm and 25 wind effects. Ingredients retain their material and motion. Wind carries translucent airflow, traveling crests, shear and pressure using its own renderer. Initial and linked formations follow their actual delivery; one complete glyph stays behind the caster. Complete release/impact work for every element and delivery is still in progress.

![Windcut gathering fine currents ahead of its rear casting glyph](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/wind-formation-windcut.png)

## The blade and its world

Learn sword strings, breathing-method arts, momentum, openings, awakenings, Ways and authored techniques. Bond a blade, teach a disciple, spar by consent and meet another strike in a timed clash. Physical Aura standards, ground fractures and Soar's feathered wings accompany the path.

Explore [Marchkeeper battlefields](progression/aura.md), sword tombs and the sleeping blade; meet highland beasts and enter village tournaments. Resonant strikes, rune-etched blades and Unity connect the swordsman and mage. See [Aura](progression/aura.md) and the [full release notes](https://github.com/defnotean/wildercord/releases/tag/v0.9.1-alpha).

## Useful fixes

- Summit Wind now recognizes actual mountain peaks; Storm Spire is rebuilt as a copper observatory.
- The Cord creation screen shows the owner's saved spell count.
- `/wildercord learnall` learns named fused runes and reveals all 77 elemental/signature recipes. It preserves saved spells and innate ownership. Exact dynamic weaves are still made at the altar.
- Cinnamon has her polished custom appearance, bell, bow, tongue, temperament and owner-defense behavior.

## Wetland lights and fox friendship

Lantern Newts bring browsing behavior, answering lanterns and peaceful Dusk Pearl gathering to swamp shallows. Pearls craft waterloggable Marshlights and three-page Tideward field notes. Fish now tames both ordinary foxes and Cinderfoxes; ordinary foxes gain saved ownership, following and an empty-hand sit toggle. See [wetland field notes](world/luminous-wetlands.md) and [companions](companions/index.md).

## Moonreed gardens

Glimmerwings now visit swamp banks and pollinate Moonreed by approaching its buds. Life prepares growth but cannot replace a moth visit. Harvest floss without destroying the root, then combine it with a Dusk Pearl, Copper and Glass to craft a Dewglass Lens. Read flowers and newt gathering conditions, or follow its short trail to a visible nearby reed. See [Moonreed Gardens](world/moonreed-gardens.md).

## Reed Refuges

Weave a waterlogged canopy from Bamboo, Moonreed Floss, String and Seagrass. Lantern Newts seek it in daylight or rain, curl up briefly, and wake when fed or answering magic. Shelter adds no healing or extra pearl. Craft Tamsin's field notes to learn the habitat rules. See [Reed Refuges](world/reed-refuges.md).

## Reedback Crabs

A new territorial creature inhabits shallow swamp banks. Its six legs, hinged claws and reed crown have a custom model and animation. Sneak past, sidestep a warned sweep, use Tidebreath to calm it or wind to interrupt it. See [Reedback Crabs](world/reedback-crabs.md).

## Earlier releases

The twelve monsters/wildlife and original Aura foundation arrived in [0.9.0](https://github.com/defnotean/wildercord/releases/tag/v0.9.0-alpha). Harmonies, mastery and residues arrived in [0.8.0](https://github.com/defnotean/wildercord/releases/tag/v0.8.0-alpha). The living-world expansion continues; this release does not complete the roadmap.
