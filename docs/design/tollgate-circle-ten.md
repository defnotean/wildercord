# Tollgate: Circle X control

First lesson of the authored pack (`LessonPackRules`, `LessonPackCasting`). Same architecture as Reweave and Excise: Grimoire study, Codex equip, rebound Cast input, one payment, shared rest.

## Use case
Hold a doorway or corridor against a rush without damage. Rampart blocks everyone and Starlight Tether drags marked targets. Tollgate stops each foe once, at a line, then lets them through.

## Discovery and lesson
- Feat **Tempered** (Cinder Warden) makes *The Warden's Threshold* retrievable in Grimoire > Master studies. Old saves holding the feat get it with no altar or book.
- A live, ordered, three-page reading with active Circle X teaches the Tollgate rune. Death, a dimension change, cracking below X, disconnecting or five minutes all retire the reading.
- A Tollgate rune item is refused until studied. It never drops from random loot, the forge, trades or wild swaps.
- Practice is recorded on the first real toll.

## Rules
- The row must be exactly Wall + Tollgate in an ordinary Echo Cord slot. Modifiers, links, Knots, woven runes, scrolls, Imbue and passives are all refused.
- Pressing Cast on visible ground within 10 blocks lays a 5-block gate across your view for 6 seconds.
- The first time a hostile body walks into the band (feet below 1 block), it is stopped, pushed back to the side it came from and given Slowness II for 1.5 s. Each foe is tolled once; there are 3 tolls in all.
- Cost: 30 base mana with Heart/mastery discounts, paid once. Rest is 10 s, shared across slots and saved, and never renews.
- Sneak + Cast lifts the gate with no refund.

## Counterplay
Jump it, walk around its ends, wait 6 s, or spend its 3 tolls with lesser bodies. Allies, pets and party members pass. Claims (`Casters.mayEdit`) and dungeon movement wards refuse the ground.

## Presentation
- Wall cast pose plus formation VFX.
- Bell on placement.
- Two posts with a sill beam, and one orb per toll left.
- Each toll plays a rising bell and wax-off sparks.
- Earth seal signature.
- Owner HUD line with seconds left and the lift hint.

## Verification
- `LessonPackRulesTest` covers grammar, storage, bounds, geometry and rest.
- `LessonPackPlayableTest` (single player) covers learn → Codex equip → rebound press → real zombie toll → refused press during rest → sneak lift.
- Party and PvP checks with a second connected player need CI.
