# The bonded blade


A mage has the Cord. A swordsman has a blade. From **Edge** you can **bond** one: kneel in the breathing stance at a ley crossing
with it in hand, and it's yours. A bonded blade is kept through death, never breaks, and nobody else can pick it up or use its
aura. It grows with every fight you win with it, takes on your aura's colour, takes a **name** (its own, or yours), takes a
**trait** drawn from how you actually fight, and keeps its story: where and when it was bonded, every foe it has felled, the art it
has played most, the bosses and duels it has won.

![The bond ceremony at a ley crossing seen from in front: a player kneeling with a diamond sword, motes of orange light rising from the ground into the blade; then the two ley lines lit across the grass running in to them under a turning ring; then a ring of light closing in; then a column of light rising from them as the bond is sealed](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/bonded-blade-ceremony.jpg)
<span>The ceremony: kindling, joining, sealing, and the seal.</span>


## At a glance

| | |
|---|---|
| **When** | From **Edge**. One bonded blade at a time |
| **Which blades** | Any **sword**, **axe**, **spear** or the **mace**, aura-forged ones too (anything in the `wildercord:bondable_blades` item tag). Not the trident |
| **The ceremony** | The **breathing stance** at a **ley crossing** with the blade in hand: ten seconds, holding still |
| **It grows** | By **resonance**, gathered in every fight with it in hand: **Bonded**, **Named** (300), **Awakened** (1,200, from Form), **Soulforged** (3,600, from Sovereign, and a boss felled with it) |
| **Named** | It takes a **name**, its own (built from its story) or one you give it, and blows with it draw a tenth more aura |
| **Awakened** | It offers **three traits** drawn from how you fight, and takes the one you choose |
| **Soulforged** | Its trait **half again as strong**, a fifth more aura from blows, its fullest look, and its traits offered once more |
| **Only yours** | Kept through death, never breaking, can't burn, rot away or be lost to the void; nobody else can pick it up, take it from a chest or use its aura, and in anyone else's hands it comes straight home |
| **Passing it on** | To a disciple, in a ceremony of its own (when masters and disciples come) |
| **See it** | Its glow, its tooltip (hold Shift for its story), and the **Blade** tab on the Aura page |

## Bonding a blade

1. **Hold the blade.** A sword, an axe, a spear or the mace, one at a time, that wears (an aura-forged Lumenedge, Skyrend Glaive or
   Bulwark Maul is a sword, a spear and an axe, so they bond too). The trident doesn't: thrown and left lying, it's out of your hand
   more often than in it.
2. **Find a ley crossing**, a place of power where two ley lines meet (see [Ley lines](ley-lines.md)).
   It's the same kind of place the stillness trial asks for.
3. **Kneel in the breathing stance** (sneak and stand still with the blade in hand) and **stay still**. Once the stance settles, the
   blade answers:
   - **Kindling** (three seconds): motes of your aura rise out of the ground along the crossing's two lines and into the blade, and
     the blade's glow kindles up from the hilt.
   - **Joining** (four seconds): the two ley lines light up across the ground and run in to you, a ring turning under you.
   - **Sealing** (three seconds): a ring closes in on you and the blade blazes.
   - **The seal**: a burst of light at the blade, a column of light rising from you, a ring racing out, and a bell. The blade is
     bonded to you.

Anything that breaks the stance breaks the ceremony: moving, a blow, letting the blade go. A line above your aura bar counts the
ceremony down, and everyone near sees all of it; through your own eyes the lines lie low on the ground ahead and everything round
your body is left out, so you can see what you're doing.

