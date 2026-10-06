# Connected cast-receipt acceptance

The action-admission contract requires the exact current `PlayerList` body. Level-only fake players can be passive witnesses but cannot own a held spell. The native consistency fixtures now use actual connected recipients and real `Charging.request`. No fixture inserts a charge, spoofs the player registry or clears production immunity/ward state.

## Preserved cases

The ordinary `SwordMasterTrialTest` retains the published receipt-seam matrix and all 15 connected-recipient `BREAK_CAST` cases. The mandatory `connected-combat-native` job runs the complete 35-case matrix on two genuine JVM/TCP profiles. The exact ordered contract is `src/gametest/resources/cast-receipt-native-contract.json`:

- Both `BREAK_CAST` and `DRIVING_CUT`: `HEALTH`, `FULL_ABSORPTION`, `MANA_SKIN`, `REVERSAL`, `TOTEM`, `GUARD`, `STEP`, `WARD`, `RESISTANCE`, `REJECTED`, `REPLACED`, `EQUAL_TOKEN`, `NEW_CHARGE`, `IDLE`, `WINDUP_REPLACEMENT` (30 cases).
- `SHARED_BREAK_CAST_TO_DRIVING_CUT` and `SHARED_DRIVING_CUT_TO_BREAK_CAST`: retained interruption protection on tick 159 and admission on tick 160.
- `IDLE_SEAL_RECOVERY`: seal present on tick 19, expired on tick 20, with its existing recovery retained.
- `CHARGED_SEAL_RECOVERY`: a damaging hit during seal recovery preserves the protected charge and does not consume shared immunity.
- `NONPLAYER`: genuine damage and the existing lock against an ordinary, unpromoted Husk.

The recipient keeps its real UUID. Separate cases wait actual 160-tick spacing and native ward expiry; repeated Reversal respects `DeathsDoor` recovery. Equipment reconciliation precedes charging. Permanent invulnerability is used only for the rejection case and is restored. The exact pre-impact charge token is checked at each strike. Cross-route probes end the completed held-spell probe and begin a fresh real charge near tick 154 for the 159/160 boundary; normal overchannel remains enabled.

The host executes the combat assertions. Shared/protected-charge and idle-seal outcomes are observed before their deliberate interruption or cleanup; the later exact boundary assertions remain mandatory. A distinct case-passed message follows all remaining checks, and only then does either role advance its completed ledger. After one actual synchronization tick, the peer acknowledges its native client health, absorption, held-charge start and both player identities. The host waits at Fabric's paused test phase for at most 15 wall-clock seconds, so no server ticks silently age the seal or replace the captured health through later regeneration. Peer delivery evidence complements the authoritative server checks; it is not a second implementation of combat rules. The NONPLAYER mob damage/lock check remains server-authoritative; its peer receipt witnesses the connected observer, not remote mob-health validation.

## Mandatory routing and bounds

The two-client entrypoint is supervised-only, never part of the ordinary one-client descriptor. Gradle exports its explicit descriptor. The existing Build workflow runs it as a mandatory job without `continue-on-error`. Missing, duplicated or mismatched cases, identities, nonces, source hashes, process IDs or terminal witnesses fail the job.

Only one launch group runs at a time. Two owned Java processes are capped at 2GiB each; the cast launch has a 900-second ceiling. Export plus native execution has a 24-minute shared ceiling inside a 30-minute job, followed by an always-upload evidence step. Failures retain both logs and partial receipts. The supervisor terminates only children it started. Existing explicit disposable-CI EULA acceptance or an already accepted local file is required.

Runtime sourceHead and checkoutSha both identify the actual verified Git checkout. prHeadSha is separately supplied by explicit workflow metadata, empty for non-PR runs and validated when present. Neither process assumes the PR head equals the checkout; both witnesses bind the distinct fields.

This checkpoint is cast-only. It does not reference absent Moon classes/contracts or count any Moon view as passed. Moon integration remains a separate reviewed change.

## Evidence limits

This source was rebuilt from published `160ff125` after replacement of the execution filesystem. The vanished unpublished `2e729174` payload and its old local verification are not current evidence. Fresh compilation, independent review, actual Loom export and actual two-client native execution must be recorded against the rebuilt immutable commit. A source/unit pass alone does not establish native TCP, gameplay or visual success.
