# Rune Catalog

## What it is

The Rune Catalog is a big browsing screen for every rune in the mod. Where the
[Cord screen](cord-screen.md) is for threading spells, the Catalog is for answering
"which rune does this job?" and "what goes well with it?". You can filter by what a rune is for (farming, mining,
travel and so on), sort the list, and pick any rune to read it and see its best partners.

The game doesn't pause while the Catalog is open.

## How to open it

- On the Cord screen, press `Ctrl`+`B`.
- Or click **Catalog** at the right end of the filter row on the Cord screen. The link can't be clicked on the
  Grimoire page, but `Ctrl`+`B` still works there.

**Back** returns you to the Cord screen.

## The layout

| Part | What it does |
|---|---|
| Top bar | Search box, the **Sort** button and **Back**. |
| Filter row | **Family**, **Element**, **Use** and **Show**. Click a button to step through its choices. |
| Left side | The rune list, laid out in columns. |
| Right side | The detail panel for the rune you picked. |
| Bottom bar | `<` and `>` to turn pages, "Page X of Y", how many runes match, and **Show in Cord**. |

Your filters, search, page and picked rune are remembered until you close the game.

## Filters

| Filter | Choices |
|---|---|
| Family | Any, Shape, Effect, Modifier, Link, Knot |
| Element | Any, each element in A-Z order, then None (runes with no element) |
| Use | Any, Combat, Support, Farming, Fishing, Mining, Building, Exploring, Travel, Passive |
| Show | Known, All, Cord holds, Still reading |

- **Use** sorts runes by the job they do. Harmful effects count as Combat, helpful ones as Support, movement as
  Travel, and so on. A rune can have more than one use. **Passive** means the rune can be set as a
  [passive](passives.md).
- **Show: Known** (the default) lists only runes you know.
- **Show: All** lists every rune. Runes you don't know are drawn dark with a faint name, and only their hint is
  shown.
- **Show: Cord holds** lists known runes that your worn Cord can fire.
- **Show: Still reading** lists runes you know but haven't fully read yet. See
  [Reading runes](harmonies.md).

The list also includes your own [Knots](../fusion-altar/knots.md) and woven runes.

## Searching

Just start typing, or press `Ctrl`+`F` to jump to the search box.

- Every word you type must match. A word matches a rune's name or its tags: element, category, family and use.
  So "fire area" or "mining" both work.
- Words of four letters or more also match the rune's text, but only the part you can read. A rune you haven't
  read yet only offers its hint.
- When you search, runes whose name starts with what you typed come first.
- `Enter` picks the first rune on the page.
- While you're typing in the search box, the first `Esc` clears the search. The next `Esc` closes the Catalog.

## Sorting

| Sort | Order |
|---|---|
| Group (default) | By family, then category, then tier, then name |
| Name | A to Z |
| Tier | Lowest tier first |
| Cost | Cheapest mana first |

## What each entry shows

Pick a rune in the list to fill the detail panel:

- Its icon and name.
- A line with its **family, category, element and tier**.
- **For:** the uses it's good for, or "Fits most spells." for runes that work almost anywhere.
- "Not learned yet." if you don't know it, or how far along you are in reading it.
- Its description.
- **Goes well with:** partner runes, grouped as Shapes, Effects, Modifiers and Links. Up to 6 are shown per group,
  with "+N more" if there are extra. Runes that share a use with your pick come first. Click a partner's icon to jump
  to it.

How partners are chosen:

- A modifier is listed if it can change the rune.
- A shape is listed if it reaches the right target: harmful effects pair with shapes that reach outward, helpful
  effects with shapes that reach you and allies, world effects with shapes that hit blocks, and movement with shapes
  that move you or aim at a point.
- A link is listed if it shares a use with the rune.

The partner list draws from runes you know. Set **Show** to **All** to see partners you haven't learned yet. You may
also see:

- "A lesson spell: only these runes, nothing added." for lesson runes, which can't take extra runes.
- "Works with any spell." for runes that fit everything.
- "Learn more runes to see what goes with this one." when you don't know a fitting partner yet.
- "None of the runes shown fit with this one." when your filters hide every partner.

## Show in Cord

**Show in Cord** takes you back to the Cord screen and searches its rune list for the rune you picked, so you can
thread it straight away. It works only for runes you know, and only when you opened the Catalog from the Cord
screen. Double-clicking a rune in the list does the same.

## Quick keys

| Key | Action |
|---|---|
| `Ctrl`+`B` | Open the Catalog from the Cord screen |
| Typing, `Ctrl`+`F` | Search |
| `Up` / `Down` | Pick the previous or next rune |
| `Page Up` / `Page Down`, mouse wheel over the list | Turn the page |
| Mouse wheel over the detail panel | Scroll the details |
| `Enter` in the search | Pick the first rune on the page |
| Double-click | Show the rune in the Cord |
| `Esc` | Clear the search, then close |

## Tips

- Set **Use** to Travel for dashes, glides, safe falls, mounts and water paths, and to Exploring for runes that tell
  you where things are.
- Pick an effect, then look at **Goes well with** to find a shape for it instead of trying them one by one.
- New to the everyday utility runes? See [Everyday Rune Packs](../runes/everyday-packs.md), or
  the full list in the [Rune Codex](../runes/codex.md).
