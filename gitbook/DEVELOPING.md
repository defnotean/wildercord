# About this guide

The player guide describes **0.10.0-alpha**, for **Minecraft Java 26.3 / Fabric / Java 25**. The searchable website is hosted on [GitHub Pages](https://defnotean.github.io/wildercord/). An ordinary Markdown export in the repository's `gitbook/` folder supports GitBook imports and Git Sync using the root `.gitbook.yaml`.

## Keeping it accurate

Player explanations live in `wiki/`. The rune catalogue, icons and crafting diagrams are generated from the mod's definitions by `tools/wiki.py`; editing their generated output alone will be overwritten on the next refresh. `tools/player_docs.py` validates page navigation, template links and image references, then produces `gitbook/SUMMARY.md` and the plain Markdown export.

GitHub Pages builds the same source with Jekyll. The GitBook export removes Jekyll-specific tags and uses repository-hosted images. GitBook hosting requires a GitBook space connected to this repository; publishing the GitHub website does not itself create a cloud GitBook space.

## Evidence and scope

The screenshots illustrate real development client runs. Text distinguishes tested mechanics from hardware-dependent performance. The repository's `docs/features/` covers implementation details, while `docs/audit/` retains detailed engineering findings and verification history.

Before this release, 474 unit tests passed, along with a complete 59-entrypoint client run and focused circle, physical-magic, owner-weave and shader checks. The current descriptor has 60 entries; the new circle suite was run separately. GitHub Actions provides the release's fresh remote build and gameplay status.

If a guide and your installed version disagree, check the jar version first. [Report an error](https://github.com/defnotean/wildercord/issues/new/choose) with the page, version and corrected behaviour.
