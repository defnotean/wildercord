# Nursery interaction diagnostics (2026-10-04)

Hosted Build37205210837 atf5c079f4 failed the second gather after the canopy visit. The first gather succeeded. Four later snapshots had server shift=true, line of sight, an available reserve and elapsed rest, but omitted the actual hand contents and the client crouch value transmitted by the interaction packet. They do not prove a production admission defect.

The fixture now records actual packet-side crouch, held item and selected slot, plus server UseEntity callback and noncancellable mobInteract HEAD/RETURN state. A native-only mixin is enabled only during the finite gather helper. It changes no interaction result, preparation, resource, clock, wait, approach, attempt count or production behavior. Actual callback admissions include visibility, body/hand/world, pose, hidden deadline, reserve and gather deadline.

Focused FungalNurseryTest passed140seconds locally. The original first gather actually succeeded at tick147 and emitted its journal; the second succeeded at tick706 with actual MAIN_HAND air and client/server crouch. Reserve became false and gather deadline3106. Repeat ordinary forage/nursery and full saved-world reopen remained in the same accepted fixture. This local pass does not diagnose the historical hosted miss or establish all-CI-green. The next hosted run retains the missing receipt evidence if it fails.

Original log and focused diagnostic log are preserved under artifacts/drafts/ward-lifecycle-review. Reflection correction is separately accepted in reflect-receipt-authority.md. No new content credit is awarded for these test diagnostics.
