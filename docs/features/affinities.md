# Creature affinities and elemental climate

Two systems that make the choice of element matter: what a spell lands on (**affinities**) and where it's
cast (**climate**). Both are one factor in `Effects.hurt`:

```java
amount *= Affinities.multiplier(cast, target, source, currentElement);
```

`cast.Affinities.multiplier` is the caster's climate for the element (`cast.Climate.factor`) times the
target's affinity to it. The element is the effect's (`Effects.currentElement`); a hit with no effect
behind it (a collision's burst, a secret spell's blast, the Tide Scribe's flood) counts by its damage
type: burning is fire, freezing frost, lightning storm, a sonic boom void (`Affinity.elementOf`).

Only damage changes. Heals, durations, knockback and marks are the same everywhere and on everything.

## Affinities

| Affinity | Multiplier | Tag |
|---|---|---|
| Weak | x1.5 | `wildercord:affinity/weak_to_<element>` |
| Resists | x0.5 | `wildercord:affinity/resists_<element>` |
| Immune | x0 (the hit is dropped: no flinch, no sound) | `wildercord:affinity/immune_to_<element>` |

`<element>` is one of `fire`, `frost`, `storm`, `wind`, `earth`, `life`, `void`, `arcane`, `time`,
`blood`. The rules (`spell.Affinity.judge`, unit-tested):

- **Players have none.** PvP is untouched.
- **A reaction breaks through a resistance.** A hit that sets off a reaction on the creature
  (`Reactions.reactedWithin(target, 0)`) ignores its resistance; immunity still holds. This is what
  keeps the Cinder Warden's fight: it resists fire, but a Shatter lands in full.
- **Weak and resisting cancel.** Only possible with a Runebound (below).
- **Runebound resist their Cord's element** (its first effect's), on top of their kind's. That comes from
  the monster, not its kind, so it isn't written into the Bestiary.
- **Fireproof creatures** (vanilla `fireImmune`: blazes, striders, ghasts...) already take nothing from
  burning damage. Such a hit is reported as **Immune** and recorded as immune to fire. Fire damage that
  doesn't burn (Explode's blast, a Meteor) still meets their `resists_fire`.
- **Vanilla's fivefold freezing damage** on `#minecraft:freeze_hurts_extra_types` (blazes, striders, magma
  cubes), meant for powder snow, is divided back out, so the table decides: those take frost x1.5, not x5.
- **Being wet doesn't stack with resisting fire.** Wet targets take 25% less fire (`WorldMagic.wetDamage`);
  on a creature that resists fire, the resistance already counts that, so it takes half either way.

With `features.creature_affinities` off, all of this is skipped (and vanilla's x5 freeze comes back).

### The table

Written by `tools/affinity_data.py` (called from `generate_assets.py`). Only tags with creatures in them
are written.

| Tag | Creatures |
|---|---|
| `weak_to_fire` | stray, snow golem, polar bear, creaking |
| `resists_fire` | the Nether's creatures\*, `#minecraft:aquatic`, drowned, Cinder Warden |
| `weak_to_frost` | the Nether's creatures\*, slime, Cinder Warden |
| `resists_frost` | stray, polar bear, Tide Scribe |
| `immune_to_frost` | snow golem |
| `weak_to_storm` | iron golem, copper golem |
| `resists_storm` | creeper |
| `weak_to_wind` | `#minecraft:arthropod`, phantom |
| `resists_wind` | breeze |
| `weak_to_earth` | breeze |
| `resists_earth` | iron golem, copper golem, slime, magma cube |
| `weak_to_life` | `#minecraft:undead`, Tide Scribe, Star-Eater |
| `resists_life` | `#minecraft:arthropod`, witch |
| `weak_to_void` | Archivist |
| `resists_void` | enderman, endermite, shulker, ender dragon, wither, warden, Star-Eater |
| `weak_to_arcane` | vex |
| `resists_arcane` | evoker, illusioner, Archivist |
| `weak_to_time` | enderman, endermite, shulker |
| `resists_blood` | `#minecraft:skeletons`, wither, iron golem, copper golem, snow golem, blaze, breeze, vex |

\* blaze, magma cube, ghast, strider, wither skeleton, hoglin, zoglin, piglin, piglin brute, zombified piglin.

Water creatures aren't weak to storm: in water they're wet, so every storm hit already Conducts (+50%,
arcing on). The Tide Scribe takes five times a shock through its flood, which is weakness enough; as a
drowned sorcerer it's weak to life instead.

### Changing it in a datapack

The tags are ordinary entity type tags, so a datapack changes the table without touching code. To add to
one (the mod's entries stay), put a file at the same path in your datapack, for example
`data/wildercord/tags/entity_type/affinity/weak_to_fire.json`:

```json
{
  "values": ["minecraft:spider", {"id": "othermod:frost_troll", "required": false}]
}
```

Replace one outright with `"replace": true` (an empty list with `"replace": true` removes every creature
from it). Tags that the mod doesn't write (`weak_to_blood`, `resists_time`, `immune_to_fire`...) are read
all the same, so a datapack can start one. Use `"required": false` for another mod's creatures, so the tag
still loads without that mod. `/reload` applies it. Clients get the tags from the server, so the
Bestiary's count of what's left to find matches.

## Feedback and the Bestiary

- **Callouts.** A weakness struck floats a bold **"Weak!"** over the creature in the element's colour;
  a resistance **"Resisted"** and an immunity **"Immune"** in grey. They're text displays (seen by
  everyone near, gone in a second), at most one a second per caster, each with a quiet sound (a crit,
  a weak thud, a no-damage tap). Every such hit also gets a cue on the creature: a crack of sparks in
  the element's colour for a weakness, a dull grey puff otherwise. Only a player's spells show them;
  a monster's spells (a Runebound's storm on an iron golem) still get the multiplier, silently.
