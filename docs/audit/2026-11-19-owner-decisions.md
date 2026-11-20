# Owner decisions — 2026-11-19

One page for the calls the polish loop cannot make on its own. Each item has the context, the
options as I see them, and what I would pick. Answer inline or in chat; implementation follows in
the next rounds with tests and receipts as usual.

## 1. Party leadership when the leader leaves for good — **Implemented (auto-promote)**

`party/Parties.java`, `PartyRules.java`. Today a party keeps its leader slot even if that player
never comes back; nobody can take over without a disband and re-form.

- **Promote automatically:** longest-serving online member becomes leader when the leader has been
  offline past a cutoff (say 3 days).
- **Vote or claim:** `/party claim` takes leadership after the same cutoff.
- **Leave it:** parties are casual; disband-and-reform is fine.

Pick: promote automatically, with the promotion announced in chat. *(Implemented: `PartyRules.LEADER_OFFLINE_TICKS`, `Parties.prune`, `PartyRulesTest`.)*

## 2. Offline members and the guild training bonus — **Implemented (online-only + count shown)**

`guild/Guilds.java`. The bonus scales with members near each other, so an offline member counts
for nothing — which makes big guilds feel smaller than they are.

- **Keep online-only** (honest numbers, current behaviour).
- **Count offline members at a reduced weight** (say half), capped.
- **Count everyone** and accept the grind-away of the incentive to log in together.

Pick: online-only, but show "3 of 9 online" on `/guild info` so the gap is visible. *(Implemented in `Guilds.show`.)*

## 3. Logging out of a duel — **Retained safe immediate forfeit + pre-save restore**

`duel/Duels.java`, `DuelRules.java`. A duelist who logs out mid-duel forfeits immediately in `ServerDisconnects` so their pre-duel health and effects are safely restored before Minecraft writes their player save.

- **Instant loss** on logout (harsh on lag/restarts, exploitable by ping-bombing).
- **Short grace:** 60 s to return, then the logout counts as a loss.
- **No contest:** void the duel, no rating change (invite griefing).

Status: kept immediate `Ending.LOGGED_OFF` forfeit before player save so mid-duel damage/effects never persist across offline NBT saves.

## 4. Dungeon boss reset after a full-party wipe — **Implemented (full reset + cooldown)**

The 0.14 boss rework gave staged deaths and clearer wind-ups; the audit asks whether a boss should
come back at full health when the whole party dies and re-enter later.

- **Full reset** (retry-friendly; bosses are encounters, not attrition walls).
- **Keep damage**, slow regen while unclaimed (rewards chip damage; risks third-party cheese).
- **Full reset after N minutes**, persisted damage before that.

Pick: full reset, with a short re-engagement cooldown so the wipe still costs something. *(Implemented: `BossRules.shouldReset`, `DungeonBoss.resetIfWiped`, `BossRulesTest`.)*

## 5. Masters of Tomorrow leftovers — **Stone Hinge documented as experimental**

From the archived roadmap (now `docs/archive/MASTERS_OF_TOMORROW_ROADMAP.md`):

- **Shared BIND reward** — decide what the parties get when a bind completes together.
- **Vow balance and the `/vow` UI** — the seven odd-circle vows work via commands; a UI is a
  screen of work. Decide: keep commands-only, or schedule the screen.
- **Stone Hinge** — kept behind `aura.experimental_stone_hinge` (default `false`); player guide (`wiki/masters/master-forms.md`, `wiki/whats-new.md`) explicitly notes the flag.
- **Tide / Iron / Dune poses** — articulated poses for the three newer signatures. Art work;
  decide whether it is on the 0.15 list or parked.

## 6. Keybinds — **Implemented (`X` + merged into `masters_arts`)**

Four keybind categories (wildercord, masters_arts, master_forms, lore) and Master Form on **C**
clashes with vanilla's *Save Toolbar Activator* in creative.

- **Rebind Master Form** to e.g. `X` (players know C).
- **Merge the four categories** into one (cleaner options screen, loses grouping).
- **Leave both** as they are.

Pick: rebind to `X` and merge `master_forms` into `masters_arts`; keep `lore` separate. *(Implemented in `MasterFormsClient`, `MastersArtsClient`, lang sources, `README.md`, and guide pages.)*

## 7. Party bounty escorts — **Implemented (companion emerald share)**

`town/BountyEscorts.java`. The escort bounty is strictly solo: the llama is tagged to one UUID,
only that player can start it, and a party member walking the llama home earns nothing (and can
even lose the bounty for the owner if the llama dies on their watch). The Ferryman fight went
cooperative this round; escorts are the other party-shaped activity.

- **Party-share the escort:** any party member may escort, arrival credits the owner (or splits
  coin), and the llama is lost only if no party member is around to save it.
- **Companion credit:** keep ownership solo but pay a small coin share to members within range at
  arrival.
- **Leave it solo:** escorts are a personal ledger; parties have the Ferryman and dungeons.

Pick: companion credit first (small change, no ownership rewrite), party-share later if players
actually run escorts in parties. *(Implemented: `BountyRules.escortCompanionPay`, `BountyEscorts.arrive`, `BountyRulesTest`.)*
