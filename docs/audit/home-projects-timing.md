# Runic Hearth lantern acceptance timing

The full Linux client gate at commit 65256266 reached gameplay and failed the
reading-lantern assertion in HomeProjectsTest. The server lantern accepts a
qualifying charge, waits 20 ticks, and updates on game-time multiples of 20.
The old 25-tick test wait could fall between charge readiness and its first
eligible update. A 40-tick wait covers both actual intervals.

The test now separately checks that configuring the lantern succeeded and its
Fire charge was admitted, then checks actual Night Vision after this bounded
window. No production duration, cooldown, charge, utility or ownership rule was
changed. It still checks the native planter, real falling chime, solo ritual,
cooldown refusal and teammate-contributed faster ritual.

Focused native `HomeProjectsTest` passed on 2026-10-03. The log and original
Runic Hearth screenshot are preserved at
`artifacts/review/home-projects-timing`. This local focused result does not
establish completion of the entire Linux client descriptor. See
`ci-graphics-startup.md` for the subsequent full CI requirement.