You can bond **one blade at a time**. To bond another, [release](#releasing-the-bond) the first. A server can let the ceremony be
held anywhere (`bond_at_power`).

## Resonance and tiers

A bonded blade grows by **resonance**, gathered in every fight with it **in your hand**:

| What | Resonance |
|---|---|
| A foe felled (a monster, or another player once a day) | about 1, more for a stronger one, less for the tenth of the same kind in the same place |
| A boss felled | 30 more |
| An art landing | 0.6 to 1 (the Final Art 3), and a little more for each foe after the first |
| A technique of your own landing | as the art it's worth |
| A finisher | 2 (on a boss 5) |
| A stance broken, a perfect guard | 0.4, 0.5 |
| An awakening | 4 |
| A duelist beaten | 15 |
| Carried through a breakthrough to Form, to Sovereign | 120, 240 |
| One of your techniques reaching Peerless | 25 |

One foe can only give so much through arts, finishers, stances and guards (about 5 to 8, a boss 40), so pounding a patient foe never
grows a blade, and nothing comes from training dummies, the practice arena, or foes that can't fight back. A fighter's hour gives a
blade about 200 to 300.

| Tier | Resonance | Waits on | What it gives |
|---|---|---|---|
| **Bonded** | the ceremony | Edge | Kept through death, never breaking, only yours; it starts gathering resonance |
| **Named** | 300 | Edge | A name, and blows with it draw a tenth more aura |
| **Awakened** | 1,200 | Form | A trait, chosen from three it offers |
| **Soulforged** | 3,600 | Sovereign, and a boss felled with it | Its trait half again as strong, a fifth more aura from blows, its fullest look, and its traits offered once more |

Each tier also waits on your **stage**, as a breakthrough waits on its trial: resonance keeps gathering while it waits, so a blade
reaches Awakened the moment you break through to Form if it has earned it. A tier reached is a moment: the blade flares, its voice
rings, its name crosses your screen, and it goes into your Grimoire.

## Naming it

At **Named** the blade takes a **name** of its own, built from its story: your method's element, your Way, where it was bonded, what
it has felled most, the art it has played most (an Ember blade bonded in a desert might call itself *Cinderwake* or *Oath of the
Dunes*). It goes by that name everywhere: in your hand, in its tooltip, in chat.

You can give it another on the **Blade** tab: click the name line and type (up to 24 characters; formatting and invisible characters
are taken out, and it's shown to everyone as plain text), or click **Suggest** for another of its own names, then **Name it**. Its
tooltip remembers the day it took its name.

## Traits

At **Awakened** the blade reads how you've fought with it and offers **three traits**, the habits it sees most strongly (your
method and your Way lean it a little: a Stone blade leans to Sundering Steel, a blade of the Bulwark to Riposte). Choose one on the
Blade tab. Before then, the tab shows what it reads in you so far. The first choice is free; changing it for another it offered costs
10 experience levels, except once, free, when it becomes **Soulforged** (it offers its three again, from its whole story). A trait
only works with the blade in your hand.

| Trait | Drawn from | What it does |
|---|---|---|
| **Well-Worn Verse** | one art played far more than the rest | That art costs 15% less aura and rests 15% less |
| **Closing Stroke** | finishers | Finishers give back half again as much aura, and build 4 more momentum |
| **Sundering Steel** | stances broken | Blows, arts and slashes wear a stance 12% harder (6% on a player) |
| **Riposte** | perfect guards | For two seconds after a perfect guard, your next coated blow lands 15% harder (half that on a player) |
| **Wind Step** | Aura Steps | Aura Step costs a quarter less and rests 15% less |
| **Long Crescent** | slashes loosed | Aura Slash costs a fifth less and flies a fifth further |
| **Inkbound Steel** | techniques of your own landed | Techniques cost 10% less and rank a fifth faster |
| **Second Blaze** | awakenings | Your awakening comes back a quarter sooner, and you're spent a quarter less long |
| **Mountainfeller** | bosses and mighty foes felled | Against bosses and Runebound foes, coated blows and arts land 10% harder and wear their stance 15% harder |
| **Gravewarden** | the undead felled (much of what it fells) | Coated blows land 12% harder on the undead |
| **Last Light** | foes felled when you were nearly down | Below a third of your health, you take 8% less from foes |
| **Moonwake** | foes felled by night (most of what it fells) | At night, blows with it draw a quarter more aura |
| **Rallying Steel** | foes felled beside allies | Your finishers give allied swordsmen within 10 blocks 5 momentum |

At **Soulforged** each of these is half again as strong (Well-Worn Verse's 15% becomes 22.5%, and so on).

**Fair against players.** Every trait is a small share of a swordsman's strength, about even with the others. Mountainfeller and
Gravewarden never touch a player; Riposte and Sundering Steel are half against one, and a damage bonus counts inside the same cap as
every other bonus against players. Nothing a trait does ignores armour or kills through a totem.

## How it looks

![Four views of a player holding a glowing diamond sword: a thin vein of light down the blade; a row of bright marks running up it; tongues of aura licking off its edges; and its whole edge burning white-hot inside a soft corona, the swordsman's aura blazing round them](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/bonded-blade-tiers.jpg)
<span>Bonded, Named, Awakened and Soulforged.</span>

A bonded blade glows in your aura's colour, more as it grows:

- **Bonded**: a vein of light down the blade's middle, beating slowly, twice and a rest, like a heart.
- **Named**: a row of marks along it lighting one after another from hilt to tip, as if its name ran up the steel.
- **Awakened**: its aura licking off the edges in flickering tongues.
- **Soulforged**: its whole edge burning white-hot with a light running round it, a slow-turning ring of light about the guard,
  two sparks winding up the blade, and a corona round all of it.

It blazes while you're awakened. In anyone else's hands it's cold: the vein alone, grey and faint. Lying on the ground it lights a
pool of its colour under itself and motes drift up from it; a Soulforged blade sends up a thin column of light, so you can find it
from afar. Through your own eyes all of it stays close and soft, low in the corner of your view.

![A bonded blade lying on stone in a pool of its own light by day, and a Soulforged blade lying in the dark at night with a thin column of light rising from it](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/bonded-blade-ground.jpg)

## Its story

Hover a bonded blade for its tooltip: its tier and whose it is, its resonance toward the next tier (or what that waits on), and its
trait. **Hold Shift** for its story: the day and the place it was bonded, the day it took its name, who carried it before you, its
deeds counted (foes, bosses, arts, finishers, techniques, perfect guards, duels won), the art it has played most, and its notable
deeds by day: a boss felled, a breakthrough carried through, a tier reached, a duel won, a technique gone Peerless, a Way walked, a
passing.

![A bonded blade's tooltip in the inventory: its name in green, Awakened blade, bonded to the player, a bar of resonance toward Soulforged, its trait Last Light and what it does, then its story: bonded on a day at a ley crossing, its deeds counted and its notable deeds by day](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/bonded-blade-tooltip.jpg)

## Only ever yours

A bonded blade can't be lost, stolen or copied:

- **Death**: it's taken out of your inventory before anything drops and given back to you when you respawn, in the slot it was in
  (Curse of Vanishing or not, keepInventory or not). One you were holding on the cursor comes back too.
- **It never breaks**: worn to its last point, it stays there, notched, until you mend it (an anvil, a grindstone, Mending).
- **On the ground**: it never rots away, and fire, lava, cactus and blasts can't touch it. **Nobody but you** can pick it up: no
  other player, no mob, no hopper. It waits where you left it.
- **The void**: a blade that falls out of the world comes home to your inventory.
- **In a chest**: it's safe there, and only you can take it out. Hoppers never pull it out or push it on.
- **In anyone else's hands**: it carries no aura for them, and slips straight back to you (with a word to you both). From inside a
  shulker box or a bundle they carry, too. If you aren't here, the world keeps it for you and gives it to you when you join.
- **An anvil, a grindstone or the crafting grid** never use it up: it can be mended and enchanted as the left-hand item, never
  melted into another, and no crafting recipe takes it as an ingredient (a rune that asks for an axe passes a bonded one by). A
  furnace won't melt it down either. A smithing table's upgrade or forging keeps the bond (a bonded diamond sword made netherite, or forged into a
  Lumenedge, is still your blade).
- There is only ever **one** of it: it's only ever moved, never copied.

If it's away from you, the Blade tab says where it was last with you.

## Passing it on

A blade can be passed to a **disciple**. A master holds their bonded blade in the breathing stance; the disciple **kneels before
them** (sneaking, within a few blocks, the two facing each other). For eight seconds their two colours braid between them, then the
blade passes into the disciple's hands, bonded to them, with its tier, its name, its trait and its whole story, the master's name
written into its lineage. A disciple below the blade's tier gets what their own stage allows, and the rest wakes as they grow into
it (a Soulforged blade passed to a Flow disciple sleeps, a faint vein, until they reach Edge). Masters and disciples are a later
part of the swordsman's path: until then the passing has nobody to pass to.

![A master in pink aura holding a diamond sword, a disciple before them, a circle of light round the two and a beam of light running from the blade to the disciple; then a column of light rising from the disciple as the blade passes to them](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/bonded-blade-passing.jpg)
<span>The passing, and the blade passed.</span>

## Releasing the bond

On the Blade tab, **Release the bond** (click it twice). The blade is only steel again, its growth and gifts gone, though its
tooltip remembers whose it was. You can then bond it, or another, anew at a ley crossing.

## The Blade tab

The Aura page's fifth tab. Before a bond it says how the ceremony goes, whether the blade in your hand can be bonded, and what a bond
gives, with the four tiers. With one: the blade large in its frame and colour, its name and tier, its resonance toward the next tier
and what that waits on, its name to give (or one of its own to choose), its trait (what it reads in you so far, then the three
it offers as cards), its story, and releasing it. The tab breathes gold while a trait waits to be chosen.

![The Aura page's Blade tab: a diamond sword large in a frame of orange light, its name and Awakened tier, a resonance bar, a name line with Suggest and Name it buttons, three trait cards to choose from (Well-Worn Verse, Riposte and Sundering Steel, each saying why it's offered), its story in an inset, and a Release the bond button](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/bonded-blade-page.jpg)

## For server owners

`aura` section: `bonded_blades` (switch bonding, resonance and a blade's gifts off: a blade already bonded keeps its protections, so
nobody loses one to a setting), `resonance_gain` (0 to 100), `bond_at_power`, `blade_traits`. Which weapons bond is the
`wildercord:bondable_blades` item tag. Operators: `/wildercord aura blade` (what it stands at), `blade bond` (the held blade bonded at
once), `blade resonance <amount>`, `blade tier <1-4>`, `blade name <name>`, `blade trait <trait>`, `blade release`,
`blade pass <player>` (to anyone, no disciple needed).
