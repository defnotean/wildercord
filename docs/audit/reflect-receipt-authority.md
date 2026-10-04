# Reflect incoming damage receipt (2026-10-04)

## Actual reproduction and correction

The unchanged payment-corrected production failed a31-line native assertion after an earlier AFTER_DAMAGE phase admitted a real foreign injury. Baseline execution failed29seconds. An ordinary five-point physical strike took the recipient20→15 and returned three to the actual Husk (100→97), spending one facet. The alternate branch admitted genericKill6 during normal vanilla damage cooldown: exactly one extra health damage, recipient15→14, and an actual new final source. Reflect nevertheless returned damage to the earlier attacker. Logs preserve the first failure; this is an actual local callback reproduction, not remote opponent gameplay.

Reflect now checks finite positive damage and exact entity.getLastDamageSource()==source before initiating the return. The attacker must be current, alive, nonremoved and in the recipient's world; the recipient must not be removed. No recipient-isAlive condition was added, preserving the existing mortal-wound path. This fixture does not separately prove mortal wounds or cross-dimension callbacks.

## Focused acceptance

WardReflectReceiptTest passed31seconds with the same two actual paid Self→Reflect packet routes and unchanged strict controls:

- Ordinary receipt: recipient15, attacker97, five remaining facets.
- Genuine superseding receipt: recipient14, original attacker100, all six facets remain.
- Actual price/spent/synchronous mana debit receipts match; original injuries are retained.

The test registers a scoped earlier event phase, applies real damage, and does not reset health/cooldown/time inside the callback. The current production check does not change the six-facet limit, sixty-percent coefficient, duration, mana price, mirror effects, Stasis, Foresight or shared payment policy. Output-damage callback authority, lifecycle across replacement bodies, safe dodge destinations and Stasis attribution remain separate outstanding reviews.

The full build and named review JAR pass1008unit tests with zero failures/errors/skips. Exact5469processed resources/1871classes, CRC/duplicates and absence of native test classes are verified. Review artifact: wildercord-reflect-receipt-review+mc26.3.jar; SHA256 c9cc8eb4da0ff962898c5cec41882c1f5d1166936eef59d548a3c7f7922aa970. Prior Rainshield gallery remains separately attributed; this fix changes no presentation assets. No new content credit or public/server deployment.