- **The Bestiary** (`spell.Bestiary`, Grimoire keys):
  - `bestiary:<type>`: a kind of creature met, written quietly the first time a player's spell strikes
    one that has any affinity (or reveals one it has without tags, like a fireproof creature).
  - `bestiary:<type>|weak|<element>`: a weakness found. The only announced Bestiary entry: a Grimoire
    toast, the discovery sound, and 25 mana condensed toward the next Heart Circle.
  - `bestiary:<type>|resists|<element>` and `|immune|`: written quietly (the callout already said it).
  None of these count toward a full Grimoire or any advancement.
- **The Grimoire page** lists every creature met: `Blaze · weak: Frost · resists: Fire (immune), ?`, a
  `?` for each affinity still to find (the client counts them from the synced tags), and a tooltip
  with what each does.

## Climate

`spell.ClimateRules` (pure, unit-tested) holds the table and which conditions hold where;
`cast.Climate` reads the world for a player (`surroundings`).

| Condition (id) | When | Shift |
|---|---|---|
| The Nether (`nether`) | In the Nether | fire +20%, frost -25% |
| The End (`end`) | In the End | void +20% |
| A thunderstorm (`thunder`) | Thundering and the caster can see the sky | storm +25% |
| Rain (`rain`) | Rain (not snow) falling on the caster | fire -10% |
| Snow and frost (`snow`) | Cold enough to snow where the caster stands (snowy and frozen biomes, high peaks) | frost +20%, fire -10% |
| Hot, dry land (`heat`) | Biome base temperature 1.5 or more and never rains (desert, badlands, savanna) | fire +15%, frost -10% |
| Night (`night`) | 13000-23000 on the clock, in a dimension with days, under the open sky | void +10% |
| Sunlight (`sun`) | Day, open sky, not raining | life +10% |
| Deep underground (`deep`) | Below y=0 in the Overworld | earth +15% |
| Ley line or mana storm (`ley`) | `LeyWalker.onLine` or `ManaStorm.inside` | arcane +15% |

The Nether and the End are their own climate: no weather, biome, day or depth there (a ley line or a
mana storm still counts). Shifts multiply, and each element stays within 0.5-1.5 however they stack.

- **Players only.** Monsters' spells (Runebound, the bosses) aren't affected, so no boss fight changes
  with the weather. It applies in PvP, the same for both sides.
- **Cached** per player for the current second (and redone at once in a new dimension), since every
  hit asks.
- **Synced** to the HUD: once a second `Climate` sends each player the ids of the conditions holding
  (`Climate.Sync`, only when they change; empty with the feature off or for a spectator). The client
  works out the factors from the same `ClimateRules` table.
- **The HUD** (`SpellHud`, drawn by `client.ElementGlyphs`): after the spell's name, a 7-pixel mark per
  element changed, with a green ▲ (favoured) or red ▼ (hindered). With marks showing, the name row
  counts as present, so the Shield line moves up above it as before.
- **The Grimoire page** lists the conditions holding under *Where you stand*, each with its shifts.

With `features.elemental_climate` off, every factor is 1 and the HUD shows no marks.

## Testing

- `src/test/.../spell/AffinityAndClimateTest`: the verdicts and multipliers, reactions breaking a
  resistance, the Bestiary's keys, rewards and quiet entries, the climate table, stacking and bounds,
  which conditions hold where, and that no creature in the generated tags is both weak to and
  resisting (or immune to) one element.
- `src/gametest/.../WildercordAffinitiesTest`: frost on a blaze against a husk (x1.5, and "Weak!"
  over it), fire on a hoglin (half) and a blaze (nothing), a snow golem that takes no frost and
  doesn't flinch, a Shatter through the hoglin's resistance, a Runebound resisting its own element, the
  Bestiary entries, fire in the Nether (+20%) and void in the End (+20%) with the HUD told, and both
  config switches.
