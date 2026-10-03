# Aura progression review

## Report and findings

The player report was that Aura users might be unable to advance, without a known rank or reproduction. Code inspection found no universal progression lock. The distinction between replenishing aura, earning experience and completing a breakthrough trial was poorly explained on the screen. The lifetime practice limit could silently stop gains at 40 XP, below Flow's 150 XP threshold. A registered mentorship trial also rendered its untranslated language key instead of instructions.

## Changes

- The Aura page now shows a contextual instruction before a breakthrough: fight hostile foes with recovered swings, leave exhausted practice for real combat, hold an eligible weapon, or contact the server owner if Aura is disabled.
- Hovering the experience area explains eligible attacks, the shared lifetime practice limit and the requirement to finish a trial after reaching the threshold.
- Crossing the practice cap sends one chat explanation. It does not repeat on every exhausted practice hit.
- The mastery trial has a localized description that states knockout, own master and the stage restriction.
- Stillness instructions explicitly say to crouch still with a blade for 30 seconds. The player guide includes the full rank table, training terrain and optional mentorship path.
- Existing experience thresholds and trial requirements remain the same.

## Verification

`AuraProgressionTest` passed in a native Minecraft client in 83 seconds on October 3, 2026. The final rerun, including translation coverage, passed in **64 seconds**. It verifies learning Glow through the normal teaching path, reaching the practice cap, an actual recovered Survival sword attack against a husk continuing to earn experience, the experience threshold waiting for a trial, a wrong trial refusing advancement, an actual 30-second waterfall crouching trial advancing to Flow, and saved-world restart preserving both rank and the practice cap. It then checks continued real experience and later-stage gates through the production completion API.

Two final native screenshots are preserved under `artifacts/review/aura-progression/`. Initial visual review identified the missing master translation. The final rerun passes an assertion that every registered trial from Flow through Sovereign has a loaded language entry; visual inspection confirms the master's trial now renders readable instructions. Logs are `native.log` and `native-final.log` in the same folder.

The later-stage API checks verify eligibility and advancement; they do not simulate defeating bosses or duelists. This review cannot establish the cause of a particular player's report without their stage, experience and server configuration. Aura disabled by configuration, a zero experience multiplier, repeatedly using practice after its lifetime limit, or remaining at a full experience threshold are separate conditions with different next steps.
