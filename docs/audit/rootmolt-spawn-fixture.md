# Rootmolt spawn-fixture correction (2026-10-04)

Full GitHub gameplay run37182502193 at4b0a4a93 failed RootmoltPresentationTest at its hard-coded16-health contact assertion. The retained diagnostic was health28, armor0, actual HOLDING pose, holding=true, tether=true and exact matching owner/source UUIDs.

The existing Runebound ENTITY_LOAD listener randomly admits fresh unnamed Zombies. Its ordinary +0.6 base max-health modifier changes20 to32 and fills health. Thus32 minus Rootmolt's four physical damage gives the observed28; the original pre-add20-health assertion did not constrain post-add spawn listeners. A Runebound could also cast unrelated spells during this rig fixture.

The supplied contact and miss Zombies now receive a visible custom name before addFreshEntity, following the existing named-fixture convention. The real admission listener preserves named creatures. Immediately after add, the test captures actual full post-spawn health and checks zero armor. Contact must subtract exactly4 and retain the actual hold; the miss must preserve exactly its original admitted health and no tether. Warning/contact/recovery/missed-rake native rig and material assertions remain intact. No creature behavior, damage, timing, pose, test deadline or RNG has changed.

Focused native RootmoltPresentationTest passes33s, log artifacts/drafts/ember-woodlands/native-rootmolt-named-health.log. Independent review confirmed the actual Runebound listener and existing named Husk convention. This focused pass does not claim the full Linux CI rerun has passed.
