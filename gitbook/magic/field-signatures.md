# Field Signatures

These signatures give two familiar runes a new job. Place the exact pair in a Fusion Altar with an
amethyst shard and pay three experience levels. Other runes of the same elements continue to make
their ordinary elemental fusion. Each result is a tier III rune, so the spell's normal heart-circle
requirements still apply.

| Signature | Exact pair | Base mana | What it does |
|---|---|---:|---|
| **Springbed** | Basinfill + Grow | 14 | Makes a small water vessel, then gives existing bank plants one stage of growth. |
| **Cinder Sieve** | Ember + Collect | 12 | Uses one carried coal or charcoal to process visible loose furnace inputs and recover their output. |
| **Ashen Mercy** | Fireward + Cleanse | 16 | Turns actual harmful conditions removed from an ally into a small heal and brief heat protection. |
| **Clockroot** | Root + Foresight | 16 | Remembers a foe's safe ground and returns it once if it flees. |
| **Skylatch** | Levitate + Anchor | 14 | Lifts an ally slightly and holds its height briefly while leaving lateral movement free. |
| **Thresherwind** | Harvest + Windcut | 12 | Sends three narrow harvest lanes forward, gathering crops and replanting with real seeds. |

![The six carved field signature runes in the inventory](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/field-signatures/inventory.png)

The listed mana is the effect's base cost. Shapes, links, gear and modifiers still change the actual
spell price shown by the spell editor. These signatures have fixed resource and time limits; Amplify
cannot turn a small utility spell into an unbounded harvest or a permanent restraint.

## Water first, roots second

**Springbed** needs the same enclosed, one-block-deep vessel as Basinfill: no more than sixteen
connected air cells, with a safe floor and closed edges. Aim Touch or Bolt at the floor. The water
stays after the spell ends and after a world reload.

Once the vessel fills, up to eight existing immature crops or moist Moonreed buds on its bank gain
one growth stage. Their soil must be within the new water's hydration reach. Both the plant and its
support need build permission. Springbed creates no plants and cannot skip Moonreed's pollinator
requirement. It performs one pour and one growth pass per paid cast.

Its preparation gathers pouring threads above a branching seed root. The travelling parcel carries
water inside a living fork. At the bank, water settles before thin rootlets trace the eligible plants.

![Springbed gathers water threads around branching roots before firing](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/field-signatures/springbed-prepare.png)

## A portable kiln with an honest inventory

**Cinder Sieve** uses one coal or charcoal from your carried inventory. It handles at most sixteen
loose inputs within four blocks of the impact, using their actual furnace recipes. Processed goods
must fit completely before the spell spends fuel or changes any input. A full inventory leaves those
items and fuel intact.

Drops belonging to someone else, items still under pickup delay, blocked sight lines and protected
land are refused. Leftover input stays on the ground. The spell grants no furnace XP. Repeating or
reflecting a spell does not buy another processing pass from the same payment.

The ember basket closes around a hollow intake. Its hot ribs remain visible around the dark
collection current; the spell never becomes an ordinary explosion with a different color.

![Cinder Sieve prepares its ember ribs and hollow intake](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/field-signatures/cinder-sieve-prepare.png)

## Helpful heat

**Ashen Mercy** extinguishes eligible allies and removes their harmful status effects. Only conditions
actually removed can supply healing: two health for each of up to two conditions, with a total cap
of four health. Missing health remains the upper limit. A clean target gets no free healing.

The ally also receives five seconds of Fire Resistance. One payment can help at most eight allied
UUIDs, once each, including repeating copies of that spell. It cannot cleanse an enemy for you.

Scorched leaves fold toward living tissue around a small heat seam. At arrival, ash flakes and
living petals separate rather than producing a large flash.

## Remembered ground

**Clockroot** records the target's safe grounded position for four seconds. If the foe travels more
than two horizontal blocks, it can be returned there **once**. The return only occurs if the target,
recorded floor, loaded space and unobstructed route are still eligible. A wall, removed floor, unsafe
terrain, changed dimension or escape beyond eight blocks releases the memory harmlessly.

Bosses, permanently invulnerable targets, Cinnamon, mounted targets, anchored targets, creative or
spectator players and warded arenas are refused. It stops when the caster or target leaves or dies.
One payment can mark at most eight foes; each target then needs eight seconds before a new imprint.
Clockroot never damages the target or keeps pulling it back repeatedly.

Its root prongs pin a little sand hourglass. A low physical imprint stays on the original ground;
if it fires, earth grains travel back along the escape path.

## A latch you can leave

**Skylatch** checks the space above an eligible ally, then lifts it about one block and holds vertical
height for four seconds. The ally can still move sideways. Crouching releases the latch early into
two seconds of Slow Falling. Death, logout, dimension changes, blocked space and the deadline also
end the hold.

A stronger or longer Levitation effect arriving during the hold takes over immediately. Skylatch
releases its own latch and preserves that incoming effect.

The spell grants no flight abilities. Existing flight abilities or Levitation, anchors, mounted
targets, bosses and warded arenas prevent admission. It affects at most eight allied targets per
payment, with eight seconds of rest for each target. Repeated pulses cannot renew the hold forever.

An open updraft catches a dark knot. Two travelling air brackets stay open around the target;
the short tether loosens when the hold ends.

## Three passing harvest lanes

Aim **Thresherwind** at a mature crop or its ground. Three rows, each three blocks wide, pass forward
over a short interval. At most nine mature crops can be harvested. Every row checks that the caster
is still present, the blocks are loaded, the sight line is open and the land permits editing.

The spell uses the crop's real drops. Replanting spends one matching seed from that harvest; a crop
that drops no seed is not replanted for free. Only this harvest's goods are gathered. A full inventory
leaves the remaining harvest as loose items reserved for you.

The blades shear in separate lanes. Severed stalk fibres and grain follow the air, so the spell's
motion describes the harvest rather than drawing another generic circle.

## Trying them together

Use Springbed beside a small garden, then pass Thresherwind over the mature rows later. Carry fuel
and leave inventory room before using Cinder Sieve on expedition supplies. Keep Ashen Mercy ready
for burning or poisoned allies. Clockroot buys a short positioning opportunity against ordinary
foes, while Skylatch can give an ally a brief elevated view without taking away its choice to move.

*These six signatures are new in 0.10.0-alpha.*
