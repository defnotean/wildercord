# Aura beast query and damage authority — 2026-10-04

## Production correction

Typed candidate queries now collect at most13 raw entities and refuse pools above12 before semantic filtering. Only an explicitly known exact source can be excluded before counting. Quiet/same-species/nonfood/protected candidates still count toward completeness. Player threat acquisition, living attack recipients, nearby scraps and whistles use this bounded policy. Population cap2 remains its own bounded threshold query. A cap does not claim the native spatial index visits no unrelated sections.

Runtime natural placement requires an actual ServerLevel on its owning thread and refuses CHUNK_GENERATION before querying live entities. The old accessor query returned no entities during regional generation, so its claimed generation crowd cap was ineffective. The isolated bounded candidate initially converted this into a live getLevel query; review corrected that before promotion. No witnessed historical concurrency crash is claimed.

Attack epochs, actual source/recipient liveness and world, retained phase and final damage receipt guard callbacks. Original Warning40/recovery50, Charge7/Leap5 and movement/shape/costs remain unchanged. Authored knockback requires the exact physical DamageSource to remain final; scar authority is rechecked after knockback. Galeclaw meal requires an admitted actual fatal wound with the same final owner. Positive-health removal and foreign fatal damage cannot fabricate its meal.

## Native acceptance so far

| Gate | Result | Scope |
|---|---|---|
|AuraBeastAdmissionTest|36s pass|Actual runtime positives, native test-thread/generation refusal, population threshold, feeding/whistle and saved state|
|AuraRawTypedQueryTest|22s pass|256 nonfood/same-species candidates count before eligibility; real overflow removal reopens complete pools|
|AuraBeastProvenanceTest|48s pass|Genuine original5/7 attacks; actual nested one-health foreign wound suppresses authored original followups; positive retained owner works|
|AuraBeastCallbackTest|1m28s pass|Eight actual injury/Warning/Charge-or-Leap encounters with source/recipient discard or real Nether departure|
|AuraBeastDenseAttackTest|48s pass|Actual Warning/Charge-or-Leap/recovery; all-phase13 supplied receivers refuse all damage, complete12 admits original physical hits|
|GaleclawMealCallbackTest|47s pass|Positive-health discard/earned second kill/foreign nested fatal owner with exact actual death receipts|

All six focused native suites pass. Final build/JAR receipt is recorded below. These are native client/integrated-server controls with supplied actors and geometry, not remote multiplayer or complete ecosystem credit.

## Retained failures and corrections

The original provenance fixture used nested genericKill(1) after a real seven/five input. Native diagnostics show admitted=false, unchanged13HP and the original receipt. Installed vanilla damage cooldown refuses inputs at or below the preceding hit; its bypasses_cooldown tag is empty. A genuine8/6 input produces exactly the intended one-health differential (13→12/15→14) with a foreign final receipt. No health or cooldown reset is used. Strict production owner guards are unchanged.

The original meal fixture counted both wounds through AFTER_DAMAGE. Actual diagnostics show two earned Warnings, dead0HP prey and a genuine meal deadline, but only one event count. Installed Fabric LivingEntityMixin skips AFTER_DAMAGE for dead/dying recipients. The fixture now captures the original second5-input request against real1HP prey, credits it only when actual AFTER_DEATH confirms the identical DamageSource, live source/world/Leap epoch and dead health, and retains the original240-tick window. Requests alone never count as damage. Discard and foreign fatal controls retain zero owned fatal receipts.

A separate scoped Belowkeeper fixture correction passes57s; the full Linux descriptor remains open. See belowkeeper-fresh-warning-fixture.md. No new content or universal performance claim accompanies these safety changes.

The original Dense scene spread the final non-target row beyond the earliest actual recoil-to-Leap query. Native failure records source z=-.893641/y101.42, query maxz2.581359,9 actual raw peers, unchanged20HP receivers, and the last row atz2.9. It incorrectly required13 during every Leap phase although Galeclaw strikes at landing. The fixture keeps its targeted two-block receiver, original full-AI injury/Warning/attack/recovery, original100-tick windows and all-phase13 assertion. Only initial non-target row spacing changes .4→.2, fitting actual recoil-to-landing geometry; no actors move by test code during attacks. Supplied NoAI receivers remain explicit indexed controls, with actual bodies and accumulated velocity recorded. Both creatures'13 refusal/12 original-damage cases pass48s. This is bounded attack admission evidence, not peaceful ecological behavior or thirteen genuine multiplayer clients.

Final build passes13s with1000 unit tests and zero failures/errors/skips. Checked development JAR SHA256 **0b43fdcd29d35dadc3e74ff100cc10a181a2526aa070f8803b37aaf355322f77**,5399 exact resources and1815 exact production/client classes. ZIP CRC, duplicates and class matching pass; gametest instrumentation is excluded. Embedded version remains0.9.1-alpha.1+mc26.3. Public release and VPS activation remain separate.
