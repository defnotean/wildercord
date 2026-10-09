# Wildercord lore canon

This is the reference for the story behind Wildercord. New text should agree with it. In-game text should stay short and plain. Combat instructions go in lesson lines and books. This page is background only.

Status labels:
- **Shipped**: in the game today, with the source file named.
- **Canon**: agreed background for content still being built, such as the six newer breaths and the Masters who are not in the game yet. Build to it and do not contradict it. Mark it Shipped once it is in the game.

## 1. The two traditions

The world has two ways to fight. Most heroes learn some of both.

- **Magic, the Heart and the Cord**
  - A caster's Heart grows through **Heart Circles** I to X and beyond, with Archmage at VIII.
  - Spells are written from runes on a **Cord**, and discoveries go in the **Grimoire**.
  - Element marks meet in **reactions** such as shatter, conduct, wildfire, implode and collapse, plus the newer ones (overload, fracture, blight, unweave, rupture and elapse).
  - Secret spells hide behind riddles on Torn Pages. Shipped: `spell/Feats.java`, `spell/Secrets.java`, `cast/Grimoire.java`.
- **Aura, the swordsman's path**
  - A fighter learns one **breathing method** and grows through five stages: **Glow, Flow, Edge, Form and Sovereign**.
  - **Techniques** are cuts the fighter writes themselves.
  - **Ways** (Blade, Bulwark, Shadowstep and Banner) are creeds chosen at the **Crossroads**.
  - A **lineage** joins a master and a disciple.
  - Shipped: `aura/`.

Canon rule: neither tradition is "better". Teachers of one respect the other. The Masters' trial is open at Aura Form *or* Heart Circle VIII.

## 2. Breathing methods

Each method is a way of breathing taught by people, not a gift from a god. Each has a duelist voice and a Master (section 3).

| Method | Element | Breath (one line) | Status |
|---|---|---|---|
| Ember | fire | Breathe as a banked fire breathes: slow, deep, and hot at the heart. | Shipped |
| Rime | frost | Breathe as frost settles: so still the air forgets to move. | Shipped |
| Thunder | storm | Breathe in the moment before the storm breaks, and hold it there. | Shipped |
| Gale | wind | Breathe as the high wind breathes: never twice in the same place. | Shipped |
| Stone | earth | Breathe as the mountain does, once a century, and do not move. | Shipped |
| Verdant | life | Breathe as the green things do, taking in the light and giving it back. | Shipped |
| Hollow | void | Breathe out until nothing is left, and let the hollow pull. | Shipped |
| Starlit | arcane | Breathe in time with the far lights, and they lend you theirs. | Shipped |
| Hourglass | time | Breathe in the space between one grain falling and the next. | Shipped |
| Crimson | blood | Breathe to the beat of the blood, and the blood answers. | Shipped |
| Tide | water | Breathe as the sea does: rise, fall, and never fight the pull. | Canon |
| Iron | metal | Breathe as the forge breathes: heat, strike, cool, and again. | Canon |
| Dune | sand | Breathe as sand shifts: nothing stays where you left it. | Canon |
| Echo | sound | Breathe once and listen; the answer is the second blow. | Canon |
| Dawn | light | Breathe as morning comes: slow to wake, then bright. | Canon |
| Venom | poison | Breathe small and patient; the sting is short and the end is sure. | Canon |

Canon for the six newer breaths. Mechanics belong to whoever builds each method; this gives only the story.

- **Tide**
  - Kept by the coast-road wardens after the sea took the old road.
  - Linked to Tideward and its surveyors (Iona and Tamsin).
  - Fighting style: give ground, then take it back.
- **Iron**
  - The forge-guards' breath.
  - Its first teacher forged blades for the Marchkeepers before the line broke.
  - Fallen Knights carry iron-bound manual pages.
  - Fighting style: temper. Bend before you break.
- **Dune**
  - Desert road-keepers who bury names in the sand so no tyrant can take them.
  - Cairn-following is shared with the Marchkeepers' guides.
  - Fighting style: footwork. Read the feet, not the blade.
- **Echo**
  - Taught from the Empty Bell (Belowkeepers).
  - It mirrors the Returned Step lesson that "a stroke can leave an answer behind it".
  - Fighting style: strike, wait, then the answer.
  - Not to be confused with the technique intent *Echo*. The intent is a single cut; the breath is a whole school.
- **Dawn**
  - The watchers of the last night hour.
  - Fighting style: patience that ends in a bright flare. Look away from the flare, then strike.
- **Venom**
  - Marsh hunters who learned from things that bite.
  - Fighting style: small cuts and time. Taught alongside knowing which beasts carry poison, from the Field Guide.

## 3. Schools and Sword Masters

**Shipped** (`aura/world/SwordMaster.java`, `MastersRules.java` and `MasterVictories.java`):
- Ember, Gale and Stone Sword Masters.
- A Master is called by sneaking and speaking twice to a duelist teacher, once you reach Aura Form or Heart Circle VIII.
- The trial is lethal and open to up to 8 challengers. A first clear teaches a technique part and is kept in the Masters' record.

Every method has one Master. In-game they are called "Master of <Breath>". Each title and seat below is canon.

