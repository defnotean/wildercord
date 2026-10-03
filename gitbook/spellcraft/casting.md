# Casting


You've threaded a spell. This page is about firing it: tapping or charging, pushing a charge past full and
letting it go on the beat, steadying it by tracing its glyph, the incantation it speaks, what the game checks
when you press the key, how cooldowns and mana work, the small skills that make a caster stronger (rhythm and
leaning), switching between spells, and reading the spell panel beside your hotbar.


---

## Reading a fire preparation

The brief preparation tells you which fire effect is being assembled. Ember closes a hooked spark onto a cinder; Primer burns a pink fuse; Meteor gathers stone beside a falling fire tail. Smelt forms a heated grate, Fireward closes a shield silhouette, and Ashen Veil keeps an open lane between two ash curtains.

Mixed fire effects also show their ingredients: Firestorm braids wind around flame, Steam vents vapor from wet lobes, and Boiling Surge carries hot vapor along a curling wet crest. Phoenix Pyre raises feathered flame wings around a green renewal stem.

![Firestorm's braided wind and flame](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/fire-formations/fire_formation_firestorm.png)

![Primer's charge and pink fuse](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/fire-formations/fire_formation_primer.png)

![Phoenix Pyre's feathered preparation](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/fire-formations/fire_formation_phoenix_pyre.png)

These pictures show the short preparation in a controlled stage. The selected shape still determines delivery: the decoration does not change a Bolt into wings, summon a creature, or grant a shield by itself. The rear casting circle stays behind you. Minimal formation quality keeps each authored outline with less midpoint detail.

## Where the preparation happens

Each initial group prepares its own shape and effects. In `Bolt · Ember · Self · Fireward`, Ember gathers ahead for the bolt while Fireward prepares on you. Both groups share one rear casting glyph.

Burst, Nova and Ring prepare around your body. Zone, Wall, Pillar, Mine, Totem and Vortex prepare at the ground point you aim at. Rain gathers overhead above that point and marks the ground beneath it. Turning your aim during the brief preparation moves an aimed preview with it.

![A Zone preparation at the aimed ground point](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/group-formations/group_formation_zone.png)

![A caster-centered Burst preparation with its glyph behind the player](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/group-formations/group_formation_burst.png)

Effects after a Delay or another link are not part of the initial assembly. The rear glyph can still show the complete written spell.

## Pressing the cast key

**`R`** casts your **selected** spell. Every key can be changed in the game's Controls options, under
**Wildercord** (see [Controls](../controls.md)).

When you cast, the game checks, in order:

| Check | If it fails, you see |
|---|---|
| You're wearing a Cord | Wear a Cord first: the slot above your offhand (E) |
| Your Cord has this spell | Spell 2 needs a Copper Cord |
| The spell isn't cooling down | Recharging... 1.2s |
| The spell has runes that can fire | Spell 1 is empty. Press K to thread runes |
| You can pay for it | Not enough mana (12/29), or for a Blood Price spell, Blood Price needs more than 3 health |

Nothing is spent when a cast fails. If you're short of mana but have a Heart Circle, the message offers to
**overcast** instead: press again within two seconds and a circle cracks to pay for it (see
[Overcasting and wild magic](overcasting.md)).

When a spell goes off, its casting circle opens behind your shoulders and tracks your turns, you move with the spell's
shape (a thrust for a bolt, arms flung up for a zone or a burst, a sweep for a crescent), the beads on your
wrist flare, and everyone nearby hears it. Magic can't be cast while you're dead or spectating, and a cast
key held while a screen is open (chat, the pause menu) simply waits, like a drawn bow.

In **creative** mode spells cost no mana and passives no upkeep, but cooldowns still apply.

## Tap or charge

![A player with raised hands charging a spell, its authored magic circle behind the shoulders](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/circle-bloom.jpg)

**Tap `R`** and the spell goes off at once, at its normal power.

**Hold `R`** and after a quarter of a second you start to **charge**:

- You raise both hands and the spell's **magic circle** opens behind your shoulders as the charge builds: the
  frame first, then its script and star drawing themselves, then a roundel for each rune in turn. Everyone
  around you sees it. In first person the large circle is behind you; a smaller shape-specific focus forms ahead without replacing the spell's delivery.
- A **full charge** takes a second and a half. The circle flares and a chime sounds, and your spell panel
  shows the charge climbing ("40%", "80%") until it blinks **FULL**.
- The beads on your wrist burn brighter, and a hum rises as the charge builds.
- You **walk 40% slower** while you charge.
- **Let go** to cast. The more charge, the more power, up to **+40%** at a full charge:

| Charge | Extra power |
|---|---|
| A tap | none |
| A quarter | +10% |
| Half | +20% |
| Three quarters | +30% |
| Full | +40% |

A release from a fifth of a charge or more leaves your hands with a rush of air and a small kick of the
view. Mana is only spent when you let go, so starting a charge costs nothing.

Some things to know:

- A charge **can't start while the spell is cooling down**. Hold the key anyway and letting go just tells you
  it's recharging.
- Keep holding past full and the charge **overchannels**: stronger and wilder, until it tears loose (see
  [Overchannel](#overchannel-holding-on-past-full) below). A charge that can't overchannel (short of spare mana)
  just waits, and held longer than **12 seconds** it **fizzles** ("The charge fizzles"): letting go then does
  nothing, and you can start again.
- Taking your Cord off, dying or spectating ends a charge.
- A **Focus of Haste** in your focus slot (or held in your offhand) fills a charge 40% faster (see [Casting Gear](../gear.md)).
- The direct "Cast spell 1 to 5" keys only tap: they can't charge.
- Releasing a fully charged spell for the first time earns the feat **Full Charge** (and its advancement),
  which condenses mana toward your next [Heart Circle](../progression/heart-circles.md).

### The reticle

While you charge, **you** (and only you) see where the spell will go, for the spell's first shape:

| Shape | What you see |
|---|---|
| Zone, Rain, Pillar, Totem, Mine | A ring on the ground where you're looking, as wide as the spell |
| Burst, Ring, Nova, Domain, Imprint | A ring on the ground around you (an Imprint's under your feet), as wide as the spell |
| Bolt, Arc, Crescent, Orb, Wave, Wisp, Ricochet, Comet, Cluster, Beam, Spark, Ray, Lance, Prism, Sweep, Stream, Glaive | A faint dotted line as far as it can reach (a Glaive's, as far as it flies before it turns back), and a mark where it meets a block |
| Latch | The dotted line, and a mark under the creature it would take hold of |

## Overchannel: holding on past full

![A caster seen from behind at the third overchannel stage: the fire bolt's circle cracked from its frame inward and shedding sparks, the incantation over it, the screen's edges closing in and the panel showing the third stage](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/overchannel.png)

A full charge doesn't have to be the end. **Keep holding** and the charge **overchannels**: you pour your spare mana
into the circle, and it climbs a **stage** a little over every second.

| Stage | Reached after full | Extra power (on top of the full charge's +40%) | Chance it surges into wild magic |
|---|---|---|---|
| I | 1.2 s | +20% | 7% |
| II | 2.4 s | +40% | 14% |
| III | 3.6 s | +60% | 21% |

- **Your heart sets how far you can go.** Anyone can push one stage. With two working
  [Heart Circles](../progression/heart-circles.md) you can push two, and from the 4th all three. A
  circle cracked by [overcasting](overcasting.md) doesn't count until it mends.
- **It drains spare mana.** From stage I on, the circle drinks a little mana every moment (about 15% of the spell's
  price each second), but it **never touches the mana the spell itself needs**: short of spare mana it simply stops
  climbing and waits. Overchannel never cracks a Heart Circle, and a spell you couldn't afford can't overchannel.
- **You can feel each stage land.** The circle behind your shoulders swells, cracks a little further (white-hot cracks
  running in from its frame) and throws a ring of sparks; a crackle of light runs across it; the hum strains higher and
  wavers; and the edges of your screen close in a little more, with a hairline crack from each corner. Everyone near
  you sees the circle crack and tremble, so they know you're pushing your luck.
- **The surge chance** works like an overcast's: the spell may come out twice, as another element, at double size, as
  a shower of butterflies of light... (see [Overcasting and wild magic](overcasting.md)).
  An overchannelled spell never hurts you with its surge any more than an overcast does. Tracing the spell's glyph
  (below) steadies it.
- Your spell panel shows the stage where the charge was: **✦I**, **✦II**, **✦III**, hotter in colour with each.

### Let go on the beat

The moment a charge **fills**, and the moment each **stage lands**, is a **beat**. Let go just then (within about a
third of a second after it) and the spell leaves with **+10% more power** and a bright chime. While you charge, the
ring on your spell badge closes in on the charge's next beat, pale violet, and glows while it's open. (Outside a
charge the ring shows your [rhythm](#rhythm) as usual, and the two bonuses stack.)

When you let go of an overchannelled or well-timed spell, the line above your hotbar says how it went:
"Overchannel III · on the beat · steadied 92% (+90% power)".

### Holding too long: the channel tears loose

At your last stage the circle starts to **redden and shake**, sparks fly off it in red, and the ring on your badge turns
red as it closes on one more beat. Hold on past it (another 3 seconds) and the channel **tears loose**:

- the circle bursts and the spell **fizzles** (letting go afterwards does nothing),
- the loose magic **surges** harmlessly: butterflies of light, a blink a few blocks aside, a moment of floating (with
  a slow fall after), slowed time or healing for everyone near,
- you're **dazed** for a second and a half (you can't cast or charge, and you move slowly),
- and you lose **30% of your mana**.

Tearing loose **never costs health**: on one heart you walk away on one heart. It's the price of greed, not a trap: let
go at any stage before it and you keep everything you built.

## Sigil tracing

![A glyph of four violet strokes round the crosshair while a spell charges, with a gold path traced along it and the accuracy shown below](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/sigil-trace.png)

While a spell charges, a **faint glyph** of three to five straight strokes appears round your crosshair. It's made
from the spell's own runes, so a spell always has the same glyph, and a longer spell draws more strokes.

**Hold sneak while charging** to steady your hands: the camera **holds still** and moving the mouse draws a point of
light along the glyph instead. It starts at the bright dot; follow the strokes in one unbroken line. Your accuracy shows
under the glyph as you go.

- A good trace **steadies** an overchannel (a perfect one takes away 60% of its surge chance) and adds up to **+8%
  power**. A scribble earns nothing.
- **Not tracing loses nothing.** It's a skill for those who want it, never a requirement.
- You have to really steady your hands: a trace only counts as far as the time you spent holding sneak allows (about a
  second for a full one), however well it was drawn.
- The glyph is the same share of your screen at every GUI scale, and the mouse moves the point at your own
  sensitivity, held within a comfortable range so neither the slowest nor the fastest setting makes it awkward.
- In the **Magic visual settings** screen (its key is unbound until you set it, under **Wildercord** in Controls), **Sigil tracing** turns it off
  (sneaking while charging then just sneaks), and **Trace assist** sets how much help you get: *None*, *Light* (the
  default: a little forgiveness, and the point is drawn gently back onto the line) or *Strong*.

## Incantations

![A charging caster seen from the front, three glowing syllables in a line of script over their shoulders](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/incantation.png)

Every rune has its own spoken **syllable**: "vo" for Bolt, "ign" for Fire, "hrim" for Frost, "sei" for Split, "mae" for
Heal, and one for every other rune, no two alike. While you charge, your spell's syllables **rise one by one from
behind your shoulders**, each as its rune's roundel opens on the circle (with a soft whisper under its note), and
gather into a line of glowing script over your shoulders: the spell's **incantation**.

- **Everyone near you can read it**, clearest up close and fading with distance. In a duel, a sharp-eyed opponent can
  read what's coming, and learn which syllable means which rune.
- **A tap is cast without a word.** Only a charge speaks, so a quick spell keeps its secret, and a charged one pays for
  its power with a tell.
- The incantation thins away the moment the spell leaves your hands.
- In first person your own incantation is behind you, out of your way; in third person you can watch it form.
- **Incantations** in the Magic visual settings screen chooses whose you see: all of them, all but your own, only your own, or
  none. Hidden ones are silent too.

## Cooldowns

After a spell goes off it has to recharge before you can cast **that spell** again. Each spell has its own
cooldown, so on a Cord with several spells you can cast one while another recharges.

- The cooldown comes from the spell's **cost**: about **a second for every 20 mana**, never less than half a
  second and never more than twenty.
- **Rapid** halves it and **Vow** makes it four times longer; **Celerity** and the 5th Heart Circle's
  **Flow** shorten it. It's never under a quarter of a second.
- [Secret spells](secret-spells.md) you've found take half as long again.
- The readout, the spell panel and the spell wheel all show your real cooldown.
- Cooldowns are **kept** when you log out or die, so leaving the game doesn't reset a long one.

The exact rules are on [How a Spell Is Read](reading-spells.md#cooldown).

## Mana

Every Cord gives you a pool of mana and refills it steadily:

| Cord | Max mana | Mana a second |
|---|---|---|
| Twine | 100 | 5 |
| Copper | 150 | 6 |
| Amethyst | 225 | 7 |
| Echo | 300 | 8 |

- Mana refills all the time, a little every quarter of a second, whatever you're doing.
- **Meditate** to refill it twice as fast: sneak and stand still on the ground for about a second while
  wearing your Cord (not while using an item). A ring of soft lights turns round your feet. Move, stand up or
  start using something and it stops.
- **Every source stacks**: Mana Crystals, Heart Circles, Reservoir and Wellspring on your Cord, Clarity and
  Mana potions, ley lines, a Wellstone, a mana storm, a familiar, a Focus of the Deep Well. Hover the mana
  badge in the Cord screen to see exactly where yours comes from. All of it is on
  [Mana](../progression/mana.md).
- A spell that costs more than your whole pool can't be cast normally (the readout turns red); it can only
  be overcast.
- After you die you come back with an **empty** pool, which refills as usual.
- Every point of mana you spend on spells **condenses** toward your next
  [Heart Circle](../progression/heart-circles.md). Passive upkeep doesn't.

## Rhythm

Cast again **right as your last spell comes off cooldown** and you're casting **on the beat**. Each cast on
the beat adds a step, up to three, and each step makes that cast stronger:

| Step | Extra power |
|---|---|
| 1 | +8% |
| 2 | +16% |
| 3 | +24% |

- The beat opens the moment the spell you just cast is ready again, and lasts a quarter of its cooldown,
  but never less than a quarter of a second or more than half a second.
- The next cast can be **any** spell, as long as it lands inside the beat.
- **Early or late starts over.** Pressing a spell that's still recharging before the beat, casting something
  else too soon, or letting the beat pass all drop you back to nothing. Nothing else is lost.
- On your spell panel, a **gold ring closes in** on the spell badge as the beat comes, and glows while it's
  open. Each step shows a note (♪) above the panel, and you hear a chime that rises with each step, with
  "♪ Rhythm x2 (+16% power)" above your hotbar.
- Three on the beat is the feat **In Rhythm**, which the 7th Heart Circle asks for.

Short spells are easiest to keep in rhythm, because their beat comes round quickly.

## Leaning

Every spell you pay for grows your [affinity](../progression/affinity.md) with the elements
of its effects, by the mana each one's effects cost (the runes inside a Knot count too), and so do everyday things
that fit each element. Your **leaning** is your deepest affinity: once one element reaches **level I** and has
**a quarter more points than any other**, your magic **leans** toward it:

- your Heart Circles turn toward its colour, and a charging circle for a spell with no effect of its own takes
  its colour,
- you're told "Your magic leans toward Fire..." and earn the feat **Leaning**.

Leaning is the face of your affinities, not a bonus of its own: the power comes from each affinity's level (+3% a
level with that element, for every element you grow). Your leaning can change: if another element pulls far enough
ahead, your magic leans toward that one instead. The Grimoire page and the heart badge show your leaning.

## Switching spells

A Copper Cord holds 2 spells, an Amethyst Cord 3 and an Echo Cord 4, and the
[Tome of the Fifth Page](../gear.md) adds a fifth while it's in its slot (or your offhand). There are four
ways to pick one:

- **Tap `V`** to move to the next spell. It steps through every spell you can use, the tome's included, and
  back round to the first. The line above your hotbar shows the spell's number and its runes.
- **Hold `V`** for the **spell wheel** (below).
- **Click a spell's row** in the Cord screen: the spell you're editing is also the one you'll cast.
- **Bind the "Cast spell 1" to "Cast spell 5" keys** to cast a spell directly, without selecting it. They're
  unbound until you set them, and they only tap (no charge).

The spell you have selected is the one your wrist beads show.

### The spell wheel

![The spell wheel: four spells in a ring around a centre showing the pointed spell's mana and cooldown, each with its name and rune icons outside the ring](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/spell-wheel.png)

Hold `V` for a quarter of a second and your spells fan out in a ring (you need at least two spells: a Cord
with two or more, or the Tome of the Fifth Page in its slot or your offhand). Each shows its number, its name and runes,
a coloured bar for its first element, and a dark shade while it's still cooling down. The centre shows the
pointed spell's mana and cooldown. While the tome counts, its spell is on the wheel too, as
spell 5.

- **Point and let go:** move the mouse toward a spell and let go of `V` to select it.
- **Let go without pointing** and the wheel stays open. Then click a spell, press its **number**, or point
  at one and press `V` or `Enter` again.
- **Right-click** or press **`Esc`** to close it without choosing.

## The HUD

![The spell panel to the right of the hotbar: a badge with the spell's number, twelve rune icons and a cost of 168, a mana bar with a gold mark, and 218/380 mana with an upward chevron](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/hud.png)

The **spell panel** sits to the right of your hotbar. It moves aside for an offhand item or the attack indicator
beside the hotbar; where there's no room beside them it sits on top of them instead, and only on a very narrow
window does it tuck into the corner. It's hidden while you aren't wearing a Cord.

| Part | Shows |
|---|---|
| **The badge** | The selected spell's number: gold when you can cast it, red when you can't afford it, dim when the spell is empty. A dark shade drains out of it as the spell recharges. A gold ring closes in on it as your [rhythm](#rhythm) beat comes; while you charge, a pale ring closes on the charge's own beats (red as an overchannel nears tearing loose). |
| **Dots under the badge** | One for each spell you can use, the selected one in gold (the tome's in violet). |
| **Top row** | The spell's rune icons (with "+2" and so on when there are too many to fit) and, on the right, its cost in mana, or in health with a heart for Blood Price. The cost turns red when you can't afford it. |
| **The mana bar** | Your mana. A **gold mark** shows how much this spell will take; it turns red when you haven't enough. A soft shine runs along the bar while you're meditating or under Clarity. |
| **Bottom row** | Your mana ("218/380"). It turns bright, with a small up-arrow, while your regeneration is boosted (violet on a ley line or near a Wellstone). On the right: your **charge** ("60%", "FULL", then "✦I" to "✦III" while it overchannels), or the **cooldown** left ("1.2s"), or, when neither, what your **passives drain** ("-3.4/s", red if you can't keep them up). |
| **Above the panel** | The spell's **name** in its colour (a secret spell's own name only once you've found it), **♪** notes for your rhythm steps, **✦** and a number in red for Heart Circles cracked by overcasting, and, when you're wearing a Shield, its strength and time left ("Shield 12 · 28s"). |
| **After the name** | The **elemental climate** where you stand: a small mark for each element it changes (a flame for fire, a snowflake for frost, a bolt for storm...), with a green **▲** if that element hits harder here or a red **▼** if it hits softer. In the Nether you'll see fire ▲ and frost ▼. See [Creature Affinities and Climate](affinities.md#elemental-climate). |

If the selected spell is empty, the panel just shows your **Open Cord** key (`K` unless you've changed it), a
reminder that the Cord screen is where you thread it.

## How a spell sounds and looks

Every spell reads from how it's built:

- **Its shape sets the gesture.** Each shape leaves your hands its own way, with its own sound: a flick
  (Spark, Ray, Touch), a throw (Bolt, Comet, Orb...), a line of light (Beam, Lance, Stream...), a slash
  (Crescent, Cone, Barrage...), a blast (Burst, Nova, Ring, Pillar), a seal pressed into the ground (Zone,
  Domain, Totem...), a call to the sky (Rain, Constellation) or an aura (Self, Orbit). You hold each kind
  differently while you charge (drawing back for a throw, palms down for a seal, arms up for a call), and no
  two shapes of a kind sound alike.
- **Its size follows its cost.** A cheap spell opens a small circle under your feet; a costly, charged, high-tier
  one opens a great circle, and the biggest leave your hands with a shove.
- **Its melody.** While you charge, every rune plays its own note as its roundel appears on the circle: a knock
  for a shape, a glass note for an effect, a bell for a modifier, a clink for a link. Each spell has its own tune,
  so you (and anyone near) can learn to hear it. Your charge hums in your first element's key.
- **Modifiers show in gold.** As the spell leaves: a gold ring for each Amplify (and a low rumble), a crackle
  for Overcharge, a ring closing in for Focus, one racing out for Widen, sand falling for Extend, a streak for
  Quicken (and a higher snap), three small circles and a chord for Split, a needle for Pierce, an oath ring and a
  bell for Vow, a red ring and a drop for Blood Price. Bolts show theirs in flight: fatter for Amplify and
  Overcharge, thin for Frugal, a needle for Pierce, a mote circling it for Homing. Each Volley shot and each Chain
  jump is a step higher up the scale.
- **Links show in violet.** Delay ticks down, Pulse ticks each beat, Echo ripples before its replay, On Hit rings
  at each creature it hands on to (a step higher each), On Kill tolls, a condition clicks open or tocks shut, and
  On Land, On Hurt and On Low Health put a violet seal at your feet when they're armed.
- **Fields keep time.** A Zone beats softly each pulse, a Totem's bell climbs a step each time it strikes (you can
  count them), a Domain tolls, Orbit's orbs each chime their own note, a Stream, a Latch and a Ricochet climb the
  scale as they strike or bounce, and a Constellation rings each star it finds.
- **What's left behind.** A spell of any size lands with a trace of its element for a moment: embers, rime, sparks,
  a gust, cracks, petals, a dark wisp, glyphs, golden ticks or drops.
- A tap has its own little snap as it leaves, a cast that can't go off fizzles, and the HUD chimes and flashes
  gold when a spell with a wait of a second and a half or more is ready again.

## What other players see

Casting is meant to be read by everyone around you:

- the Cord on your wrist, with a bead for each rune of your ready spell,
- your hands raised and your spell's circle opening behind your shoulders while you charge, cracking and trembling if
  you overchannel,
- your spell's [incantation](#incantations) rising over your shoulders as you charge,
- the circle under your feet and the shape's pose when a spell goes off,
- your Heart Circles' rings turning round you as you cast and meditate.

Anyone who knows the runes can read what you're about to cast from your circle, or from the syllables you speak. See
[Magic Circles](magic-circles.md).

Big impacts (explosions, bursts, pillars, Domains, meteors) shake the camera of everyone nearby, a charged
release kicks your own view, and one of your spells landing a heavy hit gives a small punch. Inside a Domain
the edges of your screen are tinted. All of these follow the game's own Screen Effect Scale setting.
