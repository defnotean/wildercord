# The Sleeping Blade

Oathkeeper rests in a cracked stone socket among small Marchkeeper ruins. Look in **meadows, windswept hills, windswept gravelly hills, stony peaks and plains**. Its resting places are rare, reject flooded ground and avoid abrupt cliffs. A site may be surrounded by cairns, a broken threshold, or the remains of a shelter.

![Oathkeeper resting in its cracked socket on a grassy hillside](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/sleeping-blade-landmark.png)

## Draw it by intent

1. Reach **Form** in Aura. Aura and bonded blades must be enabled on the server.
2. If you already have a blade bond, decide whether to release it from the Aura page. The stone will never replace a standing bond automatically.
3. Empty your **main hand**, stand on solid ground beside the socket, and kneel by holding sneak.
4. Use the stone. Remain still, nearby and unhurt for **six seconds**, with your main hand empty.

The blade rises through three visible lift poses. Steel strains against the socket and small ground fractures show its movement. The progress message reports the elapsed seconds. Movement, standing up, damage, leaving the ground, looking through an obstruction, changing dimensions, losing the required intent, or holding an item interrupts the draw. An interrupted blade settles back into its stone.

Only one swordsman can draw a given blade at a time. The first completed draw claims that site's blade permanently. It enters the swordsman's main hand already bonded, with a modest resonance head start and its origin recorded as a sleeping blade.

## Oathkeeper's answer

Oathkeeper is a diamond-grade sword with shaped steel, a wrapped grip and a brass guard. Its special effects require **your active bond and the blade in your main hand**.

![Oathkeeper drawn and held in first person beside the cleared socket](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/oathkeeper-in-hand.png)

| Choice | Effect |
| --- | --- |
| Raise or hold Aura Guard | Pay 15% less Aura |
| Land a perfect guard | Build 30% more momentum, with its own answering sound and ground cut |
| Land ordinary hits | Build 20% less momentum |
| Other momentum sources | Keep their usual values |

The blade favours defensive timing. It grants no unconditional damage increase and does not widen the perfect-guard window. Existing Aura limits, costs, attack requirements and enemy counters still apply.

The bond follows the same ownership, death retention, durability and progression rules as other bonded blades. Its small resonance head start respects the server's resonance gain settings; it does not skip the later tiers or their deeds. You may give it a personal bond name when it earns Named.

## The empty stone remembers

The successful swordsman receives **The Last Oath**, a portable three-page history connecting the Marchkeepers' retreat, shelter and buried blades. The cleared stone preserves that history for later travellers, each receiving the lore book once. It never issues a second blade, even after the original bond is released.

A newly placed stone has no generated provenance and cannot issue a weapon or history. The site records its claimant, and the player's saved journal records the book discovery. Interrupted lift poses reset through scheduled checks without ticking every resting place or loading remote chunks.

## Server settings

`aura_world.sleeping_blades` controls new landmark generation and stone interactions. Disabling it keeps existing terrain and acquired blades; those blades continue to follow the server's bonded-blade settings. No new recipe turns an ordinary sword into Oathkeeper.

Related: [Aura](aura.md) · [The Sword Tombs](sword-tombs.md) · [The Old Battlefields](battlefields.md)