| School | Master's title | Seat and story |
|---|---|---|
| Ember | Keeper of the Kiln Gate | Heir to Hessa's watch. Patient until the strike. |
| Gale | Reader of the Scout's Wall | Read the Marchkeeper scout's last lesson under a watchtower. |
| Stone | The Quarry Hinge | Maren's line. "A gate does not stop the wind. It swings." |
| Rime | Warden of the North Ford | Held a ford through an endless winter. |
| Thunder | Bell-Tower Counter | Learned by counting between flash and roar. |
| Verdant | Gardener of the Last Shelter | Tends the garden the Shelter's keeper planted. |
| Hollow | Keeper of Empty Places | "They are fuller than they look." |
| Starlit | Sky-Reader of the Archive | Read the sky for the Archive before the copyists left. |
| Hourglass | The One Who Waits | Has waited a long time for each duel. |
| Crimson | Oath-Keeper of the Tomb | Kept the Sword Tomb's oath after its Keeper slept. |
| Tide | Walker of the Drowned Road | Walked the coast road when the sea took it. |
| Iron | Marchkeeper Smith | Forged the Marchkeepers' blades. |
| Dune | Name-Burier | Buried a city's name so no one could take it. |
| Echo | Singer of the Empty Bell | Sang until the bell rang back. |
| Dawn | Watcher of the Last Hour | Keeps watch until the sun. |
| Venom | Marsh Teacher | Learned where every bite waits. |

Each Master's greeting, hint and parting line is in `tools/lore_journal_text.py`. Master hints are background, never full instructions. The combat lessons stay in `tools/masters_art.py`.

## 4. Named teachers (Shipped)

- **Iven Reed, road tutor**
  - Teaches **Wall Turn** (the scout's kick off a watchtower wall) and **Reed Slip** (the river reeds' sidestep).
  - Source: `tools/wall_turn_art.py` and `tools/form_dash_art.py`.
- **Hessa Vane, kiln guard**
  - Teaches **Cinder Lunge**.
  - Held the furnace gate for eleven winters. "The crouch is the warning."
- **Maren Holt, quarry warden**
  - Teaches **Stone Hinge**.
  - "A gate does not stop the wind. It swings."
- **Wandering duelists**
  - Teach their own breath to anyone who beats them in a fair duel.
  - Teachers among them can call a Master.

## 5. Ruins and histories (Shipped)

- **The Marchkeepers** (`tools/battlefield_art.py`). Three memorials, each teaching one technique intent:
  - *The Line That Broke*: the captain cut her own barricade. Teaches Sunder.
  - *The Last Shelter*: the keeper held the garden door. Teaches Bind.
  - *The Returned Step*: the scout and the paired cairns. Teaches Echo.
  - The Marchkeepers ended as an army and began again as guides.
- **The Sword Tomb**
  - The Buried Keeper wears a blindfold and guards a promise.
  - The Keeper's Testament says the old masters left openings on purpose. This is the root of the earned counters.
- **The Sleeping Blade "Oathkeeper"**
  - The Last Oath, set in stone by the Marchkeepers' scout.
- **Village tournaments, "The Three Bows"**
  - Run by stewards with a ledger.
  - Three schools bow before a fair contest.
- **Earned counters**
  - Each breath's answer after a perfect guard: Backdraft, Rooted Parry, Glacier Mirror, Static Riposte, Eye of the Storm, Sanguine Parry, Constellation Guard, Stopped Moment, Unmoved and Null Parry.
  - They come from the Testament's "openings".

## 6. The magic world (Shipped)

- **Dungeon bosses**: the Cinder Warden, Star-Eater, Tide Scribe, Archivist, Root Guardian and Storm Conductor. Their feats are named in `spell/Feats.java`.
- **The Archive**: Ilyra Venn, the Third Copyist, left the Relay lesson.
- **Lesson packs**:
  - Tollgate: Circle X, after the Cinder Warden.
  - Lifeline: Circle XIV, after the Star-Eater.
  - Conduit: Circle XVIII, after the Storm Conductor.
  - Reweave.
- **The living world**:
  - Tideward: Iona the surveyor, Tamsin her apprentice, the Lantern Newts and the Reed Refuge.
  - The Belowkeepers: Mara, the Fungal Nursery, the Drainhouse and the Empty Bell.
  - Rook's Rainshield.
  - Highland beasts: Stonehorn and Galeclaw.
  - Fallen Knights carry manual pages.

## 7. Lore delivery: the journal and leads (Shipped by the lore pack)

- **Lore Journal**
  - Opens with **H**, which can be changed under "Wildercord: Lore".
  - Saved per player, kept through death, and synced only to its owner.
  - Older saves open empty, then fill in from progress the player already has.
  - Tabs: Leads, Places, People, Learned and Talk.
- **Six discovery leads**:

| Lead | Clue from | Goal | Reward |
|---|---|---|---|
| The Scout's Wall | a Marchkeeper memorial | speak with a Gale duelist | 30 XP, 2 Aura Shards, Aura XP |
| Hessa's Gate | an Ember duelist | breathe at a waterfall or summit | 30 XP, 2 Aura Shards, Aura XP |
| The Keeper's Openings | a Sword Tomb | land a perfect Aura guard | 40 XP, 3 Aura Shards, Aura XP |
| The Warden's Threshold | a Field Guide entry | set off any element reaction | 30 XP, 2 Blank Runes |
| Different Hands | a tournament board | win a duel | 30 XP, 2 Aura Shards, 1 Blank Rune, Aura XP |
| The Masters' Ledger | a Stone duelist | meet a Sword Master | 50 XP, 3 Aura Shards, 1 Blank Rune, Aura XP |

- **How leads behave**
  - The goal and hint can be read again in the journal at any time.
  - A teacher spoken to again may repeat the open lead's hint.
  - A goal met before the clue finishes the lead on the next check, so no lead can get stuck.
  - Aura XP is paid only to characters who know a breath. Vanilla XP and items are paid to everyone.

## 8. Writing rules

- Short and plain: one idea per line. Lines of 90 characters or less.
- Background lines never explain controls. Control text lives in lesson books and `message.wildercord.*` lessons.
- Masters and teachers speak in the first person and never mock the player.
- All text goes through the generators in `tools/`. Never edit `en_us.json` by hand.
