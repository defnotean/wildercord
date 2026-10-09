# About this guide

This guide covers the current Wildercord update for **Minecraft Java 26.3 / Fabric / Java 25**. It's hosted on [GitHub Pages](https://defnotean.github.io/wildercord/), and a plain Markdown copy lives in the repository's `gitbook/` folder for GitBook.

## Keeping it accurate

Hand-written pages live in `wiki/`. Some pages are generated from the mod's own definitions, so don't edit them by hand:

- the rune pages under `wiki/runes/`, including the [Rune Codex](runes/codex.md),
- the [Rune Recipes](items/rune-recipes.md) page,
- the [Master techniques](masters/techniques.md) list,
- the [Breathing Methods](progression/breathing-methods.md) list.

To refresh them, then the GitBook copy:

1. `python tools/wiki.py` regenerates the generated pages.
2. `python tools/player_docs.py` re-exports the GitBook copy.
3. `python tools/player_docs.py --check` checks links, navigation and images without writing anything.

If this guide and your game disagree, check your Wildercord version first, then [report the error](https://github.com/defnotean/wildercord/issues/new/choose) with the page, your version and what actually happens.
