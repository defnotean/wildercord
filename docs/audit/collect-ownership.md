# Collect ownership and pickup reservations

## Verified issue and fix

`Effects.collect` previously admitted every item/experience entity on editable ground within its circular reach. It teleported an item even if another UUID was its pickup target or recorded thrower, and cleared its pickup delay. Target metadata could still prevent immediate acquisition, but the original drop had already moved away from its owner.

The narrow fix admits item entities only when they are alive, pickup-ready, and their target/thrower UUIDs are absent or match the caster. A foreign thrower's UUID is checked from the stored EntityReference even when that entity is absent. Refused goods remain in place. Existing editable-ground checks, circular radius, 48-entity cap and experience collection remain intact. Ready own and unowned goods still collect. Manual ordinary pickup behavior is unchanged; remote collection deliberately preserves foreign thrower attribution.

## Native acceptance

`CollectOwnershipTest` passed in 29 seconds on 2026-10-03. Log: `artifacts/review/collect-ownership/native.log`.

The suite casts actual paid Survival Self/Collect through SpellCaster, checks actual inventory and experience gain, and verifies foreign-targeted and absent-foreign-thrower items remain at their original positions. Ordinary delayed and never-pickup drops stay in place. An actual Fabric block-break veto supplies a claim boundary; a drop outside the circular radius remains in place. A repeated paid cast preserves every refusal. Once the ordinary delayed unowned drop becomes pickup-ready, a subsequent paid cast acquires it. The claim callback is disabled in finally to leave later suites unaffected.

No renderer or damage behavior was changed by this fix.
