# Cinder Bailiffs and Ferns

> **New in 0.10.0-alpha.**
> Focused crafting, paid spell interactions, ecology, combat and visual checks
> have passed. Multiplayer and sustained population testing remain open.

A Cinder Bailiff is a broad, four-footed woodland animal with a fired-clay
breastplate, a brush tail and three opening back vents. It tends a cooled fern,
settles beside the root after a meal, and warns intruders before blowing a
short fan of ash. The warning and attack use physical clay and ash fragments.
Watch its posture and vent order to decide when to leave or answer it.

![A Cinder Bailiff browsing beside a cooled fern in a supplied native test scene](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/ember/bailiff_browse.png)

## Find a living patch

Cinder Fern patches can appear on dry ground in newly generated Overworld
**forests and wooded badlands**. Existing explored chunks are not retrofitted.
The fern has a young, prepared and mature stage, with separate cooled and hot
frond appearances. A young patch needs time to grow before it can attract a
Bailiff.

Natural Bailiff admission requires suitable dry ground and a real **mature,
cooled fern within four blocks horizontally and one block vertically**.
Nearby populations are limited: two living Bailiffs within the local 32-block
area prevent another natural admission. Ordinary spawn attempts still depend on
the biome, difficulty, spawn limits and chance; planting a fern does not summon
an animal or promise one will arrive.

Natural Bailiffs are disabled in Peaceful. An animal already present remains
alive and can browse or rest, but cancels its aggression. Spawn eggs and commands
remain available. The creature provides **no special death loot or experience**.

## Make a fern in an older world

Craft **one ordinary fern + one clay ball + one charcoal** to make one Cinder
Fern root. The recipe is shapeless and fits your inventory crafting grid. Clay
unlocks its recipe-book entry. Plant the root on grass, dirt, coarse dirt,
rooted dirt or podzol with no water occupying the plant cell.

A cooled root grows through its three stages during ordinary random ticks when
the local brightness is at least eight. Hot fronds do not advance naturally;
rain above the plant can cool them. There is no fixed maturation countdown.

Interact with a **mature, cooled** fern to gather one replantable root item.
The original root stays in the ground and returns to its young stage. You need
a clear view, ordinary building and claim permission, and close interaction
range. Ordinary harvesting still works when the server disables spell terrain editing; claims and building rights continue to apply. A hot or immature fern gives no harvest. Breaking the plant removes the
root instead of performing this retained-root harvest.

## Tend it with actual spell outcomes

Use a delivery such as **Touch** that actually hits the plant. These interactions
belong to successful spell outcomes, rather than the colour of a passing cast.

| Actual owner effect | Result on a fern | Limit or refusal |
|---|---|---|
| **Grow** | Advances a cooled fern by one stage | A hot or already mature root does not advance |
| **Fire** | Heats the fronds and advances growth by one stage, up to maturity | An already hot, mature root is unchanged |
| **Tidebreath** | Cools the fronds | Already cooled roots do not gain growth |
| **Frost or Freeze** | Cools the fronds | Already cooled roots are unchanged |

One mana payment can admit **one changed fern cell**, shared by linked deliveries
and Echo. An Echo is not another free maturation step. A separate paid cast can
advance the root again. World-edit permissions and the cast's block budget also
apply. Passive, Creative, spectator or invalid caster contexts do not earn this
paid tending interaction.

## What the Bailiff eats

A Bailiff searches the loaded neighborhood for mature, cooled fronds. It must
physically reach the plant, see it and browse uninterrupted for **two seconds**.
A successful meal changes the fern from mature to prepared; the root remains,
and no player item is awarded. Two animals cannot consume the same mature stage
twice.

The animal settles beside that actual plant for up to **eight seconds** and waits
**one minute** before another meal. Removing or heating its resting plant, leaving
it, or ordinary damage ends the current rest. Its meal deadline remains; disturbing
it does not make another meal immediately available. The saved home and exact
meal, rest and attack deadlines survive a world restart. Reloading cancels a
partly completed attack rather than resuming a stale fan.

Once a real meal has established a home fern, close visible players near that
plant may trigger its territorial warning. It also retaliates against living
attackers. Creative and spectator players are excluded from its attack targets.

## Read the three vents

| Phase | What to watch | Your opportunity |
|---|---|---|
| **Warning — 1.2 seconds** | The creature braces, vents rise, ceramic clicks and low ground fragments mark three lanes | Move around the marked sector, retreat, or answer the warning |
| **Fan — 0.9 seconds** | Three vents fire in sequence, six ticks apart | Its facing is committed; move out of the original lanes |
| **Recovery — 2 seconds** | Vents settle and ash falls | Reposition before another threat |

The three lanes point **30 degrees left, straight ahead and 30 degrees right**,
each with a narrow 15-degree half-angle. Together they cover a 90-degree sector
out to four blocks. The fan requires line of sight. One creature can be struck
only **once in the entire fan**, for four physical damage before ordinary armor
and other defenses. It does not burn the ground, ignite victims or leave a
damage-over-time effect.

The creature has a finite attack rest. An answered warning extends that rest
as needed; swapping spells does not let you renew the same payment's answer.

## Answer the warning

- A **genuine damaging direct player melee hit during Warning** interrupts it.
  Its retaliation can return after the finite rest, so striking is not taming.
- **Frost or Freeze** can interrupt Warning when the actual hit successfully
  applies fresh cold state. They keep their ordinary damage and cold behavior.
  A blocked, unchanged or ineligible result is not a cooling answer.
- Move sideways outside the committed sector, retreat beyond its reach, or put
  real cover between yourself and the fan. During Warning, it does not keep
  steering the marked direction to follow your sidestep.

Cooling only answers **Warning**; it does not cancel an already committed Fan.
The cooling interaction requires a living Survival caster, a visible close
target and one admission per shared mana payment.

**Tidebreath's creature-target effect normally helps only you and your allies.**
A wild Bailiff does not become an ally because you aim a helpful spell at it.
Tidebreath can still cool the **fern** through an actual block hit. Use Frost or
Freeze for a wild creature's warning, or rely on physical positioning and melee.

## Server settings

In `config/wildercord.json`, `creatures.cinder_bailiff` controls natural Bailiff
spawns. The shared `creatures.wildlife` switch and `wildlife_spawn_multiplier`
also apply. Commands and eggs remain usable when natural spawns are disabled;
these creature switches do not remove existing fern plants.

After reloading settings, the placement predicate reads the current switches.
Spawn-table weight changes take effect on the next world load. This chapter
covers the Bailiff and fern; it adds no quest, relic reward or lore-book chain.

## Read the fronds and warning

![All six supplied young, prepared and mature cooled/hot fern states](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/ember/fern_states.png)

![An actual retaliatory warning, with three raised ceramic vents and physical clay fragments](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/ember/bailiff_warning.png)

A creature already standing beside a visible mature root can begin browsing there. Retreating beyond its warning reach or behind a wall releases an idle attacker target so it can return to feeding. Once its warning begins, the fan keeps its original direction; move sideways rather than expecting the creature to track you.
