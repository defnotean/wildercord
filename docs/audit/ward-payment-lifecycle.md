# Ward payment lifecycle correction (2026-10-04)

## Reproduced defect

An actual CastSpell packet for Pulse / Self / Foresight paid44mana once (300â†’256 synchronously). After two supplied physical attacks exhausted the ward, an ordinary delayed Pulse restored protection. The unchanged production baseline failed the original-charge preservation assertion in28seconds. This establishes this actual local route, not every ward or a remote PvP exploit.

The first27-second attempt stopped at a fixture mistake: it observed mana at delayed ward admission after regeneration and called that the payment instant. The corrected passive BEFORE_CAST/AFTER_CAST receipts measure actual cost/spent/debit synchronously. Normal regeneration and cast scheduling remain intact. At delayed ward admission mana258 was legitimate. Those original failures are preserved separately.

## Production correction

Reflect and Foresight retain the exact finite Cast.payment owner. Continuations can extend their current ward duration but cannot replenish consumed charges. A per-payment/per-target once key prevents an old payment rearming after expiry cleanup or replacing a newer paid ward. Foresight keeps its real original duration after exhaustion instead of shortening it to five ticks and making a late pulse eligible for replacement.

Separately paid casts still buy ordinary six Reflect facets or two Foresight charges. Existing Stasis behavior, dodge motion, damage caps, mirror visuals and spell presentation remain unchanged.

## Actual focused evidence

WardPaymentLifecycleTest passed39seconds. Real normal packets cover Pulse and Echo routes for both wards, exact compiled payment/debit, supplied physical pressure exhausting charges, ordinary delayed continuations and genuinely separate paid refresh. Captured paid again/reflected copies are explicitly trusted replay seams, not real opponent parry packets. The test-only probe lives only in the gametest mod.

The first corrected production attempt reached separate-payment refresh before its three-tick formation release completed. The final fixture polls actual production admission with a finite100tick bound; it does not force the release or clock. Failure34seconds is preserved as observation-timing evidence, not a production regression.

## Outstanding verification

WardPaymentRetirementTest passed63seconds: actual separately paid replacement, exact newer state/deadline/charges unchanged by labelled old-paid replay, real ordinary deadline and normal cleanup, old-paid replay refused after cleanup, genuinely fresh paid reopening. Foresight deadline355 was naturally removed by tick400; Reflect deadline612 was naturally removed by tick800. No expiry/map/clock writes manufacture these results.

Existing WildercordVoidTimeTest passed80seconds, including capped Foresight/repeat behavior and Starmaw ward consumption. Existing WildercordParryTest passed50seconds, including actual projectile/beam turn and shared-payment checks. SpellDamageBalanceTest passed33seconds, preserving shared damage allowances. Full build and exact packaged resource/class verification pass: 1008 unit tests, zero failures/errors/skips. Checked JAR wildercord-ward-payment-review+mc26.3.jar SHA256 aa78618960f03179c866fdb74c6c352214ea36c534353f43a0641d2e28928006. Real TCP opponents, disconnect/respawn/dimension lifecycle and callback authority remain separate. Source findings concerning Stasis attribution, unsafe Foresight destinations and final reflection damage receipts remain unverified and are not claimed fixed by this payment correction.

Latest hosted Build37205210837 atf5c079f4 failed FungalNurseryTest's actual visitor gathering reserve, before the new ward source was committed. Its original FUNGAL_GATHER admissions are retained for a separate evidence-based investigation. No hosted-all-green claim follows these local ward passes.

Logs are retained under artifacts/drafts/ward-lifecycle-review. This correction adds no creature, item, ability or ecosystem credit. The full living-world goal remains active.

The checked ward review JAR includes the accepted Rainshield and Bittern fixes. No presentation assets changed in this ward correction; prior Rainshield native gallery remains separately attributed. Native test classes/probes are excluded from the production JAR. Receipt: docs/evidence/ward-payment-20261004.json. No content credit or public/server deployment.
