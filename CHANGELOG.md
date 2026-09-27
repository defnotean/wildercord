# Changelog

All notable changes to Wildercord. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.1.0-alpha] - 2026-09-27

The first public version.

### Spells
- The Cord slot (above the offhand) and four Cords: Twine, Copper, Amethyst and Echo, with more
  sockets, spell slots, rune tiers and mana at each step.
- The rune sequencing engine: shapes, effects, modifiers and links, read left to right, with a
  plain-English readout, cost and cooldown for every spell.
- 135 runes across four tiers and ten elements (Fire, Frost, Storm, Wind, Earth, Life, Void,
  Arcane, Time, Blood), each with a hand-drawn icon.
- Element reactions: Shatter, Conduct, Wildfire, Implode and Collapse.
- Stasis holds every hit on a stopped target and lands it all when time moves again.

### Progression
- Mana: Mana Crystals, Clarity and Mana potions, meditation, and the Reservoir, Wellspring and
  Siphon Cord enchantments.
- Spell enchantments for Cords: Potency, Celerity, Thrift and Persistence.
- Heart Circles: condense mana by casting, earn breakthroughs, and meditate to form up to eight
  circles, each adding mana, regeneration and power, with perks at the 3rd, 5th, 7th and 8th.
- Passive spells: up to two always-on spells that cost mana every second, from a restricted set of
  sustainable runes.
- Every Tier I-III rune is craftable (costlier by tier) and appears in the recipe book; Tier IV
  runes drop from bosses and rare structures. Chest loot favours low tiers.

### Interface
- The Cord screen: drag and drop, type-to-search Codex, family tabs, category chips that appear as
  you learn runes, a Spells/Passives switch, and heart, mana and help badges.
- The spell HUD beside the hotbar, readable at every GUI scale.

### Technical
- Server-authoritative casting with per-cast budgets, friendly fire off, and PvP damage scaling.
- A generated asset pipeline (`tools/`) driven by the rune roster.
- Unit tests for the spell engine and client game tests for mechanics and layout.
