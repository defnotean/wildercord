# Background tooltip thread safety (2026-10-03)

## Observed failure

GitHub full native run37172524778 completed after60m36s with a real thread-guard exception, not a timeout. The ordinary build passed. WildercordFamiliarTest.taming intentionally asks for item tooltip lines from the game-test worker thread to cover background creative-search indexing. The global BladeTooltip callback called Minecraft.getInstance() twice, even for an unrelated familiar lantern, before checking its thread. Fabric rejected that access. Its existing on-thread tooltip check was already correctly dispatched through computeOnClient.

## Production correction

BladeTooltip captures the client once during client initialization. Each callback checks client.isSameThread() before reading the player or Shift state. Background callers retain the base and other safe static tooltip content; blade additions that use live player/input state are produced on the client thread. The original Familiar onThread > offThread assertion remains unchanged. Moving that assertion to the render thread would have removed the regression coverage.

A read-only review of all six callback registrations found the other client-state paths already guarded: BlankRuneTooltip, FamiliarClient and font wrapping capture/check their initialization thread. ImbuedTooltip uses item/rune metadata; forged gear description uses item/configuration data. No additional speculative guards were added.

## Verification and limits

The unchanged complete focused dev.wildercord.gametest.WildercordFamiliarTest suite passed locally in37s, including real bonding, familiar behavior, lantern interaction, cosmetic controls and the intentional on/off-thread tooltip assertion. The original terminal CI log and this focused run are retained under artifacts/drafts/third-breath. This establishes the targeted production regression fix; the full Linux native descriptor still requires a subsequent run. It does not establish all gameplay, remote multiplayer or the living-world goal complete.

This is a development source fix. No public JAR, CurseForge upload or VPS deployment is part of this commit.
