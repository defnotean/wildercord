# What's new in 0.7.0-alpha

This release brings together the gameplay expansion, fixes and visual work completed since 0.6.1. All players and servers should update together.

## Build spells with more personality

- **350 named runes**: 39 shapes, 258 effects, 36 modifiers, 17 links. The 297 shape/effect animations have individually authored three-beat sequences.
- Twelve circle disciplines alter coverage, flying speed, duration, support, mixed elements, movement, crouching, weather and night conditions, with costs and drawbacks shown in the editor.
- Large circles remain behind the caster during turns. The forward assembly keeps the delivery shape. Fire, wind, frost and the other materials carry their own motion, rather than just a mixed colour.
- Strata Rise, Tidal Lift and Wind Steps create temporary collision terrain or borrow real water. Six signature fusions extend them. Friendly and hostile elements can transform or dispel that terrain.
- Exact elemental weaves extend to two through eight effect leaves, preserve duplicates and canonical ordering, and support owner-bound innate soul weaving at Tier IV. Named fusion recipes now total 77.

## Explore, practise and settle down

- Clockwork Crypt, Living Greenhouse and Moving Sky Ruin each have three layout variants, interactive mechanisms and a guaranteed handling relic.
- Root Guardian and Storm Conductor have custom models, staged fights and physical counterplay.
- A dedicated practice arena supports moving targets, stress scenarios, damage readouts and three optional 90-second trials.
- A native Research Notebook records permanent experiments and up to 24 named spell builds. Runic Hearth projects and cooperative rituals give magic uses at home.
- Familiars can scout, guard or help with gardens. Ended world events leave finite, once-per-player echoes.
- Cinnamon is a small custom dog with a darker body, tan features, immortal rescue behaviour, configured ownership, remembered sitting, petting, rest and toy play.

## Survive and read a fight

- Emberweave, Rimebound and Stonebound armour, a Mirror-thread mantle, and Reprieve/Grounding foci offer defence with timing, movement and outgoing-power tradeoffs.
- Reprieve debt survives swaps/logout; casting locks have capped duration and recovery. The same-cast spellguard loophole is corrected.
- Fuse button handling and Cord mouse interactions are fixed for Minecraft 26.3. Backpack storage, worn presentation and interaction safeguards have been tightened.
- Cord, backpack, relic, armour and rune art has more detail, with original animated material textures for spells.

## Performance, release and guide

- Bounded compiler caching, decorative delivery limits, local visual settings and diagnostics support busy scenes.
- Performance, Balanced and Cinematic launcher profiles pin dependencies and package the same release jar. Shader packs are not bundled.
- The player guide now covers installation, practical builds, all new systems, troubleshooting, tested limits and curated real gameplay screenshots. A GitBook-compatible export shares the same source.

See the repository CHANGELOG and docs/audit reports for the engineering history. New dungeon generation requires new terrain; installed alpha worlds should be backed up before updating.
