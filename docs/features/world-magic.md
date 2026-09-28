# Magic that changes the world

Spells don't just hit creatures: where they land, the ground answers. A fight by a pond, in a
meadow or in the rain plays differently from one on bare stone. In the Cord screen, a rune that
does this says so in its tooltip, in a dark green line under its description (the lang keys
`tooltip.wildercord.world.<interaction>`, from `WORLD_MAGIC_LANG` in `tools/generate_assets.py`):

| Element | Tooltip line |
|---|---|
| Fire | Where it lands: sets grass and leaves alight, melts snow and ice, boils water into blinding steam |
| Frost | Where it lands: freezes water into ice you can walk on, puts out fires and campfires |
| Storm | Where it lands: in water, shocks every foe in the same water |
| Wind | Where it lands: knocks arrows and fireballs away, blows out small fires, scatters loose items |
| Earth | Where it lands: the ground heaves up, throwing foes standing on it |
| Life | Where it lands: grass and flowers bloom, crops grow |
| Void | Where it lands: draws loose items and experience in |

Only **harmful** spells of an element do this (Fireward won't burn the grass round your friend),
except Life, whose heals and blessings bloom too. Runes that already change blocks their own way
(Grow, Icepath, Smelt and the other World runes) are left as they are, and so are Bubble, Root,
Weigh, Shackle and Kindling.

## What each element does

**Fire** (Fire, Explode, Meteor, Inferno, Ember, Flashfire, Primer...)
- Sets up to **3** fires within **2 blocks** of where it lands, on or beside anything that burns:
  grass, flowers, leaves, logs, wool, planks. These are ordinary fires, so they spread and burn out
  by vanilla's rules. No fire is lit where fire can't spread (the `fire_spread_radius_around_player`
  game rule), since it would never go out there.
- Melts up to **6** snow layers, blocks of powder snow and ice (ice turns to water).
- Landing in water, it boils: a cloud of **steam** hangs for **4 seconds**, **2 blocks** across,
  hiding what's behind it. Anyone you could harm inside is blinded for 2 seconds, again each second,
  and left wet. A puddle of **4 water blocks or fewer** boils away completely; a pond just steams.
  Two steam clouds per cast at most.

**Frost** (Frost, Freeze, Chill, Icicle, Coldsnap)
- Freezes the surface of water within **2.5 blocks** into frosted ice, **16 blocks** at most, even
  when the spell sank to the bottom (up to 4 blocks deep). Widen reaches further (up to 5 blocks)
  only when it attaches to the frost effect itself, which only frost effects with a radius take
  (Coldsnap, say); a Widen on the spell's shape leaves the freeze as it is. You can walk on the
  ice. It melts in the light like Frost Walker's ice, and anything still standing after **30 seconds**
  melts back anyway, even in the dark or at night. The thaw is saved with the world: if the server
  stops, or nobody is near when the time comes, the ice melts as soon as its chunk is loaded again,
  so frozen water always comes back. Never around a creature swimming there.
- Puts out up to **6** fires and lit campfires nearby.
- Lava is left alone.

**Storm** (Lightning, Shock, Thunderclap, Ripple, Jolt)
- Landing in water, the shock runs through all the water joined to it, up to **8 blocks** away,
  striking up to **8** creatures in it (not those the spell already hit) for **4 damage** × the
  spell's power within 3 blocks, fading to **2.4** at the edge. **3** conductions per cast at most.
- Storm on a wet target sets off **Conduct**: +50% damage, arcing to two more enemies.

**Wind** (Push, Launch, Dash, Levitate, Windcut, Cyclone, Repel)
- Knocks up to **8** projectiles in flight within **3.5 blocks** back the way the wind blows:
  arrows, tridents, fireballs, even another caster's bolts. Never yours or an ally's.
- Blows out up to **4** fires, and scatters up to **16** loose items and experience orbs.

**Earth** (Tremor, Aftershock, Pelt)
- Heaves **5** slabs of the ground up round where it lands (**2 blocks** out), which settle back
  after a second. They're only a picture of the ground: no block is moved.
- Throws foes standing within 2 blocks upward (0.45, more for a stronger spell).

**Life** (Heal, Regrowth, Venom, Restore, Haven...)
- Up to **3** flowers and tufts of grass spring up within **2 blocks**, on grass or dirt, and a crop
  or sapling it lands on grows a stage, like bone meal.

**Void** (Pull, Wither, Gravity Well, Hollow, Banish...)
- Draws up to **16** loose items and experience orbs within **4 blocks** in toward where it lands.

## Wet

A creature is wet while it's in water or rain, and for **5 seconds** after Tidebreath, a popped
Bubble or a steam cloud. On a wet creature:
- **storm** conducts (+50%, arcing to two more),
- **fire** hits **25% softer**, and dries it,
- **frost** freezes it solid at once, for at least 3 seconds, even a light Chill, ready to Shatter.

## Feats

- **Conductor**: shock five creatures at once through the water they stand in.
- **Icebridge**: walk across water you froze with a spell, before it thaws.

## Rules

- Only a player's spells change blocks, and only where they may build: spawn protection, claims and
  Adventure mode all stop it, and so does a server config with `features.world_changing_magic` (or
  `casting.spells_edit_blocks`) set to false. A monster's spells never change blocks, but still conduct through
  water, heave the ground and blow arrows away.
- Every change comes out of the cast's block budget (32 per strike), and a whole cast, links and
  echoes included, makes at most **24** world changes.
- A passive renewing itself changes no blocks.
- Every change is vanilla's own and temporary or natural: fire, frosted ice, grass and flowers.
