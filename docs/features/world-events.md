# World events

Now and then the world's magic stirs by itself. The events below happen only in the Overworld, only
near players and only in ground that's already loaded. Monsters never come on Peaceful. Nothing an
event does is permanent.

## Mana storm

**When:** about once every three in-game days, over a stretch of ley line near a player. A region
that just had one won't get another for a day and a half. At most two storms rage at once.

**What happens:** for 3 to 5 minutes, everything within 96 blocks of the storm's heart is under it.
The sky and fog take a faint violet cast. Ley lines surge brighter, and violet arcs crackle between
points of the line and across the sky. You're told when you enter and leave it, and when it passes.

Under the storm:
- mana regenerates **twice as fast** (+100%, on top of everything else)
- spells cost **25% less**
- every cast has a **10% chance to surge**:
  - **Swell** (most often): the spell has 60% more power.
  - **Stray element**: a spell of a random element (fire, frost, storm, wind, earth, void or arcane)
    flies along with it.
  - **Echo**: the spell goes off again a moment later, for free.
  - **Backfire** (least often): the storm's mana kicks back. You take up to 3 damage (never
    enough to kill), get shoved back and lose 10 mana.

**Feat:** *Stormcaller*: cast 20 spells under a mana storm.

## Starfall crater

**When:** at night, rarely: about once in five nights for a given player, and at most one fallen star
per world every half a day.

**What happens:** a star streaks down 60 to 150 blocks away and lands with a boom you can hear from
far off. You're told which way it fell. If the mobGriefing rule is on and the ground may be built on
(spawn protection and claims are respected), it blasts a small, shallow crater out of the natural
ground. Otherwise it only leaves scorch marks. A **Fallen Star** lies in the middle, and a column of
starlight marks it for five minutes so you can race to it.

When someone comes within 40 blocks, **2 to 4 Runebound** (one of them an Adept) rise to guard it.
The star won't open while they stand.

**Rewards:** use the star to break it open for:
- a **Tier III rune** (one time in four a **Tier IV** rune)
- a **Mana Crystal**
- 30 experience

Then it crumbles and the crater fills back in. An unlooted star fades after 20 minutes.

**Feat:** *Stargazer*: loot a Fallen Star.

## Rift siege

**When:** at night near a settled place (a base or a village with enough chests, furnaces,
lecterns and the like), rarely: about once in six nights for a given player. There's at most one
open rift per world, and none for a day after the last.

**What happens:** a tear of violet light opens 14 to 26 blocks away, with a spell circle turning on
either face. Three waves of Runebound pour out over about two minutes. Beat a wave early and the
next comes sooner.

| Wave | Monsters (1 player) | Extra per extra player | Adepts |
|---|---|---|---|
| 1 | 3 | +1 | none |
| 2 | 4 | +1 | 1 |
| 3 | 4, and the **Riftcaller** | +2 | 1, +1 per 2 extra players |

A wave holds at most 8 monsters. The **Riftcaller** is a Runebound illager with about four times a
vindicator's health and a boss bar.

**Closing it:** beat every wave, or strike the tear with spells of **three different elements**.

**Rewards:**
- **2 runes** (mostly Tier II, sometimes III, rarely IV)
- **2 to 4 Blank Runes**
- a **Mana Crystal**
- 60 experience

If nobody closes the rift, it closes itself 90 seconds after its last wave, or after 30 seconds with
nobody near. It gives nothing then, and takes its monsters back with it.

**Feat:** *Riftwarden*: close a rift (everyone who stood against it gets it).

## For operators

`/wildercord event <mana_storm|starfall|rift> [here]` starts an event as the server would place
it. With `here`, it happens right by you instead. All the frequencies and numbers are in one place,
`cast/events/EventRules.java`, ready for a config file.
