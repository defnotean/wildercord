# What's new in 0.11.1

- **Cinnamon grows stronger when she's big.** While treats keep her full size she bites harder, has double health and armour, joins her owner's fights and casts fire, frost, lightning and arcane bolts, plus a healing beam when her owner is low. At normal size she is unchanged. See [Cinnamon](companions/cinnamon.md).
- **Screen fixes.** Tooltips in the Cord screen show again (badges, spell sockets and Codex runes). A Master study you can't read yet stays open and says what it needs. The Aura screen's header no longer prints text over the portrait or the Master's Arts help.

Install `wildercord-0.11.1-alpha+mc26.3.jar` on the server and every client.

# What's new in 0.11.0: Masters of Tomorrow

This update adds Sword Masters, new player sword arts and movement forms, 48 places to explore, a lore journal, a bigger magic climb, everyday runes and parties. Install `wildercord-0.11.0-alpha+mc26.3.jar` on the server and every client together: **0.10 clients must update before joining**. See [Installation and Updates](installing.md).

## Sword Masters

There are **16 Sword Masters**, one for each breathing method, and each offers an optional trial. Each Master fights with named techniques you can learn to read: clear warnings, counter windows, and a boss bar that names the move. Every school has its own signature moves, such as Ember's Cinder Wake and Kiln Ring, Gale's Crosswind Reprise, Stone's Fracture and Fault March, Echo's Tolling Bell, Dawn's Noon Glare and Venom's Serpent Coil. Your first clear of each Master teaches you a technique part.

See [Sword Masters](masters/index.md) and the [full technique list](masters/techniques.md): all 417 techniques. Each school has its own page:

[Crimson](masters/crimson.md), [Dawn](masters/dawn.md), [Dune](masters/dune.md), [Echo](masters/echo.md), [Ember](masters/ember.md), [Gale](masters/gale.md), [Hollow](masters/hollow.md), [Hourglass](masters/hourglass.md), [Iron](masters/iron.md), [Rime](masters/rime.md), [Starlit](masters/starlit.md), [Stone](masters/stone.md), [Thunder](masters/thunder.md), [Tide](masters/tide.md), [Venom](masters/venom.md) and [Verdant](masters/verdant.md).

## New breathing methods

Six new methods join the first ten, for 16 in all: **Tide**, **Iron**, **Dune**, **Echo**, **Dawn** and **Venom**. Each has its own element, gift and five arts. See [Breathing Methods](progression/breathing-methods.md).

## New sword arts

Three player arts: **Spellcut** (U), **Rising Break** (Y) and **Driving Cut** (J). Wandering Duelists also teach starter lessons: Echo, Afterimage and Sunder. See [Controls](controls.md) to rebind keys.

## Master forms and field forms

Equip one Master form and use it with **C**. The field forms move your real body, stop at walls and ledges and end in a short recovery: **Cinder Lunge**, **Reed Slip**, **Air Step**, **Plunging Strike**, **Wall Turn** and **Stone Hinge**. Masters and their teachers teach them. See [Master Forms](masters/master-forms.md).

**Spell Cut** is a learned swing that cuts a hostile spell bolt out of the air just as it reaches you. It needs no slot and no extra key. See [Spell Cut](masters/spell-cut.md).

## Combat presentation

An opt-in Articulated animation style for selected arts and Master attacks, plus a Stable camera that removes shake and bob. It only changes what you see. See [Combat Presentation](masters/combat-presentation.md).

## 48 new places to explore

Six families of buildings and places now appear in newly generated chunks, eight in each. Each has its own loot, advancement and page:

- [Farmsteads](world/farm-sites.md): a windmill, an herbalist's cottage, an apiary, a walled orchard, a shepherd's hut, a mushroom ring, a granary barn and a scarecrow field.
- [Master halls](world/masters-sites.md): eight halls and shrines for the breathing schools, each with practice dummies and a short trial.
- [Roadside places](world/travel-sites.md): an inn, a watchtower, a broken bridge, a rune library, standing stones, a caravan camp, a cartographer's hut and a vow circle.
- [Water places](world/water-sites.md): a stilt smokehouse, a lighthouse, a watermill, an ice fishing camp, a pier, a sunken shrine, net-weavers' huts and a tidepool grotto.
- [Mines and forges](world/mine-sites.md): a hillside mine, a forge hall, a crystal lab, a collapsed delve, a Nether foundry, a deepslate vault, a prospector's camp and a miner's rest.
- [Wild places](world/wilds-sites.md): a jungle ziggurat, a desert temple, a snowy monastery, a badlands gatehouse, a cherry-grove pavilion, a deep dark post, a Nether outpost and an End lantern.

Existing chunks don't change, so explore new land to find them.

## New weapons

The Oathkeeper, Bulwark Maul and Skyrend Glaive have new 3D models. See [Weapons](progression/weapons.md).

## More magic

- [Heart Circles](progression/heart-circles.md) now go up to Circle XX (was VIII).
- You can now use up to 100 [Mana Crystals](progression/mana.md#mana-crystals) (was 10).
- New lessons: [Relay Circle](spellcraft/relay-circle.md), [Reweave](spellcraft/reweave.md) through the Ebb Ledger, [Excise](spellcraft/excise.md), and the [Tollgate, Lifeline and Conduit](spellcraft/lesson-pack.md) lessons.

## Runes for everyday life and the Rune Catalog

There are now 756 runes, and many are for farming, fishing, mining, building, exploring and helping others, not only fighting. To find one, open the **Rune Catalog** with `Ctrl`+`B` or the **Catalog** link on the Cord screen. You can search, filter by family, element and use, and see which runes **go well with** the one you picked. See [Rune Catalog](spellcraft/rune-catalog.md).

Nearly 300 of the new runes come in everyday packs: hearth, Tide, farmstead, delving, Wayfarer's and support. None of them deals damage. See [Everyday Rune Packs](runes/everyday-packs.md).

## Lore journal

Press **H** to open your lore journal. It records the places you find, the duelists and Masters you meet, what you learn and what they say to you, and it gives you six **leads** to follow, each with a reward. See [Lore Journal](progression/lore-journal.md).

## Circle Vows

Seven of the new Heart Circles (IX, XI, XIII, XV, XVII, XIX and XX) ask you to choose one of two vows, such as more mana or faster mana. Type `/vow` to see them and `/vow take <vow>` to choose. Changing your mind costs 5 levels. See [Circle Vows](progression/circle-vows.md).

## Parties

Group up with `/party invite <player>` for up to 8 players. The other player clicks **[Accept]**, or types `/party accept <your name>`. Party members can't hurt each other, and your helpful spells reach them. Invites expire after 60 seconds, and parties end when the server restarts. See [Parties](social/parties.md).

## For server owners

Back up worlds and the `config` folder before updating. Existing chunks don't change. Operators can place a few small encounters, such as a Wayfarer Training Pavilion, in old terrain with `/wildercord-upgrade`, which shows a preview first and can be rolled back. See [Pavilion Upgrade for Old Worlds](world/pavilion-upgrade.md).

## Fixes

- Delayed effects remember who cast them and check who is an ally when they land.
- Interrupting a spell, by a Master or with Driving Cut, now needs damage that actually lands. A full guard or a dodge keeps your cast.
- Lantern Newts no longer drift upward in tight turns, and rimehares settle at the end of a route.
- Villagers at water sites stay home as residents, so they're never mistaken for wanderers.
- Hailfall stops hitting and chilling once it has ended.
- Glimmerwings find the lantern light around them and follow it.

Older releases: see the [full changelog](https://github.com/defnotean/wildercord/blob/main/CHANGELOG.md).
