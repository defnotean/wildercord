---
title: Sword Masters
nav_order: 5.5
has_children: true
permalink: /masters/
---

# Sword Masters

The Sword Masters are the hardest swordsmen in Wildercord. There are **16 Masters**, one for each [breathing method]({{ '/progression/breathing-methods/' | relative_url }}). A Master only fights players who choose to fight it, in a short, deadly **trial**. Beat one and you earn a permanent record and a new lesson.

| Master | Element and feel | Signature moves | First clear teaches |
|---|---|---|---|
| [Ember Master]({{ '/masters/ember/' | relative_url }}) | Fire; steady pace, fire that lingers after the cut | Cinder Wake, Kiln Ring | Echo |
| [Gale Master]({{ '/masters/gale/' | relative_url }}) | Wind; fastest, slips sideways and strikes where you stood | Crosswind Reprise | Afterimage |
| [Stone Master]({{ '/masters/stone/' | relative_url }}) | Earth; slowest and heaviest, braces after every attack | Stone Fracture, Stone Fault March | Sunder |
| [Rime Master]({{ '/masters/techniques/' | relative_url }}#rime) | Frost; long, even wind-ups | Rime Lattice | Ward |
| [Thunder Master]({{ '/masters/techniques/' | relative_url }}#thunder) | Storm; long, even wind-ups | Thunder Chain | Pierce |
| [Verdant Master]({{ '/masters/techniques/' | relative_url }}#verdant) | Life; long, even wind-ups | Verdant Bloom | Rally |
| [Hollow Master]({{ '/masters/techniques/' | relative_url }}#hollow) | Void; long, even wind-ups | Hollow Pull | Bind |
| [Starlit Master]({{ '/masters/techniques/' | relative_url }}#starlit) | Arcane; long, even wind-ups | Constellation | Burst |
| [Hourglass Master]({{ '/masters/techniques/' | relative_url }}#hourglass) | Time; long, even wind-ups | Rewind | Bind |
| [Crimson Master]({{ '/masters/techniques/' | relative_url }}#crimson) | Blood; long, even wind-ups | Blood Frenzy | Infuse |
| [Tide Master]({{ '/masters/techniques/' | relative_url }}#tide) | Brine; long, even wind-ups | Undertow Ring | Wave |
| [Iron Master]({{ '/masters/techniques/' | relative_url }}#iron) | Metal; long, even wind-ups | Anvil Verdict | Pierce |
| [Dune Master]({{ '/masters/techniques/' | relative_url }}#dune) | Sand; long, even wind-ups | Shifting Sands | Bind |
| [Echo Master]({{ '/masters/techniques/' | relative_url }}#echo) | Wind; Ember's pace | Tolling Bell (leaves you sick and dizzy) | Wave |
| [Dawn Master]({{ '/masters/techniques/' | relative_url }}#dawn) | Arcane; Ember's pace, braces after every attack like Stone | Noon Glare (blinds you) | Ward |
| [Venom Master]({{ '/masters/techniques/' | relative_url }}#venom) | Life; Ember's pace | Serpent Coil (poisons you) | Bind |

See also:
- [Master Techniques]({{ '/masters/techniques/' | relative_url }}): all 417 named combos of all 16 Masters, with each strike's shape, answer and damage.
- [Master Forms]({{ '/masters/master-forms/' | relative_url }}): Wall Turn, Cinder Lunge, Reed Slip and Stone Hinge, which an Ember, Gale or Stone clear lets you learn.
- [Combat Presentation]({{ '/masters/combat-presentation/' | relative_url }}): articulated combat and the look of fights.

## How to get a trial

Masters don't live anywhere in the world. A [wandering duelist]({{ '/world/creatures/' | relative_url }}#wandering-duelist) calls one for you.

1. Reach **Aura Form** or **Heart Circle VIII** (see [Aura]({{ '/progression/aura/' | relative_url }}#form) and [Heart Circles]({{ '/progression/heart-circles/' | relative_url }})).
2. Play in Survival, on any difficulty except Peaceful, with no duel or spar running.
3. **Sneak and use** a wandering duelist. It offers to call a Master. Sneak and use it again within 10 seconds.
4. A Master appears nearby and waits **one minute** for you. Only you can accept it.
5. Talk to the Master. It warns you the trial is lethal. Talk to it again within **10 seconds** to accept.

The Master you get matches the duelist's breathing method: a Tide duelist calls the Tide Master, a Venom duelist the Venom Master, and so on. If the duelist breathes Echo, Dawn or Venom, or **you** do, that Master comes. A duelist with no matching Master calls the Ember Master.

You can also type `/master challenge <name>`, where the name is one of `ember`, `gale`, `stone`, `rime`, `thunder`, `verdant`, `hollow`, `starlit`, `hourglass`, `crimson`, `tide`, `iron`, `dune`, `echo`, `dawn` or `venom`. The same Form or Circle VIII requirement applies.

The Master needs clear, level ground around you. Only one trial can run in the same area at a time.

## The trial

- **Lobby:** once you accept, the trial starts in **30 seconds**. Type `/master ready` to start sooner.
- **Joining:** up to **8 players** can fight together. Each one must opt in: talk to the Master twice, or type `/master join` while standing near it. Being in your party doesn't sign anyone up.
- **Arena:** stay within **24 blocks** of where the trial began. Leaving, dying or disconnecting removes you from the trial.
- **Locked roster:** the Master's health is set when the trial starts. It has 480 health alone and 55% more for each extra challenger. People who leave or die don't make it easier for the rest.
- **Outsiders:** players who didn't join can't hurt the Master, and it can't hurt them.
- **Time limit:** a trial ends after **20 minutes**. If every challenger is gone, the Master leaves after 10 seconds.

Use `/party invite <player>` before a group trial so your spells and arts spare each other. See [Playing together]({{ '/social/playing-together/' | relative_url }}).

## Reading a Master

Every attack follows the same three beats: a **wind-up**, a **locked aim**, and an **open recovery**.

1. **Wind-up.** The Master draws back. The boss bar names the move, and signature moves also show a banner with a hint.
2. **Lock.** In the last moment before the hit, its aim locks. It won't turn to follow you after that, so a late sidestep still works.
3. **Recovery.** After the hit, the boss bar says **Open to counters**. This is your window to strike.

Read the **shape** of each strike and answer it:

| Shape | What it looks like | Answer |
|---|---|---|
| Broad cut | A wide arc in front | Step back out of reach, or to its side or back |
| Low cut | A sweep near the ground | Jump |
| High cut | A cut at head height | Crouch |
| Lane | A narrow thrust or line | Sidestep |
| Whirl | A spin all around it | Leave its 3.75-block radius |

### Tempo

Techniques chain one to four strikes. For Ember, Gale, Stone, Echo, Dawn and Venom, the first strike of a chain takes about **0.6 seconds** to land and each later strike about **0.4 seconds**. Heavy strikes take longer and quick ones less, but no strike lands in under **0.25 seconds**. Gale is about 15% faster than this, and Stone about 12% slower.

The other ten Masters (Rime to Dune) wind up every strike for **0.6 to 0.7 seconds**, so they are easier to read, and stay open for **0.7 to 0.8 seconds** afterwards.

Each Master's exact timing is on [Master Techniques]({{ '/masters/techniques/' | relative_url }}).

### Guards and braces

Most Masters raise their guard after every **third** attack. Stone and Dawn brace after **every** attack. A guard halves blows from the front. It can also cut down spell bolts flying at it from the front.

To beat a guard:
- Hit it from the side or back.
- Hit it with an **axe**, which breaks the guard and leaves the Master open for 1.5 seconds.
- Make it spend Aura. A Master has finite Aura. When it runs low it must stop for **2 seconds** to catch its breath ("Catching breath" on the boss bar).

### Spells and the pursuit dash

If you hold a spell charge for more than a moment, a free Master may **dash at you**. It warns for 0.4 seconds, dashes along a straight line, then marks a short strike lane for another 0.4 seconds. Release your spell early, sidestep the lane, back away, take cover or parry. If it reaches you, the hit can break your held spell.

If you stay far away or up high, the Master throws a **crescent volley** of flying blades instead. More challengers mean more blades. Sidestep them.

### Phases

At two-thirds and one-third health, the Master reorders its pattern. It never shortens a warning you have already learned.

## Rewards

Your **first clear** of each Master is recorded forever and teaches you a [technique part]({{ '/progression/techniques/' | relative_url }}). The table at the top of this page lists the part each Master teaches. Some parts are taught by more than one Master: **Bind** by Hollow, Hourglass, Dune and Venom; **Pierce** by Thunder and Iron; **Ward** by Rime and Dawn; **Wave** by Tide and Echo.

The first three schools also open a Master form:

| Master | Technique part | What it also opens |
|---|---|---|
| Ember | Echo | Cinder Lunge (at Sovereign) from an Ember duelist |
| Gale | Afterimage | Reed Slip (at Form) and Wall Turn (at Sovereign) from a Gale duelist |
| Stone | Sunder | Stone Hinge (at Sovereign) from a Stone duelist |

See [Master Forms]({{ '/masters/master-forms/' | relative_url }}) for how to learn and use those forms.

- Every challenger still in the arena and alive when the Master falls shares the clear.
- If you already know the technique part, only the clear is recorded.
- There is no item or XP drop, and winning again gives nothing new.
- Type `/master victories` to see which Masters you have cleared.

## Retrying

You can retry as often as you like. A failed trial costs you nothing but the fight itself. Find a duelist and ask again, or use `/master challenge`.

## Tips and counterplay

- Watch the boss bar. It names every move, and tells you when the Master is guarding, open or out of breath.
- Punish recoveries, not wind-ups. Swinging into a wind-up gets you hit.
- A spell can interrupt a wind-up early, but not in its final locked moment.
- In a group, spread out. More challengers add more lanes to some moves, but one move never hits the same player twice.
