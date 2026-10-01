# Research and spell library

`/runelab research` reports three permanent experiments: cast five different shapes, cast three different named or exact woven fusions, and cleanse a Living Greenhouse. Their first completion awards one Torn Page, two Blank Runes, and one Mana Crystal respectively. Repeating an experiment never awards its prize again. Creative and spectator casts do not count. Progress and rewards survive death and logout.

The notebook offers one fusion experiment using ingredients already learned. A book used on a Runic Hearth, or `/runelab notebook`, opens a native screen with progress, named builds, a slot selector, save/load/delete buttons and a fusion-hint button. Actions are validated by the server and the screen receives a fresh snapshot after each change. The Cord's Grimoire also lists learned woven pairs, both effects, and the Amethyst Block/three-level recipe. Hover the Fusion Altar's preview panel for the effect description, base mana and formation ingredients. The final cast cost still depends on shape, rank, modifiers and equipment.

## Saved builds

* `/runelab builds` lists up to 24 named builds.
* `/runelab builds save "Warm breeze" 1` saves the current sequence in slot 1.
* `/runelab builds load "Warm breeze" 2` loads it into slot 2.
* `/runelab builds delete "Warm breeze"` removes it.

Names are cleaned and replacement is case insensitive. Saving an existing name replaces that entry even when the library is full. Loading validates the entire sequence before changing the spell or its name: the slot must be open, every rune learned and present, and the Cord's socket and tier limits respected. Missing add-on runes remain saved. Builds save rune sequences, not equipment, mana or separate copies of rune mastery.

ResearchLibraryTest checks overwrites, limits, failed-load atomicity, invalid slots, once-only rewards, the research codec and actual mouse clicks on the native screen at large and small window sizes. It does not substitute for a multiplayer save/rejoin test.
